// Generated by delombok at Sat Jul 14 05:49:42 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.modpack

import com.fasterxml.jackson.annotation.JsonManagedReference
import com.fasterxml.jackson.annotation.JsonProperty
import com.skcraft.launcher.model.minecraft.VersionManifest
import java.net.URL
import java.util.*

class Manifest(
        var minimumVersion: Int = 0
) : BaseManifest() {

    var baseUrl: URL? = null
    var librariesLocation: String? = null
    var objectsLocation: String? = null
    var gameVersion: String? = null
    @JsonProperty("launch")
    var launchModifier: LaunchModifier? = null
    var features: List<Feature>? = emptyList()
    @JsonManagedReference("manifest")
    var tasks: MutableList<ManifestEntry> = mutableListOf()
    var versionManifest: VersionManifest? = null

    fun updateName(name: String?) {
        if (name != null) {
            super.name = name
        }
    }

    fun updateTitle(title: String?) {
        if (title != null) {
            super.title = title
        }
    }

    fun updateGameVersion(gameVersion: String?) {
        if (gameVersion != null) {
            this.gameVersion = gameVersion
        }
    }

    override fun toString(): String {
        return "Manifest(minimumVersion=" + this.minimumVersion + ", baseUrl=" + this.baseUrl + ", librariesLocation=" + this.librariesLocation + ", objectsLocation=" + this.objectsLocation + ", gameVersion=" + this.gameVersion + ", launchModifier=" + this.launchModifier + ", features=" + this.features + ", tasks=" + this.tasks + ", versionManifest=" + this.versionManifest + ")"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Manifest) return false
        if (!super.equals(other)) return false
        if (this.minimumVersion != other.minimumVersion) return false
        if (if (this.baseUrl == null) other.baseUrl != null else this.baseUrl != other.baseUrl) return false
        if (if (this.librariesLocation == null) other.librariesLocation != null else this.librariesLocation != other.librariesLocation) return false
        if (if (this.objectsLocation == null) other.objectsLocation != null else this.objectsLocation != other.objectsLocation) return false
        if (if (this.gameVersion == null) other.gameVersion != null else this.gameVersion != other.gameVersion) return false
        if (if (this.launchModifier == null) other.launchModifier != null else this.launchModifier != other.launchModifier) return false
        if (if (this.features == null) other.features != null else this.features != other.features) return false
        if (this.tasks != other.tasks) return false
        return !if (this.versionManifest == null) other.versionManifest != null else this.versionManifest != other.versionManifest
    }

    override fun canEqual(other: Any): Boolean {
        return other is Manifest
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = super.hashCode()
        result = result * PRIME + this.minimumVersion
        result = result * PRIME + (baseUrl?.hashCode() ?: 43)
        result = result * PRIME + (librariesLocation?.hashCode() ?: 43)
        result = result * PRIME + (objectsLocation?.hashCode() ?: 43)
        result = result * PRIME + (gameVersion?.hashCode() ?: 43)
        result = result * PRIME + (launchModifier?.hashCode() ?: 43)
        result = result * PRIME + (features?.hashCode() ?: 43)
        result = result * PRIME + tasks.hashCode()
        result = result * PRIME + (versionManifest?.hashCode() ?: 43)
        return result
    }

    companion object {
        val MIN_PROTOCOL_VERSION = 2
    }
}
