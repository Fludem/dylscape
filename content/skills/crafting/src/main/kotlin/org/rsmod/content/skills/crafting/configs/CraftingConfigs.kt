package org.rsmod.content.skills.crafting.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * Every obj Crafting consumes or produces.
 *
 * As with Fletching, nothing here is tagged into a content group: the recipes are relational, and a
 * group edit would be a permanent cache addition bought for no benefit.
 */
object CraftingObjs : ObjReferences() {
    // Tools.
    val needle = find("needle")
    val thread = find("thread")
    val chisel = find("chisel")

    // Spinning.
    val wool = find("wool")
    val ball_of_wool = find("ball_of_wool")
    val flax = find("flax")
    val bow_string = find("bow_string")

    // Bars.
    val gold_bar = find("gold_bar")
    val silver_bar = find("silver_bar")

    // Hides and the leather they tan into.
    val cowhide = find("cow_hide")
    val leather = find("leather")
    val hard_leather = find("hard_leather")
    val snake_hide = find("village_snake_hide")
    val snakeskin = find("village_snake_skin")
    val dragonhide_green = find("dragonhide_green")
    val dragonhide_blue = find("dragonhide_blue")
    val dragonhide_red = find("dragonhide_red")
    val dragonhide_black = find("dragonhide_black")
    val dragon_leather = find("dragon_leather")
    val dragon_leather_blue = find("dragon_leather_blue")
    val dragon_leather_red = find("dragon_leather_red")
    val dragon_leather_black = find("dragon_leather_black")

    // Soft leather products.
    val leather_gloves = find("leather_gloves")
    val leather_boots = find("leather_boots")
    val leather_cowl = find("leather_cowl")
    val leather_vambraces = find("leather_vambraces")
    val leather_armour = find("leather_armour")
    val leather_chaps = find("leather_chaps")
    val coif = find("coif")
    val hardleather_body = find("hardleather_body")

    // Dragonhide products.
    val green_dhide_vambraces = find("dragon_vambraces")
    val green_dhide_chaps = find("dragonhide_chaps")
    val green_dhide_body = find("dragonhide_body")
    val blue_dhide_vambraces = find("blue_dragon_vambraces")
    val blue_dhide_chaps = find("blue_dragonhide_chaps")
    val blue_dhide_body = find("blue_dragonhide_body")
    val red_dhide_vambraces = find("red_dragon_vambraces")
    val red_dhide_chaps = find("red_dragonhide_chaps")
    val red_dhide_body = find("red_dragonhide_body")
    val black_dhide_vambraces = find("black_dragon_vambraces")
    val black_dhide_chaps = find("black_dragonhide_chaps")
    val black_dhide_body = find("black_dragonhide_body")

    // Uncut and cut gems.
    val uncut_opal = find("uncut_opal")
    val uncut_jade = find("uncut_jade")
    val uncut_red_topaz = find("uncut_red_topaz")
    val uncut_sapphire = find("uncut_sapphire")
    val uncut_emerald = find("uncut_emerald")
    val uncut_ruby = find("uncut_ruby")
    val uncut_diamond = find("uncut_diamond")
    val uncut_dragonstone = find("uncut_dragonstone")
    val uncut_onyx = find("uncut_onyx")
    val uncut_zenyte = find("uncut_zenyte")

    val opal = find("opal")
    val jade = find("jade")
    val red_topaz = find("red_topaz")
    val sapphire = find("sapphire")
    val emerald = find("emerald")
    val ruby = find("ruby")
    val diamond = find("diamond")
    val dragonstone = find("dragonstone")
    val onyx = find("onyx")
    val zenyte = find("zenyte")

    val crushed_gemstone = find("crushed_gemstone")

    // Moulds.
    val ring_mould = find("ring_mould")
    val necklace_mould = find("necklace_mould")
    val amulet_mould = find("amulet_mould")
    val bracelet_mould = find("jewl_bracelet_mould")
    val holy_symbol_mould = find("holy_symbol_mould")
    val unholy_symbol_mould = find("unholy_symbol_mould")
    val tiara_mould = find("tiara_mould")
    val sickle_mould = find("sickle_mould")

    // Gold jewellery.
    val gold_ring = find("gold_ring")
    val sapphire_ring = find("sapphire_ring")
    val emerald_ring = find("emerald_ring")
    val ruby_ring = find("ruby_ring")
    val diamond_ring = find("diamond_ring")
    val dragonstone_ring = find("dragonstone_ring")
    val onyx_ring = find("onyx_ring")
    val zenyte_ring = find("zenyte_ring")

    val gold_necklace = find("gold_necklace")
    val sapphire_necklace = find("sapphire_necklace")
    val emerald_necklace = find("emerald_necklace")
    val ruby_necklace = find("ruby_necklace")
    val diamond_necklace = find("diamond_necklace")
    val dragonstone_necklace = find("dragonstone_necklace")
    val onyx_necklace = find("onyx_necklace")
    val zenyte_necklace = find("zenyte_necklace")

    val unstrung_gold_amulet = find("unstrung_gold_amulet")
    val unstrung_sapphire_amulet = find("unstrung_sapphire_amulet")
    val unstrung_emerald_amulet = find("unstrung_emerald_amulet")
    val unstrung_ruby_amulet = find("unstrung_ruby_amulet")
    val unstrung_diamond_amulet = find("unstrung_diamond_amulet")
    val unstrung_dragonstone_amulet = find("unstrung_dragonstone_amulet")
    val unstrung_onyx_amulet = find("unstrung_onyx_amulet")
    val unstrung_zenyte_amulet = find("unstrung_zenyte_amulet")

    val gold_bracelet = find("jewl_gold_bracelet")
    val sapphire_bracelet = find("jewl_sapphire_bracelet")
    val emerald_bracelet = find("jewl_emerald_bracelet")
    val ruby_bracelet = find("jewl_ruby_bracelet")
    val diamond_bracelet = find("jewl_diamond_bracelet")
    val dragonstone_bracelet = find("jewl_dragonstone_bracelet")
    val onyx_bracelet = find("jewl_onyx_bracelet")
    val zenyte_bracelet = find("zenyte_bracelet")

    // Silver jewellery and the rest of interface 6.
    val opal_ring = find("opal_ring")
    val jade_ring = find("jade_ring")
    val topaz_ring = find("topaz_ring")
    val opal_necklace = find("opal_necklace")
    val jade_necklace = find("jade_necklace")
    val topaz_necklace = find("topaz_necklace")
    val unstrung_opal_amulet = find("unstrung_opal_amulet")
    val unstrung_jade_amulet = find("unstrung_jade_amulet")
    val unstrung_topaz_amulet = find("unstrung_topaz_amulet")
    val opal_bracelet = find("opal_bracelet")
    val jade_bracelet = find("jade_bracelet")
    val topaz_bracelet = find("topaz_bracelet")

    val unstrung_holy_symbol = find("nostringstar")
    val unstrung_unholy_symbol = find("nostringsnake")
    val tiara = find("tiara")
    val silver_sickle = find("silver_sickle")
    val silver_bolts = find("xbows_crossbow_bolts_silver")

    // Strung amulets, for the ball-of-wool step.
    val gold_amulet = find("strung_gold_amulet")
    val sapphire_amulet = find("strung_sapphire_amulet")
    val emerald_amulet = find("strung_emerald_amulet")
    val ruby_amulet = find("strung_ruby_amulet")
    val diamond_amulet = find("strung_diamond_amulet")
    val dragonstone_amulet = find("strung_dragonstone_amulet")
    val onyx_amulet = find("strung_onyx_amulet")
    val zenyte_amulet = find("zenyte_amulet")
    val opal_amulet = find("strung_opal_amulet")
    val jade_amulet = find("strung_jade_amulet")
    val topaz_amulet = find("strung_topaz_amulet")
    val holy_symbol = find("stringstar")
}

object CraftingSeqs : SeqReferences() {
    val leather = find("human_leather_crafting")
    val chisel = find("human_fletching_huntingbolts_chisel")
    val spin = find("human_spinningwheel")
    val furnace = find("human_furnace")
    val craft = find("human_crafting")
}

/**
 * The furnace group is Smithing's, reached by name rather than by importing `SmithingContent`.
 *
 * Content groups resolve out of the shared symbol tables, so naming one costs nothing and keeps
 * Crafting from depending on the Smithing module for a single reference.
 */
object CraftingContent : ContentReferences() {
    val smithing_furnace = find("smithing_furnace")
}

object CraftingLocs : LocReferences() {
    val spinningwheel = find("spinningwheel")
    val spinningwheel_2 = find("spinningwheel_2")
    val spinningwheel_quetzacali = find("spinningwheel_quetzacali")
}

/**
 * The four tanners in the cache. Every one of them carries `Talk-to` on op1 and `Trade` on op3, so
 * the panel hangs off op3 and the dialogue op is left alone.
 */
object CraftingNpcs : NpcReferences() {
    val ellis_tanner = find("ellis_tanner")
    val tanner = find("tanner")
    val auburn_tanner = find("auburn_tanner")
    val werewolf_tanner = find("werewolftanner")

    val tanners = listOf(ellis_tanner, tanner, auburn_tanner, werewolf_tanner)
}

object CraftingInterfaces : InterfaceReferences() {
    val crafting_gold = find("crafting_gold")
    val silver_crafting = find("silver_crafting")
    val tanner = find("tanner")
}

/**
 * Interface 446, the gold jewellery panel.
 *
 * The panel draws itself: every product row carries an `onLoad` hook that hands the client its gem,
 * its product and its mould, and the header rows swap between "you need a ring mould" and the
 * product list on their own. So the server's whole job is to open it and enable the buttons.
 *
 * `make_1` through `make_all` are the client's own quantity buttons. They run
 * `[clientscript,skillmain_setquantity]` locally and store the result in a varc, which the server
 * cannot read -- see [org.rsmod.content.skills.crafting.scripts.Jewellery] for how the quantity is
 * recovered anyway.
 */
object CraftingGoldComponents : ComponentReferences() {
    val gold_ring = find("crafting_gold:gold_ring")
    val sapphire_ring = find("crafting_gold:sapphire_ring")
    val emerald_ring = find("crafting_gold:emerald_ring")
    val ruby_ring = find("crafting_gold:ruby_ring")
    val diamond_ring = find("crafting_gold:diamond_ring")
    val dragon_ring = find("crafting_gold:dragon_ring")
    val onyx_ring = find("crafting_gold:onyx_ring")
    val zenyte_ring = find("crafting_gold:zenyte_ring")

    val gold_necklace = find("crafting_gold:gold_necklace")
    val sapphire_necklace = find("crafting_gold:sapphire_necklace")
    val emerald_necklace = find("crafting_gold:emerald_necklace")
    val ruby_necklace = find("crafting_gold:ruby_necklace")
    val diamond_necklace = find("crafting_gold:diamond_necklace")
    val dragon_necklace = find("crafting_gold:dragon_necklace")
    val onyx_necklace = find("crafting_gold:onyx_necklace")
    val zenyte_necklace = find("crafting_gold:zenyte_necklace")

    val gold_amulet = find("crafting_gold:gold_amulet")
    val sapphire_amulet = find("crafting_gold:sapphire_amulet")
    val emerald_amulet = find("crafting_gold:emerald_amulet")
    val ruby_amulet = find("crafting_gold:ruby_amulet")
    val diamond_amulet = find("crafting_gold:diamond_amulet")
    val dragon_amulet = find("crafting_gold:dragon_amulet")
    val onyx_amulet = find("crafting_gold:onyx_amulet")
    val zenyte_amulet = find("crafting_gold:zenyte_amulet")

    val gold_bracelet = find("crafting_gold:gold_bracelet")
    val sapphire_bracelet = find("crafting_gold:sapphire_bracelet")
    val emerald_bracelet = find("crafting_gold:emerald_bracelet")
    val ruby_bracelet = find("crafting_gold:ruby_bracelet")
    val diamond_bracelet = find("crafting_gold:diamond_bracelet")
    val dragon_bracelet = find("crafting_gold:dragon_bracelet")
    val onyx_bracelet = find("crafting_gold:onyx_bracelet")
    val zenyte_bracelet = find("crafting_gold:zenyte_bracelet")

    val make_1 = find("crafting_gold:make_1")
    val make_5 = find("crafting_gold:make_5")
    val make_10 = find("crafting_gold:make_10")
    val make_all = find("crafting_gold:make_all")

    val quantityButtons = listOf(make_1 to 1, make_5 to 5, make_10 to 10, make_all to Int.MAX_VALUE)
}

/** Interface 6, the silver panel. Same contract as [CraftingGoldComponents]. */
object SilverCraftingComponents : ComponentReferences() {
    val opal_ring = find("silver_crafting:opal_ring")
    val jade_ring = find("silver_crafting:jade_ring")
    val topaz_ring = find("silver_crafting:topaz_ring")
    val opal_necklace = find("silver_crafting:opal_necklace")
    val jade_necklace = find("silver_crafting:jade_necklace")
    val topaz_necklace = find("silver_crafting:topaz_necklace")
    val opal_amulet = find("silver_crafting:opal_amulet")
    val jade_amulet = find("silver_crafting:jade_amulet")
    val topaz_amulet = find("silver_crafting:topaz_amulet")
    val opal_bracelet = find("silver_crafting:opal_bracelet")
    val jade_bracelet = find("silver_crafting:jade_bracelet")
    val topaz_bracelet = find("silver_crafting:topaz_bracelet")

    val holy_symbol = find("silver_crafting:holy_symbol")
    val unholy_symbol = find("silver_crafting:unholy_symbol")
    val sickle = find("silver_crafting:sickle")
    val crossbow_bolt = find("silver_crafting:crossbow_bolt")
    val tiara = find("silver_crafting:tiara")

    val make_1 = find("silver_crafting:make_1")
    val make_5 = find("silver_crafting:make_5")
    val make_10 = find("silver_crafting:make_10")
    val make_all = find("silver_crafting:make_all")

    val quantityButtons = listOf(make_1 to 1, make_5 to 5, make_10 to 10, make_all to Int.MAX_VALUE)
}

/**
 * Interface 324, the tanning panel.
 *
 * Eight rows, `a` through `h`, each with a model, a name, a price and its own four quantity
 * buttons. Unlike the jewellery panels this one carries no `onLoad` hooks and no clientscript
 * references it at all, so every row is filled in and enabled by the server. That is also why the
 * quantity is unambiguous here: it is which button was pressed, not a number hidden in a varc.
 */
object TannerComponents : ComponentReferences() {
    private val rows = "abcdefgh"

    val models = rows.map { find("tanner:tanning_${it}_model") }
    val names = rows.map { find("tanner:tanning_${it}_text") }
    val prices = rows.map { find("tanner:tanning_${it}_price") }

    val buttons1 = rows.map { find("tanner:tanning_${it}_1") }
    val buttons5 = rows.map { find("tanner:tanning_${it}_5") }
    val buttonsX = rows.map { find("tanner:tanning_${it}_x") }
    val buttonsAll = rows.map { find("tanner:tanning_${it}_all") }

    /** Row count, which is also the ceiling on how many hides the panel can offer. */
    const val ROWS: Int = 8
}
