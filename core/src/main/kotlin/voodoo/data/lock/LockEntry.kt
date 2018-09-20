package voodoo.data.lock

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import kotlinx.coroutines.experimental.runBlocking
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialSaver
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.UpdateMode
import kotlinx.serialization.internal.EnumSerializer
import kotlinx.serialization.json.JSON
import mu.KLogging
import voodoo.data.Side
import voodoo.data.curse.CurseConstancts.PROXY_URL
import voodoo.data.curse.FileID
import voodoo.data.curse.ProjectID
import voodoo.provider.CurseProvider
import voodoo.provider.DirectProvider
import voodoo.provider.JenkinsProvider
import voodoo.provider.LocalProvider
import voodoo.provider.ProviderBase
import voodoo.provider.Providers
import voodoo.provider.UpdateJsonProvider
import java.io.File
import java.time.Instant

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Serializable
data class LockEntry(
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var provider: String = "",
    @JsonInclude(JsonInclude.Include.ALWAYS)
    var id: String = "",
    @Optional var name: String = "",
    @Optional var fileName: String? = null,
    @Optional var side: Side = Side.BOTH,
    // CURSE
    @Optional var curseMetaUrl: String = PROXY_URL,
    @Optional var projectID: ProjectID = ProjectID.INVALID,
    @Optional var fileID: FileID = FileID.INVALID,
    // DIRECT
    @Optional var url: String = "",
    @Optional var useUrlTxt: Boolean = true,
    // JENKINS
    @Optional var jenkinsUrl: String = "",
    @Optional var job: String = "",
    @Optional var buildNumber: Int = -1,
    @Optional var fileNameRegex: String = ".*(?<!-sources\\.jar)(?<!-api\\.jar)(?<!-deobf\\.jar)(?<!-lib\\.jar)(?<!-slim\\.jar)$",
    // JSON
    @Optional var updateJson: String = "",
    @Optional var jsonVersion: String = "",
    // LOCAL
    @Optional var fileSrc: String = ""
) {
    @JsonIgnore
    @Transient
    lateinit var parent: LockPack

    /**
     * relative to src folder
     */
    @JsonIgnore
    @Transient
    lateinit var file: File

    @JsonIgnore
    fun provider(): ProviderBase = Providers[provider]

    @JsonIgnore
    fun name(): String = name.takeIf { it.isNotBlank() } ?: runBlocking { provider().generateName(this@LockEntry) }

    @JsonIgnore
    fun version(): String = runBlocking { provider().getVersion(this@LockEntry) }

    @JsonIgnore
    fun license(): String = runBlocking { provider().getLicense(this@LockEntry) }

    @JsonIgnore
    fun thumbnail(): String = runBlocking { provider().getThumbnail(this@LockEntry) }

    @JsonIgnore
    fun authors(): String = runBlocking { provider().getAuthors(this@LockEntry).joinToString(", ") }

    @JsonIgnore
    fun projectPage(): String = runBlocking { provider().getProjectPage(this@LockEntry) }

    @JsonIgnore
    fun releaseDate(): Instant? = runBlocking { provider().getReleaseDate(this@LockEntry) }

    @JsonIgnore
    fun isCurse(): Boolean = provider == CurseProvider.id

    @JsonIgnore
    fun isJenkins(): Boolean = provider == JenkinsProvider.id

    @JsonIgnore
    fun isDirect(): Boolean = provider == DirectProvider.id

    @JsonIgnore
    fun isJson(): Boolean = provider == UpdateJsonProvider.id

    @JsonIgnore
    fun isLocal(): Boolean = provider == LocalProvider.id

    @Serializer(forClass = LockEntry::class)
    companion object : KLogging() {
        override fun save(output: KOutput, obj: LockEntry) {
            val elemOutput = output.writeBegin(serialClassDesc)
            elemOutput.writeStringElementValue(serialClassDesc, 0, obj.provider)
            elemOutput.writeStringElementValue(serialClassDesc, 1, obj.id)
            elemOutput.writeStringElementValue(serialClassDesc, 2, obj.name)
            with(LockEntry(provider = obj.provider, id = obj.id)) {
                if (this.fileName != obj.fileName) {
                    elemOutput.writeStringElementValue(serialClassDesc, 3, obj.fileName!!)
                }
                elemOutput.serializeObj(this.side, obj.side, EnumSerializer(Side::class), 4)
                elemOutput.serialize(this.curseMetaUrl, obj.curseMetaUrl, 5)
                elemOutput.serializeObj(this.projectID, obj.projectID, ProjectID.Companion, 6)
                elemOutput.serializeObj(this.fileID, obj.fileID, FileID.Companion, 7)
                elemOutput.serialize(this.url, obj.url, 8)
                elemOutput.serialize(this.useUrlTxt, obj.useUrlTxt, 9)
                elemOutput.serialize(this.jenkinsUrl, obj.jenkinsUrl, 10)
                elemOutput.serialize(this.job, obj.job, 11)
                elemOutput.serialize(this.buildNumber, obj.buildNumber, 12)
                elemOutput.serialize(this.fileNameRegex, obj.fileNameRegex, 13)
                elemOutput.serialize(this.updateJson, obj.updateJson, 28)
                elemOutput.serialize(this.jsonVersion, obj.jsonVersion, 29)
                elemOutput.serialize(this.fileSrc, obj.fileSrc, 27)
            }
            output.writeEnd(serialClassDesc)
        }

        private inline fun <reified T : Any> KOutput.serialize(default: T, actual: T, index: Int) {
            if (default != actual)
                when (actual) {
                    is String -> this.writeStringElementValue(serialClassDesc, index, actual)
                    is Int -> this.writeIntElementValue(serialClassDesc, index, actual)
                    is Boolean -> this.writeBooleanElementValue(serialClassDesc, index, actual)
                }
        }

        private fun <T : Any?> KOutput.serializeObj(default: T, actual: T, saver: KSerialSaver<T>, index: Int) {
            if (default != actual) {
                this.writeElement(serialClassDesc, index)
                this.write(saver, actual)
            }
        }

        private val json = JSON(
            indented = true,
            updateMode = UpdateMode.BANNED,
            nonstrict = true,
            unquoted = true,
            indent = "  ",
            context = SerialContext().apply {
                registerSerializer(Side::class, Side)
            })

        fun loadEntry(file: File): LockEntry = json.parse(file.readText())
    }

    fun serialize(): String = json.stringify(this)
}