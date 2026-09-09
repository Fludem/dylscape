package org.rsmod.content.custom.barrows.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * The Strange Old Man on the surface, and the crypt bestiary underground.
 *
 * **Only the Gradle `packCache` task runs this.** None of these npcs appear until it has been run
 * with the server stopped; a normal boot does not repack map data.
 *
 * These used to come from `world-spawns`' generated `minigame_barrows_brothers.toml`, bridged from
 * void's 2011 map, and were wrong for this cache: every crypt monster sat on level 0 of mapsquare
 * 55_151, which has no walkable tile at all, and the old man stood on Ahrim's dig spot. The
 * generator now excludes the group so it cannot reintroduce them, and barrows owns its own spawns.
 */
object BarrowsNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<BarrowsNpcSpawns>("npcs.toml")
    }
}
