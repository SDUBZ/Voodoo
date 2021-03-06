package voodoo.poet.importer

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import list
import voodoo.poet.Poet
import voodoo.poet.PoetPack
import voodoo.curse.CurseClient
import voodoo.data.curse.CurseManifest
import voodoo.data.curse.FileType
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import voodoo.util.UnzipUtility.unzip
import voodoo.util.blankOr
import voodoo.util.download
import voodoo.util.withPool
import java.io.File
import java.net.URL
import java.util.UUID

/**
 * Created by nikky on 13/06/18.
 * @author Nikky
 */

object CurseImporter : AbstractImporter() {
    override val label = "Curse Importer"

    suspend fun import(
        modpackId: String,
        sourceUrl: String,
        rootFolder: File,
        packsDir: File
    ) {
//        Thread.currentThread().contextClassLoader = CurseImporter::class.java.classLoader
        val tmpName = modpackId.blankOr ?: UUID.randomUUID().toString()

        val cacheHome = directories.cacheHome.resolve("IMPORT")

        val zipFile = File(sourceUrl).takeIf { it.exists() } ?: run {
            val zipFile = cacheHome.resolve("$tmpName.zip")
            zipFile.deleteRecursively()
            val url = URL(sourceUrl)
            zipFile.download(
                sourceUrl,
                cacheDir = null
//                directories.cacheHome.resolve("DIRECT").resolve(url.host + url.path.substringBeforeLast('/'))
            )
            zipFile
        }
        val extractFolder = cacheHome.resolve(tmpName)
        unzip(zipFile, extractFolder)

        val manifestFile = extractFolder.resolve("manifest.json")
        require(manifestFile.exists()) { "$manifestFile does not exist" }
        logger.info("parsing \n${manifestFile.readText()}")

        val manifest: CurseManifest = Json(JsonConfiguration.Default).parse(CurseManifest.serializer(), manifestFile.readText())

        val validMcVersions = mutableSetOf<String>()

        val source = modpackId
        val local = "local"
        val sourceFolder = rootFolder.resolve(source)
        sourceFolder.deleteRecursively()
        sourceFolder.mkdirs()

        extractFolder.resolve(manifest.overrides).copyRecursively(sourceFolder)

        val curseChannel = Channel<Triple<String, String, ProjectID>>(Channel.UNLIMITED)

        coroutineScope {
            withPool { pool ->
                for (file in manifest.files) {
                    launch(context = pool + CoroutineName("${file.projectID}:${file.fileID}")) {
                        logger.info { file }
                        val addon = CurseClient.getAddon(file.projectID)!!
                        val addonFile = CurseClient.getAddonFile(file.projectID, file.fileID)!!

                        if (addonFile.gameVersion.none { version -> validMcVersions.contains(version) }) {
                            validMcVersions += addonFile.gameVersion
                        }

                        curseChannel.send(
                            Triple(
                                Poet.defaultSlugSanitizer(addon.slug),
                                Regex.escape(addonFile.fileName),
                                addon.id
                            )
                        )
                    }
                    delay(10)
                }
            }

            delay(10)
            logger.info("waiting for jobs to finish")
        }
        logger.info("jobs finished")
        curseChannel.close()

        val curseEntries = mutableListOf<Triple<String, String, ProjectID>>()
        for (curseEntry in curseChannel) {
            curseEntries += curseEntry
        }

//        val forge = manifest.minecraft.modLoaders
//            .find { it.id.startsWith("forge-") }?.id?.substringAfterLast('.')

        val scriptEnv = ModpackBuilder(
            NestedPack.create(
                rootFolder = rootFolder,
                id = modpackId
            )
        ).apply {
            mcVersion = manifest.minecraft.version
            authors = listOf(manifest.author)
            title = manifest.name
            version = manifest.version
            // TODO pick correct forge version number
            manifest.minecraft.modLoaders.forEach { logger.info { it } }
            val forgeVersion = manifest.minecraft.modLoaders.find {
                it.primary && it.id.startsWith("forge-")
            }?.id?.substringAfterLast("forge-")
            if(forgeVersion != null) {
                modloader {
                    forge(forgeVersion)
                }
            }

            localDir = local
            root<NestedEntry.Curse> {
                builder ->
                this.validMcVersions = validMcVersions - manifest.minecraft.version
                releaseTypes = sortedSetOf(FileType.Release, FileType.Beta, FileType.Alpha)
                builder.list {
                    curseEntries.forEach { (identifier, versionStr, curseProjectID) ->
                        +ProjectID(curseProjectID.value) configure {
                            version = versionStr
                        }
                    }
                    val modsFolder = sourceFolder.resolve("mods")
                    if (modsFolder.exists()) {
                        withType<NestedEntry.Local>().list {
                            val localFolder = rootFolder.resolve(local)
                            this@CurseImporter.logger.info("listing $modsFolder")
                            modsFolder.listFiles { file ->
                                this@CurseImporter.logger.debug("testing $file")
                                when {
                                    file == null -> false
                                    !file.isFile -> false
                                    file.name.endsWith(".entry.json") -> false
                                    file.name.endsWith(".lock.json") -> false
                                    else -> true
                                }
                            }!!.forEach { file ->
                                if (!file.isFile) return@forEach
                                val relative = file.relativeTo(modsFolder)
                                val targetFile = localFolder.resolve(relative)
                                targetFile.parentFile.mkdirs()
                                file.copyTo(targetFile, overwrite = true)
                                this@CurseImporter.logger.info("adding local entry for ${relative.path}")

                                +file.nameWithoutExtension configure {
                                    fileSrc = relative.path
                                    folder = file.parentFile.relativeTo(sourceFolder).path
                                }
                                file.delete()
                            }
                        }
                    }
                }
            }
        }

        PoetPack.createModpack(
            folder = packsDir,
            nestedPack = scriptEnv.pack
        )

//        val modpack = Importer.flatten(nestedPack)
//        val lockPack = Builder.build(modpack, name = modpackId, args = *arrayOf("build"))
//        Tome.generate(modpack, lockPack, mainEnv.tomeEnv)
//        logger.info("finished")

        extractFolder.deleteRecursively()
        zipFile.delete()
    }

    // TODO: options filename, src-folder/overrides,
}