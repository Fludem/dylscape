@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.towns

import org.rsmod.api.shops.config.ShopParams
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.content.custom.cityshops.ShopAssignment
import org.rsmod.content.custom.cityshops.configs.specialistShop
import org.rsmod.content.custom.cityshops.shop
import org.rsmod.content.skills.magic.spellbooks.configs.Teletabs

/**
 * Wizard Akutha's teleport tablets, north of the Edgeville bank beside the spellbook altars.
 *
 * Ours rather than OSRS's: no shop in the game sells teleport tablets. Players make their own on a
 * lectern in their house, and this server has no houses.
 *
 * Akutha is the cache's `magic_store_owner` ("A Supplier of Magical items."). He already carries
 * `Talk-to` on op1 and `Trade` on op3, and nothing spawns him anywhere, so he can be borrowed.
 *
 * He stocks exactly [Teletabs.all], the list `TeletabScript` in `magic-spellbooks` binds `Break`
 * on, so the counter can never sell a tablet that does nothing when broken. `TeletabShopTest` holds
 * the stock and the price.
 */
internal object TeletabShop {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Akutha's Teleport Tablets",
                inv = TeletabShopInvs.dueldisplay_dummy,
                greeting = "Teleport tablets! Break one and you're there.",
                TeletabShopNpcs.wizard_akutha,
            )
        )
}

object TeletabShopNpcs : NpcReferences() {
    val wizard_akutha = find("magic_store_owner")
}

/**
 * `dueldisplay_dummy` is a cache-named inv that no shop stocks and nothing on this server uses, the
 * same trade `herbloreshop2` makes for Primula, so no new inv type has to be packed.
 */
object TeletabShopInvs : InvReferences() {
    val dueldisplay_dummy = find("dueldisplay_dummy")
}

/**
 * Pins Akutha to his tile and fixes his margins: `sell = 1000` is 100% of [TELETAB_COST], and
 * `change = 0` keeps that price the same whether a tablet is the first off the shelf or the last.
 */
internal object TeletabShopNpcEditor : NpcEditor() {
    init {
        edit(TeletabShopNpcs.wizard_akutha) {
            wanderRange = 0
            param[ShopParams.shop_sell_percentage] = 1000
            param[ShopParams.shop_buy_percentage] = 600
            param[ShopParams.shop_change_percentage] = 0
        }
    }
}

/**
 * Prices every tablet at [TELETAB_COST].
 *
 * The cache gives every tablet a cost of 1, so at any margin the shop would sell them for a coin
 * each. Moving the cost is the only way to reach a real price, and it keeps alchemy and sell-back
 * values in line with it. An editor applies on a normal boot, with no `packCache`, but the edit is
 * permanent once written: deleting this will not put 1 back.
 */
internal object TeletabCostEditor : ObjEditor() {
    init {
        for (teletab in Teletabs.all) {
            edit(teletab.tab) { cost = TELETAB_COST }
        }
    }
}

internal object TeletabShopInvEditor : InvEditor() {
    init {
        // Tablets are bought by the stack, so the shelf is deep and refills one of each per tick.
        edit(TeletabShopInvs.dueldisplay_dummy) {
            specialistShop()
            for (teletab in Teletabs.all) {
                stock += stock(teletab.tab, count = TELETAB_STOCK, restockCycles = 1)
            }
        }
    }
}

private const val TELETAB_COST = 1_000
private const val TELETAB_STOCK = 500
