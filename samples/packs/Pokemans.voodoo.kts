//@file:GenerateMods("Mod", "1.12")
//@file:GenerateMods("Mod", "1.12.1")
//@file:GenerateMods("Mod", "1.12.2")
//@file:GenerateForge("Forge_12_2", "1.12.2")

import com.skcraft.launcher.model.modpack.Recommendation

mcVersion = "1.12.2"
title = "Pokemans Reloaded"
version = "1.1"
icon = rootFolder.resolve("icon.png")
authors = listOf("capitalthree", "NikkyAi")
modloader {
    forge(Forge_12_2.mc1_12_2_recommended)
}

//pack {
//    multimc {
//        skPackUrl = "insert/something/here"
//    }
//}

root<Curse> { builder ->
    releaseTypes = setOf(FileType.Release, FileType.Beta)
    skipFingerprintCheck = false
    builder.list {
        //TODO: group mods by category (eg. tweakers)
        +Mod.abyssalcraft
        +Mod.advancedRocketry {
            releaseTypes = setOf(FileType.Release, FileType.Beta, FileType.Alpha)
        }
        +Mod.apricornTreeFarm
        +Mod.armorplus
        +Mod.betterfps
        +Mod.chiselsBits
        +Mod.crafttweaker
        +Mod.customNpcs
        +Mod.enderIo
        +Mod.extraBitManipulation
        +Mod.farseek
        +Mod.foamfixOptimizationMod
        +Mod.immersiveEngineering
        +Mod.industrialCraft
        +Mod.ivtoolkit
        +Mod.jei
        +Mod.lingeringLoot
        +Mod.minecolonies
        +Mod.modtweaker
        +Mod.multiMine
        +Mod.openmodularturrets
        +Mod.pamsHarvestcraft
        +Mod.quark
        +Mod.railcraft
        +Mod.recurrentComplex
        +Mod.repose
        +Mod.roguelikeDungeons
        +Mod.streams
        +Mod.structuredCrafting
        +Mod.tails
        +Mod.tinkersConstruct
        +Mod.timberjack
        +Mod.wearableBackpacks

//        withType(Direct::class)
//            .list {
//                //TODO: change to local
//                +"pixelmonDark" configure {
//                    url =
//                        "https://meowface.org/craft/repo/objects/db/5d/db5db11bcda204362d62705b1d5f4e5783f95c2c"
//                    fileName = "PixelmonDark2.4.jar"
//                }
//                //TODO: change to local
//                +"gameShark" configure {
//                    url =
//                        "https://meowface.org/craft/repo/objects/b9/21/b9216143fd5214c31e109b24fb1513eb8b23bc77"
//                    fileName = "Gameshark-1.10.2-5.0.0.jar"
//                }
////                            add("gameShark") url "https://pixelmonmod.com/mirror/sidemods/gameshark/5.2.0/gameshark-1.12.2-5.2.0-universal.jar"
////                    }
//            }

        group {
            side = Side.CLIENT
        }.list {
            group {
                optional {
                    selected = true
                    skRecommendation = Recommendation.starred
                }
            }.list {
                +Mod.xaerosMinimap {
                    description = "lightweight minimap"
                }
            }
            group {
                optional {
                    selected = false
                }
            }.list {
                //TODO: add Optifine ?
            }
        }

//        withTypeClass(Jenkins::class) {
//        withType<Jenkins> {// this works even though idea is protesting
//            jenkinsUrl = "https://ci.elytradev.com"
//        }.list {
//            +"probe-data-provider" job "elytra/ProbeDataProvider/1.10.2"
//            +"fruit-phone" job "elytra/FruitPhone/1.10.2"
//
//            group {
//                side = Side.SERVER
//            }.list {
//            }
//        }
    }
}
