package org.rsmod.api.toxins

import jakarta.inject.Inject
import org.rsmod.api.config.refs.timers
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Runs upstream's `toxins` timer, which the cache has always reserved and nothing used.
 *
 * A soft timer rather than a normal one, so a poison keeps ticking while the bank is open, as on
 * live. Death clears everything; `deathResetTimers` only re-arms the regeneration timers and would
 * otherwise send a player back to Lumbridge still envenomed.
 */
public class ToxinsScript @Inject constructor(private val toxins: Toxins) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerSoftTimer(timers.toxins) { toxins.tick(player) }
        onPlayerLogin { toxins.resume(player) }
        onEvent<PlayerDeathEvents.Death> { toxins.clear(player) }
    }
}
