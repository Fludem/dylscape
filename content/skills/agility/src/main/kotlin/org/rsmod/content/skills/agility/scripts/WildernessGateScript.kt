package org.rsmod.content.skills.agility.scripts

import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.courses.Wilderness
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The two gates into the Wilderness Agility course.
 *
 * They are wall locs with an "Open" op and **no opened variant in the cache**, so they cannot go
 * through the generic gate script, which swaps a closed loc for an open one. Instead they act as a
 * level check the player steps through: below the course level they refuse, otherwise the player is
 * moved to the tile on the other side of the wall. No experience.
 *
 * All three placements sit on their tile's south edge (angle 3), so "the other side" is a one-tile
 * step north or south depending on where the player stands.
 */
class WildernessGateScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(AgilityLocs.wilderness_gate_outer) { pass(it.loc) }
        onOpLoc1(AgilityLocs.wilderness_gate_left) { pass(it.loc) }
        onOpLoc1(AgilityLocs.wilderness_gate_right) { pass(it.loc) }
    }

    private suspend fun ProtectedAccess.pass(gate: BoundLocInfo) {
        val level = Wilderness.course.level
        if (player.agilityLvl < level) {
            mes("You need an Agility level of $level to use this course.")
            return
        }
        val gateTile = gate.coords
        val dest =
            if (player.coords.z < gateTile.z) {
                CoordGrid(gateTile.x, gateTile.z, gateTile.level)
            } else {
                CoordGrid(gateTile.x, gateTile.z - 1, gateTile.level)
            }
        faceLoc(gate)
        delay(1)
        telejump(dest)
    }
}
