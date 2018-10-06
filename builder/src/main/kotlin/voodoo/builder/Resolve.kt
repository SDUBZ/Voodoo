package voodoo.builder

import com.skcraft.launcher.model.modpack.Feature
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.consume
import kotlinx.coroutines.experimental.coroutineScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.joinAll
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import voodoo.data.curse.DependencyType
import voodoo.data.flat.Entry
import voodoo.data.flat.ModPack
import com.skcraft.launcher.model.ExtendedFeaturePattern
import voodoo.memoize
import voodoo.provider.Providers
import voodoo.util.pool
import java.io.File
import java.util.Collections
import kotlin.system.exitProcess

/**
 * Created by nikky on 28/03/18.
 * @author Nikky
 */

private val logger = KotlinLogging.logger {}

private fun ModPack.getDependenciesCall(entryId: String): List<Entry> {
    val modpack = this
    val entry = modpack.findEntryById(entryId) ?: return emptyList()
    val result = mutableListOf(entry)
    for ((depType, entryList) in entry.dependencies) {
        if (depType == DependencyType.EMBEDDED) continue
        for (depName in entryList) {
            result += this.getDependencies(depName)
        }
    }
    return result
}

private val ModPack.getDependencies: (entryName: String) -> List<Entry>
    get() = ::getDependenciesCall.memoize()

private fun ModPack.resolveFeatureDependencies(entry: Entry, defaultName: String) {
    val entryFeature = entry.feature ?: return
    val featureName = entry.name.takeIf { it.isNotBlank() } ?: defaultName
    // find feature with matching id
    var feature = features.find { f -> f.feature.name == featureName }

    // TODO: merge existing features with matching id
    if (feature == null) {
        var description = entryFeature.description
        if (description.isEmpty()) description = entry.description
        feature = ExtendedFeaturePattern(
            entries = setOf(entry.id),
            files = entryFeature.files,
            feature = Feature(
                name = featureName,
                selected = entryFeature.selected,
                description = description,
                recommendation = entryFeature.recommendation
            )
        )
        processFeature(this, feature)
        features += feature
        entry.optional = true
    }
    logger.debug("processed ${entry.id}")
}

private fun processFeature(modPack: ModPack, feature: ExtendedFeaturePattern) {
    logger.info("processing feature: $feature")
    var processedEntries = emptyList<String>()
    var processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
    while (processableEntries.isNotEmpty()) {
        processableEntries = feature.entries.filter { f -> !processedEntries.contains(f) }
        for (entry_id in processableEntries) {
            logger.info("searching $entry_id")
            val entry = modPack.findEntryById(entry_id)
            if (entry == null) {
                logger.warn("$entry_id not in entries")
                processedEntries += entry_id
                continue
            }
            var depNames = entry.dependencies.values.flatten()
            logger.info("depNames: $depNames")
            depNames = depNames.filter { d ->
                modPack.entrySet.any { entry -> entry.id == d && entry.optional }
            }
            logger.info("filtered dependency names: $depNames")
            for (dep in depNames) {
                if (!(feature.entries.contains(dep))) {
                    feature.entries += dep
                }
            }
            processedEntries += entry_id
        }
    }
}

suspend fun ModPack.resolve(
    folder: File,
    updateAll: Boolean = false,
    updateDependencies: Boolean = false,
    updateEntries: List<String> = listOf()
) {
//    this.loadEntries(folder)
    this.loadLockEntries(folder)

    val srcDir = folder.resolve(sourceDir)

    if (updateAll) {
        lockEntrySet.clear()
        // delete all lockfiles
        folder.walkTopDown().asSequence()
            .filter {
                it.isFile && it.name.endsWith(".lock.hjson")
            }
            .forEach {
                it.delete()
            }
    } else {
        for (entryId in updateEntries) {
            val entry = findEntryById(entryId)
            if (entry == null) {
                logger.error("entry $entryId not found")
                exitProcess(-1)
            }
            lockEntrySet.find { it.id == entryId }?.let {
                it.serialFile.delete()
                lockEntrySet.remove(it)
            }
        }
    }

    if (updateDependencies || updateAll) {
        // remove all transient entries
        lockEntrySet.removeIf { (id, _) ->
            findEntryById(id)?.transient ?: true
        }
    }

    // recalculate all dependencies
    var unresolved: Set<Entry> = entrySet.toSet()
    val resolved: MutableSet<String> = Collections.synchronizedSet(mutableSetOf<String>())
//    val accumulatorContext = newSingleThreadContext("AccumulatorContext")
    coroutineScope {
        do {
            val newEntriesChannel = Channel<Pair<Entry, String>>(Channel.UNLIMITED)

            logger.info("unresolved: ${unresolved.map { it.id }}")

            val jobs = mutableListOf<Job>()
            for (entry in unresolved) {
                jobs += launch(context = pool + CoroutineName("job-${entry.id}")) {
                    logger.info("resolving: ${entry.id}")
                    val provider = Providers[entry.provider]

                    val lockEntry = provider.resolve(entry, this@resolve.mcVersion, newEntriesChannel)
                    logger.debug("received locked entry: $lockEntry")

                    logger.debug("validating: $lockEntry")
                    if (!provider.validate(lockEntry)) {
                        throw IllegalStateException("did not pass validation")
                    }

                    logger.debug("trying to merge entry")
                    val actualLockEntry = addOrMerge(lockEntry) { old, new ->
                        old ?: new
                    }
                    logger.debug("merged entry: $actualLockEntry")

                    logger.debug("validating: actual $actualLockEntry")
                    if (!provider.validate(actualLockEntry)) {
                        logger.error { actualLockEntry }
                        throw IllegalStateException("actual entry did not validate")
                    }

//                    logger.debug("setting display name")
//                    actualLockEntry.name = actualLockEntry.name()

                    logger.debug("adding to resolved")
                    resolved += entry.id

                    logger.debug("resolved: $resolved\n")
                    logger.debug("unresolved: ${entrySet.map { entry -> entry.id }.filter { id -> !resolved.contains(id) }}\n")
                }.also {
                    logger.info("started job resolve ${entry.id}")
                    delay(100)
                }
            }

            val newEntries = async(context = pool) {
                val accumultator = mutableSetOf<Entry>()
                for ((entry, path) in newEntriesChannel) {
                    logger.info("channel received: ${entry.id}")

                    if (entry.id in resolved) {
                        logger.info("entry already resolved ${entry.id}")
                        continue
                    }
                    if (this@resolve.entrySet.any { it.id == entry.id }) {
                        logger.info("entry already added ${entry.id}")
                        continue
                    }
                    if (accumultator.any { it.id == entry.id }) {
                        logger.info("entry already in queue ${entry.id}")
                        continue
                    }

                    this@resolve.addEntry(entry, dependency = true)
                    logger.info { "added entry ${entry.id}" }
                    accumultator += entry
                }
                accumultator
            }

            newEntriesChannel.consume {
                jobs.joinAll()
            }

            logger.info("added last step: ${newEntries.await().map { it.id }}")
            logger.info("resolved last step: ${unresolved.map { it.id }}")

//        unresolved = newEntries.await()
            unresolved = entrySet.asSequence().filter { !resolved.contains(it.id) }.toSet()
        } while (unresolved.isNotEmpty())
    }
    val unresolvedIDs = resolved - this.entrySet.map { it.id }
    logger.info("unresolved ids: $unresolvedIDs")
    logger.info("resolved ids: ${lockEntrySet.map { it.id }}")

    features.clear()

    entrySet.filter {
        findLockEntryById(it.id) == null
    }.run {
        if (isNotEmpty()) throw IllegalStateException("unresolved entries: $this")
    }

    for (entry in entrySet) {
        this.resolveFeatureDependencies(
            entry, findLockEntryById(entry.id)?.name
                ?: throw NullPointerException("cannot find lockentry for ${entry.id}")
        )
    }

    // resolve features
    for (feature in features) {
        logger.info("processed feature ${feature.feature.name}")
        for (id in feature.entries) {
            logger.info("processing feature entry $id")
            val dependencies = this.getDependencies(id)
            feature.entries += dependencies.asSequence().filter {
                logger.debug("testing ${it.id}")
                it.optional && !feature.entries.contains(it.id)
            }.map { it.id }
        }
        logger.info("build entry: ${feature.entries.first()}")
        val mainEntry = findEntryById(feature.entries.first())!!
        feature.feature.description = mainEntry.description

        logger.info("processed feature $feature")
    }

    // TODO: rethink history, since packs are now mainly file based
}
