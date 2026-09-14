package org.rsmod.content.custom.skillingtasks.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Puts the Taskmaster on the map. **Only the Gradle `packCache` task runs this**, with the server
 * stopped; a normal boot never repacks spawns. `SkillingTaskSpawnTest` reads [FILE] back and checks
 * the npc and the tile.
 */
object SkillingTaskNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<SkillingTaskNpcSpawns>(FILE)
    }

    /** Public rather than internal because the integration source set compiles separately. */
    const val FILE: String = "npcs.toml"
}
