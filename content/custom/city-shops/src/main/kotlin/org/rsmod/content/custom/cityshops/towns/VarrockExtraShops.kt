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
 * Varrock's helmet shop, plus the two Champions' Guild shops just south-west of the city.
 *
 * Stock counts, restock rates and margins are OSRS's own. A count of 0 is not a mistake: vanilla
 * shops hold those lines empty until a player sells one, and the engine models that with a
 * zero-count stock obj.
 */
internal object VarrockExtraShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Peksa's Helmet Shop",
                inv = shop_invs.helmetshop,
                greeting = "Would you like to buy a helmet?",
                shop_npcs.peksa,
            ),
            shop(
                title = "Valaine's Shop of Champions",
                inv = shop_invs.adventurershop,
                greeting = "Can I interest you in an adventurer's kit?",
                shop_npcs.valaine,
            ),
            shop(
                title = "Scavvo's Rune Store",
                inv = shop_invs.runiteshop,
                greeting = "Would you like to buy some rune equipment?",
                shop_npcs.scavvo,
            ),
        )
}

internal object VarrockExtraShopsInvs : InvEditor() {
    init {
        // Peksa's Helmet Shop.
        edit(shop_invs.helmetshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_med_helm, count = 5, restockCycles = 100)
            stock += stock(shop_objs.iron_med_helm, count = 3, restockCycles = 200)
            stock += stock(shop_objs.steel_med_helm, count = 3, restockCycles = 400)
            stock += stock(shop_objs.mithril_med_helm, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.adamant_med_helm, count = 1, restockCycles = 12000)
            stock += stock(shop_objs.bronze_full_helm, count = 4, restockCycles = 100)
            stock += stock(shop_objs.iron_full_helm, count = 3, restockCycles = 200)
            stock += stock(shop_objs.steel_full_helm, count = 2, restockCycles = 400)
            stock += stock(shop_objs.mithril_full_helm, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.adamant_full_helm, count = 1, restockCycles = 12000)
        }

        // Valaine's Shop of Champions.
        edit(shop_invs.adventurershop) {
            specialistShop()
            stock += stock(shop_objs.blue_cape, count = 2, restockCycles = 1000)
            stock += stock(shop_objs.black_full_helm, count = 1, restockCycles = 1000)
            stock += stock(shop_objs.black_platelegs, count = 1, restockCycles = 20000)
            stock += stock(shop_objs.adamant_platebody, count = 1, restockCycles = 8000)
        }

        // Scavvo's Rune Store.
        edit(shop_invs.runiteshop) {
            specialistShop()
            stock += stock(shop_objs.rune_plateskirt, count = 1, restockCycles = 12000)
            stock += stock(objs.rune_platelegs, count = 1, restockCycles = 11500)
            stock += stock(shop_objs.rune_mace, count = 1, restockCycles = 7000)
            stock += stock(shop_objs.rune_chainbody, count = 1, restockCycles = 15000)
            stock += stock(shop_objs.rune_longsword, count = 1, restockCycles = 14000)
            stock += stock(shop_objs.rune_sword, count = 1, restockCycles = 10000)
            stock += stock(shop_objs.dragonhide_chaps, count = 1, restockCycles = 500)
            stock += stock(shop_objs.dragon_vambraces, count = 1, restockCycles = 500)
            stock += stock(shop_objs.coif, count = 2, restockCycles = 1000)
        }
    }
}
