package org.rsmod.content.skills.mining.configs

import org.rsmod.api.type.refs.content.ContentReferences

/**
 * Content groups this module owns.
 *
 * These are resolved from `.data/symbols/.local/content.sym` rather than upstream's `content.sym`.
 * `ContentReferenceResolver` fails a boot outright on an unknown name (`ImplicitNameNotFound`), so
 * the symbol file and this object have to stay in step.
 */
object MiningContent : ContentReferences() {
    val mining_pickaxe = find("mining_pickaxe")
    val mining_rock = find("mining_rock")
}
