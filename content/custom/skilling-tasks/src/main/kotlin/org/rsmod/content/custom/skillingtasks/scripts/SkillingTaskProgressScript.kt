package org.rsmod.content.custom.skillingtasks.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.custom.skillingtasks.SkillingTaskState
import org.rsmod.content.custom.skillingtasks.TaskKind
import org.rsmod.content.skills.cooking.scripts.CookedFood
import org.rsmod.content.skills.crafting.scripts.Crafted
import org.rsmod.content.skills.fishing.scripts.Fishing
import org.rsmod.content.skills.fletching.scripts.Fletched
import org.rsmod.content.skills.herblore.scripts.MixedPotion
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.smithing.scripts.Smelted
import org.rsmod.content.skills.smithing.scripts.Smithed
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Turns what players make into task progress.
 *
 * Every hook is an unbound event a skill already publishes on the tick the product lands, so no
 * skill knows about the Taskmaster. Burnt food does not count; a batch (`Smithed`, `Fletched`,
 * `Crafted`) counts by its `count`.
 */
class SkillingTaskProgressScript @Inject constructor(private val state: SkillingTaskState) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin { greet() }
        onEvent<SessionStateEvent.Delete> { state.forget(player) }

        onEvent<Woodcutting.CutLogs> { state.advance(player, TaskKind.Chop, product) }
        onEvent<Mining.MinedOre> { state.advance(player, TaskKind.Mine, product) }
        onEvent<Fishing.CaughtFish> { state.advance(player, TaskKind.Fish, fish) }
        onEvent<CookedFood> {
            if (!burnt) {
                state.advance(player, TaskKind.Cook, product)
            }
        }
        onEvent<Smelted> { state.advance(player, TaskKind.Smelt, bar) }
        onEvent<Smithed> { state.advance(player, TaskKind.Smith, product, count) }
        onEvent<Fletched> { state.advance(player, TaskKind.Fletch, product, count) }
        onEvent<Crafted> { state.advance(player, TaskKind.Craft, product, count) }
        onEvent<MixedPotion> { state.advance(player, TaskKind.Potion, product) }
    }

    private fun SessionStateEvent.Login.greet() {
        val tasks = state.peek(player) ?: return
        val task = tasks.task ?: return
        if (!tasks.hasTask) {
            return
        }
        player.mes(
            "Skilling task: ${task.describe(tasks.target)} " +
                "(${tasks.progress}/${tasks.target}). Points: ${tasks.points}."
        )
    }
}
