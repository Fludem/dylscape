package org.rsmod.content.custom.zulrah.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.proj.ProjAnimReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.content.custom.zulrah.ZulrahForm

typealias zulrah_npcs = ZulrahNpcs

typealias zulrah_locs = ZulrahLocs

typealias zulrah_objs = ZulrahObjs

typealias zulrah_seqs = ZulrahSeqs

typealias zulrah_spots = ZulrahSpotanims

typealias zulrah_projanims = ZulrahProjAnims

typealias zulrah_timers = ZulrahTimers

/**
 * The cache calls Zulrah `snakeboss`. Every form is a separate npc type, and the fight moves one
 * npc between them with `changeType` rather than spawning a new one, which is what carries its
 * hitpoints across a dive.
 */
object ZulrahNpcs : NpcReferences() {
    val serpentine = find("snakeboss_boss_ranged")
    val magma = find("snakeboss_boss_melee")
    val tanzanite = find("snakeboss_boss_magic")

    val forms =
        mapOf(
            ZulrahForm.Serpentine to serpentine,
            ZulrahForm.Magma to magma,
            ZulrahForm.Tanzanite to tanzanite,
        )

    val snakeling_melee = find("snakeboss_minion_melee")
    val snakeling_magic = find("snakeboss_minion_magic")
}

object ZulrahLocs : LocReferences() {
    /**
     * The Sacrificial boat at Zul-Andra. The placed loc is a multiloc on `snakeboss_info` whose
     * every state resolves to [boat_board], and it is the resolved child that carries `Board`.
     */
    val boat = find("snakeboss_boat")
    val boat_board = find("snakeboss_boat_1op")

    /** The multiloc's out-of-range default: `Board` and `Quick-Board`. */
    val boat_board_quick = find("snakeboss_boat_2ops")

    /** 3x3, and never placed on the map: every cloud in the fight is spawned by the fight. */
    val poison_cloud = find("snakeboss_poisoncloud")
}

object ZulrahObjs : ObjReferences() {
    /** Its only op is iop1 `Teleport`, back to Zul-Andra. */
    val zul_andra_teleport = find("teleportscroll_zulandra")
}

object ZulrahSeqs : SeqReferences() {
    val attack = find("snakeboss_attack_acidx1")
    val tail_left = find("snakeboss_attack_tail_left")
    val tail_right = find("snakeboss_attack_tail_right")
    val defend = find("snakeboss_defend")
    val dive = find("snakeboss_sinkfast")
    val emerge = find("snakeboss_emergefast")
    val rise = find("snakeboss_spawn")
    val death = find("snakeboss_death")

    val snakeling_attack = find("snakeboss_pet_attack")
    val snakeling_defend = find("snakeboss_pet_defend")
    val snakeling_death = find("snakeboss_pet_death")
    val snakeling_spawn = find("snakeboss_pet_spawn")
}

object ZulrahSpotanims : SpotanimReferences() {
    /** The green form's ranged spit, and the blue form's occasional one. */
    val ranged = find("snakeboss_orb")

    /** The venom barrage that leaves a toxic cloud where it lands. */
    val cloud = find("snakeboss_double_orb")

    val magic = find("snakeboss_fireball")

    /** The egg that hatches into a snakeling. */
    val egg = find("snakeboss_egg")

    val snakeling_magic = find("snakeboss_minion_spell")
}

object ZulrahProjAnims : ProjAnimReferences() {
    /** The cache names no Zulrah-specific flight, so every spit rides the generic spell arc. */
    val spit = find("magic_spell")
}

object ZulrahVarps : VarpReferences() {
    /** The vanilla kill counter, which the collection log and the kill-count message both read. */
    val kills = find("total_snakeboss_kills")
}

object ZulrahTimers : TimerReferences() {
    /**
     * Drives the fight, once a tick, on the Zulrah npc. Server-only, so it is hand-added to
     * `.data/symbols/.local/timer.sym` and needs no `packCache`.
     */
    val fight = find("zulrah_fight")

    /** The grace period after a kill before the shrine sends the player back to Zul-Andra. */
    val return_home = find("zulrah_return")
}
