package voodoo

import kotlinx.coroutines.runBlocking
import org.gradle.api.internal.AbstractTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import voodoo.plugin.PluginConstants
import voodoo.util.jenkins.downloadVoodoo
import java.io.File

open class DownloadVoodooTask : AbstractTask() {

    @OutputDirectory
    val outputFolder = project.buildDir.resolve("voodoo")
    @OutputFile
    val lastFile = outputFolder.resolve("last.txt")
    @OutputFile
    val jarFile: File = outputFolder.resolve("voodoo-${PluginConstants.JENKINS_BUILD_NUMBER}.jar")
    init {
        group = "voodoo"

        outputs.upToDateWhen {
//            if(PluginConstants.JENKINS_BUILD_NUMBER < 0)
//                return@upToDateWhen false

            if(jarFile.exists() && lastFile.exists()) {
                val lastBuild = lastFile.readText().toIntOrNull() ?: run {
                    return@upToDateWhen false
                }
                lastBuild == PluginConstants.JENKINS_BUILD_NUMBER
            } else {
                false
            }
        }
    }

    @TaskAction
    fun exec() {
        logger.lifecycle("download voodoo")

        runBlocking {
            downloadVoodoo(
                component = "voodoo",
                binariesDir = outputFolder,
                outputFile = jarFile,
                bootstrap = false,
                buildNumber = PluginConstants.JENKINS_BUILD_NUMBER
            )
        }
        lastFile.writeText("${PluginConstants.JENKINS_BUILD_NUMBER}")
    }
}