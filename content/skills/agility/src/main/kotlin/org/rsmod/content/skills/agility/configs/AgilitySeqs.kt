package org.rsmod.content.skills.agility.configs

import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * Animations for the rooftop obstacles.
 *
 * Only one `rooftops_*` seq exists in this revision - [pole_vault]. Every other obstacle reuses a
 * generic animation, which is what the live game does too: there is no bespoke "Draynor tightrope"
 * animation, just the shared balancing walk. Names verified against `.data/symbols/seq.sym`.
 */
public object AgilitySeqs : SeqReferences() {
    /** Hauling yourself up a rough wall. Also the tree climbs. */
    val climb_up = find("human_climbing")

    /** Dropping off a roof back to ground level. */
    val climb_down = find("human_climbing_down")

    /** The balancing walk shared by every tightrope, plank, ledge and narrow wall. */
    val balance = find("myq3_human_tightrope")

    /** Rope swings and the Al Kharid cable. */
    val rope_swing = find("human_ropeswing")

    /** The Al Kharid zip line. */
    val zipline = find("zipline_slide")

    /** The only obstacle-specific seq in the cache. */
    val pole_vault = find("rooftops_pole_vault")

    /** Pollnivneach's monkey bars: mount, traverse, dismount. */
    val monkeybars_on = find("human_monkeybars_on")
    val monkeybars_walk = find("human_monkeybars_walk")
    val monkeybars_off = find("human_monkeybars_off")

    /** Clearing a gap between roofs. */
    val jump_gap = find("human_jump_stones")

    /** Hurdling a low obstacle rather than clearing a gap. */
    val hurdle = find("human_jump_hurdle")

    /** Scrambling up over a wall, and dropping down off one. */
    val wall_scramble = find("agility_shortcut_wall_jump")
    val wall_drop = find("agility_shortcut_wall_jumpdown")

    /** Grabbing a banner / drying line. */
    val grab = find("human_pickupfloor")
}
