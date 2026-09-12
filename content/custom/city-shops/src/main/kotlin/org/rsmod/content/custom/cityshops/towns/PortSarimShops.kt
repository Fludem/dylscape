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
 * Port Sarim's row of shops along the docks. The town has no general store in OSRS; these five
 * cover food, runes, fishing gear, battleaxes and jewellery instead.
 *
 * Margins are OSRS's own. Stock counts are not: vanilla holds Wydin's bread, Gerrant's raw fish and
 * every one of Grum's fifteen lines empty until a player sells one in, so on this server they were
 * empty shelves. They are stocked here instead.
 *
 * Raw fish is stocked only up to lobster. Anything above that ceiling stays at 0 on purpose:
 * swordfish and up are meant to be caught and cooked, not bought.
 */
internal object PortSarimShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Wydin's Food Store",
                inv = shop_invs.foodshop,
                greeting = "Would you like to buy some food?",
                shop_npcs.wydin,
            ),
            shop(
                title = "Betty's Magic Emporium",
                inv = shop_invs.magicshop,
                greeting = "Do you want to buy some magical runes?",
                shop_npcs.betty,
            ),
            shop(
                title = "Gerrant's Fishy Business",
                inv = shop_invs.fishingshop,
                greeting = "Would you like to buy some fishing equipment?",
                shop_npcs.gerrant,
            ),
            shop(
                title = "Brian's Battleaxe Bazaar",
                inv = shop_invs.battleaxeshop,
                greeting = "Would you like to buy a battleaxe?",
                shop_npcs.brian,
            ),
            shop(
                title = "Grum's Gold Exchange",
                inv = shop_invs.goldshop,
                greeting = "Would you like to buy some jewellery?",
                shop_npcs.grum,
            ),
        )
}

internal object PortSarimShopsInvs : InvEditor() {
    init {
        // Wydin's Food Store.
        edit(shop_invs.foodshop) {
            specialistShop()
            stock += stock(shop_objs.pot_flour, count = 3, restockCycles = 100)
            stock += stock(shop_objs.raw_beef, count = 1, restockCycles = 100)
            stock += stock(shop_objs.raw_chicken, count = 1, restockCycles = 100)
            stock += stock(objs.cabbage, count = 3, restockCycles = 100)
            stock += stock(shop_objs.banana, count = 3, restockCycles = 100)
            stock += stock(shop_objs.redberries, count = 1, restockCycles = 100)
            stock += stock(shop_objs.bread, count = 5, restockCycles = 100)
            stock += stock(shop_objs.chocolate_bar, count = 1, restockCycles = 100)
            stock += stock(shop_objs.cheese, count = 3, restockCycles = 100)
            stock += stock(shop_objs.tomato, count = 3, restockCycles = 100)
            stock += stock(objs.potato, count = 1, restockCycles = 100)
        }

        // Betty's Magic Emporium.
        edit(shop_invs.magicshop) {
            specialistShop()
            stock += stock(objs.fire_rune, count = 5000, restockCycles = 10)
            stock += stock(objs.water_rune, count = 5000, restockCycles = 10)
            stock += stock(objs.air_rune, count = 5000, restockCycles = 10)
            stock += stock(objs.earth_rune, count = 5000, restockCycles = 10)
            stock += stock(objs.mind_rune, count = 5000, restockCycles = 10)
            stock += stock(shop_objs.body_rune, count = 5000, restockCycles = 10)
            stock += stock(objs.chaos_rune, count = 250, restockCycles = 10)
            stock += stock(objs.death_rune, count = 250, restockCycles = 15)
            stock += stock(shop_objs.pack_firerune, count = 80, restockCycles = 10)
            stock += stock(shop_objs.pack_waterrune, count = 80, restockCycles = 10)
            stock += stock(shop_objs.pack_airrune, count = 80, restockCycles = 10)
            stock += stock(shop_objs.pack_earthrune, count = 80, restockCycles = 10)
            stock += stock(shop_objs.pack_mindrune, count = 40, restockCycles = 10)
            stock += stock(shop_objs.pack_chaosrune, count = 35, restockCycles = 10)
            stock += stock(shop_objs.eye_of_newt, count = 300, restockCycles = 10)
            stock += stock(shop_objs.pack_eye_newt, count = 60, restockCycles = 50)
            stock += stock(shop_objs.bluewizhat, count = 3, restockCycles = 100)
            stock += stock(shop_objs.blackwizhat, count = 3, restockCycles = 100)
            // This cache has no wizard robe bottom to pair with it -- `wizards_robe` is the only
            // half that exists, so a starter mage buys the top here and finds legs elsewhere.
            stock += stock(shop_objs.wizards_robe, count = 3, restockCycles = 100)
        }

        // Gerrant's Fishy Business.
        // Not in this cache, so dropped: Bait pack.
        edit(shop_invs.fishingshop) {
            specialistShop()
            stock += stock(shop_objs.net, count = 5, restockCycles = 100)
            stock += stock(shop_objs.fishing_rod, count = 5, restockCycles = 100)
            stock += stock(shop_objs.fly_fishing_rod, count = 5, restockCycles = 100)
            stock += stock(shop_objs.harpoon, count = 2, restockCycles = 400)
            stock += stock(shop_objs.lobster_pot, count = 2, restockCycles = 400)
            stock += stock(shop_objs.fishing_bait, count = 1500, restockCycles = 1)
            stock += stock(shop_objs.feather, count = 1000, restockCycles = 1)
            stock += stock(shop_objs.pack_feather, count = 100, restockCycles = 2)
            stock += stock(shop_objs.raw_shrimp, count = 100, restockCycles = 10)
            stock += stock(shop_objs.raw_sardine, count = 200, restockCycles = 1)
            stock += stock(objs.raw_herring, count = 100, restockCycles = 20)
            stock += stock(shop_objs.raw_anchovies, count = 100, restockCycles = 30)
            stock += stock(shop_objs.raw_trout, count = 50, restockCycles = 50)
            stock += stock(shop_objs.raw_pike, count = 50, restockCycles = 60)
            stock += stock(shop_objs.raw_salmon, count = 30, restockCycles = 100)
            stock += stock(shop_objs.raw_tuna, count = 30, restockCycles = 150)
            stock += stock(shop_objs.raw_lobster, count = 20, restockCycles = 200)
            // Above the lobster ceiling: caught and cooked, not bought.
            stock += stock(shop_objs.raw_swordfish, count = 0, restockCycles = 2900)
        }

        // Brian's Battleaxe Bazaar.
        edit(shop_invs.battleaxeshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_battleaxe, count = 4, restockCycles = 200)
            stock += stock(objs.iron_battleaxe, count = 3, restockCycles = 300)
            stock += stock(objs.steel_battleaxe, count = 2, restockCycles = 500)
            stock += stock(shop_objs.black_battleaxe, count = 1, restockCycles = 700)
            stock += stock(objs.mithril_battleaxe, count = 1, restockCycles = 1000)
            stock += stock(shop_objs.adamant_battleaxe, count = 1, restockCycles = 15000)
        }

        // Grum's Gold Exchange.
        edit(shop_invs.goldshop) {
            specialistShop()
            stock += stock(shop_objs.gold_ring, count = 3, restockCycles = 200)
            stock += stock(shop_objs.sapphire_ring, count = 3, restockCycles = 300)
            stock += stock(shop_objs.emerald_ring, count = 2, restockCycles = 500)
            stock += stock(shop_objs.ruby_ring, count = 2, restockCycles = 800)
            stock += stock(shop_objs.diamond_ring, count = 1, restockCycles = 1500)
            stock += stock(shop_objs.gold_necklace, count = 3, restockCycles = 200)
            stock += stock(shop_objs.sapphire_necklace, count = 3, restockCycles = 300)
            stock += stock(shop_objs.emerald_necklace, count = 2, restockCycles = 500)
            stock += stock(shop_objs.ruby_necklace, count = 2, restockCycles = 800)
            stock += stock(shop_objs.diamond_necklace, count = 1, restockCycles = 1500)
            stock += stock(shop_objs.strung_gold_amulet, count = 3, restockCycles = 200)
            stock += stock(shop_objs.strung_sapphire_amulet, count = 3, restockCycles = 300)
            stock += stock(shop_objs.strung_emerald_amulet, count = 2, restockCycles = 500)
            stock += stock(shop_objs.strung_ruby_amulet, count = 2, restockCycles = 800)
            stock += stock(shop_objs.strung_diamond_amulet, count = 1, restockCycles = 1500)
        }
    }
}
