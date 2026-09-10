package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Total Recall relic's Crystal of echoes: op3 Save-memory, op1 Teleport-back, op2
 * Check-memories.
 *
 * The saved tile lives in the vanilla `league_last_recall_source_coord` varp, which is `Perm`, so a
 * memory survives logging out. The saved Hitpoints, Prayer and special energy are held for the
 * session only; after a relog Teleport-back still works but has nothing to restore.
 */
class TotalRecallScript : PluginScript() {
    private data class Memory(val hitpoints: Int, val prayer: Int, val specialEnergy: Int)

    private val memories = HashMap<Player, Memory>()

    override fun ScriptContext.startup() {
        onOpHeld3(league_objs.crystal_of_echoes) { saveMemory() }
        onOpHeld1(league_objs.crystal_of_echoes) { teleportBack() }
        onOpHeld2(league_objs.crystal_of_echoes) { checkMemories() }
        onEvent<SessionStateEvent.Delete> { memories.remove(player) }
    }

    private fun ProtectedAccess.saveMemory() {
        if (!player.hasRelic(Relic.TotalRecall)) {
            mes(NOT_YOURS)
            return
        }
        vars[league_varps.last_recall_coord] = coords.packed
        memories[player] = Memory(stat(stats.hitpoints), stat(stats.prayer), vars[varps.sa_energy])
        mes("The crystal hums as it takes a memory of this place.")
    }

    private fun ProtectedAccess.teleportBack() {
        if (!player.hasRelic(Relic.TotalRecall)) {
            mes(NOT_YOURS)
            return
        }
        val packed = vars[league_varps.last_recall_coord]
        if (packed == 0) {
            mes("The crystal holds no memory yet. Save one first.")
            return
        }
        telejump(CoordGrid(packed))
        val memory = memories[player] ?: return
        restore(stats.hitpoints, memory.hitpoints)
        restore(stats.prayer, memory.prayer)
        vars[varps.sa_energy] = memory.specialEnergy
        mes("You feel yourself as you were when you saved the memory.")
    }

    private fun ProtectedAccess.checkMemories() {
        val packed = vars[league_varps.last_recall_coord]
        if (packed == 0) {
            mes("The crystal holds no memory yet.")
            return
        }
        val coords = CoordGrid(packed)
        mes("The crystal remembers ${coords.x}, ${coords.z} on level ${coords.level}.")
        val memory = memories[player] ?: return
        mes(
            "Hitpoints ${memory.hitpoints}, Prayer ${memory.prayer}, special attack " +
                "${memory.specialEnergy / 10}%."
        )
    }

    /** Sets [stat]'s current level to [level], up or down. */
    private fun ProtectedAccess.restore(stat: StatType, level: Int) {
        val current = stat(stat)
        when {
            level > current -> statHeal(stat, level - current, 0)
            level < current -> statSub(stat, current - level, 0)
        }
    }

    private companion object {
        const val NOT_YOURS = "The crystal stays dark in your hands."
    }
}
