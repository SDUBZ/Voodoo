// Generated by delombok at Sat Jul 14 04:26:20 CEST 2018
/*
 * SK's Minecraft Launcher
 * Copyright (C) 2010-2014 Albert Pham <http://www.sk89q.com> and contributors
 * Please see LICENSE.txt for license information.
 */
package com.skcraft.launcher

import com.google.common.io.Closer
import java.io.*
import java.net.MalformedURLException
import java.net.URL
import java.util.Properties
import java.util.regex.Pattern

object LauncherUtils {
    private val log = java.util.logging.Logger.getLogger(LauncherUtils::class.java.name)
    private val absoluteUrlPattern = Pattern.compile("^[A-Za-z0-9\\-]+://.*$")

    @Throws(IOException::class)
    fun loadProperties(clazz: Class<*>, name: String, extraProperty: String): Properties {
        val closer = Closer.create()
        val prop = Properties()
        try {
            var `in`: InputStream? = closer.register(clazz.getResourceAsStream(name))
            if (`in` != null) {
                prop.load(`in`)
                val extraPath = System.getProperty(extraProperty)
                if (extraPath != null) {
                    log.info("Loading extra properties for " + clazz.canonicalName + ":" + name + " from " + extraPath + "...")
                    `in` = closer.register(BufferedInputStream(closer.register(FileInputStream(extraPath))))
                    prop.load(`in`)
                }
            } else {
                throw FileNotFoundException()
            }
        } finally {
            closer.close()
        }
        return prop
    }

    @Throws(MalformedURLException::class)
    fun concat(baseUrl: URL, url: String): URL {
        if (absoluteUrlPattern.matcher(url).matches()) {
            return URL(url)
        }
        val lastSlash = baseUrl.toExternalForm().lastIndexOf("/")
        if (lastSlash == -1) {
            return URL(url)
        }
        val firstSlash = url.indexOf("/")
        if (firstSlash == 0) {
            val portSet = baseUrl.defaultPort == baseUrl.port || baseUrl.port == -1
            val port = if (portSet) "" else ":" + baseUrl.port
            return URL(baseUrl.protocol + "://" + baseUrl.host + port + url)
        } else {
            return URL(baseUrl.toExternalForm().substring(0, lastSlash + 1) + url)
        }
    }
}