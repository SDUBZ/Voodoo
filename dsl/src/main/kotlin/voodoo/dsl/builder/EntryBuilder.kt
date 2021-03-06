package voodoo.dsl.builder

import voodoo.data.DependencyType
import voodoo.data.nested.NestedEntry
import voodoo.dsl.VoodooDSL
import voodoo.property

class EntryBuilder<E: NestedEntry>(
    entry: E
) : AbstractBuilder<E>(entry) {
    //    var id by property(entry::id)
//    var name by property(entry::name)
//    var websiteUrl by property(entry::websiteUrl)
//    var fileNameRegex by property(entry::fileNameRegex)

    @VoodooDSL
    @Deprecated("use invoke operator", replaceWith = ReplaceWith("this.invoke(configureEntry)"), level = DeprecationLevel.WARNING)
    infix fun configure(configureEntry: E.(EntryBuilder<E>) -> Unit): EntryBuilder<E> {
        entry.configureEntry(this)
        return this
    }

    @VoodooDSL
    operator fun invoke(configureEntry: E.(EntryBuilder<E>) -> Unit): EntryBuilder<E> {
        entry.configureEntry(this)
        return this
    }

//    infix fun name(s: String) = apply {
//        name = s
//    }
//
//    infix fun websiteUrl(s: String) = apply {
//        websiteUrl = s
//    }
//
//    infix fun fileNameRegex(r: String) = apply {
//        fileNameRegex = r
//    }

    @VoodooDSL
    fun dependencies(type: DependencyType = DependencyType.REQUIRED, vararg dependencies: String)  {
        dependencies.forEach { dep ->
            entry.dependencies.putIfAbsent(dep, type)
        }
    }
}