package org.rsmod.content.custom.barrows.scripts

import jakarta.inject.Inject
import kotlin.math.min
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.barrows.BarrowsCrypt
import org.rsmod.content.custom.barrows.BarrowsCrypt.inCrypt
import org.rsmod.content.custom.barrows.BarrowsProgress.resetRun
import org.rsmod.content.custom.barrows.configs.BarrowsInterfaces
import org.rsmod.content.custom.barrows.configs.barrows_timers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Everything that happens *because* the player is standing in the crypt: the killcount overlay and
 * the prayer drain.
 *
 * Both hang off one soft timer, started when a player digs into a mound and stopping itself the
 * moment they are no longer underground, so it costs nothing for anyone who is not doing Barrows.
 *
 * The overlay needs no component work at all. `barrows_overlay` component 0 carries
 * `onLoad=[clientscript,barrows_overlay_init, ...]` listing its own twelve components, so it wires
 * itself up on open and reads the `barrows_killed_*` varbits directly. Opening it and writing
 * varbits is the whole of the server side.
 */
class BarrowsCryptScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(barrows_timers.crypt_tick) { player.cryptTick() }

        // Soft timers do not survive a session, so a player who logged out underground would come
        // back to no overlay and free prayer without this.
        onPlayerLogin { player.startCryptTick(eventBus) }

        // Dying in the crypt costs you the run, as it does on live. Hung off the death event
        // rather than the death queue because the queue takes the player somewhere else first, and
        // `leaveCrypt` has to see them still underground to know to close the overlay.
        onEvent<PlayerDeathEvents.Death> {
            player.leaveCrypt(eventBus)
            player.resetRun()
        }
    }

    private fun Player.cryptTick() {
        if (!inCrypt) {
            leaveCrypt(eventBus)
            return
        }
        if (!ui.containsOverlay(BarrowsInterfaces.overlay)) {
            ifOpenOverlay(BarrowsInterfaces.overlay, eventBus)
        }
        drainPrayer()
    }

    /**
     * The crypt saps prayer just by being in it, shown as a hitsplat the way OSRS does it.
     *
     * `statSub` and not `statDrain`: `statDrain` floors at `base - drain`, so it would stop taking
     * points once a fixed amount had been lost, whereas the crypt keeps taking them until the book
     * is empty.
     */
    private fun Player.drainPrayer() {
        if (prayerLvl <= 0) {
            return
        }
        statSub(stats.prayer, constant = min(prayerLvl, DRAIN_PER_TICK), percent = 0)
    }

    private companion object {
        /** One point per tick of the crypt timer - about a point every six seconds. */
        const val DRAIN_PER_TICK = 1
    }
}

/**
 * Starts the crypt tick if the player is underground, and does nothing if they are not. Safe to
 * call whenever a player might have arrived: entering, or logging back in.
 */
internal fun Player.startCryptTick(eventBus: EventBus) {
    if (!inCrypt) {
        return
    }
    softTimer(barrows_timers.crypt_tick, BarrowsCrypt.TICK_INTERVAL)
    if (!ui.containsOverlay(BarrowsInterfaces.overlay)) {
        ifOpenOverlay(BarrowsInterfaces.overlay, eventBus)
    }
}

/** Stops the crypt tick and takes the overlay away. */
internal fun Player.leaveCrypt(eventBus: EventBus) {
    clearSoftTimer(barrows_timers.crypt_tick)
    if (ui.containsOverlay(BarrowsInterfaces.overlay)) {
        ifCloseOverlay(BarrowsInterfaces.overlay, eventBus)
    }
}
