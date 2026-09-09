package org.rsmod.content.custom.dagannothkings.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Places the three kings, so this module owns them rather than `world-spawns`.
 *
 * The generated world spawns come from void's 2011 data, which clusters all three in the middle of
 * the lair. `tools/npc-spawns/generate.py` now skips them by name for the same reason it skips the
 * Barrows brothers: a module that runs its own `NpcEditor` for an npc should place it too, or the
 * two sources drift.
 *
 * **These only take effect after `./gradlew packCache` with the server stopped**, or the live
 * server's own first-boot pack. Nothing here spawns on a normal boot, however green the tests are.
 */
public object DagannothKingsSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<DagannothKingsSpawns>(FILE)
    }

    public const val FILE: String = "npcs.toml"
}
