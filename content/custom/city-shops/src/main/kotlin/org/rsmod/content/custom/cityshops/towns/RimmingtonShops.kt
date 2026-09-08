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
 * Rimmington. The general store is wired through `generalshop7`; this is the town's other shop.
 *
 * Stock counts, restock rates and margins are OSRS's own. A count of 0 is not a mistake: vanilla
 * shops hold those lines empty until a player sells one, and the engine models that with a
 * zero-count stock obj.
 */
internal object RimmingtonShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Rommik's Crafty Supplies",
                inv = shop_invs.craftingshop2,
                greeting = "Do you want to buy any crafting equipment?",
                shop_npcs.rommik,
            )
        )
}

internal object RimmingtonShopsInvs : InvEditor() {
    init {
        // Rommik's Crafty Supplies.
        edit(shop_invs.craftingshop2) {
            specialistShop()
            stock += stock(objs.chisel, count = 2, restockCycles = 100)
            stock += stock(shop_objs.ring_mould, count = 4, restockCycles = 100)
            stock += stock(shop_objs.necklace_mould, count = 2, restockCycles = 100)
            stock += stock(shop_objs.amulet_mould, count = 2, restockCycles = 100)
            stock += stock(shop_objs.needle, count = 3, restockCycles = 100)
            stock += stock(shop_objs.thread, count = 100, restockCycles = 5)
            stock += stock(shop_objs.holy_symbol_mould, count = 3, restockCycles = 100)
            stock += stock(shop_objs.sickle_mould, count = 6, restockCycles = 15)
            stock += stock(shop_objs.tiara_mould, count = 10, restockCycles = 10)
            stock += stock(shop_objs.xbows_silver_bolt_mould, count = 10, restockCycles = 10)
            stock += stock(shop_objs.jewl_bracelet_mould, count = 5, restockCycles = 100)
        }
    }
}
