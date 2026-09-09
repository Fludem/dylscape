package org.rsmod.content.skills.slayer.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.script.onEvent
import org.rsmod.content.skills.slayer.SlayerProgress.hasSlayerTask
import org.rsmod.content.skills.slayer.SlayerProgress.slayerRemaining
import org.rsmod.content.skills.slayer.SlayerProgress.slayerTask
import org.rsmod.content.skills.slayer.data.SlayerTaskRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Credits a slayer kill: the experience, and one off the task counter.
 *
 * This hangs off `NpcDeathEvents.Killed` rather than the death queue on purpose. Queues take
 * exactly one handler and `DropTableScript` already owns the type-specific `queues.death` for
 * nearly every npc that has a drop table, so a second registration would be a boot failure rather
 * than a second listener. The event is unbound, and unbound events allow as many subscribers as
 * want them.
 *
 * Experience is paid **only on task**, which is how Slayer works: an abyssal demon killed while
 * something else is assigned pays combat experience and nothing else. That is also what stops the
 * skill from training itself as a side effect of ordinary combat.
 */
class SlayerKillScript @Inject constructor(private val repo: SlayerTaskRepository) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<NpcDeathEvents.Killed> { killer?.let { creditKill(npc, it) } }
    }

    private fun creditKill(npc: Npc, killer: Player) {
        if (!killer.hasSlayerTask) {
            return
        }
        val taskId = repo.taskIdForNpc(npc.id)
        if (taskId == 0 || taskId != killer.slayerTask) {
            return
        }

        killer.statAdvance(stats.slayer, npc.slayerXp().toDouble())

        val remaining = (killer.slayerRemaining - 1).coerceAtLeast(0)
        killer.slayerRemaining = remaining
        if (remaining > 0) {
            return
        }
        // Completion payout lands with the rewards work; for now the task simply ends.
        val task = repo.task(taskId)
        killer.mes("You have completed your task! Return to a Slayer master for another.")
        if (task != null) {
            killer.mes("You have killed every ${task.name} you were assigned.")
        }
    }

    /**
     * How much slayer experience this npc is worth.
     *
     * The vanilla rule is "as much as the monster has hitpoints", which is why nothing has to be
     * authored per npc. `slayer_experience` is honoured when it is set, so a monster that breaks
     * the rule can be corrected through the cache enricher rather than through a table here, but it
     * is unset on every slayer monster in this cache today.
     */
    private fun Npc.slayerXp(): Int {
        val override = visType.param(params.slayer_experience)
        return if (override > 0) override else visType.hitpoints
    }
}
