package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.refs.struct.StructReferences
import org.rsmod.api.type.refs.timer.TimerReferences

typealias league_structs = LeagueStructs

typealias league_objs = LeagueObjs

typealias league_params = LeagueParams

typealias league_timers = LeagueTimers

/**
 * The 23 relics `league_relics` offers, as vanilla structs. Their names come from
 * `.data/symbols/.local/struct.sym`, which aliases the real ids.
 */
object LeagueStructs : StructReferences() {
    val power_miner = find("league_relic_power_miner")
    val lumberjack = find("league_relic_lumberjack")
    val animal_wrangler = find("league_relic_animal_wrangler")
    val corner_cutter = find("league_relic_corner_cutter")
    val friendly_forager = find("league_relic_friendly_forager")
    val dodgy_deals = find("league_relic_dodgy_deals")
    val clue_compass = find("league_relic_clue_compass")
    val bank_heist = find("league_relic_bank_heist")
    val fairys_flight = find("league_relic_fairys_flight")
    val golden_god = find("league_relic_golden_god")
    val reloaded = find("league_relic_reloaded")
    val equilibrium = find("league_relic_equilibrium")
    val treasure_arbiter = find("league_relic_treasure_arbiter")
    val production_master = find("league_relic_production_master")
    val slayer_master = find("league_relic_slayer_master")
    val total_recall = find("league_relic_total_recall")
    val bankers_note = find("league_relic_bankers_note")
    val pocket_kingdom = find("league_relic_pocket_kingdom")
    val grimoire = find("league_relic_grimoire")
    val overgrown = find("league_relic_overgrown")
    val specialist = find("league_relic_specialist")
    val guardian = find("league_relic_guardian")
    val last_stand = find("league_relic_last_stand")
}

/** The items the implemented relics hand out - each relic struct's param 2049. */
object LeagueObjs : ObjReferences() {
    val echo_pickaxe = find("league_trailblazer_pickaxe")
    val echo_axe = find("league_trailblazer_axe")
    val echo_harpoon = find("league_trailblazer_harpoon")
    val sage_greaves = find("league_relic_agility_boots")
    val bankers_briefcase = find("league_bank_heist_teleport")
    val crystal_of_echoes = find("league_trailblazer_last_recall_teleport")
    val bankers_note = find("league_bankers_note")
    val arcane_grimoire = find("league_3_magic_book")
}

object LeagueParams : ParamReferences() {
    /** Vanilla param 880, named in `.local/param.sym`. */
    val relic_description = find<String>("league_relic_description")
}

object LeagueTimers : TimerReferences() {
    /** Drains Last Stand's 255 combat stats back down, one tick at a time. */
    val last_stand = find("league_last_stand")

    /** Pays Sage's greaves' running experience. */
    val sage_greaves = find("league_sage_greaves")
}
