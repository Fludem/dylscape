package org.rsmod.content.skills.fishing.configs

import org.rsmod.api.type.refs.content.ContentReferences

/**
 * Content groups this module owns.
 *
 * Only one is needed. A content group is a single field on a type, so a spot cannot be tagged "net
 * spot" and "bait spot" at once — and it does not need to be: which methods a spot offers is
 * already stated by its own op text in the cache, and [FishingSpots] reads it from there.
 *
 * Resolved from `.data/symbols/.local/content.sym`, which `SymbolModule.shallowSymbolDirectories`
 * merges over upstream's. An unknown name fails the boot outright, so the two must stay in step.
 */
object FishingContent : ContentReferences() {
    val fishing_spot = find("fishing_spot")
}
