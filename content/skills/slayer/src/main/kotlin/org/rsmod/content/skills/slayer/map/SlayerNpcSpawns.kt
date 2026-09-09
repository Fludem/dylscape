package org.rsmod.content.skills.slayer.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Puts the slayer masters on the map.
 *
 * Four of the six needed spawning here. Vannaka and Chaeldar were already in the world with their
 * real npc types, but Turael, Mazchna and Duradel stood in Burthorpe, Canifis and Shilo Village as
 * `wgs_heroes_*` types - *While Guthix Sleeps* scene actors with **no ops at all**, so all three
 * were unclickable statues. Nieve was not in the world in any form.
 *
 * The three replacements keep the exact tiles the scene actors used, which are the vanilla spawn
 * tiles; only the npc type changes. The old entries are gone from `world-spawns`, and
 * `tools/npc-spawns/generate.py` treats the master types as one dedupe family so a regen cannot
 * bring them back.
 *
 * **Only the Gradle `packCache` task runs this**, with the server stopped. A new spawn silently
 * does nothing until then.
 */
object SlayerNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SlayerNpcSpawns>(FILE)
    }

    /** Public because the integration source set compiles separately. */
    const val FILE: String = "npcs.toml"
}
