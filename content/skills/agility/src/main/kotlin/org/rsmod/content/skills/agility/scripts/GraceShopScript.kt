package org.rsmod.content.skills.agility.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.skills.agility.configs.AgilityCurrencies
import org.rsmod.content.skills.agility.configs.AgilityInvs
import org.rsmod.content.skills.agility.configs.AgilityNpcs
import org.rsmod.content.skills.agility.shop.MarkOfGraceShopOperations
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Grace, and the only shop in the game that deals in marks of grace.
 *
 * Registering the currency is what makes the shop work at all: `ShopScript` looks the operations up
 * by the shop's `CurrencyType` and logs an error and does nothing if there are none, so a shop
 * opened on an unregistered currency is a shop whose buttons are silently inert.
 *
 * Grace carries `Talk-to` on op1 and `Trade` on op3, verified against the cache rather than
 * assumed. She also carries `View Laps` (op4) and `Toggle Counter` (op5), which are the lap counter
 * added in 2020; neither is bound, because nothing tracks lifetime lap counts yet.
 */
public class GraceShopScript
@Inject
constructor(
    private val shops: Shops,
    private val operationMap: ShopOperationMap,
    private val markOperations: MarkOfGraceShopOperations,
) : PluginScript() {
    override fun ScriptContext.startup() {
        operationMap.register(AgilityCurrencies.mark_of_grace, markOperations)
        onOpNpc1(AgilityNpcs.grace) { graceDialogue(it.npc) }
        onOpNpc3(AgilityNpcs.grace) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(
            player = this,
            activeNpc = npc,
            title = SHOP_TITLE,
            shopInv = AgilityInvs.roguesden_shop,
            currency = AgilityCurrencies.mark_of_grace,
            subtext = SUBTEXT,
        )
    }

    private suspend fun ProtectedAccess.graceDialogue(npc: Npc) = startDialogue(npc) { grace(npc) }

    private suspend fun Dialogue.grace(npc: Npc) {
        chatNpc(happy, "Hello there. Been keeping fit, I hope?")
        val choice =
            choice2(
                "What have you got for my marks of grace?",
                1,
                "Just passing through, thanks.",
                2,
            )
        if (choice == 1) {
            player.openShop(npc)
        } else {
            chatPlayer(neutral, "Just passing through, thanks.")
        }
    }

    private companion object {
        const val SHOP_TITLE = "Grace's Graceful Clothing"

        /** The default subtext offers selling, which this shop does not do. */
        const val SUBTEXT = "Right click on shop to buy item"
    }
}
