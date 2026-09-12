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
 * Kandarin: Catherby's three shops and Yanille's two.
 *
 * Margins are OSRS's own. Stock counts are not: vanilla holds Hickton's arrows above iron and every
 * one of Harry's eleven fish lines empty until a player sells one in, so on this server they were
 * empty shelves. They are stocked here instead.
 *
 * One ceiling is kept on purpose, and the fish lines that stay at 0 are deliberate: raw fish is
 * stocked only up to lobster. Arrows carry no such cap -- Lowe's in Varrock already sells the full
 * ladder, and ammo is consumed rather than kept, so capping it here would only be inconsistent.
 */
internal object KandarinShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Hickton's Archery Emporium",
                inv = shop_invs.archeryshop2,
                greeting = "Would you like to buy a bow or some arrows?",
                shop_npcs.hickton,
            ),
            shop(
                title = "Harry's Fishing Shop",
                inv = shop_invs.fishingshop2,
                greeting = "Would you like to buy some fishing equipment?",
                shop_npcs.harry,
            ),
            shop(
                title = "Candle Shop",
                inv = shop_invs.candleshop,
                greeting = "Would you like to buy a candle?",
                shop_npcs.candle_maker,
            ),
            shop(
                title = "Aleck's Hunter Emporium",
                inv = shop_invs.hunting_shop_yanille,
                greeting = "Would you like to buy some hunting equipment?",
                shop_npcs.aleck,
                // The only shopkeeper on the server whose `Trade` is not op3.
                tradeOp = 5,
            ),
            shop(
                title = "Frenita's Cookery Shop",
                inv = shop_invs.cookeryshop,
                greeting = "Would you like to buy some cookery equipment?",
                shop_npcs.frenita,
            ),
        )
}

internal object KandarinShopsInvs : InvEditor() {
    init {
        // Hickton's Archery Emporium.
        // Not in this cache, so dropped: Bronze brutal, Iron brutal, Steel brutal, Black brutal,
        // Mithril brutal, Adamant brutal, Rune brutal, Comp ogre bow, Fletching cape, Fletching
        // cape(t).
        edit(shop_invs.archeryshop2) {
            specialistShop()
            stock += stock(shop_objs.bronze_bolts, count = 200, restockCycles = 10)
            stock += stock(objs.bronze_arrow, count = 1000, restockCycles = 10)
            stock += stock(shop_objs.iron_arrow, count = 800, restockCycles = 20)
            stock += stock(shop_objs.steel_arrow, count = 600, restockCycles = 40)
            stock += stock(shop_objs.mithril_arrow, count = 400, restockCycles = 80)
            stock += stock(objs.adamant_arrow, count = 200, restockCycles = 150)
            stock += stock(objs.rune_arrow, count = 100, restockCycles = 300)
            stock += stock(shop_objs.bronze_arrowheads, count = 1000, restockCycles = 10)
            stock += stock(shop_objs.iron_arrowheads, count = 800, restockCycles = 20)
            stock += stock(shop_objs.steel_arrowheads, count = 600, restockCycles = 40)
            stock += stock(shop_objs.mithril_arrowheads, count = 400, restockCycles = 40)
            stock += stock(shop_objs.adamant_arrowheads, count = 200, restockCycles = 40)
            stock += stock(shop_objs.rune_arrowheads, count = 100, restockCycles = 40)
            stock += stock(objs.shortbow, count = 4, restockCycles = 100)
            stock += stock(shop_objs.longbow, count = 2, restockCycles = 200)
            stock += stock(shop_objs.crossbow, count = 2, restockCycles = 400)
            stock += stock(shop_objs.oak_shortbow, count = 4, restockCycles = 800)
            stock += stock(shop_objs.oak_longbow, count = 4, restockCycles = 1600)
            stock += stock(shop_objs.studded_body, count = 2, restockCycles = 1000)
            stock += stock(shop_objs.studded_chaps, count = 2, restockCycles = 1000)
        }

        // Harry's Fishing Shop.
        // Not in this cache, so dropped: Bait pack.
        edit(shop_invs.fishingshop2) {
            specialistShop()
            stock += stock(shop_objs.net, count = 5, restockCycles = 100)
            stock += stock(shop_objs.fishing_rod, count = 5, restockCycles = 100)
            stock += stock(shop_objs.harpoon, count = 2, restockCycles = 400)
            stock += stock(shop_objs.lobster_pot, count = 2, restockCycles = 400)
            stock += stock(shop_objs.fishing_bait, count = 1200, restockCycles = 1)
            stock += stock(shop_objs.big_net, count = 5, restockCycles = 100)
            stock += stock(shop_objs.raw_shrimp, count = 100, restockCycles = 10)
            stock += stock(shop_objs.raw_sardine, count = 100, restockCycles = 20)
            stock += stock(objs.raw_herring, count = 100, restockCycles = 20)
            stock += stock(shop_objs.raw_mackerel, count = 50, restockCycles = 40)
            stock += stock(shop_objs.raw_cod, count = 50, restockCycles = 50)
            stock += stock(shop_objs.raw_anchovies, count = 100, restockCycles = 30)
            stock += stock(shop_objs.raw_tuna, count = 30, restockCycles = 150)
            stock += stock(shop_objs.raw_lobster, count = 20, restockCycles = 200)
            // Above the lobster ceiling: caught and cooked, not bought.
            stock += stock(shop_objs.raw_bass, count = 0, restockCycles = 2700)
            stock += stock(shop_objs.raw_swordfish, count = 0, restockCycles = 2900)
            stock += stock(shop_objs.raw_shark, count = 0, restockCycles = 3500)
        }

        // Candle Shop.
        edit(shop_invs.candleshop) {
            specialistShop()
            stock += stock(shop_objs.unlit_candle, count = 10, restockCycles = 100)
        }

        // Aleck's Hunter Emporium.
        // Not in this cache, so dropped: Magic box, Rabbit snare, Magic imp box pack.
        edit(shop_invs.hunting_shop_yanille) {
            specialistShop()
            stock += stock(shop_objs.hunting_butterfly_net, count = 5, restockCycles = 10)
            stock += stock(shop_objs.butterfly_jar, count = 100, restockCycles = 10)
            stock += stock(shop_objs.noose_wand, count = 50, restockCycles = 5)
            stock += stock(shop_objs.hunting_ojibway_bird_snare, count = 50, restockCycles = 5)
            stock += stock(shop_objs.hunting_box_trap, count = 25, restockCycles = 10)
            stock += stock(shop_objs.hunting_teasing_stick, count = 5, restockCycles = 10)
            stock += stock(shop_objs.torch_unlit, count = 20, restockCycles = 10)
            stock += stock(shop_objs.pack_ojibway_bird_snare, count = 3, restockCycles = 10)
            stock += stock(shop_objs.pack_box_trap, count = 3, restockCycles = 10)
        }

        // Frenita's Cookery Shop.
        edit(shop_invs.cookeryshop) {
            specialistShop()
            stock += stock(shop_objs.piedish, count = 5, restockCycles = 200)
            stock += stock(shop_objs.cooking_apple, count = 2, restockCycles = 200)
            stock += stock(objs.cake_tin, count = 2, restockCycles = 200)
            stock += stock(objs.bowl_empty, count = 2, restockCycles = 200)
            stock += stock(objs.potato, count = 5, restockCycles = 200)
            stock += stock(objs.tinderbox, count = 4, restockCycles = 200)
            stock += stock(objs.jug_empty, count = 1, restockCycles = 100)
            stock += stock(objs.pack_jug_empty, count = 3, restockCycles = 15)
            stock += stock(objs.pot_empty, count = 8, restockCycles = 100)
            stock += stock(shop_objs.chocolate_bar, count = 2, restockCycles = 100)
            stock += stock(shop_objs.pot_flour, count = 8, restockCycles = 100)
            stock += stock(shop_objs.cup_empty, count = 20, restockCycles = 50)
        }
    }
}
