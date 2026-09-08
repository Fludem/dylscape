package org.rsmod.content.areas.misc.tutorial.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.content.areas.misc.tutorial.TutorialIsland

/**
 * Places the tutorial instructors. The coordinates in `npcs.toml` are **best-effort** — the cache
 * carries no npc spawns, so the island's real tiles are not readable from it. They put each
 * instructor in roughly the right room of mapsquare 48_48 and should be nudged once seen in a
 * running client. Nothing in the tutorial's logic depends on them.
 */
object TutorialNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TutorialIsland>("npcs.toml")
    }
}
