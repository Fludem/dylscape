package org.rsmod.content.custom.dagannothkings.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.content.custom.dagannothkings.DagannothKing
import org.rsmod.game.type.npc.NpcType

typealias dk_npcs = DagannothKingNpcs

typealias dk_seqs = DagannothKingSeqs

typealias dk_spotanims = DagannothKingSpotanims

typealias dk_locs = DagannothKingLocs

public object DagannothKingNpcs : NpcReferences() {
    val supreme = find("dagcave_ranged_boss")
    val magic = find("dagcave_magic_boss")
    val melee = find("dagcave_melee_boss")

    /**
     * The cache names the kings by the style they *attack* with, not by their names -- so
     * `dagcave_magic_boss` is Prime, who casts, and not the king you kill with magic. Mapping them
     * once here keeps that confusion in a single place.
     */
    val byKing: Map<DagannothKing, NpcType> =
        mapOf(
            DagannothKing.Supreme to supreme,
            DagannothKing.Prime to magic,
            DagannothKing.Rex to melee,
        )
}

public object DagannothKingSeqs : SeqReferences() {
    val attack_melee = find("dagannoth_meganoth_attack_melee")
    val attack_mage = find("dagannoth_meganoth_attack_mage")
    val attack_range = find("dagannoth_meganoth_attack_range")
    val defend = find("dagannoth_meganoth_defend")
    val death = find("dagannoth_meganoth_death")
}

public object DagannothKingSpotanims : SpotanimReferences() {
    /** Supreme's shot. */
    val arrow_travel = find("dagannoth_arrow_spotanim_travel")

    /** Prime's cast. */
    val spine_travel = find("dagannoth_spine_spotanim_travel")
}

public object DagannothKingLocs : LocReferences() {
    /** In the lair, at the west edge. op1 `Climb-up`, back to the Waterbirth antechamber. */
    val boss_ladder_up = find("dagexp_bossroomladder_up")

    /**
     * The way in, in the Waterbirth antechamber.
     *
     * The placed loc is `dagexp_bossroomladder_down` (3831), a multiloc on varbit
     * `dagboss_chamber_entrance`. `LocInteractions` resolves the multiloc before dispatching, so
     * the binding has to name the resolved child rather than the parent.
     */
    val boss_ladder_down = find("dagexp_bossroomladder_down_normal")
}
