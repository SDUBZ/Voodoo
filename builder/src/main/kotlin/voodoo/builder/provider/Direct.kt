package voodoo.builder.provider

import khttp.get
import voodoo.builder.ProviderThingy
import mu.KLogging
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Created by nikky on 30/12/17.
 * @author Nikky
 * @version 1.0
 */

class DirectProviderThing : ProviderThingy() {
    override val name = "Direct provider"

    companion object: KLogging()

    //    override fun validate(): Boolean {
//        return entry.url.isNotBlank()
//    }
    init {
        register("setFileName",
                { it.fileName.isBlank() && it.url.isNotBlank() },
                { e, _ ->
                    val u = URL(e.url)
                    e.fileName = u.file.substringAfterLast('/')
                }
        )
        register("setName",
                { it.name.isBlank() && it.fileName.isNotBlank() },
                { e, _ ->
                    e.name = e.fileName.substringBeforeLast('.')
                }
        )
        register("setTargetPath",
                { it.targetPath.isBlank() },
                { e, _ ->
                    e.targetPath = "mods"
                }
        )
        register("cacheRelpath",
                { it.cacheRelpath.isBlank() && it.url.isNotBlank()},
                { e, _ ->
                    val u = URL(e.url)
                    e.cacheRelpath = File(e.provider.toString()).resolve(u.path.substringAfterLast('/')).path
                }
        )
        register("writeUrlTxt",
                {
                    with(it) {
                        listOf(url, filePath).all { it.isNotBlank() } && !urlTxtDone
                    }
                },
                { e, m ->
                    if(e.urlTxt) {
                        val urlPath = File(m.outputPath, e.filePath + ".url.txt")
                        File(urlPath.parent).mkdirs()
                        urlPath.writeText(URLDecoder.decode(e.url, "UTF-8"))
                    }
                    e.urlTxtDone = true
                }
        )
        register("download",
                {
                    with(it) {
                        listOf(url, name, fileName, filePath, cachePath).all { it.isNotBlank() }
                                && urlTxtDone
                                && resolvedOptionals
                    }
                },
                { entry, m ->
                    val cacheDir = File(entry.cachePath)
                    if (!cacheDir.isDirectory) {
                        cacheDir.mkdirs()
                    }

                    val cacheFile = cacheDir.resolve(entry.fileName)
                    if (!cacheFile.exists() || !cacheFile.isFile) {
                        logger.info("downloading ${entry.name} to $cacheFile")
                        val r = get(entry.url, allowRedirects = true, stream = true)
                        cacheFile.writeBytes(r.content)
                    } else {
                        logger.info("skipping downloading ${entry.name} (is cached)")
                    }
                    val destination = File(m.outputPath).resolve(entry.filePath)
                    logger.info("copying $cacheFile -> $destination")
                    cacheFile.copyTo(destination, overwrite = true)
                    entry.done = true
                }
        )
    }

    fun doDirectThingy() {
        logger.warn("doDirectThingy not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
