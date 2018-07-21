// Generated by delombok at Sat Jul 14 05:49:42 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.modpack

open class BaseManifest {
    var title: String? = null
    var name: String? = null
    var version: String? = null

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is BaseManifest) return false
        if (if (this.title == null) other.title != null else this.title != other.title) return false
        if (if (this.name == null) other.name != null else this.name != other.name) return false
        return !if (this.version == null) other.version != null else this.version != other.version
    }

    protected open fun canEqual(other: Any): Boolean {
        return other is BaseManifest
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = 1
        result = result * PRIME + (title?.hashCode() ?: 43)
        result = result * PRIME + (name?.hashCode() ?: 43)
        result = result * PRIME + (version?.hashCode() ?: 43)
        return result
    }

    override fun toString(): String {
        return "BaseManifest(title=" + this.title + ", name=" + this.name + ", version=" + this.version + ")"
    }
}
