package org.rsmod.content.custom.vorkath.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.proj.ProjAnimReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varp.VarpReferences

typealias vorkath_npcs = VorkathNpcs

typealias vorkath_locs = VorkathLocs

typealias vorkath_seqs = VorkathSeqs

typealias vorkath_spots = VorkathSpotanims

typealias vorkath_projanims = VorkathProjAnims

typealias vorkath_varps = VorkathVarps

typealias vorkath_timers = VorkathTimers

/**
 * Vorkath is two npc types: the one asleep on its platform with a `Poke` op, and the one that
 * fights. A poke moves the one npc between them with `changeType`, so the sleeping spawn is the
 * only thing the map ever places.
 */
public object VorkathNpcs : NpcReferences() {
    /** op1 `Poke`. 10 hitpoints and every level at 1; the fight never reads them. */
    val sleeping = find("vorkath_sleeping")

    /** op2 `Attack`. The cache already carries its 750 hitpoints and every combat level. */
    val awake = find("vorkath")

    /** The Rellekka pier. op1 `Talk-to`, op3 `Ungael`. */
    val torfinn_rellekka = find("torfinn_travel_rellekka")

    /** The Ungael shore. op1 `Talk-to`, op3 `Rellekka`. */
    val torfinn_ungael = find("torfinn_travel_ungael")
}

public object VorkathLocs : LocReferences() {
    /** The boat on the Ungael shore. op1 `Travel`. */
    val ungael_boat = find("ungael_boat")

    /**
     * The south lip of the crater, placed at (2272, 4053). op1 `Climb-over`. The vanilla arena is
     * an instance behind this; here it is the crater itself. It is also the only way out: the north
     * lip at (2272, 4077) is a multiloc on the Dragon Slayer II progress varbit, and in this cache
     * every one of its children is the op-less `ungael_crater_exit_no_op`.
     */
    val crater_entrance = find("ungael_crater_entrance")
}

public object VorkathSeqs : SeqReferences() {
    /** Rearing up and roaring; played on the poke. */
    val wake = find("ds2_vorkath_spawn")

    /** Live fires both the ranged and the magic attack off this one animation. */
    val attack = find("ds2_vorkath_ranged")

    val death = find("ds2_vorkath_death")
}

public object VorkathSpotanims : SpotanimReferences() {
    val ranged_travel = find("vorkath_ranged_travel")
    val ranged_impact = find("vorkath_ranged_impact")
    val magic_travel = find("vorkath_magic_travel")
    val magic_impact = find("vorkath_magic_impact")
}

public object VorkathProjAnims : ProjAnimReferences() {
    /** The cache names no Vorkath-specific flight, so both attacks ride the generic spell arc. */
    val spit = find("magic_spell")
}

public object VorkathVarps : VarpReferences() {
    /** The vanilla kill counter, which the collection log and kill-count message both read. */
    val kills = find("total_vorkath_kills")
}

public object VorkathTimers : TimerReferences() {
    /**
     * Fires on the awake form while nobody is fighting it, and puts it back to sleep. Server-only,
     * so it is hand-added to `.data/symbols/.local/timer.sym` and needs no `packCache`.
     */
    val sleep = find("vorkath_sleep")
}
