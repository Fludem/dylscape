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
 * Stock counts, restock rates and margins are OSRS's own. A count of 0 is not a mistake: vanilla
 * shops hold those lines empty until a player sells one, and the engine models that with a
 * zero-count stock obj.
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
            stock += stock(shop_objs.amulet_of_magic, count = 0, restockCycles = 200)
            stock += stock(shop_objs.amulet_of_defence, count = 0, restockCycles = 200)
            stock += stock(shop_objs.amulet_of_strength, count = 0, restockCycles = 200)
            stock += stock(shop_objs.amulet_of_power, count = 0, restockCycles = 200)
        }
    }
}
