package org.rsmod.content.skills.farming.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences

/**
 * The loc that is actually placed on the map for every farming patch in the game.
 *
 * Each of these is a **multiloc**: it carries no ops and no name of its own, and the client swaps
 * in one of up to 256 child locs according to [FarmingVarBits]. Everything a player sees and every
 * op they can click -- `Rake`, `Harvest`, `Pick`, `Check-health`, `Clear` -- belongs to the child,
 * and `LocInteractions` resolves the child before dispatching, so scripts bind these parents once
 * and the engine does the rest.
 *
 * The tree and hardwood patches also place a `*_canopy` loc a couple of tiles away. Those share the
 * patch's varbit, so they follow along on their own and are deliberately absent here.
 *
 * Grape vines are the odd one out: the visible `grapevine_patch_*` loc has no ops at all, and the
 * ops live on the invisible `grapevine_patch_clickzone_*` twin standing on the same tile. Both read
 * the same varbit, so binding the clickzone drives the visual.
 */
object FarmingLocs : LocReferences() {
    // allotment
    val farming_veg_patch_1 = find("farming_veg_patch_1")
    val farming_veg_patch_2 = find("farming_veg_patch_2")
    val farming_veg_patch_3 = find("farming_veg_patch_3")
    val farming_veg_patch_4 = find("farming_veg_patch_4")
    val farming_veg_patch_5 = find("farming_veg_patch_5")
    val farming_veg_patch_6 = find("farming_veg_patch_6")
    val farming_veg_patch_7 = find("farming_veg_patch_7")
    val farming_veg_patch_8 = find("farming_veg_patch_8")
    val farming_veg_patch_9 = find("farming_veg_patch_9")
    val farming_veg_patch_10 = find("farming_veg_patch_10")
    val farming_veg_patch_11 = find("farming_veg_patch_11")
    val farming_veg_patch_13 = find("farming_veg_patch_13")
    val farming_veg_patch_12 = find("farming_veg_patch_12")
    val farming_veg_patch_15 = find("farming_veg_patch_15")
    val farming_veg_patch_14 = find("farming_veg_patch_14")
    val farming_veg_patch_17 = find("farming_veg_patch_17")
    val farming_veg_patch_16 = find("farming_veg_patch_16")
    // flower
    val farming_flower_patch_1 = find("farming_flower_patch_1")
    val farming_flower_patch_2 = find("farming_flower_patch_2")
    val farming_flower_patch_3 = find("farming_flower_patch_3")
    val farming_flower_patch_4 = find("farming_flower_patch_4")
    val farming_flower_patch_5 = find("farming_flower_patch_5")
    val farming_flower_patch_6 = find("farming_flower_patch_6")
    val farming_flower_patch_7 = find("farming_flower_patch_7")
    val farming_flower_patch_8 = find("farming_flower_patch_8")
    val farming_flower_patch_9 = find("farming_flower_patch_9")
    // herb
    val farming_herb_patch_1 = find("farming_herb_patch_1")
    val farming_herb_patch_2 = find("farming_herb_patch_2")
    val farming_herb_patch_3 = find("farming_herb_patch_3")
    val farming_herb_patch_4 = find("farming_herb_patch_4")
    val farming_herb_patch_5 = find("farming_herb_patch_5")
    val farming_herb_patch_6 = find("farming_herb_patch_6")
    val farming_herb_patch_7 = find("farming_herb_patch_7")
    val farming_herb_patch_8 = find("farming_herb_patch_8")
    // hops
    val farming_hops_patch_1 = find("farming_hops_patch_1")
    val farming_hops_patch_2 = find("farming_hops_patch_2")
    val farming_hops_patch_3 = find("farming_hops_patch_3")
    val farming_hops_patch_4 = find("farming_hops_patch_4")
    val farming_hops_patch_5 = find("farming_hops_patch_5")
    // bush
    val farming_bush_patch_1 = find("farming_bush_patch_1")
    val farming_bush_patch_2 = find("farming_bush_patch_2")
    val farming_bush_patch_3 = find("farming_bush_patch_3")
    val farming_bush_patch_4 = find("farming_bush_patch_4")
    val farming_bush_patch_5 = find("farming_bush_patch_5")
    // fruit tree
    val farming_fruit_tree_patch_1 = find("farming_fruit_tree_patch_1")
    val farming_fruit_tree_patch_2 = find("farming_fruit_tree_patch_2")
    val farming_fruit_tree_patch_3 = find("farming_fruit_tree_patch_3")
    val farming_fruit_tree_patch_4 = find("farming_fruit_tree_patch_4")
    val farming_fruit_tree_patch_5 = find("farming_fruit_tree_patch_5")
    val farming_fruit_tree_patch_6 = find("farming_fruit_tree_patch_6")
    val farming_fruit_tree_patch_7 = find("farming_fruit_tree_patch_7")
    // tree
    val farming_tree_patch_1 = find("farming_tree_patch_1")
    val farming_tree_patch_2 = find("farming_tree_patch_2")
    val farming_tree_patch_3 = find("farming_tree_patch_3")
    val farming_tree_patch_4 = find("farming_tree_patch_4")
    val farming_tree_patch_5 = find("farming_tree_patch_5")
    val farming_tree_patch_6 = find("farming_tree_patch_6")
    val farming_tree_patch_7 = find("farming_tree_patch_7")
    // hardwood
    val farming_hardwood_tree_patch_2 = find("farming_hardwood_tree_patch_2")
    val farming_hardwood_tree_patch_3 = find("farming_hardwood_tree_patch_3")
    val farming_hardwood_tree_patch_1 = find("farming_hardwood_tree_patch_1")
    val farming_hardwood_tree_patch_4 = find("farming_hardwood_tree_patch_4")
    // cactus
    val farming_cactus_patch = find("farming_cactus_patch")
    val farming_cactus_patch_2 = find("farming_cactus_patch_2")
    // calquat
    val farming_calquat_tree_patch = find("farming_calquat_tree_patch")
    val farming_calquat_tree_patch_2 = find("farming_calquat_tree_patch_2")
    // mushroom
    val farming_mushroom_patch = find("farming_mushroom_patch")
    // belladonna
    val farming_belladonna_patch = find("farming_belladonna_patch")
    val farming_belladonna_patch_2 = find("farming_belladonna_patch_2")
    // seaweed
    val farming_seaweed_patch_1 = find("farming_seaweed_patch_1")
    val farming_seaweed_patch_2 = find("farming_seaweed_patch_2")
    // spirit tree
    val farming_spirit_tree_patch_1 = find("farming_spirit_tree_patch_1")
    val farming_spirit_tree_patch_2 = find("farming_spirit_tree_patch_2")
    val farming_spirit_tree_patch_3 = find("farming_spirit_tree_patch_3")
    val farming_spirit_tree_patch_4 = find("farming_spirit_tree_patch_4")
    val farming_spirit_tree_patch_5 = find("farming_spirit_tree_patch_5")
    // celastrus
    val farming_celastrus_patch_1 = find("farming_celastrus_patch_1")
    // redwood
    val farming_redwood_tree_patch_0_1 = find("farming_redwood_tree_patch_0_1")
    val farming_redwood_tree_patch_0_2 = find("farming_redwood_tree_patch_0_2")
    val farming_redwood_tree_patch_0_3 = find("farming_redwood_tree_patch_0_3")
    val farming_redwood_tree_patch_0_4 = find("farming_redwood_tree_patch_0_4")
    val farming_redwood_tree_patch_0_5 = find("farming_redwood_tree_patch_0_5")
    val farming_redwood_tree_patch_0_6 = find("farming_redwood_tree_patch_0_6")
    val farming_redwood_tree_patch_0_7 = find("farming_redwood_tree_patch_0_7")
    val farming_redwood_tree_patch_0_8 = find("farming_redwood_tree_patch_0_8")
    val farming_redwood_tree_patch_0_9 = find("farming_redwood_tree_patch_0_9")
    val farming_redwood_tree_patch_1_1 = find("farming_redwood_tree_patch_1_1")
    val farming_redwood_tree_patch_1_2 = find("farming_redwood_tree_patch_1_2")
    val farming_redwood_tree_patch_1_3 = find("farming_redwood_tree_patch_1_3")
    val farming_redwood_tree_patch_1_4 = find("farming_redwood_tree_patch_1_4")
    val farming_redwood_tree_patch_1_5 = find("farming_redwood_tree_patch_1_5")
    val farming_redwood_tree_patch_1_6 = find("farming_redwood_tree_patch_1_6")
    val farming_redwood_tree_patch_1_7 = find("farming_redwood_tree_patch_1_7")
    val farming_redwood_tree_patch_1_8 = find("farming_redwood_tree_patch_1_8")
    val farming_redwood_tree_patch_1_9 = find("farming_redwood_tree_patch_1_9")
    val farming_redwood_tree_patch_2_1 = find("farming_redwood_tree_patch_2_1")
    // anima
    val farming_anima_patch_1 = find("farming_anima_patch_1")
    // crystal
    val farming_crystal_tree_patch_1 = find("farming_crystal_tree_patch_1")
    // hespori
    val hespori_patch = find("hespori_patch")
    // grape
    val grapevine_patch_clickzone_01 = find("grapevine_patch_clickzone_01")
    val grapevine_patch_clickzone_02 = find("grapevine_patch_clickzone_02")
    val grapevine_patch_clickzone_03 = find("grapevine_patch_clickzone_03")
    val grapevine_patch_clickzone_04 = find("grapevine_patch_clickzone_04")
    val grapevine_patch_clickzone_05 = find("grapevine_patch_clickzone_05")
    val grapevine_patch_clickzone_06 = find("grapevine_patch_clickzone_06")
    val grapevine_patch_clickzone_07 = find("grapevine_patch_clickzone_07")
    val grapevine_patch_clickzone_08 = find("grapevine_patch_clickzone_08")
    val grapevine_patch_clickzone_09 = find("grapevine_patch_clickzone_09")
    val grapevine_patch_clickzone_10 = find("grapevine_patch_clickzone_10")
    val grapevine_patch_clickzone_11 = find("grapevine_patch_clickzone_11")
    val grapevine_patch_clickzone_12 = find("grapevine_patch_clickzone_12")
}

/**
 * The 14 "transmit" varbits the client reads to decide which child loc to draw.
 *
 * They are shared: `farming_transmit_a` is the state of whichever patch using it happens to be near
 * the player. Jagex never gives two patches in the same view the same varbit, so writing the
 * nearest one is enough -- see `FarmingSyncScript`.
 */
object FarmingVarBits : VarBitReferences() {
    val farming_transmit_a = find("farming_transmit_a")
    val farming_transmit_b = find("farming_transmit_b")
    val farming_transmit_c = find("farming_transmit_c")
    val farming_transmit_d = find("farming_transmit_d")
    val farming_transmit_e = find("farming_transmit_e")
    val farming_transmit_a1 = find("farming_transmit_a1")
    val farming_transmit_a2 = find("farming_transmit_a2")
    val farming_transmit_b1 = find("farming_transmit_b1")
    val farming_transmit_b2 = find("farming_transmit_b2")
    val farming_transmit_c1 = find("farming_transmit_c1")
    val farming_transmit_c2 = find("farming_transmit_c2")
    val farming_transmit_d1 = find("farming_transmit_d1")
    val farming_transmit_d2 = find("farming_transmit_d2")
    val farming_transmit_e1 = find("farming_transmit_e1")
    val farming_transmit_e2 = find("farming_transmit_e2")
    val farming_transmit_f1 = find("farming_transmit_f1")
    val farming_transmit_f2 = find("farming_transmit_f2")
    val farming_transmit_f = find("farming_transmit_f")
    val farming_transmit_g = find("farming_transmit_g")
    val farming_transmit_h = find("farming_transmit_h")
    val farming_transmit_i = find("farming_transmit_i")
    val farming_transmit_j = find("farming_transmit_j")
    val farming_transmit_k = find("farming_transmit_k")
    val farming_transmit_l = find("farming_transmit_l")
    val farming_transmit_m = find("farming_transmit_m")
}
