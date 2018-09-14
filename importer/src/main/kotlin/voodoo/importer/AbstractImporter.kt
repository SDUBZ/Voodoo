package voodoo.importer

import kotlinx.coroutines.experimental.CoroutineScope
import mu.KLogging
import voodoo.util.Directories
import java.io.File

/**
 * Created by nikky on 30/03/18.
 * @author Nikky
 */

abstract class AbstractImporter : KLogging() {
    abstract val label: String

    abstract suspend fun import(
        coroutineScope: CoroutineScope,
        source: String,
        target: File,
        name: String? = null
    )

    val directories = Directories.get()
}