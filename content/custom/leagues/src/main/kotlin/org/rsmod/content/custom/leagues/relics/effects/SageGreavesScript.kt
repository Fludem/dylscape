package org.rsmod.content.custom.leagues.relics.effects

import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_timers
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Corner Cutter's Sage's greaves: a little Agility experience for every tick spent running in them,
 * scaled by Agility level.
 *
 * "Running" is read off movement - two tiles covered in one tick - rather than the run toggle, so
 * standing still with run on pays nothing. The amount is deliberately small: it is multiplied by
 * the player's xp rate like everything else.
 */
class SageGreavesScript : PluginScript() {
    private val lastCoords = HashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        onPlayerLogin { startIfHeld(player) }
        onEvent<RelicUnlocked> { startIfHeld(player) }
        onPlayerSoftTimer(league_timers.sage_greaves) { tick(player) }
        onEvent<SessionStateEvent.Delete> { lastCoords.remove(player) }
    }

    private fun startIfHeld(player: Player) {
        if (player.hasRelic(Relic.CornerCutter)) {
            player.softTimer(league_timers.sage_greaves, 1)
        }
    }

    private fun tick(player: Player) {
        if (!player.hasRelic(Relic.CornerCutter)) {
            player.clearSoftTimer(league_timers.sage_greaves)
            lastCoords.remove(player)
            return
        }
        val previous = lastCoords.put(player, player.coords)
        if (previous == null || league_objs.sage_greaves !in player.worn) {
            return
        }
        if (previous.level != player.coords.level) {
            return
        }
        val distance = max(abs(previous.x - player.coords.x), abs(previous.z - player.coords.z))
        if (distance == RUN_STEP) {
            player.statAdvance(stats.agility, player.agilityLvl * XP_PER_LEVEL)
        }
    }

    private companion object {
        const val RUN_STEP = 2
        const val XP_PER_LEVEL = 0.01
    }
}
