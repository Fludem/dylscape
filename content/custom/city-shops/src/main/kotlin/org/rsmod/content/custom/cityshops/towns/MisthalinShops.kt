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
 * Edgeville's herblore counter.
 *
 * Unlike every other shop in this module, this one is ours rather than OSRS's: there is no
 * secondaries shop in Edgeville, and until now there was no secondaries shop anywhere. Jatix in
 * Taverley sells vials, a pestle and eye of newt, Betty in Port Sarim sells eye of newt, and that
 * was the whole supply line — so Herblore stopped dead at attack potions unless you farmed or
 * killed for every ingredient yourself.
 *
 * It reuses `herbloreshop2`, a cache-named inv vanilla never stocks, so no new type is packed.
 */
internal object MisthalinShops {
    val all: List<ShopAssignment> =
        listOf(
            shop(
                title = "Primula's Herblore Supplies",
                inv = shop_invs.herbloreshop2,
                greeting = "Do you need any potion ingredients?",
                shop_npcs.primula,
            )
        )
}

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
    }
}
