package org.rsmod.content.custom.tollgate.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder
import org.rsmod.content.custom.tollgate.TollGateScript

object TollGateNpcSpawns : MapNpcSpawnBuilder() {
    override fun onPackMapTask() {
        resourceFile<TollGateScript>("npcs.toml")
    }
}
