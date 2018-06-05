package voodoo.curse

import aballano.kotlinmemoization.memoize
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import mu.KLogging
import org.apache.commons.compress.compressors.CompressorStreamFactory
import voodoo.core.VERSION
import voodoo.curse.data.Addon
import voodoo.curse.data.AddonFile
import voodoo.curse.data.feed.CurseFeed
import voodoo.data.flat.Entry
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader

/**
 * Created by nikky on 30/01/18.
 * @author Nikky
 * @version 1.0
 */
object CurseClient : KLogging() {
    val PROXY_URL = "https://curse.nikky.moe/api"
    val FEED_URL = "http://clientupdate-v6.cursecdn.com/feed/addons/432/v10"
    val useragent = "voodoo/$VERSION (https://github.com/elytra/Voodoo)"

    val mapper = jacksonObjectMapper() // Enable Json parsing
            .registerModule(KotlinModule()) // Enable Kotlin support
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
//            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS, true)

    private var idMap = getIdMap()
    
    data class GraphQLRequest(
            val query: String,
            val operationName: String,
            val variables: Map<String, Any> = emptyMap()
    )

    data class IdNamePair(
            val id: Int,
            val name: String
    )

    data class WrapperAddonResult(
            val addons: List<IdNamePair>
    )

    data class GraphQlResult(
            val data: WrapperAddonResult
    )

    private fun getIdMap(): Map<String, Int> {
        val url = "https://curse.nikky.moe/graphql"

        logger.debug("post $url")
        val graphQlRequest = GraphQLRequest(
                query = """
                    |{
                    |  addons {
                    |    id
                    |    name
                    |  }
                    |}
                """.trimMargin(),
                operationName = "GetNameIDPairs"
        )
        val (_, _, result) = url.httpPost()
                .body(mapper.writeValueAsBytes(graphQlRequest))
                .header("User-Agent" to useragent, "Content-Type" to "application/json")
                .responseString()
        val grapqhQlResult: GraphQlResult = when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                throw Exception("failed getting id-name pairs")
            }
        }
        return grapqhQlResult.data.addons.groupBy(
                { it.name },
                { it.id }).mapValues {
            it.value.first()
        }
    }

    private fun getAddonFileCall(addonId: Int, fileId: Int, metaUrl: String = PROXY_URL): AddonFile? {
        val url = "$PROXY_URL/addon/$addonId/file/$fileId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> null
        }
    }

    val getAddonFile = ::getAddonFileCall.memoize()

    private fun getAllFilesForAddonCall(addonId: Int, metaUrl: String = PROXY_URL): List<AddonFile> {
        val url = "$PROXY_URL/addon/$addonId/files"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> throw Exception("failed getting cursemeta data")
        }
    }

    val getAllFilesForAddon = ::getAllFilesForAddonCall.memoize()

    private fun getAddonCall(addonId: Int, metaUrl: String = PROXY_URL): Addon? {
        val url = "$PROXY_URL/addon/$addonId"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            is Result.Failure -> {
                logger.error(result.error.toString())
                null
            }
        }
    }

    val getAddon = ::getAddonCall.memoize()

    fun getFileChangelogCall(addonId: Int, fileId: Int, proxyUrl: String = PROXY_URL): String {
        val url = "$PROXY_URL/addon/$addonId/file/$fileId/changelog"

        logger.debug("get $url")
        val (_, _, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .responseString()
        return when (result) {
            is Result.Success -> {
                mapper.readValue(result.value)
            }
            else -> throw Exception("failed getting cursemeta data")
        }
    }

    val getFileChangelog = ::getFileChangelogCall.memoize()

    fun getAddonByName(name: String, metaUrl: String = PROXY_URL): Addon? {
        val addon = idMap[name]
                ?.let { getAddon(it, metaUrl) }
        if (addon != null) {
            return addon
        }
        idMap = getIdMap()
        return idMap[name]
                ?.let { getAddon(it, metaUrl) }
    }

    fun findFile(entry: Entry, mcVersion: String, proxyUrl: String = PROXY_URL): Triple<Int, Int, String> {
        val mcVersions = listOf(mcVersion) + entry.validMcVersions
        val name = entry.name
        val version = entry.version
        val releaseTypes = entry.curseReleaseTypes
//        if(curseReleaseTypes.isEmpty()) {
//            curseReleaseTypes = setOf(ReleaseType.RELEASE, ReleaseType.BETA) //TODO: is this not already set because i enforce defaults ?
//        }
        var addonId = -1 // entry.lock?.projectID ?: -1
        val fileNameRegex = entry.curseFileNameRegex

        val addon = if (addonId < 0) {
            if (name.isNotBlank())
                getAddonByName(name, proxyUrl)
            else
                null
        } else {
            getAddon(addonId, proxyUrl)
        }

        if (addon == null) {
            logger.error("no addon matching the parameters found for '$entry'")
            System.exit(-1)
            return Triple(-1, -1, "")
        }

        addonId = addon.id

        val re = Regex(fileNameRegex)

        var files = getAllFilesForAddon(addonId, proxyUrl).sortedWith(compareByDescending { it.fileDate })

        var oldFiles = files

        if (version.isNotBlank()) {
            files = files.filter { f ->
                (f.fileName.contains(version, true) || f.fileName == version)
            }
            if (files.isEmpty()) {
                logger.error("filtered files did not match version {}", oldFiles)
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                mcVersions.any { v -> f.gameVersion.contains(v) }
            }

            if (files.isEmpty()) {
                logger.error("filtered files did not match mcVersion {}", oldFiles)
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                releaseTypes.contains(f.releaseType)
            }

            if (files.isEmpty()) {
                logger.error("filtered files did not match releaseType $releaseTypes $oldFiles")
            }
            oldFiles = files
        }

        if (files.isNotEmpty()) {
            files = files.filter { f ->
                re.matches(f.fileName)
            }
            if (files.isEmpty()) {
                logger.error("filtered files did not match regex {}", oldFiles)
            }
        }

        val file = files.sortedWith(compareByDescending { it.fileDate }).firstOrNull()
        if (file == null) {
            val filesUrl = "$PROXY_URL/addon/$addonId/files"
            logger.error("no matching version found for ${addon.name} addon_url: ${addon.webSiteURL} " +
                    "files: $filesUrl mc version: $mcVersions version: $version \n" +
                    "$addon")
            logger.error("no file matching the parameters found for ${addon.name}")
            System.exit(-1)
            return Triple(addonId, -1, "")
        }
        return Triple(addonId, file.id, addon.categorySection.path)
    }

    fun getFeed(hourly: Boolean = false): List<Addon> {
        logger.info("downloading curse feed")
        val type = if(hourly) "hourly" else "complete"
        val url = "$FEED_URL/$type.json.bz2"
        logger.info("get $url")
        val (request, response, result) = url.httpGet()
                .header("User-Agent" to useragent)
                .response()
        when (result) {
            is Result.Success -> {
                val bis = ByteArrayInputStream(result.value)
                val input = CompressorStreamFactory().createCompressorInputStream(bis)
                val buf = BufferedReader(InputStreamReader(input))

                val text = buf.use { it.readText() }

                val feed = mapper.readValue<CurseFeed>(text)

                return feed.data.filter {
                    when (it.categorySection.id) {
                        6 -> true //mod
                        12 -> true //texture packs
                        17 -> false //worlds
                        4471 -> false //modpacks
                        else -> false
                    }
                }
            }
            is Result.Failure -> {
                logger.error("failed getting cursemeta data ${result.error}")
                throw Exception("failed getting cursemeta data, code: ${response.statusCode}")
            }
        }
    }

    fun getAuthors(projectID: Int, metaUrl: String = PROXY_URL): List<String> {
        val addon = getAddon(projectID, metaUrl)!!
        return addon.authors.map { it.name }
    }

    fun getProjectPage(projectID: Int, metaUrl: String): String {
        val addon = getAddon(projectID, metaUrl)!!
        return addon.webSiteURL
    }

}