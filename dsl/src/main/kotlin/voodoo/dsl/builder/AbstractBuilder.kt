package voodoo.dsl.builder

import optional
import voodoo.data.OptionalData
import voodoo.data.curse.ProjectID
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.property
import voodoo.provider.ProviderBase
import java.io.File

@VoodooDSL
abstract class AbstractBuilder<E: NestedEntry>(
    val entry: E
) {
    // TODO: why was this available in here?
//    suspend fun flatten(parent: File) = entry.flatten(parent)

//    var folder by property(entry::folder)
//    var description by property(entry::description)

//    var side by property(entry::side)

    // TODO: dependencies
    //  replaceDependencies

//    var packageType by property(entry::packageType)
//    var version by property(entry::version)
//    var fileName by property(entry::fileName)
//    var validMcVersions by property(entry::validMcVersions)

    @Deprecated("use extension functions instead of builder functions", replaceWith = ReplaceWith("this.optional(block)"))
    fun optional(block: OptionalBuilder.() -> Unit) {
        entry.optional(block)
    }

    @Deprecated("use extension functions instead of builder functions", replaceWith = ReplaceWith("this.optional(block)"))
    fun replaceDependencies(vararg replacements: Pair<ProjectID, ProjectID>) {
        val mutableMap =  entry.replaceDependencies.toMutableMap()
        replacements.forEach { (original, replacement) ->
            mutableMap[original] = replacement
        }
        entry.replaceDependencies = mutableMap.toMap()
    }
}