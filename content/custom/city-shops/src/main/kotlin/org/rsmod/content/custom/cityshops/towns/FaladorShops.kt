@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.towns

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.content.custom.cityshops.ShopAssignment
import org.rsmod.content.custom.cityshops.configs.shop_invs
import org.rsmod.content.custom.cityshops.configs.shop_npcs
import org.rsmod.content.custom.cityshops.configs.shop_objs
import org.rsmod.content.custom.cityshops.configs.specialistShop
import org.rsmod.content.custom.cityshops.shop

/**
 * Falador's four specialist shops, clustered around the north gate and the park.
 *
 * Margins are OSRS's own. Stock counts are not: vanilla holds a shop's upper tiers empty until a
 * player sells one in, which on a server with a handful of players means they stay empty forever.
 * Cassie's shields above iron and Herquin's gems above sapphire are stocked here so a low-level
 * player can actually buy them, and the slowest restock rates are brought down to match. A count of
 * 0 that remains is deliberate.
 */
internal object FaladorShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Cassie's Shield Shop",
                inv = shop_invs.shieldshop,
                greeting = "Would you like to buy a shield?",
                shop_npcs.cassie,
            ),
            shop(
                title = "Flynn's Mace Market",
                inv = shop_invs.maceshop,
                greeting = "Would you like to buy a mace?",
                shop_npcs.flynn,
            ),
            shop(
                title = "Herquin's Gems",
                inv = shop_invs.gemshop2,
                greeting = "Do you want to buy any gems?",
                shop_npcs.herquin,
            ),
            shop(
                title = "Wayne's Chains",
                inv = shop_invs.chainmailshop,
                greeting = "Would you like to buy some chainmail?",
                shop_npcs.wayne,
            ),
        )
}

internal object FaladorShopsInvs : InvEditor() {
    init {
        // Cassie's Shield Shop.
        edit(shop_invs.shieldshop) {
            specialistShop()
            stock += stock(shop_objs.wooden_shield, count = 5, restockCycles = 100)
            stock += stock(shop_objs.bronze_sq_shield, count = 3, restockCycles = 100)
            stock += stock(shop_objs.bronze_kiteshield, count = 3, restockCycles = 300)
            stock += stock(shop_objs.iron_sq_shield, count = 2, restockCycles = 400)
            stock += stock(shop_objs.iron_kiteshield, count = 2, restockCycles = 400)
            stock += stock(shop_objs.steel_sq_shield, count = 2, restockCycles = 600)
            stock += stock(shop_objs.steel_kiteshield, count = 2, restockCycles = 600)
            stock += stock(shop_objs.mithril_sq_shield, count = 1, restockCycles = 1500)
            stock += stock(shop_objs.mithril_kiteshield, count = 1, restockCycles = 1500)
            stock += stock(shop_objs.adamant_sq_shield, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.adamant_kiteshield, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.black_sq_shield, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.black_kiteshield, count = 1, restockCycles = 2000)
        }

        // Flynn's Mace Market.
        edit(shop_invs.maceshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_mace, count = 5, restockCycles = 100)
            stock += stock(shop_objs.iron_mace, count = 4, restockCycles = 200)
            stock += stock(shop_objs.steel_mace, count = 4, restockCycles = 400)
            stock += stock(shop_objs.mithril_mace, count = 3, restockCycles = 3000)
            stock += stock(shop_objs.adamant_mace, count = 2, restockCycles = 12000)
        }

        // Herquin's Gems.
        edit(shop_invs.gemshop2) {
            specialistShop()
            stock += stock(shop_objs.uncut_sapphire, count = 3, restockCycles = 1000)
            stock += stock(shop_objs.uncut_emerald, count = 2, restockCycles = 1000)
            stock += stock(shop_objs.uncut_ruby, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.uncut_diamond, count = 1, restockCycles = 4000)
            stock += stock(shop_objs.sapphire, count = 3, restockCycles = 1000)
            stock += stock(shop_objs.emerald, count = 2, restockCycles = 1000)
            stock += stock(shop_objs.ruby, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.diamond, count = 1, restockCycles = 4000)
        }

        // Wayne's Chains.
        edit(shop_invs.chainmailshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_chainbody, count = 3, restockCycles = 200)
            stock += stock(shop_objs.iron_chainbody, count = 2, restockCycles = 300)
            stock += stock(shop_objs.steel_chainbody, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.black_chainbody, count = 1, restockCycles = 2500)
            stock += stock(shop_objs.mithril_chainbody, count = 1, restockCycles = 3500)
            stock += stock(shop_objs.adamant_chainbody, count = 1, restockCycles = 9000)
        }
    }
}
