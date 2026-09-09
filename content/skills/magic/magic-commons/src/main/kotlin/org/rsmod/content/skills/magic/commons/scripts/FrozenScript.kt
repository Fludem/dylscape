package org.rsmod.content.skills.magic.commons.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.walktriggers
import org.rsmod.api.player.output.clearMapFlag
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onNpcWalkTrigger
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.script.onPlayerWalkTrigger
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The walk-trigger and timer half of [FreezeManager].
 *
 * `PlayerMovementProcessor.processWalkTrigger` clears the trigger before publishing it, so a frozen
 * entity's trigger has to be put back on every attempt or the second click would walk. The route is
 * aborted rather than the interaction cleared: a frozen player can still cast or shoot at something
 * already in range, exactly as in the official game.
 */
class FrozenScript @Inject constructor(private val freeze: FreezeManager) : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerWalkTrigger(walktriggers.frozen) {
            if (!freeze.isFrozen(player)) {
                return@onPlayerWalkTrigger
            }
            player.abortRoute()
            player.clearMapFlag()
            player.walkTrigger(walktriggers.frozen)
            player.mes("A magical force stops you from moving.")
        }
        onPlayerSoftTimer(magic_timers.frozen) { freeze.thaw(player) }
        onPlayerSoftTimer(magic_timers.freeze_immunity) {
            player.clearSoftTimer(magic_timers.freeze_immunity)
        }

        onNpcWalkTrigger(walktriggers.frozen) {
            if (!freeze.isFrozen(npc)) {
                return@onNpcWalkTrigger
            }
            npc.abortRoute()
            npc.walkTrigger(walktriggers.frozen)
        }
        onNpcTimer(magic_timers.frozen) { freeze.thaw(npc) }
        onNpcTimer(magic_timers.freeze_immunity) { npc.clearTimer(magic_timers.freeze_immunity) }
    }
}
