package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.refs.npc.NpcReferences

/**
 * The huntable creatures and the invisible npcs that stand on a laid trap.
 *
 * Birds are four biome-generic types, not one per species, and the cache's display names settle
 * which biome is which: jungle is the Crimson swift, desert the Golden warbler, woodland the Copper
 * longtail and polar the Cerulean twitch. The Tropical wagtail has no npc here, so it is not
 * implemented rather than faked onto one of these four.
 *
 * The kebbits live under `huntingbeast_*`, and the names matter because two of that family are
 * falconry creatures, not deadfall ones: `huntingbeast_silent` (Dark kebbit), `huntingbeast_speedy`
 * (Spotted kebbit) and `huntingbeast_speedy2` (Dashing kebbit) all carry "Catch" on op1 and belong
 * to falconry, which is out of scope. The four deadfall kebbits are the ones below.
 *
 * [trapNpcs] are the vanilla attractor npcs. Each trap kind has an armed and a disarmed variant;
 * the armed one is what a creature hunts, and springing the trap swaps it for the `_off` twin so it
 * stops attracting. They are invisible in game (no model op, `hidden` on op5).
 */
object HunterNpcs : NpcReferences() {
    // Bird snare quarry.
    val crimson_swift = find("hunting_bird_jungle")
    val golden_warbler = find("hunting_bird_desert")
    val copper_longtail = find("hunting_bird_woodland")
    val cerulean_twitch = find("hunting_bird_polar")

    // Box trap quarry.
    val ferret = find("hunting_ferret")
    val chinchompa = find("hunting_chinchompa")
    val carnivorous_chinchompa = find("hunting_chinchompa_big")
    val black_chinchompa = find("hunting_chinchompa_black")

    // Net trap quarry.
    val swamp_lizard = find("salamander_green")
    val orange_salamander = find("salamander_orange")
    val red_salamander = find("salamander_red")
    val black_salamander = find("salamander_black")

    // Deadfall quarry.
    val wild_kebbit = find("huntingbeast_claws")
    val barb_tailed_kebbit = find("huntingbeast_barbedtail")
    val prickly_kebbit = find("huntingbeast_spiky")
    val sabre_toothed_kebbit = find("huntingbeast_sabreteeth")

    // Invisible trap attractors, armed and disarmed.
    val snare_trap_npc = find("hunting_ojibway_trap_npc")
    val snare_trap_npc_off = find("hunting_ojibway_trap_npc_off")
    val box_trap_npc = find("hunting_box_trap_npc")
    val box_trap_npc_off = find("hunting_box_trap_npc_off")
    val net_trap_npc = find("hunting_sapling_trap_npc")
    val net_trap_npc_off = find("hunting_sapling_trap_npc_off")
    val deadfall_trap_npc = find("hunting_deadfall_trap_npc")
    val deadfall_trap_npc_off = find("hunting_deadfall_trap_npc_off")

    val birds = listOf(crimson_swift, golden_warbler, copper_longtail, cerulean_twitch)
    val boxQuarry = listOf(ferret, chinchompa, carnivorous_chinchompa, black_chinchompa)
    val salamanders = listOf(swamp_lizard, orange_salamander, red_salamander, black_salamander)
    val kebbits = listOf(wild_kebbit, barb_tailed_kebbit, prickly_kebbit, sabre_toothed_kebbit)

    val creatures = birds + boxQuarry + salamanders + kebbits

    val armedTrapNpcs = listOf(snare_trap_npc, box_trap_npc, net_trap_npc, deadfall_trap_npc)
    val disarmedTrapNpcs =
        listOf(snare_trap_npc_off, box_trap_npc_off, net_trap_npc_off, deadfall_trap_npc_off)
    val trapNpcs = armedTrapNpcs + disarmedTrapNpcs
}
