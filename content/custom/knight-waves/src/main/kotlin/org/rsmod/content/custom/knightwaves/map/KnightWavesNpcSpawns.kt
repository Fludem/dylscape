package org.rsmod.content.custom.knightwaves.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Puts the Squire in Camelot's courtyard. Without him the trial has no entrance: npc spawns are
 * authored rather than read from the cache, and upstream only ships Lumbridge's.
 *
 * **Only the Gradle `packCache` task runs this.** The Squire will not appear until it has been run
 * with the server stopped.
 */
object KnightWavesNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<KnightWavesNpcSpawns>("npcs.toml")
    }
}
