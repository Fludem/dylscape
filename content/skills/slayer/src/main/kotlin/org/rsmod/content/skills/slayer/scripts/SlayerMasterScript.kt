package org.rsmod.content.skills.slayer.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.skills.slayer.SlayerProgress.hasSlayerTask
import org.rsmod.content.skills.slayer.SlayerProgress.slayerAssigned
import org.rsmod.content.skills.slayer.SlayerProgress.slayerMaster
import org.rsmod.content.skills.slayer.SlayerProgress.slayerRemaining
import org.rsmod.content.skills.slayer.SlayerProgress.slayerTask
import org.rsmod.content.skills.slayer.configs.slayer_npcs
import org.rsmod.content.skills.slayer.data.SlayerAssigner
import org.rsmod.content.skills.slayer.data.SlayerTaskRepository
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The six slayer masters.
 *
 * Op slots were decoded from the cache rather than assumed, and all six masters agree: `Talk-to` on
 * op1, nothing on op2, `Assignment` on op3, `Trade` on op4, `Rewards` on op5. Having a dedicated
 * `Assignment` op is why getting a task does not need to go through a dialogue menu - the vanilla
 * right-click already offers it.
 *
 * `Trade` and `Rewards` are deliberately not bound yet; they arrive with the reward shop.
 */
class SlayerMasterScript
@Inject
constructor(private val repo: SlayerTaskRepository, private val assigner: SlayerAssigner) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for ((masterId, master) in slayer_npcs.byMasterId) {
            onOpNpc1(master) { greet(it.npc, masterId) }
            onOpNpc3(master) { assignment(it.npc, masterId) }
        }
    }

    private suspend fun ProtectedAccess.greet(npc: Npc, masterId: Int) {
        startDialogue(npc) {
            chatNpc(neutral, "'Ere, what are you after?")
            val wantsTask = choice2("I need an assignment.", true, "Nothing, thanks.", false)
            if (!wantsTask) {
                chatPlayer(neutral, "Nothing, thanks.")
                return@startDialogue
            }
            chatPlayer(quiz, "I need an assignment.")
            offerTask(masterId)
        }
    }

    /**
     * The `Assignment` op. Reports the current task if there is one and hands out a new one
     * otherwise, which is what the vanilla right-click does in both states.
     */
    private suspend fun ProtectedAccess.assignment(npc: Npc, masterId: Int) {
        startDialogue(npc) { offerTask(masterId) }
    }

    private suspend fun Dialogue.offerTask(masterId: Int) {
        val player = access.player
        if (player.hasSlayerTask) {
            val task = repo.task(player.slayerTask)
            val name = task?.name ?: "something"
            chatNpc(
                neutral,
                "You're still hunting $name; you have ${player.slayerRemaining} to go. " +
                    "Come back when you're done.",
            )
            return
        }

        val assignment = assigner.roll(player, masterId)
        if (assignment == null) {
            // Reachable: a low-level player at a high-level master really can have nothing on the
            // list they qualify for.
            chatNpc(neutral, "You're not experienced enough for anything I have. Try someone else.")
            return
        }

        val amount = assigner.amount(assignment)
        player.slayerTask = assignment.task.id
        player.slayerRemaining = amount
        player.slayerAssigned = amount
        player.slayerMaster = masterId

        chatNpc(neutral, "Your new task is to kill $amount ${assignment.task.name}. Off you go.")
    }
}
