// Generated by delombok at Sat Jul 14 04:26:21 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher.model.modpack

class LaunchModifier {
    val flags: List<String> = emptyList()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is LaunchModifier) return false
        return flags == other.flags
    }

    protected fun canEqual(other: Any): Boolean {
        return other is LaunchModifier
    }

    override fun hashCode(): Int {
        val PRIME = 59
        var result = 1
        val `$flags` = this.flags
        result = result * PRIME + `$flags`.hashCode()
        return result
    }

    override fun toString(): String {
        return "LaunchModifier(flags=" + this.flags + ")"
    }
}
