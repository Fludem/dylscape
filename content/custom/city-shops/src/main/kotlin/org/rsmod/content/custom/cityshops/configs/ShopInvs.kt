@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.refs.inv.InvReferences

internal typealias shop_invs = ShopInvs

/**
 * The shop invs the cache already names. They carry a `size` and nothing else: stock is an RSMod
 * extension (`.inv` codes 200-205) that vanilla never writes, so every shop's contents has to be
 * declared here or the shop opens empty.
 */
object ShopInvs : InvReferences() {
    val scimitarshop = find("scimitarshop")
    val skirtshop = find("skirtshop")
    val legsshop = find("legsshop")
    val craftingshop = find("craftingshop")
    val gemshop = find("gemshop")
    val runeshop = find("runeshop")
    val staffshop = find("staffshop")
    val archeryshop = find("archeryshop")
    val armourshop = find("armourshop")
    val clotheshop = find("clotheshop")
    val swordshop = find("swordshop")
    val generalshop2 = find("generalshop2")
    val generalshop3 = find("generalshop3")
    val generalshop4 = find("generalshop4")
    val generalshop5 = find("generalshop5")
    val generalshop6 = find("generalshop6")
    val generalshop7 = find("generalshop7")

    // Falador.
    val shieldshop = find("shieldshop")
    val maceshop = find("maceshop")
    val gemshop2 = find("gemshop2")
    val chainmailshop = find("chainmailshop")

    // Port Sarim.
    val foodshop = find("foodshop")
    val magicshop = find("magicshop")
    val fishingshop = find("fishingshop")
    val battleaxeshop = find("battleaxeshop")
    val goldshop = find("goldshop")

    // Varrock and the Champions' Guild.
    val helmetshop = find("helmetshop")
    val adventurershop = find("adventurershop")
    val runiteshop = find("runiteshop")

    // Rimmington, the Dwarven Mine and Taverley. `2handedshop` is not a legal Kotlin identifier,
    // so this is the one place a property name has to differ from the cache name.
    val craftingshop2 = find("craftingshop2")
    val pickaxeshop = find("pickaxeshop")
    val herbloreshop = find("herbloreshop")
    val twohandedshop = find("2handedshop")

    // Catherby, Yanille and Brimhaven.
    val archeryshop2 = find("archeryshop2")
    val fishingshop2 = find("fishingshop2")
    val candleshop = find("candleshop")
    val hunting_shop_yanille = find("hunting_shop_yanille")
    val cookeryshop = find("cookeryshop")
    val amuletshop = find("amuletshop")
}

internal object ShopInvBuilder : InvEditor() {
    init {
        // Zeke's Superior Scimitars.
        edit(shop_invs.scimitarshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_scimitar, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_scimitar, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_scimitar, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_scimitar, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_scimitar, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_scimitar, count = 1, restockCycles = 3000)
        }

        // Ranael's Super Skirt Store.
        edit(shop_invs.skirtshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_plateskirt, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_plateskirt, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_plateskirt, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_plateskirt, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_plateskirt, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_plateskirt, count = 1, restockCycles = 3000)
        }

        // Louie Legs' Leg Wear.
        edit(shop_invs.legsshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_platelegs, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_platelegs, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_platelegs, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_platelegs, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_platelegs, count = 2, restockCycles = 1500)
            stock += stock(objs.rune_platelegs, count = 1, restockCycles = 3000)
        }

        // Dommik's Crafting Store.
        edit(shop_invs.craftingshop) {
            specialistShop()
            stock += stock(objs.chisel, count = 5, restockCycles = 100)
            stock += stock(shop_objs.ring_mould, count = 5, restockCycles = 100)
            stock += stock(shop_objs.necklace_mould, count = 5, restockCycles = 100)
            stock += stock(shop_objs.amulet_mould, count = 5, restockCycles = 100)
            stock += stock(shop_objs.needle, count = 10, restockCycles = 50)
            stock += stock(shop_objs.thread, count = 100, restockCycles = 20)
        }

        // Gem trader. Uncut only -- cutting them is the player's problem.
        edit(shop_invs.gemshop) {
            specialistShop()
            stock += stock(shop_objs.uncut_opal, count = 10, restockCycles = 100)
            stock += stock(shop_objs.uncut_jade, count = 8, restockCycles = 150)
            stock += stock(shop_objs.uncut_red_topaz, count = 6, restockCycles = 200)
            stock += stock(shop_objs.uncut_sapphire, count = 5, restockCycles = 300)
            stock += stock(shop_objs.uncut_emerald, count = 4, restockCycles = 600)
            stock += stock(shop_objs.uncut_ruby, count = 3, restockCycles = 1200)
            stock += stock(shop_objs.uncut_diamond, count = 2, restockCycles = 2400)
        }

        // Aubury's Rune Shop. The elemental runes restock fast and deep; without a Runecrafting
        // implementation this is the only rune supply on the server.
        edit(shop_invs.runeshop) {
            specialistShop()
            stock += stock(objs.air_rune, count = 1000, restockCycles = 5)
            stock += stock(objs.water_rune, count = 1000, restockCycles = 5)
            stock += stock(objs.earth_rune, count = 1000, restockCycles = 5)
            stock += stock(objs.fire_rune, count = 1000, restockCycles = 5)
            stock += stock(objs.mind_rune, count = 1000, restockCycles = 5)
            stock += stock(shop_objs.body_rune, count = 1000, restockCycles = 5)
            stock += stock(objs.chaos_rune, count = 250, restockCycles = 30)
            stock += stock(objs.death_rune, count = 250, restockCycles = 30)
            stock += stock(shop_objs.cosmic_rune, count = 250, restockCycles = 30)
            stock += stock(objs.nature_rune, count = 250, restockCycles = 30)
            stock += stock(objs.law_rune, count = 250, restockCycles = 30)
        }

        // Zaff's Superior Staffs.
        edit(shop_invs.staffshop) {
            specialistShop()
            stock += stock(shop_objs.staff, count = 5, restockCycles = 100)
            stock += stock(shop_objs.magic_staff, count = 5, restockCycles = 100)
            stock += stock(objs.air_staff, count = 5, restockCycles = 200)
            stock += stock(shop_objs.staff_of_water, count = 5, restockCycles = 200)
            stock += stock(shop_objs.staff_of_earth, count = 5, restockCycles = 200)
            stock += stock(shop_objs.staff_of_fire, count = 5, restockCycles = 200)
            stock += stock(shop_objs.battlestaff, count = 5, restockCycles = 1500)
        }

        // Lowe's Archery Emporium.
        edit(shop_invs.archeryshop) {
            specialistShop()
            stock += stock(objs.bronze_arrow, count = 250, restockCycles = 10)
            stock += stock(shop_objs.iron_arrow, count = 150, restockCycles = 20)
            stock += stock(shop_objs.steel_arrow, count = 100, restockCycles = 40)
            stock += stock(shop_objs.mithril_arrow, count = 50, restockCycles = 80)
            stock += stock(objs.adamant_arrow, count = 25, restockCycles = 160)
            stock += stock(objs.rune_arrow, count = 10, restockCycles = 320)
            stock += stock(objs.shortbow, count = 5, restockCycles = 100)
            stock += stock(shop_objs.longbow, count = 5, restockCycles = 100)
            stock += stock(shop_objs.oak_shortbow, count = 4, restockCycles = 200)
            stock += stock(shop_objs.oak_longbow, count = 4, restockCycles = 200)
            stock += stock(shop_objs.willow_shortbow, count = 3, restockCycles = 400)
            stock += stock(shop_objs.willow_longbow, count = 3, restockCycles = 400)
            stock += stock(shop_objs.maple_shortbow, count = 2, restockCycles = 800)
            stock += stock(shop_objs.maple_longbow, count = 2, restockCycles = 800)
            stock += stock(shop_objs.yew_shortbow, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.yew_longbow, count = 1, restockCycles = 2000)
            stock += stock(shop_objs.crossbow, count = 5, restockCycles = 100)
            stock += stock(shop_objs.bronze_bolts, count = 250, restockCycles = 10)
        }

        // Horvik's Armour Shop.
        edit(shop_invs.armourshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_chainbody, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_chainbody, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_chainbody, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_chainbody, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_chainbody, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_chainbody, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.bronze_platebody, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_platebody, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_platebody, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_platebody, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_platebody, count = 2, restockCycles = 1500)
            stock += stock(objs.rune_platebody, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.leather_armour, count = 5, restockCycles = 100)
            stock += stock(shop_objs.studded_body, count = 2, restockCycles = 600)
        }

        // Varrock Swordshop.
        edit(shop_invs.swordshop) {
            specialistShop()
            stock += stock(shop_objs.bronze_sword, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_sword, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_sword, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_sword, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_sword, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_sword, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.bronze_longsword, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_longsword, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_longsword, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_longsword, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_longsword, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_longsword, count = 1, restockCycles = 3000)
            stock += stock(shop_objs.bronze_dagger, count = 10, restockCycles = 100)
            stock += stock(shop_objs.iron_dagger, count = 5, restockCycles = 150)
            stock += stock(shop_objs.steel_dagger, count = 4, restockCycles = 300)
            stock += stock(shop_objs.mithril_dagger, count = 3, restockCycles = 600)
            stock += stock(shop_objs.adamant_dagger, count = 2, restockCycles = 1500)
            stock += stock(shop_objs.rune_dagger, count = 1, restockCycles = 3000)
        }

        // Thessalia's Fine Clothes.
        edit(shop_invs.clotheshop) {
            specialistShop()
            stock += stock(shop_objs.pink_skirt, count = 5, restockCycles = 100)
            stock += stock(shop_objs.black_skirt, count = 5, restockCycles = 100)
            stock += stock(shop_objs.blue_skirt, count = 5, restockCycles = 100)
            stock += stock(shop_objs.red_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.blue_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.yellow_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.green_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.purple_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.orange_cape, count = 5, restockCycles = 100)
            stock += stock(shop_objs.black_cape, count = 5, restockCycles = 100)
        }

        val generalShops =
            listOf(
                shop_invs.generalshop2,
                shop_invs.generalshop3,
                shop_invs.generalshop4,
                shop_invs.generalshop5,
                shop_invs.generalshop6,
                shop_invs.generalshop7,
            )
        for (generalShop in generalShops) {
            edit(generalShop) { generalStore() }
        }
    }
}
