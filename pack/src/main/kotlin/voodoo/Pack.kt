package voodoo

import blue.endless.jankson.Jankson
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import voodoo.builder.resolve
import voodoo.data.UserFiles
import voodoo.data.flat.Entry
import voodoo.data.flat.EntryFeature
import voodoo.data.flat.ModPack
import voodoo.data.lock.LockEntry
import voodoo.data.lock.LockPack
import voodoo.data.sk.FeatureFiles
import voodoo.data.sk.FeatureProperties
import voodoo.data.sk.Launch
import voodoo.data.sk.SKFeature
import voodoo.pack.*
import java.io.File
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

object Pack : KLogging() {
    @JvmStatic
    fun main(vararg args: String) = mainBody {

        val jankson = Jankson.builder()
                .registerTypeAdapter(ModPack.Companion::fromJson)
                .registerTypeAdapter(Entry.Companion::fromJson)
                .registerTypeAdapter(LockPack.Companion::fromJson)
                .registerTypeAdapter(LockEntry.Companion::fromJson)
                .registerTypeAdapter(EntryFeature.Companion::fromJson)
                .registerTypeAdapter(UserFiles.Companion::fromJson)
                .registerTypeAdapter(Launch.Companion::fromJson)
                .registerTypeAdapter(SKFeature.Companion::fromJson)
                .registerTypeAdapter(FeatureProperties.Companion::fromJson)
                .registerTypeAdapter(FeatureFiles.Companion::fromJson)
                .registerSerializer(ModPack.Companion::toJson)
                .registerSerializer(Entry.Companion::toJson)
                .registerSerializer(LockPack.Companion::toJson)
                .registerSerializer(LockEntry.Companion::toJson)
//            .registerSerializer(EntryFeature.Companion::toJson)
                .build()

        val arguments = Arguments(ArgParser(args))

        arguments.run {

            logger.info("loading $modpackLockFile")
            val jsonObject = jankson.load(modpackLockFile)
            val modpack: LockPack = jankson.fromJson(jsonObject)
            val rootFolder = modpackLockFile.absoluteFile.parentFile

            modpack.loadEntries(rootFolder, jankson)

            val packer = when (methode) {
                "sk" -> SKPack
                "mmc" -> MMCPack
                "mmc-static" -> MMCStaticPack
                "server" -> ServerPack
                "curse" -> CursePack

                else -> {
                    logger.error("no such packing methode: $methode")
                    exitProcess(-1)
                }
            }

            runBlocking {
                packer.download(rootFolder = rootFolder, modpack = modpack, target = targetArg, clean = true, jankson = jankson)
            }
        }
    }

    private class Arguments(parser: ArgParser) {
        val methode by parser.positional("METHODE",
                help = "format to package into") { this.toLowerCase()}
                .default("")

        val modpackLockFile by parser.positional("FILE",
                help = "input pack .lock.json") { File(this) }

        val targetArg by parser.storing("--output", "-o",
                help = "output folder")
                .default<String?>(null)

//        val clean by parser.flagging("--clean", "-c",
//                help = "clean output folder before packaging")
//                .default(true)
    }
}