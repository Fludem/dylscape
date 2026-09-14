package org.rsmod.content.custom.vorkath.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Places the sleeping Vorkath and both Torfinns.
 *
 * None of the three exist in `world-spawns`: its data predates Dragon Slayer II, so Ungael is empty
 * and the Rellekka pier has no ferryman.
 *
 * **These only take effect after `./gradlew packCache` with the server stopped**, or the live
 * server's own first-boot pack. Nothing here spawns on a normal boot, however green the tests are.
 */
public object VorkathSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<VorkathSpawns>(FILE)
    }

    public const val FILE: String = "npcs.toml"
}
