package voodoo.fabric

import io.ktor.client.features.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.list
import mu.KLogging
import voodoo.fabric.meta.FabricInstaller
import voodoo.fabric.meta.FabricIntermediary
import voodoo.fabric.meta.FabricLoader
import voodoo.fabric.meta.FabricLoaderForVersion
import voodoo.util.client
import voodoo.util.json
import java.io.IOException

object FabricUtil : KLogging() {

    // https://meta.fabricmc.net/v2/versions/loader
    suspend fun getLoaders(): List<FabricLoader> = withContext(Dispatchers.IO) {
        val url = "https://meta.fabricmc.net/v2/versions/loader"

        val response = try {
            client.get<HttpResponse>(url) {
//                header("User-Agent", Downloader.useragent)
            }
        } catch(e: IOException) {
            logger.error("getLoaders")
            logger.error("url: $url")
            throw e
        } catch(e: TimeoutCancellationException) {
            logger.error("getLoaders")
            logger.error("url: $url")
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error("getLoaders")
            logger.error("url: $url")
            logger.error("response: $response")
            error("failed receiving")
        }
        return@withContext json.parse(FabricLoader.serializer().list, response.readText())
    }

    // https://meta.fabricmc.net/v2/versions/loader/1.15
    suspend fun getLoadersForGameversion(version: String): List<FabricLoaderForVersion> = withContext(Dispatchers.IO) {
        val url = "https://meta.fabricmc.net/v2/versions/loader/$version"

        val response = try {
            client.get<HttpResponse>(url) {
//                header("User-Agent", Downloader.useragent)
            }
        } catch(e: IOException) {
            logger.error("getLoadersForGameversion")
            logger.error("url: $url")
            throw e
        } catch(e: TimeoutCancellationException) {
            logger.error("getLoadersForGameversion")
            logger.error("url: $url")
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error("getLoadersForGameversion")
            logger.error("url: $url")
            logger.error("response: $response")
            error("failed receiving")
        }
        return@withContext json.parse(FabricLoaderForVersion.serializer().list, response.readText())
    }

    // https://meta.fabricmc.net/v2/versions/intermediary
    suspend fun getIntermediaries() : List<FabricIntermediary> = withContext(Dispatchers.IO) {
        val url = "https://meta.fabricmc.net/v2/versions/intermediary"
        val response = try {
            client.get<HttpResponse>(url) {
//                header("User-Agent", Downloader.useragent)
            }
        } catch(e: IOException) {
            logger.error("getIntermediaries")
            logger.error("url: $url")
            throw e
        } catch(e: TimeoutCancellationException) {
            logger.error("getIntermediaries")
            logger.error("url: $url")
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error("getIntermediaries")
            logger.error("url: $url")
            logger.error("response: $response")
            error("failed receiving")
        }
        return@withContext json.parse(FabricIntermediary.serializer().list, response.readText())
    }

    // https://meta.fabricmc.net/v2/versions/installer
    suspend fun getInstallers() : List<FabricInstaller> = withContext(Dispatchers.IO) {
        val url = "https://meta.fabricmc.net/v2/versions/installer"
        val response = try {
            client.get<HttpResponse>(url) {
//                header("User-Agent", Downloader.useragent)
            }
        } catch(e: IOException) {
            logger.error("getInstallers")
            logger.error("url: $url")
            throw e
        } catch(e: TimeoutCancellationException) {
            logger.error("getInstallers")
            logger.error("url: $url")
            throw e
        }
        if(!response.status.isSuccess()) {
            logger.error("getInstallers")
            logger.error("url: $url")
            logger.error("response: $response")
            error("failed receiving")
        }
        return@withContext json.parse(FabricInstaller.serializer().list, response.readText())
    }
}