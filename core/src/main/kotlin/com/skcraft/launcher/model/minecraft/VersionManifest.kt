// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.minecraft

import kotlinx.serialization.CompositeEncoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.builtins.set
import voodoo.util.serializer.TimestampSerializer
import java.time.LocalDateTime

@Serializable
data class VersionManifest(
    var id: String? = null,
    @Serializable(with = TimestampSerializer::class) var time: LocalDateTime? = null,
    @Serializable(with = TimestampSerializer::class) var releaseTime: LocalDateTime? = null,
    var assets: String? = null,
    var type: String? = null,
    var processArguments: String? = null,
    var minecraftArguments: String? = null,
    var mainClass: String? = null,
    var minimumLauncherVersion: Int = 0,
    var libraries: HashSet<Library> = hashSetOf()
) {

    @Transient
    val assetsIndex: String?
        get() = if (assets != null) assets else "legacy"

    @Serializer(forClass = VersionManifest::class)
    companion object {
        override fun serialize(encoder: Encoder, obj: VersionManifest) {
            val elemOutput = encoder.beginStructure(descriptor)
            with(VersionManifest()) {
                elemOutput.serialize(this.id, obj.id, 0)
                elemOutput.serializeObj(this.time, obj.time, TimestampSerializer, 1)
                elemOutput.serializeObj(this.releaseTime, obj.releaseTime, TimestampSerializer, 2)
                elemOutput.serialize(this.assets, obj.assets, 3)
                elemOutput.serialize(this.type, obj.type, 4)
                elemOutput.serialize(this.processArguments, obj.processArguments, 5)
                elemOutput.serialize(this.minecraftArguments, obj.minecraftArguments, 6)
                elemOutput.serialize(this.mainClass, obj.mainClass, 7)
                elemOutput.serialize(this.minimumLauncherVersion, obj.minimumLauncherVersion, 8)
                elemOutput.serializeObj(this.libraries, obj.libraries, Library.set, 9)
            }
            elemOutput.endStructure(descriptor)
        }

        private inline fun <reified T : Any> CompositeEncoder.serialize(default: T?, actual: T?, index: Int) {
            if (default != actual && actual != null) {
                when (actual) {
                    is String -> this.encodeStringElement(descriptor, index, actual)
                    is Int -> this.encodeIntElement(descriptor, index, actual)
                }
            }
        }

        private fun <T : Any?> CompositeEncoder.serializeObj(
            default: T?,
            actual: T?,
            saver: SerializationStrategy<T>,
            index: Int
        ) {
            if ((default != actual || default != null) && actual != null) {
                this.encodeSerializableElement(descriptor, index, saver, actual)
            }
        }
    }
}