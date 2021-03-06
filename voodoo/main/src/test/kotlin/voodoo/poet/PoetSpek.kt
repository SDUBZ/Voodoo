package voodoo.poet

import list
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import voodoo.data.Side
import voodoo.data.nested.NestedEntry
import voodoo.script.MainScriptEnv
import java.io.File

object PoetSpek : Spek({
    describe("create nested pack") {
        val rootFolder by memoized {
            File("run").resolve("test").resolve("poet").absoluteFile.apply {
                deleteRecursively()
                mkdirs()
            }
        }
        val scriptEnv by memoized {
            MainScriptEnv(rootFolder = rootFolder, id = "new-pack").apply {
                mcVersion = "1.12.2"
                authors = listOf("blarb something", "nikky")
                root<NestedEntry.Curse> {
                    validMcVersions = setOf("1.12.1", "1.12")
                    it.list {
                        +(Mod.wearableBackpacks)
                        +(Mod.neat)

                        group {
                            side = Side.SERVER
                        }.list {
                            +(Mod.btfuContinuousRsyncIncrementalBackup)
                        }
                    }
                }
            }
        }

        val nestedPack by memoized {
            scriptEnv.pack
        }

        it("generate kotlin source") {
            PoetPack.createModpack(
                rootFolder,
                nestedPack
            )
        }

        // TODO: compile generated file
    }
})