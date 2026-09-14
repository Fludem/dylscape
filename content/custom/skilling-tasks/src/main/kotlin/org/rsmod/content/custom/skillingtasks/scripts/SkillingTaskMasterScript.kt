package org.rsmod.content.custom.skillingtasks.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.custom.skillingtasks.SkillingTaskAssigner
import org.rsmod.content.custom.skillingtasks.SkillingTaskState
import org.rsmod.content.custom.skillingtasks.SkillingTasks
import org.rsmod.content.custom.skillingtasks.TaskKind
import org.rsmod.content.custom.skillingtasks.configs.skilling_task_currencies
import org.rsmod.content.custom.skillingtasks.configs.skilling_task_invs
import org.rsmod.content.custom.skillingtasks.configs.skilling_task_npcs
import org.rsmod.content.custom.skillingtasks.shop.SkillingPointShopOperations
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Taskmaster: talk, contract, last-tier contract, rewards.
 *
 * The op slots are the cache's own for `con_contractor_3op` (`Talk-to` 1, `Contract` 3, `Last-tier
 * contract` 4, `Rewards` 5), decoded rather than assumed and pinned by the spawn test. `Contract`
 * reports the current task or hands out a new one; `Last-tier contract` is a task in the same skill
 * as the previous one, which is the closest this npc's menu comes to "another one like that".
 */
class SkillingTaskMasterScript
@Inject
constructor(
    private val state: SkillingTaskState,
    private val assigner: SkillingTaskAssigner,
    private val shops: Shops,
    private val operationMap: ShopOperationMap,
    private val pointOperations: SkillingPointShopOperations,
) : PluginScript() {
    override fun ScriptContext.startup() {
        operationMap.register(skilling_task_currencies.skilling_points, pointOperations)

        onOpNpc1(skilling_task_npcs.taskmaster) { talk(it.npc) }
        onOpNpc3(skilling_task_npcs.taskmaster) { startDialogue(it.npc) { contract(null) } }
        onOpNpc4(skilling_task_npcs.taskmaster) { startDialogue(it.npc) { lastTierContract() } }
        onOpNpc5(skilling_task_npcs.taskmaster) { player.openRewards(it.npc) }

        onCommand("skilltask") {
            modLevel = modlevels.admin
            desc = "Show your skilling task, streak and points"
            cheat(::showTask)
        }
        onCommand("skilltaskset") {
            modLevel = modlevels.admin
            desc = "Assign a skilling task (::skilltaskset <key> [amount])"
            cheat(::setTask)
        }
        onCommand("skilltaskcomplete") {
            modLevel = modlevels.admin
            desc = "Complete your current skilling task"
            cheat(::completeTask)
        }
        onCommand("skilltaskpoints") {
            modLevel = modlevels.admin
            desc = "Add skilling points (::skilltaskpoints <n>)"
            cheat(::addPoints)
        }
    }

    private fun Player.openRewards(npc: Npc) {
        shops.open(
            player = this,
            activeNpc = npc,
            title = SHOP_TITLE,
            shopInv = skilling_task_invs.reward_shop,
            currency = skilling_task_currencies.skilling_points,
            subtext = "You have ${state.points(this)} skilling points.",
        )
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            val tasks = state[player]
            if (tasks.hasTask) {
                busy(npc)
            } else {
                idle(npc)
            }
        }

    private suspend fun Dialogue.idle(npc: Npc) {
        chatNpc(
            happy,
            "Fancy some work? I've got jobs that need doing, and I pay in experience and skilling points.",
        )
        val choice =
            choice3(
                "I'd like a task.",
                Choice.Task,
                "What can I buy with my points?",
                Choice.Rewards,
                "Not right now.",
                Choice.Nothing,
            )
        when (choice) {
            Choice.Task -> {
                chatPlayer(happy, "I'd like a task.")
                contract(null)
            }
            Choice.Rewards -> {
                chatPlayer(quiz, "What can I buy with my points?")
                player.openRewards(npc)
            }
            else -> chatPlayer(neutral, "Not right now.")
        }
    }

    private suspend fun Dialogue.busy(npc: Npc) {
        val tasks = state[player]
        val task = tasks.task ?: return idle(npc)
        chatNpc(
            neutral,
            "You're still on ${task.describe(tasks.target)}: ${tasks.progress} done, " +
                "${tasks.target - tasks.progress} to go.",
        )
        val choice =
            choice4(
                "I'm on it.",
                Choice.Nothing,
                "I'd like a different task.",
                Choice.Cancel,
                "What can I buy with my points?",
                Choice.Rewards,
                "How am I doing?",
                Choice.Status,
            )
        when (choice) {
            Choice.Cancel -> {
                chatPlayer(neutral, "I'd like a different task.")
                chatNpc(
                    confused,
                    "Giving up? I can hand you something else, but your streak of ${tasks.streak} " +
                        "goes back to nothing.",
                )
                val sure =
                    choice2(
                        "Yes, give me a different task.",
                        true,
                        "Never mind, I'll finish it.",
                        false,
                    )
                if (!sure) {
                    chatPlayer(neutral, "Never mind, I'll finish it.")
                    return
                }
                // Re-read: the task may have finished while the confirmation sat open.
                if (!state[player].hasTask) {
                    chatNpc(happy, "Looks like you finished it after all!")
                    return
                }
                val previous = state[player].taskKey
                state.cancel(player)
                contract(previous, kind = null)
            }
            Choice.Rewards -> {
                chatPlayer(quiz, "What can I buy with my points?")
                player.openRewards(npc)
            }
            Choice.Status -> {
                chatPlayer(quiz, "How am I doing?")
                chatNpc(
                    happy,
                    "You've finished ${tasks.completed} tasks, ${tasks.streak} in a row, and you have " +
                        "${tasks.points} skilling points to spend.",
                )
            }
            else -> chatPlayer(happy, "I'm on it.")
        }
    }

    /** The `Contract` op: report the task if there is one, hand one out otherwise. */
    private suspend fun Dialogue.contract(exclude: String?, kind: TaskKind? = null) {
        val tasks = state[player]
        val current = tasks.task
        if (tasks.hasTask && current != null) {
            chatNpc(
                neutral,
                "You're still on ${current.describe(tasks.target)}: ${tasks.progress} done, " +
                    "${tasks.target - tasks.progress} to go. Come back when you're finished.",
            )
            return
        }
        val task = assigner.roll(player, kind, exclude ?: tasks.lastTaskKey)
        if (task == null) {
            chatNpc(
                sad,
                "I've nothing that suits you just now. Come back when you've trained a bit.",
            )
            return
        }
        val amount = assigner.amount()
        state.assign(player, task, amount)
        chatNpc(happy, "Your task is to ${task.describe(amount)}. Off you go!")
        player.mes("New skilling task: ${task.describe(amount)}.")
    }

    /** The `Last-tier contract` op: another task in the same skill as the previous one. */
    private suspend fun Dialogue.lastTierContract() {
        val tasks = state[player]
        val previous = tasks.lastTaskKey?.let(SkillingTasks::find)
        contract(exclude = null, kind = previous?.kind)
    }

    private fun showTask(cheat: Cheat) =
        with(cheat) {
            val tasks = state.peek(player)
            if (tasks == null) {
                player.mes("No skilling task history.")
                return
            }
            val task = tasks.task
            val current =
                if (task != null && tasks.hasTask) {
                    "${task.key} ${tasks.progress}/${tasks.target}"
                } else {
                    "none"
                }
            player.mes(
                "Skilling task: $current; streak ${tasks.streak}; points ${tasks.points}; " +
                    "completed ${tasks.completed}; last ${tasks.lastTaskKey ?: "none"}."
            )
        }

    private fun setTask(cheat: Cheat) =
        with(cheat) {
            val key = args.firstOrNull()
            val task = key?.let(SkillingTasks::find)
            if (task == null) {
                player.mes(
                    "Unknown task key '${key ?: ""}'. Keys: ${SkillingTasks.all.joinToString { it.key }}"
                )
                return
            }
            val amount = args.getOrNull(1)?.toIntOrNull() ?: assigner.amount()
            state.assign(player, task, amount)
            player.mes("Skilling task set: ${task.describe(amount)}.")
        }

    private fun completeTask(cheat: Cheat) =
        with(cheat) {
            if (!state.forceComplete(player)) {
                player.mes("You have no skilling task.")
            }
        }

    private fun addPoints(cheat: Cheat) =
        with(cheat) {
            val amount = args.firstOrNull()?.toIntOrNull()
            if (amount == null) {
                player.mes("Usage: ::skilltaskpoints <n>")
                return
            }
            state.addPoints(player, amount)
            player.mes("You now have ${state.points(player)} skilling points.")
        }

    private enum class Choice {
        Task,
        Rewards,
        Nothing,
        Cancel,
        Status,
    }

    private companion object {
        const val SHOP_TITLE = "Skilling Rewards"
    }
}
