package voodoo

import mu.KLogging
import voodoo.ShellUtils.requireInPath
import voodoo.voodoo.VoodooConstants
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.function.Consumer
import kotlin.system.exitProcess

object GradleSetup : KLogging() {
    @JvmStatic
    fun main(vararg args: String) {
        val folder = ProjectSelector.select()
        println("folder: $folder")

        createProject(folder)

        launchIdea(folder)
    }

    fun createProject(projectDir: File) {
        projectDir.mkdirs()

        projectDir.resolve(".idea").deleteRecursively()

        projectDir.walkTopDown().filter {
            it.name.endsWith(".iml") || it.name.endsWith(".iws") || it.name.endsWith(".ipr")
        }.forEach { it.delete() }

        val buildScript = """
            plugins {
                id("voodoo") version "${VoodooConstants.FULL_VERSION}"
            }

            voodoo {
                addTask(name = "build", parameters = listOf("build"))
                addTask(name = "sk", parameters = listOf("pack sk"))
                addTask(name = "server", parameters = listOf("pack server"))
                addTask(name = "buildAndPackAll", parameters = listOf("build", "pack sk", "pack server", "pack mmc"))
            }

            repositories {
                mavenLocal()
            }
        """.trimIndent()

        val buildScriptFile = projectDir.resolve("build.gradle.kts")
        buildScriptFile.writeText(buildScript)

        val settings = """
            pluginManagement {
                repositories {
                    mavenLocal()
                    maven(url = "https://dl.bintray.com/kotlin/kotlin-eap")
                    maven(url = "https://kotlin.bintray.com/kotlinx")
                    maven(url = "https://jitpack.io") { name = "jitpack" }
                    mavenCentral()
                    gradlePluginPortal()
                }
            }
        """.trimIndent()

        val settingsFile = projectDir.resolve("settings.gradle.kts")
        settingsFile.writeText(settings)

        installGradleWrapper(projectDir, VoodooConstants.GRADLE_VERSION)
    }

    fun installGradleWrapper(projectDir: File, version: String = VoodooConstants.GRADLE_VERSION, distributionType: String = "bin") {
        if (!ShellUtils.isInPath("gradle")) {
            logger.error("skipping gradle wrapper installation")
            logger.error("please install 'gradle'")
            return
        }
        runProcess("gradle", "wrapper", "--gradle-version", version, "--distribution-type", distributionType,
            wd = projectDir,
            stdoutConsumer = Consumer { t -> println(t) },
            stderrConsumer = Consumer { t -> println("err: $t") }
        )
    }

    fun launchIdea(projectDir: File) {
        requireInPath(
            "idea",
            "Could not find 'idea' in your PATH. It can be created in IntelliJ under `Tools -> Create Command-line Launcher`"
        )

        runProcess("idea", projectDir.absolutePath, wd = projectDir)
    }
}

data class ProcessResult(val command: String, val exitCode: Int, val stdout: String, val stderr: String) {
    override fun toString(): String {
        return """
            Exit Code   : ${exitCode}Comand      : $command
            Stdout      : $stdout
            Stderr      : """.trimIndent() + "\n" + stderr
    }
}

fun evalBash(
    cmd: String,
    wd: File? = null,
    stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
    stderrConsumer: Consumer<String> = StringBuilderConsumer()
): ProcessResult {
    return runProcess(
        "bash", "-c", cmd,
        wd = wd, stderrConsumer = stderrConsumer, stdoutConsumer = stdoutConsumer
    )
}

fun runProcess(
    vararg cmd: String,
    wd: File? = null,
    stdoutConsumer: Consumer<String> = StringBuilderConsumer(),
    stderrConsumer: Consumer<String> = StringBuilderConsumer()
): ProcessResult {

    try {
        // simplify with https://stackoverflow.com/questions/35421699/how-to-invoke-external-command-from-within-kotlin-code
        val proc = ProcessBuilder(cmd.asList()).directory(wd).start()

        // we need to gobble the streams to prevent that the internal pipes hit their respecitive buffer limits, which
        // would lock the sub-process execution (see see https://github.com/holgerbrandl/kscript/issues/55
        // https://stackoverflow.com/questions/14165517/processbuilder-forwarding-stdout-and-stderr-of-started-processes-without-blocki
        val stdoutGobbler = StreamGobbler(proc.inputStream, stdoutConsumer).apply { start() }
        val stderrGobbler = StreamGobbler(proc.errorStream, stderrConsumer).apply { start() }

        val exitVal = proc.waitFor()

        // we need to wait for the gobbler threads or we may loose some output (e.g. in case of short-lived processes
        stderrGobbler.join()
        stdoutGobbler.join()

        return ProcessResult(cmd.joinToString(" "), exitVal, stdoutConsumer.toString(), stderrConsumer.toString())
    } catch (t: Throwable) {
        throw RuntimeException(t)
    }
}

fun quit(status: Int): Nothing {
    print(if (status == 0) "true" else "false")
    exitProcess(status)
}

internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) :
    Thread() {

    override fun run() {
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}

internal open class StringBuilderConsumer : Consumer<String> {
    val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendln(t)
    }

    override fun toString(): String {
        return sb.toString()
    }
}

object ShellUtils {
    fun isInPath(tool: String) = evalBash("which $tool").stdout.trim().isNotBlank()
    fun requireInPath(tool: String, msg: String = "$tool is not in PATH") = require(isInPath(tool)) { msg }
}

private fun createSymLink(link: File, target: File, overwrite: Boolean = false) {
    try {
        if (overwrite) link.deleteRecursively()
        Files.createSymbolicLink(link.toPath(), target.absoluteFile.toPath())
    } catch (e: IOException) {
        GradleSetup.logger.error("Failed to create symbolic link to script. Copying instead...", e)
        target.copyTo(link)
    }
}