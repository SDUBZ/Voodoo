import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonLiteral
import kotlinx.serialization.json.JsonTransformingSerializer

// TODO: is this all we need ?
// TODO: maybe convert to merged data structure? less typesafety vs control over type variable
@Serializable
sealed class Modloader {
    @Serializable
    @SerialName("modloader.Forge")
    data class Forge(
        // TODO: maybe just second part of `1.15.2-31.1.35` ? is this clear enough for the server to download the installer ?
        // https://files.minecraftforge.net/maven/net/minecraftforge/forge/maven-metadata.xml
        val mcVersion: String,
        val forgeVersion: String,
        val branch: String? = null
    ) : Modloader() {
        companion object {
            fun parse(version: String): Forge {
                val components = version.split('-')
                return Forge(
                    mcVersion = components.getOrNull(0) ?: error("no mcVersion in $version"),
                    forgeVersion = components.getOrNull(1) ?: error("no forgeVersion in $version"),
                    branch = components.getOrNull(2)
                )
            }
        }

//        val shortVersion: ShortVersion
//            get() = ShortVersion(forgeVersion.run {
//                branch?.let { "$forgeVersion-$it" } ?: forgeVersion
//            })
//
//        inline class ShortVersion(val version: String) {
//            val components: List<String>
//                get() = version.split('-')
//            val forgeVersion: String
//                get() = components[0]
//            val branch: String?
//                get() = components.getOrNull(1)
//        }
    }

    // look up versions from https://meta.fabricmc.net/
    @Serializable
    @SerialName("modloader.Fabric")
    data class Fabric(
        // https://meta.fabricmc.net/v2/versions/loader
        val loader: String,
        // https://meta.fabricmc.net/v2/versions/intermediary
        val intermediateMappings: String,
        // https://meta.fabricmc.net/v2/versions/installer
        val installer: String
    ) : Modloader()

    @Serializable
    object None : Modloader() // not sure if we want to keep this
}

// TODO: remove ugly woraround, use https://github.com/Kotlin/kotlinx.serialization/pull/772 once available
object ModloaderSerializer : JsonTransformingSerializer<Modloader>(Modloader.serializer(), "type_transform") {
    override fun readTransform(element: JsonElement): JsonElement {
        val newType = when(val type = element.jsonObject.getPrimitive("type").content) {
            "Modloader.Fabric" -> "modloader.Fabric"
            "Modloader.Forge" -> "modloader.Forge"
            else -> type
        }
        val mutableEntries = element.jsonObject.toMutableMap()
        mutableEntries["type"] = JsonLiteral(newType)
        return element.jsonObject.copy(mutableEntries).also { println(it) }
    }
}
