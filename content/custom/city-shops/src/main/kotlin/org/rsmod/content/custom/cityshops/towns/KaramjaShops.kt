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
 * Karamja. The Musa Point general store is wired through `generalshop5`; Davon's is across the
 * island in Brimhaven.
 *
 * Margins are OSRS's own. Stock counts are not: vanilla holds all four amulets empty until a player
 * sells one in, so on this server the shop never had anything to sell. They are stocked here
 * instead.
 */
internal object KaramjaShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Davon's Amulet Store",
                inv = shop_invs.amuletshop,
                greeting = "Would you like to buy an amulet?",
                shop_npcs.davon,
            )
        )
}

internal object KaramjaShopsInvs : InvEditor() {
    init {
        // Davon's Amulet Store.
        // Not in this cache, so dropped: Holy symbol.
        edit(shop_invs.amuletshop) {
            specialistShop()
            stock += stock(shop_objs.amulet_of_magic, count = 3, restockCycles = 200)
            stock += stock(shop_objs.amulet_of_defence, count = 3, restockCycles = 200)
            stock += stock(shop_objs.amulet_of_strength, count = 2, restockCycles = 300)
            stock += stock(shop_objs.amulet_of_power, count = 2, restockCycles = 300)
        }
    }
}
