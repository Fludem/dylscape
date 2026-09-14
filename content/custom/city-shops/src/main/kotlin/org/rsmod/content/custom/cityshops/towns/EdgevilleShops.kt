@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.towns

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.content.custom.cityshops.ShopAssignment
import org.rsmod.content.custom.cityshops.configs.shop_invs
import org.rsmod.content.custom.cityshops.configs.shop_npcs
import org.rsmod.content.custom.cityshops.configs.shop_objs
import org.rsmod.content.custom.cityshops.configs.specialistShop
import org.rsmod.content.custom.cityshops.shop

/**
 * Edgeville's food stall and potion stall, in the square north of the bank.
 *
 * Neither is OSRS's. Edgeville is the home hub here, and a player back from the Wilderness or a
 * slayer task had nowhere near the bank to buy food or a basic potion.
 *
 * Both stop at the entry tier on purpose. Cooked fish ends at lobster, and the potions are the
 * regular attack, strength and defence potions plus antipoison. Swordfish and up, super potions and
 * prayer potions are for players to catch, cook and brew, so Fishing, Cooking and Herblore keep
 * their reason to exist. Energy potions are left out because run energy is unlimited on this
 * server.
 *
 * Unlike the rest of this module, these are new spawns rather than stock edits, so they need
 * `packCache` locally and a cache rebuild on the live server before anyone can see them.
 */
internal object EdgevilleShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Edgeville Fishmonger",
                inv = shop_invs.misc_fishmonger,
                greeting = "Fresh fish, cooked and ready to eat!",
                shop_npcs.fishmonger,
            ),
            shop(
                title = "Huito's Potions",
                inv = shop_invs.cam_torum_shop_herbalist,
                greeting = "Need a potion for the road?",
                shop_npcs.huito,
            ),
        )
}

internal object EdgevilleShopsInvs : InvEditor() {
    init {
        // Edgeville Fishmonger. The cheap fish never run out; lobster, the best food on sale
        // anywhere on this server, comes back slowly enough that cooking your own still pays.
        edit(shop_invs.misc_fishmonger) {
            specialistShop()
            stock += stock(objs.shrimps, count = 200, restockCycles = 10)
            stock += stock(shop_objs.sardine, count = 200, restockCycles = 10)
            stock += stock(objs.herring, count = 200, restockCycles = 10)
            stock += stock(shop_objs.anchovies, count = 200, restockCycles = 10)
            stock += stock(shop_objs.trout, count = 150, restockCycles = 20)
            stock += stock(shop_objs.pike, count = 150, restockCycles = 20)
            stock += stock(shop_objs.salmon, count = 100, restockCycles = 30)
            stock += stock(shop_objs.tuna, count = 100, restockCycles = 30)
            stock += stock(shop_objs.lobster, count = 50, restockCycles = 60)
        }

        // Huito's Potions. Four-dose regular potions only.
        edit(shop_invs.cam_torum_shop_herbalist) {
            specialistShop()
            stock += stock(shop_objs.attack_potion, count = 30, restockCycles = 50)
            stock += stock(shop_objs.strength_potion, count = 30, restockCycles = 50)
            stock += stock(shop_objs.defence_potion, count = 30, restockCycles = 50)
            stock += stock(shop_objs.antipoison_potion, count = 30, restockCycles = 50)
        }
    }
}
