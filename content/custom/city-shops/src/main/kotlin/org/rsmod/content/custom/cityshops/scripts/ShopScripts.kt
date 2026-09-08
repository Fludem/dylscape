package org.rsmod.content.custom.cityshops.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.content.custom.cityshops.ShopAssignment
import org.rsmod.content.custom.cityshops.ShopAssignments
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Opens a stocked shop for every npc in [ShopAssignments].
 *
 * `Talk-to` is op1 on all of them and `Trade` is op3 on nearly all of them — but not quite all, so
 * the slot comes from [ShopAssignment.tradeOp] rather than being assumed. `ShopAssignmentsTest`
 * checks each npc's ops against the cache.
 *
 * Buy/sell margins are npc params, set per shop from OSRS's own multipliers in
 * [org.rsmod.content.custom.cityshops.configs.ShopMargins]; anything not listed there keeps the
 * defaults in [org.rsmod.api.shops.config.ShopParamBuilder].
 */
class ShopScripts @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (assignment in ShopAssignments.all) {
            for (npc in assignment.npcs) {
                onOpNpc1(npc) { shopDialogue(it.npc, assignment) }
                when (assignment.tradeOp) {
                    3 -> onOpNpc3(npc) { player.openShop(it.npc, assignment) }
                    5 -> onOpNpc5(npc) { player.openShop(it.npc, assignment) }
                    else -> error("Unsupported trade op for '${assignment.title}'.")
                }
            }
        }
    }

    private fun Player.openShop(npc: Npc, assignment: ShopAssignment) {
        shops.open(this, npc, assignment.title, assignment.inv)
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc, assignment: ShopAssignment) =
        startDialogue(npc) { shopKeeper(npc, assignment) }

    private suspend fun Dialogue.shopKeeper(npc: Npc, assignment: ShopAssignment) {
        chatNpc(happy, assignment.greeting)
        val choice = choice2("Yes please. What are you selling?", 1, "No thanks.", 2)
        if (choice == 1) {
            player.openShop(npc, assignment)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }
}
