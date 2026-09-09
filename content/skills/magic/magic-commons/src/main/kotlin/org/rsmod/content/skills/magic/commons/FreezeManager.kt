package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.annotations.InternalApi
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Freezing, built on the engine's walk-trigger primitive.
 *
 * There is no "frozen" flag in RSMod. What exists is `walktriggers.frozen`, a high-priority walk
 * trigger the movement processor fires (and clears) the moment an entity with a route is about to
 * step. A freeze is therefore a timer plus that trigger: while the timer runs, every attempt to
 * walk is caught by `FrozenScript`, which aborts the route and re-arms the trigger. Expiry clears
 * the trigger and starts a short immunity, so a barrage cannot chain-freeze.
 *
 * Players use a **soft** timer: normal player timers are protected events and stall while the
 * player is delayed or behind a modal, which would leave someone frozen past the spell's length.
 *
 * `params.freeze_resistance` is honoured (a value of 100 or more is immune, anything else shortens
 * the freeze by that percentage) but no npc in this cache carries it, so in practice every npc
 * freezes for the full duration.
 */
@OptIn(InternalApi::class)
@Singleton
public class FreezeManager @Inject constructor() {
    public fun isFrozen(target: PathingEntity): Boolean =
        when (target) {
            is Player -> target.softTimerMap[magic_timers.frozen.id.toShort()] != null
            is Npc -> target.timerMap[magic_timers.frozen.id.toShort()] != null
        }

    public fun isImmune(target: PathingEntity): Boolean =
        when (target) {
            is Player -> target.softTimerMap[magic_timers.freeze_immunity.id.toShort()] != null
            is Npc -> target.timerMap[magic_timers.freeze_immunity.id.toShort()] != null
        }

    /**
     * Freezes [target] for [ticks], or returns `false` if it is already frozen or still immune, in
     * which case the caller's spell should do its damage and nothing more.
     */
    public fun freeze(target: PathingEntity, ticks: Int): Boolean {
        if (isFrozen(target) || isImmune(target)) {
            return false
        }
        return when (target) {
            is Player -> freezePlayer(target, ticks)
            is Npc -> freezeNpc(target, ticks)
        }
    }

    private fun freezePlayer(player: Player, ticks: Int): Boolean {
        player.abortRoute()
        player.clearMapFlag()
        player.walkTrigger(walktriggers.frozen)
        player.softTimer(magic_timers.frozen, ticks)
        player.mes("You have been frozen!")
        return true
    }

    private fun freezeNpc(npc: Npc, ticks: Int): Boolean {
        val resistance = npc.visType.paramMap?.getOrNull(params.freeze_resistance) ?: 0
        if (resistance >= 100) {
            return false
        }
        val duration = ticks * (100 - resistance) / 100
        if (duration <= 0) {
            return false
        }
        npc.abortRoute()
        npc.walkTrigger(walktriggers.frozen)
        npc.timer(magic_timers.frozen, duration)
        return true
    }

    /** Called by `FrozenScript` when the freeze timer fires. */
    internal fun thaw(player: Player) {
        player.clearSoftTimer(magic_timers.frozen)
        if (player.walkTrigger == walktriggers.frozen) {
            player.clearWalkTrigger()
        }
        player.softTimer(magic_timers.freeze_immunity, IMMUNITY_TICKS)
    }

    internal fun thaw(npc: Npc) {
        npc.clearTimer(magic_timers.frozen)
        if (npc.walkTrigger == walktriggers.frozen) {
            npc.clearWalkTrigger()
        }
        npc.timer(magic_timers.freeze_immunity, IMMUNITY_TICKS)
    }

    public companion object {
        /** How long a target stays unfreezable after a freeze wears off. */
        public const val IMMUNITY_TICKS: Int = 5
    }
}
