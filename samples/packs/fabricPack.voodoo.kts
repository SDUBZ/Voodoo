mcVersion = "1.15.2"
version = "0.0.1"
icon = rootFolder.resolve("icon.png")
authors = listOf("NikkyAi")

modloader {
    fabric(
        intermediary = Fabric.intermediary.v_1_15_2
    )
}


pack {
    multimc {
        // path t the published modpack definition (not sk always)
        skPackUrl = "https://nikky.moe/.mc/experimental/fabricpack.json"
        selfupdateUrl = ""
    }
    voodoo {

    }
}

root<Curse> {
    releaseTypes = setOf(FileType.Release, FileType.Beta)
    it.list {
        +FabricMod.fabricApi

        +FabricMod.betternether

        +FabricMod.tabInventoryFabric

        +FabricMod.roughlyEnoughItems {
//            version = "abc"
        }
        +FabricMod.roughlyEnoughResources
        group {
            side = Side.CLIENT
        }.list {
            +FabricMod.roughlyEnoughItems
            +FabricMod.roughlyEnoughResources
            +FabricMod.appleskin
            +FabricMod.modmenu
        }

        +FabricMod.mouseWheelie
    }
}
