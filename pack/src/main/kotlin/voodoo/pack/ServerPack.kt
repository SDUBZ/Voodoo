package voodoo.pack

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.serialization.json.JSON
import voodoo.data.lock.LockPack
import voodoo.util.jenkins.DownloadVoodoo
import java.io.File

/**
 * Created by nikky on 06/05/18.
 * @author Nikky
 */

object ServerPack : AbstractPack() {
    override val label = "Server SKPack"

    override suspend fun download(
        coroutineScope: CoroutineScope,
        modpack: LockPack,
        target: String?,
        clean: Boolean
    ) {
        val serverDir = File(target ?: "server_${modpack.id}")

        if (clean) {
            logger.info("cleaning server directory $serverDir")
            serverDir.deleteRecursively()
        }

        serverDir.mkdirs()

        val localDir = modpack.localFolder
        logger.info("local: $localDir")
        if (localDir.exists()) {
            val targetLocalDir = serverDir.resolve("local")
            modpack.localDir = targetLocalDir.name

            if (targetLocalDir.exists()) targetLocalDir.deleteRecursively()
            targetLocalDir.mkdirs()

            localDir.copyRecursively(targetLocalDir, true)
        }

        val sourceDir = modpack.sourceFolder //rootFolder.resolve(modpack.rootFolder).resolve(modpack.sourceDir)
        logger.info("mcDir: $sourceDir")
        if (sourceDir.exists()) {
            val targetSourceDir = serverDir.resolve("src")
            modpack.sourceDir = targetSourceDir.name

            if (targetSourceDir.exists()) targetSourceDir.deleteRecursively()
            targetSourceDir.mkdirs()

            sourceDir.copyRecursively(targetSourceDir, true)
            targetSourceDir.walkBottomUp().forEach { file ->
                if (file.name.endsWith(".entry.hjson"))
                    file.delete()
                if (file.isDirectory && file.listFiles().isEmpty()) {
                    file.delete()
                }
                when {
                    file.name == "_CLIENT" -> file.deleteRecursively()
                    file.name == "_SERVER" -> {
                        file.copyRecursively(file.absoluteFile.parentFile, overwrite = true)
                        file.deleteRecursively()
                    }
                }
            }
        }

        val packFile = serverDir.resolve("pack.lock.json")
        packFile.writeText(JSON.unquoted.stringify(modpack))


        logger.info("packaging installer jar")
        val installer = DownloadVoodoo.downloadVoodoo(component = "server-installer", bootstrap = false, binariesDir = directories.cacheHome)

        val serverInstaller = serverDir.resolve("server-installer.jar")
        installer.copyTo(serverInstaller)

        logger.info("server package ready: ${serverDir.absolutePath}")
    }
}