package voodoo.data.nested

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import mu.KLogging
import voodoo.data.components.*
import voodoo.data.flat.Entry
import voodoo.provider.*
import java.io.File
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.functions
import kotlin.reflect.full.memberProperties

@Serializable
sealed class NestedEntry : CommonMutable {
    abstract var nodeName: String?
    abstract var entries: List<NestedEntry>
//    @Transient
//    abstract val provider: String

    @Serializable
    @SerialName("common")
    data class Common(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(), CommonMutable by common {
        override val provider = ""
    }

    @Serializable
    @SerialName("curse")
    data class Curse(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        val curse: CurseComponent = CurseComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(), CommonMutable by common, CurseMutable by curse {
        override val provider = CurseProvider.id
    }

    @Serializable
    @SerialName("direct")
    data class Direct(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        val direct: DirectComponent = DirectComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(), CommonMutable by common, DirectMutable by direct {
        override val provider = DirectProvider.id
    }

    @Serializable
    @SerialName("jenkins")
    data class Jenkins(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        val jenkins: JenkinsComponent = JenkinsComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(), CommonMutable by common, JenkinsMutable by jenkins {
        override val provider = JenkinsProvider.id
    }

    @Serializable
    @SerialName("local")
    data class Local(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        val local: LocalComponent = LocalComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ) : NestedEntry(), CommonMutable by common, LocalMutable by local {
        override val provider = LocalProvider.id
    }

    @Serializable
    @SerialName("noop")
    data class Noop(
        @Transient override var nodeName: String? = null,
        val common: CommonComponent = CommonComponent(),
        override var entries: List<NestedEntry> = emptyList()
    ): NestedEntry(), CommonMutable by common {
        override val provider = NoopProvider.id
    }

    @Transient
    private val debugIdentifier: String
        get() = nodeName ?: id

    companion object : KLogging() {
//        val DEFAULT = NestedEntry()
    }

    suspend fun flatten(parentFile: File? = null): List<Entry> {
        flatten("", parentFile)

        // remove duplicate entries
        val ids = mutableSetOf<String>()

        entries.forEach { entry ->
            if (entry.id in ids) {
                entries -= entry
            } else {
                ids += entry.id
            }
        }
        // copy entries
        return entries.filter { it.enabled }.map { entry ->
            when (entry) {
                is Common -> Entry.Common(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    }
                )
                is Curse -> Entry.Curse(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    },
                    curse = entry.curse.copy()
                )
                is Direct -> Entry.Direct(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    },
                    direct = entry.direct.copy()
                )
                is Jenkins -> Entry.Jenkins(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    },
                    jenkins = entry.jenkins.copy()
                )
                is Local -> Entry.Local(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    },
                    local = entry.local.copy()
                )
                is Noop -> Entry.Noop(
                    common = with(entry.common) {
                        copy(
                            optionalData = optionalData?.copy()
                        )
                    }
                )
            }
        }.toList()
    }

    private suspend fun flatten(indent: String, parentFile: File? = null) {
        val toDelete = mutableListOf<NestedEntry>()

        entries.forEach { entry ->
            logger.debug { "$indent pre_flatten: ${entry.debugIdentifier}" }

            // set feature of entry from `this` or DEFAULT

            logger.debug { "$indent copying fields of '${this.debugIdentifier}' -> '${entry.debugIdentifier}'" }

            // TODO: avoid creating and throwing away objects for defaults
            mergeProperties<CommonMutable>(this, entry, Common())

            when {
                entry is Curse && this is Curse -> {
                    mergeProperties<CurseMutable>(this, entry, Curse())
                }
                entry is Direct && this is Direct -> {
                    mergeProperties<DirectMutable>(this, entry, Direct())
                }
                entry is Jenkins && this is Jenkins -> {
                    mergeProperties<JenkinsMutable>(this, entry, Jenkins())
                }
                entry is Local && this is Local -> {
                    mergeProperties<LocalMutable>(this, entry, Local())
                }
            }

//            for (prop in NestedEntry::class.memberProperties) {
//                if (prop is KMutableProperty<*>) {
//                    val otherValue = prop.get(entry)
//                    val thisValue = prop.get(this)
//                    val defaultValue = prop.get(DEFAULT)
//                    if (otherValue == defaultValue && thisValue != defaultValue) {
//                        if (prop.name != "entries" && prop.name != "template") {
//                            // clone maps
//                            when (thisValue) {
//                                is MutableMap<*, *> -> {
//                                    val map = thisValue.toMutableMap()
//                                    // copy lists
//                                    map.forEach { k, v ->
//                                        if (v is List<*>) {
//                                            map[k] = v.toList()
//                                        }
//                                    }
//                                    prop.setter.call(entry, map)
//                                }
//                                is Set<*> -> prop.setter.call(entry, thisValue.toSet())
//                                else ->
//                                    prop.setter.call(entry, thisValue)
//                            }
//                        }
//                    }
//                }
//            }

            entry.flatten("$indent|  ", parentFile)
            if (entry.entries.isNotEmpty() || entry.id.isBlank()) {
                toDelete += entry
            }

            entry.entries.forEach { entries += it }
            entry.entries = emptyList()
        }
        entries = entries.filter { !toDelete.contains(it) }
        entries.forEach { entry ->
            if (entry.id.isEmpty()) {
                logger.error { entry }
                error("entries with blank id must not persist")
            }
        }
    }
}

private inline fun <reified T : Any> mergeProperties(a: T, other: T, default: T) {
    for (prop in T::class.memberProperties) {
        if (prop is KMutableProperty<*>) {
            val otherValue = prop.get(other)
            val thisValue = prop.get(a)
            val defaultValue = prop.get(default)
            if (otherValue == defaultValue && thisValue != defaultValue) {
                if (prop.name != "entries") {
                    // clone maps
                    when (thisValue) {
                        is MutableMap<*, *> -> {
                            val map = thisValue.toMutableMap()
                            // copy lists
                            map.forEach { (k, v) ->
                                if (v is List<*>) {
                                    map[k] = v.toList()
                                }
                            }
                            prop.setter.call(other, map)
                        }
                        is Set<*> -> prop.setter.call(other, thisValue.toSet())
                        else -> {
                            if (thisValue != null) {
                                val thisClass = thisValue::class
                                val copyFuncRef =
                                    thisClass.functions.find { it.name == "copy" && it.parameters.isEmpty() }
                                if (copyFuncRef != null) {
                                    NestedPack.logger.debug("copy found for $thisClass")
                                    prop.setter.call(other, copyFuncRef.call(thisValue))
                                } else {
                                    prop.setter.call(other, thisValue)
                                }
                            } else {
                                prop.setter.call(other, null)
                            }
                        }
                    }
                }
            }
        }
    }
}
