@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.cityshops.towns

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.content.custom.cityshops.ShopAssignment
import org.rsmod.content.custom.cityshops.configs.shop_invs
import org.rsmod.content.custom.cityshops.configs.shop_npcs
import org.rsmod.content.custom.cityshops.configs.shop_objs
import org.rsmod.content.custom.cityshops.configs.specialistShop
import org.rsmod.content.custom.cityshops.shop
import org.rsmod.game.type.obj.ObjType

/**
 * Edgeville's herblore counter and its skillcape counter.
 *
 * Unlike every other shop in this module, this one is ours rather than OSRS's: there is no
 * secondaries shop in Edgeville, and until now there was no secondaries shop anywhere. Jatix in
 * Taverley sells vials, a pestle and eye of newt, Betty in Port Sarim sells eye of newt, and that
 * was the whole supply line — so Herblore stopped dead at attack potions unless you farmed or
 * killed for every ingredient yourself.
 *
 * The skillcape counter is ours too, and has no OSRS counterpart at all: vanilla sells each cape
 * from its own skill master, scattered across the map behind guild doors this server does not yet
 * open. One shop in Edgeville replaces all 23 of them.
 *
 * Both reuse a cache-named inv vanilla never stocks -- `herbloreshop2` and `omnishop_inv_temp` --
 * so no new type is packed for either.
 */
internal object MisthalinShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Primula's Herblore Supplies",
                inv = shop_invs.herbloreshop2,
                greeting = "Do you need any potion ingredients?",
                shop_npcs.primula,
            ),
            shop(
                title = "Jack's Capes of Accomplishment",
                inv = shop_invs.omnishop_inv_temp,
                greeting = "Mastered a skill? I have the cape to prove it.",
                shop_npcs.jack,
            ),
        )
}

/**
 * Every cape Jack sells, in skill order.
 *
 * One list feeds both the stock below and [SkillcapeCostEditor], so a cape can never be priced
 * without being sold or sold without being priced.
 */
internal val SKILLCAPES: List<ObjType> =
    listOf(
        shop_objs.skillcape_attack,
        shop_objs.skillcape_defence,
        shop_objs.skillcape_strength,
        shop_objs.skillcape_hitpoints,
        shop_objs.skillcape_ranging,
        shop_objs.skillcape_prayer,
        shop_objs.skillcape_magic,
        shop_objs.skillcape_cooking,
        shop_objs.skillcape_woodcutting,
        shop_objs.skillcape_fletching,
        shop_objs.skillcape_fishing,
        shop_objs.skillcape_firemaking,
        shop_objs.skillcape_crafting,
        shop_objs.skillcape_smithing,
        shop_objs.skillcape_mining,
        shop_objs.skillcape_herblore,
        shop_objs.skillcape_agility,
        shop_objs.skillcape_thieving,
        shop_objs.skillcape_slayer,
        shop_objs.skillcape_farming,
        shop_objs.skillcape_runecrafting,
        shop_objs.skillcape_hunting,
        shop_objs.skillcape_construction,
    )

/**
 * Prices every skillcape at a flat 100,000gp.
 *
 * The shop's asking price is `obj.cost * sell%`, and OSRS gives these capes a cost of 99,000 -- so
 * there is no whole tenth of a percent that lands on a round 100,000. Moving the cost itself is the
 * only way to hit the number exactly, and it is the honest place for it: 100,000gp is what a cape
 * is worth on this server, and the high-alchemy and examine values should say so too.
 *
 * Only the 23 untrimmed capes Jack stocks are touched. Trimmed capes, hoods, the quest-point cape
 * and the max cape keep their cache values.
 *
 * This is an editor, so it applies on a normal boot -- no `packCache`. It is also additive and
 * permanent once written: deleting it will not put 99,000 back.
 */
internal object SkillcapeCostEditor : ObjEditor() {
    init {
        for (cape in SKILLCAPES) {
            edit(cape) { cost = SKILLCAPE_COST }
        }
    }
}

private const val SKILLCAPE_COST = 100_000

internal object MisthalinShopsInvs : InvEditor() {
    init {
        // Primula's Herblore Supplies. Counts and restock rates are ours — there is no vanilla
        // shop to copy — and are set so the cheap early secondaries never run dry while the ones
        // that gate a level milestone come back slowly enough to still be worth farming.
        //
        // Only the raw ingredients are stocked, never their ground forms: the pestle is on the
        // same counter, so selling `unicorn_horn_dust` alongside `unicorn_horn` would just be
        // paying to skip a click. `amylase` and `sote_crystal_dust` are the exceptions, because
        // neither has a precursor anything sells.
        edit(shop_invs.herbloreshop2) {
            specialistShop()

            // Vials and the grinder. Deliberately the same counts as Jatix's, so neither shop is
            // the obvious one to run to.
            stock += stock(shop_objs.vial_empty, count = 800, restockCycles = 100)
            stock += stock(shop_objs.pack_vial_empty, count = 800, restockCycles = 50)
            stock += stock(shop_objs.vial_water, count = 750, restockCycles = 100)
            stock += stock(shop_objs.pack_vial_water, count = 750, restockCycles = 50)
            stock += stock(shop_objs.pestle_and_mortar, count = 5, restockCycles = 100)

            // The cheap ones, for potions in the first twenty levels or so.
            stock += stock(shop_objs.eye_of_newt, count = 800, restockCycles = 20)
            stock += stock(shop_objs.pack_eye_newt, count = 100, restockCycles = 100)
            stock += stock(shop_objs.ashes, count = 500, restockCycles = 20)
            stock += stock(shop_objs.garlic, count = 500, restockCycles = 20)
            stock += stock(shop_objs.red_spiders_eggs, count = 500, restockCycles = 25)
            stock += stock(shop_objs.limpwurt_root, count = 500, restockCycles = 25)
            stock += stock(shop_objs.chocolate_dust, count = 500, restockCycles = 25)

            // Mid-level. `cactus_potato` is potato cactus and `mortmyremushroom` is mort myre
            // fungus; both are the cache's spelling, not a typo.
            stock += stock(shop_objs.white_berries, count = 300, restockCycles = 40)
            stock += stock(shop_objs.snape_grass, count = 300, restockCycles = 40)
            stock += stock(shop_objs.toads_legs, count = 300, restockCycles = 40)
            stock += stock(shop_objs.jangerberries, count = 300, restockCycles = 40)
            stock += stock(shop_objs.cactus_potato, count = 250, restockCycles = 50)
            stock += stock(shop_objs.mortmyremushroom, count = 250, restockCycles = 50)
            stock += stock(shop_objs.poisonivy_berries, count = 250, restockCycles = 50)

            // The ones a potion actually waits on. `huntingbeast_sabreteeth` is kebbit teeth and
            // `amylase` is an amylase crystal.
            stock += stock(shop_objs.unicorn_horn, count = 200, restockCycles = 50)
            stock += stock(shop_objs.blue_dragon_scale, count = 200, restockCycles = 50)
            stock += stock(shop_objs.desert_goat_horn, count = 200, restockCycles = 50)
            stock += stock(shop_objs.huntingbeast_sabreteeth, count = 200, restockCycles = 50)
            stock += stock(shop_objs.amylase, count = 200, restockCycles = 50)
            stock += stock(shop_objs.pack_amylase, count = 40, restockCycles = 100)
            stock += stock(shop_objs.wine_of_zamorak, count = 150, restockCycles = 60)
            stock += stock(shop_objs.crushed_bird_nest, count = 150, restockCycles = 60)
            stock += stock(shop_objs.yew_roots, count = 150, restockCycles = 60)
            stock += stock(shop_objs.sote_crystal_dust, count = 100, restockCycles = 80)
        }

        // Jack's Capes of Accomplishment. A flat 100,000gp each: `ShopMargins` gives Jack
        // `sell = 1000` and, crucially, `change = 0`, so the price does not drift as stock is
        // bought out the way every other shop in this module does.
        //
        // Nothing here gates the *sale*. A cape is bought at 99 or at 1; it is the cache's own
        // `statreq1_skill`/`statreq1_level` params -- 99 in the matching skill on all 23 -- that
        // stop it from being worn, enforced by `HeldEquipOp`. `SkillcapeShopTest` proves it.
        //
        // Ten of each is a stall's worth, not a warehouse's, and 100 cycles is a slow refill:
        // these are trophies, so running the shelf dry briefly costs a player nothing.
        edit(shop_invs.omnishop_inv_temp) {
            specialistShop()

            for (cape in SKILLCAPES) {
                stock += stock(cape, count = 10, restockCycles = 100)
            }
        }
    }
}
