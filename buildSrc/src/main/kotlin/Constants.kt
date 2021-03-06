import org.gradle.api.tasks.wrapper.Wrapper

fun create(
    group: String,
    name: String,
    version: String? = null
): String = buildString {
    append(group)
    append(':')
    append(name)
    version?.let {
        append(':')
        append(it)
    }
}

object Gradle {
    const val version = "6.3"
    val distributionType = Wrapper.DistributionType.ALL
}

object Kotlin {
    const val version = "1.3.70"
}

object Coroutines {
    const val version = "1.2.1" // because gradle is weird...
//    const val version = "1.3.4"
    val dependency = create(group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-core", version = version)
}

object Serialization {
    const val version = "0.20.0"
    const val plugin = "kotlinx-serialization"
    const val module = "org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin"
    val dependency = create(group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-runtime", version = version)
}

object Kotlinpoet {
    const val version = "1.5.0"
    val dependency = create(group = "com.squareup", name = "kotlinpoet", version = version)
}

object Ktor {
    const val version = "1.3.2"
    val dependency = create(group = "io.ktor", name = "ktor-client-cio", version = version)
    val dependencyJson = create(group = "io.ktor", name = "ktor-client-json", version = version)
    val dependencySerialization = create(group = "io.ktor", name = "ktor-client-serialization-jvm", version = version)
}

object Argparser {
    const val version = "2.0.7"
    val dependency = create(group = "com.xenomachina", name = "kotlin-argparser", version = version)
}

object KotlinxHtml {
    const val version = "0.6.10"
    val dependency = create(group = "org.jetbrains.kotlinx", name = "kotlinx-html-jvm", version = version)
}

object Logging {
    const val version = "1.7.9"

    val dependency = create(group = "io.github.microutils", name = "kotlin-logging", version = version)
    val dependencyLogbackClassic = create(group = "ch.qos.logback", name = "logback-classic", version = "1.3.0-alpha4")
}

object Spek {
    const val version = "2.0.0-rc.1"
//    const val version = "2.0.0"
    val dependencyDsl = create(group = "org.spekframework.spek2", name = "spek-dsl-jvm", version = version)
    val dependencyRunner = create(group = "org.spekframework.spek2", name = "spek-runner-junit5", version = version)
    val dependencyJUnit5 = create(group = "org.junit.platform", name = "junit-platform-engine", version = "1.3.0-RC1")
}

object Apache {
    val commonsCompress = create(group = "org.apache.commons", name = "commons-compress", version = "1.18")
}

object Jenkins {
    const val url: String = "https://jenkins.modmuss50.me"
    const val job: String = "NikkyAi/DaemonicLabs/Voodoo/master"
}

object Maven {
    const val url = "https://maven.modmuss50.me"
    const val shadowClassifier = "all"
}
