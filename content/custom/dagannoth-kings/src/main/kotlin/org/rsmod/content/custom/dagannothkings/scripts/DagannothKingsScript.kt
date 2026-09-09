package org.rsmod.content.custom.dagannothkings.scripts

import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.custom.dagannothkings.configs.DagannothKingsLair
import org.rsmod.content.custom.dagannothkings.configs.dk_locs
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The way in and the way out.
 *
 * That is the whole script, and the small size is the point: the kings themselves need no code.
 * They are already spawned by `world-spawns`, their stats come from the cache plus
 * `DagannothKingsNpcEditor`, their attacks run through the shared npc combat driver in
 * `api/combat`, and their loot is claimed by the generated tables in `content/custom/drop-tables` -
 * which is also why this module deliberately registers no death handler. Adding one would race the
 * generated registration through a parallel ClassGraph scan, and whichever lost would either throw
 * at boot or silently drop the loot.
 *
 * The two ladders cannot be left to `LadderScript`. That one climbs by translating the player's
 * level by one; these two connect mapsquare 29,68 to mapsquare 45,69, sideways across the map
 * rather than up or down a floor.
 */
class DagannothKingsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(dk_locs.boss_ladder_down) { climb(it.type, DagannothKingsLair.lairArrival) }
        onOpLoc1(dk_locs.boss_ladder_up) { climb(it.type, DagannothKingsLair.antechamberArrival) }
    }

    private suspend fun ProtectedAccess.climb(type: UnpackedLocType, dest: CoordGrid) {
        arriveDelay()
        // `climb_anim` carries a default of `human_reachforladder`, so this is safe on a loc that
        // does not declare one of its own.
        anim(type.param(params.climb_anim))
        delay(1)
        telejump(dest)
    }
}
