package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.refs.loc.LocReferences

/**
 * Every state of all four trap chains, decoded from the cache.
 *
 * The ops are the load-bearing part and were dumped rather than assumed:
 * - an **armed** trap carries `Dismantle` on op1 and `Investigate` on op2
 * - a **sprung** trap carries `Check` on op1 and `Reset` on op2
 * - a **failed** trap carries `Dismantle` on op1 and `Reset` on op2
 * - the transient `trapping`/`failing` states carry **no ops at all**, which is what makes them
 *   safe to sit in for a couple of ticks: the player physically cannot interact mid-animation.
 * - `hunting_sapling_up_*` and `hunting_deadfall_boulder` carry `Set-trap` on op1
 *
 * The net trap's swamp lizard is the one place the naming splits: the `swamp` set has only the tree
 * states (up/setting/set/net_set/failing/failed) and the `green` set has only the catch states
 * (catching/full). They are exactly complementary, so a swamp trap uses `swamp` for the tree and
 * `green` for the catch. That was confirmed in the dump, not inferred.
 *
 * Sizes differ and matter for placement: bird snares and box traps are 1x1, net traps are 1x1 while
 * the tree stands and 1x2 once the net falls, and deadfalls are 2x1 throughout.
 */
object HunterLocs : LocReferences() {
    // -- Bird snare ------------------------------------------------------------------------
    val snare_armed = find("hunting_ojibway_trap")
    val snare_failing = find("hunting_ojibway_trap_failing")
    val snare_broken = find("hunting_ojibway_trap_broken")

    val snare_trapping_jungle = find("hunting_ojibway_trap_trapping_jungle")
    val snare_trapping_desert = find("hunting_ojibway_trap_trapping_desert")
    val snare_trapping_woodland = find("hunting_ojibway_trap_trapping_woodland")
    val snare_trapping_polar = find("hunting_ojibway_trap_trapping_polar")

    val snare_full_jungle = find("hunting_ojibway_trap_full_jungle")
    val snare_full_desert = find("hunting_ojibway_trap_full_desert")
    val snare_full_woodland = find("hunting_ojibway_trap_full_woodland")
    val snare_full_polar = find("hunting_ojibway_trap_full_polar")

    // -- Box trap --------------------------------------------------------------------------
    val box_armed = find("hunting_boxtrap_empty")
    val box_failing = find("hunting_boxtrap_failing")
    val box_failed = find("hunting_boxtrap_failed")

    val box_trapping_ferret_n = find("hunting_boxtrap_trapping_ferret_n")
    val box_trapping_ferret_e = find("hunting_boxtrap_trapping_ferret_e")
    val box_trapping_ferret_s = find("hunting_boxtrap_trapping_ferret_s")
    val box_trapping_ferret_w = find("hunting_boxtrap_trapping_ferret_w")

    val box_trapping_chin_n = find("hunting_boxtrap_trapping_chinchompa_n")
    val box_trapping_chin_e = find("hunting_boxtrap_trapping_chinchompa_e")
    val box_trapping_chin_s = find("hunting_boxtrap_trapping_chinchompa_s")
    val box_trapping_chin_w = find("hunting_boxtrap_trapping_chinchompa_w")

    val box_trapping_chin_big_n = find("hunting_boxtrap_trapping_chinchompa_big_n")
    val box_trapping_chin_big_e = find("hunting_boxtrap_trapping_chinchompa_big_e")
    val box_trapping_chin_big_s = find("hunting_boxtrap_trapping_chinchompa_big_s")
    val box_trapping_chin_big_w = find("hunting_boxtrap_trapping_chinchompa_big_w")

    // Note the black chinchompa's directional ids are NOT contiguous: 2027 is an unrelated loc.
    val box_trapping_chin_black_n = find("hunting_boxtrap_trapping_chinchompa_black_n")
    val box_trapping_chin_black_e = find("hunting_boxtrap_trapping_chinchompa_black_e")
    val box_trapping_chin_black_s = find("hunting_boxtrap_trapping_chinchompa_black_s")
    val box_trapping_chin_black_w = find("hunting_boxtrap_trapping_chinchompa_black_w")

    val box_full_ferret = find("hunting_boxtrap_full_ferret")
    val box_full_chin = find("hunting_boxtrap_full_chinchompa")
    val box_full_chin_big = find("hunting_boxtrap_full_chinchompa_big")
    val box_full_chin_black = find("hunting_boxtrap_full_chinchompa_black")

    // -- Net trap --------------------------------------------------------------------------
    val net_up_swamp = find("hunting_sapling_up_swamp")
    val net_up_orange = find("hunting_sapling_up_orange")
    val net_up_red = find("hunting_sapling_up_red")
    val net_up_black = find("hunting_sapling_up_black")

    val net_setting_swamp = find("hunting_sapling_setting_swamp")
    val net_setting_orange = find("hunting_sapling_setting_orange")
    val net_setting_red = find("hunting_sapling_setting_red")
    val net_setting_black = find("hunting_sapling_setting_black")

    val net_roped_swamp = find("hunting_sapling_set_swamp")
    val net_roped_orange = find("hunting_sapling_set_orange")
    val net_roped_red = find("hunting_sapling_set_red")
    val net_roped_black = find("hunting_sapling_set_black")

    val net_armed_swamp = find("hunting_sapling_net_set_swamp")
    val net_armed_orange = find("hunting_sapling_net_set_orange")
    val net_armed_red = find("hunting_sapling_net_set_red")
    val net_armed_black = find("hunting_sapling_net_set_black")

    // The swamp lizard's catch states live under `green`, not `swamp`.
    val net_catching_green = find("hunting_sapling_catching_green")
    val net_catching_orange = find("hunting_sapling_catching_orange")
    val net_catching_red = find("hunting_sapling_catching_red")
    val net_catching_black = find("hunting_sapling_catching_black")

    val net_full_green = find("hunting_sapling_full_green")
    val net_full_orange = find("hunting_sapling_full_orange")
    val net_full_red = find("hunting_sapling_full_red")
    val net_full_black = find("hunting_sapling_full_black")

    val net_failing_swamp = find("hunting_sapling_failing_swamp")
    val net_failing_orange = find("hunting_sapling_failing_orange")
    val net_failing_red = find("hunting_sapling_failing_red")
    val net_failing_black = find("hunting_sapling_failing_black")

    val net_failed_swamp = find("hunting_sapling_failed_swamp")
    val net_failed_orange = find("hunting_sapling_failed_orange")
    val net_failed_red = find("hunting_sapling_failed_red")
    val net_failed_black = find("hunting_sapling_failed_black")

    // -- Deadfall --------------------------------------------------------------------------
    val deadfall_boulder = find("hunting_deadfall_boulder")
    val deadfall_setting = find("hunting_deadfall_setting")
    val deadfall_armed = find("hunting_deadfall_trap")
    val deadfall_failing = find("hunting_deadfall_failing")

    // The `_m` twin of each trapping state is the mirrored model for the boulder's other
    // orientation; the 2x1 deadfall picks one by the placed loc's angle.
    val deadfall_trapping_claw = find("hunting_deadfall_trapping_claw")
    val deadfall_trapping_claw_m = find("hunting_deadfall_trapping_claw_m")
    val deadfall_trapping_barbed = find("hunting_deadfall_trapping_barbed")
    val deadfall_trapping_barbed_m = find("hunting_deadfall_trapping_barbed_m")
    val deadfall_trapping_spike = find("hunting_deadfall_trapping_spike")
    val deadfall_trapping_spike_m = find("hunting_deadfall_trapping_spike_m")
    val deadfall_trapping_sabre = find("hunting_deadfall_trapping_sabre")
    val deadfall_trapping_sabre_m = find("hunting_deadfall_trapping_sabre_m")

    val deadfall_full_claw = find("hunting_deadfall_full_claw")
    val deadfall_full_barbed = find("hunting_deadfall_full_barbed")
    val deadfall_full_spike = find("hunting_deadfall_full_spike")
    val deadfall_full_sabre = find("hunting_deadfall_full_sabre")

    /** Trap states a player can dismantle or investigate. */
    val armedStates =
        listOf(
            snare_armed,
            box_armed,
            net_armed_swamp,
            net_armed_orange,
            net_armed_red,
            net_armed_black,
            deadfall_armed,
        )

    /** Trap states a player can check or reset: a catch, or a failure that left the trap behind. */
    val sprungStates =
        listOf(
            snare_full_jungle,
            snare_full_desert,
            snare_full_woodland,
            snare_full_polar,
            snare_broken,
            box_full_ferret,
            box_full_chin,
            box_full_chin_big,
            box_full_chin_black,
            box_failed,
            net_full_green,
            net_full_orange,
            net_full_red,
            net_full_black,
            net_failed_swamp,
            net_failed_orange,
            net_failed_red,
            net_failed_black,
            deadfall_full_claw,
            deadfall_full_barbed,
            deadfall_full_spike,
            deadfall_full_sabre,
        )

    /** Young trees a net trap can be built on. */
    val youngTrees = listOf(net_up_swamp, net_up_orange, net_up_red, net_up_black)

    /** Young trees with a rope already tied, waiting for a net. */
    val ropedTrees = listOf(net_roped_swamp, net_roped_orange, net_roped_red, net_roped_black)

    /**
     * Armed net traps, in the same order as [youngTrees] and [ropedTrees] so the three lists index
     * together: a tree at index `i` ropes to `ropedTrees[i]` and arms to `netArmed[i]`.
     */
    val netArmed = listOf(net_armed_swamp, net_armed_orange, net_armed_red, net_armed_black)
}
