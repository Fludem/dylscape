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
 * The rest of Asgarnia: Nurmof under the Dwarven Mine, and Taverley's two shops.
 *
 * Stock counts, restock rates and margins are OSRS's own. A count of 0 is not a mistake: vanilla
 * shops hold those lines empty until a player sells one, and the engine models that with a
 * zero-count stock obj.
 */
internal object AsgarnianShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Nurmof's Pickaxe Shop",
                inv = shop_invs.pickaxeshop,
                greeting = "Would you like to buy a pickaxe?",
                shop_npcs.nurmof,
            ),
            shop(
                title = "Jatix's Herblore Shop",
                inv = shop_invs.herbloreshop,
                greeting = "Do you want to buy any herblore supplies?",
                shop_npcs.jatix,
            ),
            shop(
                title = "Gaius' Two Handed Shop",
                inv = shop_invs.twohandedshop,
                greeting = "Would you like to buy a two-handed sword?",
                shop_npcs.gaius,
            ),
        )
}

internal object AsgarnianShopsInvs : InvEditor() {
    init {
        // Nurmof's Pickaxe Shop.
        edit(shop_invs.pickaxeshop) {
            specialistShop()
            stock += stock(objs.bronze_pickaxe, count = 6, restockCycles = 50)
            stock += stock(shop_objs.iron_pickaxe, count = 5, restockCycles = 100)
            stock += stock(shop_objs.steel_pickaxe, count = 4, restockCycles = 200)
            stock += stock(shop_objs.mithril_pickaxe, count = 3, restockCycles = 300)
            stock += stock(shop_objs.adamant_pickaxe, count = 2, restockCycles = 500)
            stock += stock(shop_objs.rune_pickaxe, count = 1, restockCycles = 700)
        }

        // Jatix's Herblore Shop.
        edit(shop_invs.herbloreshop) {
            specialistShop()
            stock += stock(shop_objs.vial_empty, count = 800, restockCycles = 100)
            stock += stock(shop_objs.pack_vial_empty, count = 800, restockCycles = 50)
            stock += stock(shop_objs.vial_water, count = 750, restockCycles = 100)
            stock += stock(shop_objs.pack_vial_water, count = 750, restockCycles = 50)
            stock += stock(shop_objs.pestle_and_mortar, count = 3, restockCycles = 100)
            stock += stock(shop_objs.eye_of_newt, count = 800, restockCycles = 20)
            stock += stock(shop_objs.pack_eye_newt, count = 100, restockCycles = 100)
        }

        // Gaius' Two Handed Shop.
        edit(shop_invs.twohandedshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_2h_sword, count = 4, restockCycles = 200)
            stock += stock(shop_objs.iron_2h_sword, count = 3, restockCycles = 300)
            stock += stock(shop_objs.steel_2h_sword, count = 2, restockCycles = 500)
            stock += stock(shop_objs.black_2h_sword, count = 1, restockCycles = 700)
            stock += stock(shop_objs.mithril_2h_sword, count = 1, restockCycles = 1000)
            stock += stock(shop_objs.adamant_2h_sword, count = 1, restockCycles = 15000)
        }
    }
}
