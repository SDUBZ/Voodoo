package voodoo

import kotlinx.coroutines.runBlocking
import list
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import voodoo.curse.CurseClient
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.data.nested.NestedPack
import voodoo.dsl.builder.ModpackBuilder
import voodoo.poet.Poet
import voodoo.poet.PoetPack
import java.io.File

open class CreatePackTask : DefaultTask() {
    @InputDirectory lateinit var rootDir: File
    @InputDirectory lateinit var packsDir: File

    @Input
    @Option(option = "id", description = "modpack id")
    var id: String = "rename_me"

    @Input
    @Option(option = "title", description = "modpack title (optional)")
    var titleStr: String = ""

    @Input
    @Option(option = "mcVersion", description = "minecraft version")
    var mcVersion: String = "1.12.2"

    init {
        group = "voodoo"
    }

    @TaskAction
    fun create() {
        if (id == null)
            throw GradleException("id needs to be specified with --id")
        if (mcVersion == null)
            throw GradleException("mcVersion needs to be specified with --mcVersion")

        val modIdentifiers = runBlocking {
            Poet.requestSlugIdMap(section = "Mods", gameVersions = listOf(mcVersion), categories = null)
        }.mapKeys { (key, _) ->
            Poet.defaultSlugSanitizer(key)
        }.toList().shuffled().take(10)

        val randomMods = runBlocking {
            modIdentifiers.filter { (_, projectId) ->
                val files = CurseClient.getAllFilesForAddon(projectId)
                files.any { file -> file.gameVersion.contains(mcVersion) }
            }
        }
//        val forgeData = runBlocking {
//            ForgeUtil.deferredPromo.await()
//        }

        val scriptEnv = ModpackBuilder(
            NestedPack.create(
                rootFolder = rootDir,
                id = id ?: throw GradleException("id was null")
            )
        ).apply {
            mcVersion = this@CreatePackTask.mcVersion ?: throw GradleException("mcVersion was null")
            title = titleStr.takeIf { it.isNotBlank() } ?: id.capitalize()
            authors = listOf(System.getProperty("user.name"))
            // TODO: also support fabric in newly created packs
            modloader {
                forge("${this@apply.mcVersion}-recommended")
            }
            root<NestedEntry.Curse> { builder ->
                builder.list {
                    randomMods.forEach { (identifier, projectId) ->
                        +ProjectID(projectId.value) configure {
                            //                            projectID = projectId
                        }
                    }
                }
            }
        }

        val nestedPack = scriptEnv.pack

        PoetPack.createModpack(
            folder = packsDir,
            nestedPack = nestedPack
        )
    }
}
