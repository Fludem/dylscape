package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.player.events.PlayerHitEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statBoost
import org.rsmod.api.player.stat.statHeal
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.leagues.configs.league_timers
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Last Stand relic: a hit that would kill leaves the player on 1 hp instead, boosts their
 * combat stats to 255 and keeps them from dropping below 1 hp for 16 ticks.
 *
 * It hangs off `PlayerHitEvents.Impact`, which `StandardPlayerHitProcessor` publishes after the
 * damage is subtracted but **before** it checks for 0 hp and queues the death - and unbound events
 * run synchronously - so topping the player back up here is enough to cancel the death. The hitmark
 * still shows the full damage. Hits routed through `DamageOnlyPlayerHitProcessor` publish no Impact
 * and so bypass it.
 *
 * Vanilla's end-of-effect heal (healed by the damage you dealt) is not built.
 */
class LastStandScript @Inject constructor(private val mapClock: MapClock) : PluginScript() {
    /** The map clock cycle each player's 1-hp floor lasts until. */
    private val activeUntil = HashMap<Player, Int>()

    /** The cycle each player's Last Stand is next available on. */
    private val readyAt = HashMap<Player, Int>()

    private val combatStats
        get() = listOf(stats.attack, stats.strength, stats.defence, stats.ranged, stats.magic)

    override fun ScriptContext.startup() {
        onEvent<PlayerHitEvents.Impact> { impact(player) }
        onPlayerSoftTimer(league_timers.last_stand) { drain(player) }
        onEvent<PlayerDeathEvents.Death> { forget(player) }
        onEvent<SessionStateEvent.Delete> { forget(player) }
    }

    private fun impact(player: Player) {
        if (player.hitpoints > 0) {
            return
        }
        val now = mapClock.cycle
        val floorUntil = activeUntil[player]
        if (floorUntil != null && now <= floorUntil) {
            player.statHeal(stats.hitpoints, 1, 0)
            return
        }
        if (!player.hasRelic(Relic.LastStand) || (readyAt[player] ?: 0) > now) {
            return
        }

        player.statHeal(stats.hitpoints, 1, 0)
        activeUntil[player] = now + FLOOR_TICKS
        readyAt[player] = now + COOLDOWN_TICKS
        for (stat in combatStats) {
            val headroom = (MAX_BOOSTED_LEVEL - player.statBase(stat)).coerceAtLeast(0)
            player.statBoost(stat, headroom, 0)
        }
        player.softTimer(league_timers.last_stand, 1)
        player.mes("You refuse to fall!")
    }

    /**
     * Drains each boosted combat stat towards base + [DRAIN_FLOOR], and stops once all are there.
     */
    private fun drain(player: Player) {
        var draining = false
        for (stat in combatStats) {
            val floor = player.statBase(stat) + DRAIN_FLOOR
            val current = player.stat(stat)
            if (current > floor) {
                player.statSub(stat, min(DRAIN_PER_TICK, current - floor), 0)
                draining = true
            }
        }
        if (!draining) {
            player.clearSoftTimer(league_timers.last_stand)
        }
    }

    private fun forget(player: Player) {
        activeUntil.remove(player)
        readyAt.remove(player)
    }

    private companion object {
        const val FLOOR_TICKS = 16
        const val COOLDOWN_TICKS = 300
        const val MAX_BOOSTED_LEVEL = 255
        const val DRAIN_FLOOR = 15
        const val DRAIN_PER_TICK = 10
    }
}
