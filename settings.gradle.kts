pluginManagement {
    repositories {
        maven(url = "https://dl.bintray.com/kotlin/kotlin-eap") {
            name = "Kotlin EAP"
        }
        maven(url = "https://kotlin.bintray.com/kotlinx") {
            name = "kotlinx"
        }
        maven(url = "http://maven.modmuss50.me") {
            name = "modmuss50"
        }
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }
}

plugins {
    id("com.gradle.enterprise").version("3.1.1")
}

rootProject.name = "voodoo-parent"

include("voodoo", "voodoo:main")
include("core")
include("dsl")
include("format", "format:packager")
include("multimc", "multimc:installer")
include("util", "util:download", "util:maven", "util:jenkins")
include("tome", "pack", "pack:tester")
include("server-installer")
include("bootstrap", "bootstrap:multimc-installer", "bootstrap:voodoo")

//TODO: remove
include("skcraft")

include("plugin")

fun prefixProject(project: ProjectDescriptor, prefix: String) {
    project.name = prefix + "-" + project.name
//    println("path of ${project.name} is ${project.path}")
    project.children.forEach { child ->
        prefixProject(child, project.name)
    }
}

// TODO move into publishing
rootProject.children.forEach { child ->
    child.children.forEach { grandchild ->
        prefixProject(grandchild, child.name)
    }
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(true)
    }
}