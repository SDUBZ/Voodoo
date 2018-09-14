package voodoo

import com.xenomachina.argparser.*
import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.builder.resolve
import voodoo.data.flat.ModPack
import voodoo.provider.Provider
import voodoo.util.ExceptionHelper
import voodoo.util.json
import java.io.File
import java.io.StringWriter
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object BuilderForDSL : KLogging() {
    fun build(
        modpack: ModPack,
        targetFolder: File,
        name: String,
        targetFileName: String = "$name.entry.lock.hjson",
        targetFile: File = targetFolder.resolve(targetFileName),
        vararg args: String
    ) = mainBody {
        val parser = ArgParser(args)
        val arguments = ArgumentsForDSL(parser)
        parser.force()

        runBlocking {
            arguments.run {
                targetFolder.walkTopDown().asSequence()
                    .filter {
                        it.isFile && it.name.endsWith(".lock.hjson")
                    }
                    .forEach {
                        it.delete()
                    }

                modpack.entrySet.forEach { entry ->
                    logger.info("id: ${entry.id} entry: $entry")
                }

                try {
                    modpack.resolve(
                        this@runBlocking,
                        targetFolder,
                        updateAll = updateAll,
                        updateDependencies = updateDependencies,
                        updateEntries = entries
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    coroutineContext.cancel(e)
                    exitProcess(1)
                }

                modpack.lockEntrySet.forEach { lockEntry ->
                    val provider = Provider.valueOf(lockEntry.provider).base
                    if (!provider.validate(lockEntry)) {
                        logger.error { lockEntry }
                        throw IllegalStateException("entry did not validate")
                    }
                }

                logger.info("Creating locked pack...")
                val lockedPack = modpack.lock()
                lockedPack.rootFolder = targetFolder
                lockedPack.entrySet.clear()
                lockedPack.entrySet += modpack.lockEntrySet

                lockedPack.writeLockEntries()

                logger.info("Writing lock file... $targetFile")
                targetFile.writeText(JSON.unquoted.stringify(lockedPack))

                // generate modlist

                logger.info("writing modlist")
                val sw = StringWriter()
                sw.append(lockedPack.report)
                sw.append("\n")

                modpack.lockEntrySet.sortedBy { it.name().toLowerCase() }.forEach { entry ->
                    val provider = Provider.valueOf(entry.provider).base
                    sw.append("\n\n")
                    sw.append(provider.report(entry))
                }

                val modlist = (targetFile ?: File(".")).absoluteFile.parentFile.resolve("modlist.md")
                modlist.writeText(sw.toString())
            }
        }
    }

    fun build(
        packFile: File,
        targetFolder: File,
        name: String,
        targetFileName: String = "$name.entry.lock.hjson",
        targetFile: File = targetFolder.resolve(targetFileName),
        vararg args: String
    ) {
        val modpack: ModPack = JSON.unquoted.parse(packFile.readText())
        modpack.loadEntries(targetFolder)
        return build(modpack, targetFolder, name, targetFileName, targetFile, args = *args)
    }

}

private class ArgumentsForDSL(parser: ArgParser) {
    val updateDependencies by parser.flagging(
        "--updateDependencies", "-d",
        help = "update all dependencies"
    )
        .default(false)

    val updateAll by parser.flagging(
        "--updateAll", "-u",
        help = "update all entries, implies updating dependencies"
    )
        .default(false)

    val entries by parser.adding(
        "-E", help = "entries to update"
    )
}
