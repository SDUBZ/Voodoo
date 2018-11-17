package voodoo.data.nested

import com.skcraft.launcher.model.launcher.LaunchModifier
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.UserFiles
import voodoo.data.flat.ModPack
import java.io.File

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */
data class NestedPack
@Deprecated(
    "use builder function nestedPack(id, mcVersion) {}",
    ReplaceWith("nestedPack(id, mcVersion) {}"),
    level = DeprecationLevel.WARNING
)
constructor(
    val rootDir: File,
    /**
     * unique identifier
     */
    var id: String,
    /**
     * Minecraft Version
     */
    var mcVersion: String,
    /**
     * display name
     */
    var title: String = "",
    var version: String = "1.0",
    var icon: File = rootDir.resolve("icon.png"),
    var authors: List<String> = emptyList(),
    var forge: Int? = null,
    var userFiles: UserFiles = UserFiles(),
    var launch: LaunchModifier = LaunchModifier(),
    var root: NestedEntry = NestedEntry(),
    var localDir: String = "local",
    var sourceDir: String = id,
    var tomeDir: String = id
) {
    companion object : KLogging()

    @Transient
    val sourceFolder: File
        get() = rootDir.resolve(sourceDir)
    @Transient
    val localFolder: File
        get() = rootDir.resolve(localDir)

    fun flatten(): ModPack {
        return ModPack(
            id = id,
            title = title,
            version = version,
            icon = icon,
            authors = authors,
            forge = forge,
            mcVersion = mcVersion,
            userFiles = userFiles,
            launch = launch
        ).also {
            it.rootDir = rootDir

            it.localDir = localDir
            it.sourceDir = sourceDir
            it.tomeDir = tomeDir
        }
    }
}