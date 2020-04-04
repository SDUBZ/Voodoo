plugins {
//    kotlin("scripting") version "1.3.70"
    id("voodoo") version "0.5.0-dev"
}

voodoo {
//    buildLocal = true
//    localVoodooProjectLocation = rootDir.parentFile
    addTask("build") {
        build()
    }
    addTask(name = "pack_experimental") {
        pack().experimental()
    }
    addTask(name = "pack_sk") {
        pack().sklauncher()
    }
    addTask(name = "pack_mmc-sk") {
        pack().multimcSk()
    }
    addTask(name = "pack_mmc-sk-fat") {
        pack().multimcSkFat()
    }
    addTask(name = "pack_mmc-fat") {
        pack().multimcFat()
    }
    addTask(name = "pack_server") {
        pack().server()
    }
    addTask(name = "pack_curse") {
        pack().curse()
    }
    addTask(name = "test_mmc") {
        test().multimc()
    }
    addTask(name = "buildAndPackAll") {
        build()
        pack().sklauncher()
        pack().server()
        pack().multimcSk()
        pack().multimcSkFat()
        pack().multimcFat()
        pack().curse()
    }

    generateCurseforgeMods("Mod", "1.12", "1.12.1", "1.12.2")
    generateCurseforgeMods("FabricMod", "1.15", "1.15.1", "1.15.2", categories = listOf("Fabric"))
    generateCurseforgeTexturepacks("TexturePack", "1.12", "1.12.1", "1.12.2")
    generateForge("Forge_12_2", "1.12.2")
    generateForge("Forge_15_2", "1.15.2")
    generateFabric("Fabric", true)
}

repositories {
    mavenLocal()
//    maven(url = "http://maven.modmuss50.me/") {
//        name = "modmuss50"
//    }
//    maven(url = "https://kotlin.bintray.com/kotlinx") {
//        name = "kotlinx"
//    }
//    mavenCentral()
//    jcenter()
}

dependencies {
//    kotlinScriptDef(group = "moe.nikky.voodoo", name = "voodoo", version = "0.5.0-dev")
//    kotlinScriptDef(group = "moe.nikky.voodoo", name = "dsl", version = "0.5.0-dev")
//    implementation(group = "moe.nikky.voodoo", name = "voodoo", version = "0.5.0-dev")
//    implementation(group = "moe.nikky.voodoo", name = "dsl", version = "0.5.0-dev")
}