package voodoo.data.nested

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.ModloaderPattern
import voodoo.data.PackOptions
import voodoo.data.components.CommonComponent
import voodoo.data.flat.ModPack
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
@Serializable
data class NestedPack(
    val `$schema`: String = "./schema/nested.schema.json",
//    @Serializable(with=FileSerializer::class)
    val rootFolderPath: String,
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String? = null,
    /**
     * display name
     */
    var title: String? = null,
    var version: String = "1.0",
    var iconPath: String = "icon.png",
    var authors: List<String> = emptyList(),
    @Deprecated("use modloader field instead")
    var forge: String? = null, // TODO: replace with generic modloader info
    var modloader: ModloaderPattern = ModloaderPattern.None,
    var localDir: String = "local",
    var docDir: String? = null,
    var packOptions: PackOptions = PackOptions(),
    var root: NestedEntry = NestedEntry.Common(
        nodeName = "root"
    )
) {
    val rootFolder: File by lazy {
        File(rootFolderPath)
    }
    var icon: File
        get()= rootFolder.resolve(iconPath)
        set(value) {
            iconPath = if(value.isAbsolute) {
                value.relativeTo(rootFolder).path
            } else {
                value.path
            }
        }

    companion object : KLogging() {
        fun create(rootFolder: File, id: String, builder: (NestedPack) -> Unit = {}): NestedPack {
            val pack = NestedPack(
                rootFolderPath = rootFolder.path,
                id = id
            )
            builder(pack)
            return pack
        }
    }

    init {
        if (!rootFolder.isAbsolute) {
            throw IllegalStateException("rootFolder: '$rootFolder' is not absolute")
        }
    }

    @Transient
    val sourceFolder: File
        get() = rootFolder.resolve(id)
    @Transient
    val localFolder: File
        get() = rootFolder.resolve(localDir)

    // TODO: possibly this flattening step will not be necessary
    suspend fun flatten(): ModPack {
        return ModPack(
            rootFolder = rootFolder,
            id = id,
            mcVersion = mcVersion ?: throw IllegalStateException("mcVersion must be set for pack '$id'"),
            title = title,
            version = version,
            icon = icon,
            authors = authors,
            modloader = modloader,
            localDir = localDir,
            docDir = docDir ?: id,
            packOptions = packOptions
        ).also {
            it.entrySet += root.flatten()
        }
    }
}