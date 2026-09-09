package org.rsmod.content.custom.barrows.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.proj.ProjAnimReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.timer.TimerReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.content.custom.barrows.Brother

typealias barrows_npcs = BarrowsNpcs

typealias barrows_locs = BarrowsLocs

typealias barrows_objs = BarrowsObjs

typealias barrows_varbits = BarrowsVarBits

typealias barrows_seqs = BarrowsSeqs

typealias barrows_spots = BarrowsSpotanims

typealias barrows_timers = BarrowsTimers

object BarrowsNpcs : NpcReferences() {
    val oldman = find("barrows_oldman")

    val brothers =
        mapOf(
            Brother.Ahrim to find("barrows_ahrim"),
            Brother.Dharok to find("barrows_dharok"),
            Brother.Guthan to find("barrows_guthan"),
            Brother.Karil to find("barrows_karil"),
            Brother.Torag to find("barrows_torag"),
            Brother.Verac to find("barrows_verac"),
        )

    /**
     * The crypt bestiary. Killing one adds to the reward potential, which is the only reason the
     * module cares about them.
     *
     * `barrows_giantrat2`/`3`, `barrows_skeleton_unarmed2` and `barrows_skeleton_armed2` are
     * identical duplicates of the entries below and nothing spawns them, so they are left out.
     */
    val monsters =
        listOf(
            find("barrows_bloodworm"),
            find("barrows_rat"),
            find("barrows_giantrat"),
            find("barrows_spider"),
            find("barrows_giantspider"),
            find("barrows_skeleton_unarmed"),
            find("barrows_skeleton_armed"),
        )
}

object BarrowsLocs : LocReferences() {
    val sarcophagi =
        mapOf(
            Brother.Ahrim to find("barrow_ahrim_sarcophagus"),
            Brother.Dharok to find("barrow_dharok_sarcophagus"),
            Brother.Guthan to find("barrow_guthan_sarcophagus"),
            Brother.Karil to find("barrow_karil_sarcophagus"),
            Brother.Torag to find("barrow_torag_sarcophagus"),
            Brother.Verac to find("barrow_verac_sarcophagus"),
        )

    val staircases =
        mapOf(
            Brother.Ahrim to find("barrows_stairs_ahrim"),
            Brother.Dharok to find("barrows_stairs_dharok"),
            Brother.Guthan to find("barrows_stairs_guthan"),
            Brother.Karil to find("barrows_stairs_karil"),
            Brother.Torag to find("barrows_stairs_torag"),
            Brother.Verac to find("barrows_stairs_verac"),
        )

    /**
     * The chest we place. This is the multiloc parent: it switches on [BarrowsVarBits.chest_open]
     * between [stone_chest_closed] (op1 `Open`) and [stone_chest_open] (op1 `Search`, op2 `Close`),
     * so the chest's state is a varbit write and is per-player for free.
     */
    val stone_chest = find("barrows_stone_chest")
    val stone_chest_closed = find("barrows_stone_chest_closed")
    val stone_chest_open = find("barrows_stone_chest_open")
}

object BarrowsObjs : ObjReferences() {
    val spade = find("spade")

    /** Karil's bolt racks, the bulk consolation drop. */
    val bolt_rack = find("barrows_karil_ammo")

    /** The Crystal chest key halves. `keyhalf1` is the loop, `keyhalf2` the tooth. */
    val loop_half_key = find("keyhalf1")
    val tooth_half_key = find("keyhalf2")

    val coins = find("coins")
    val mind_rune = find("mindrune")
    val chaos_rune = find("chaosrune")
    val death_rune = find("deathrune")
    val blood_rune = find("bloodrune")
    val bronze_arrow = find("bronze_arrow")
    val iron_arrow = find("iron_arrow")
    val steel_arrow = find("steel_arrow")
    val mithril_arrow = find("mithril_arrow")

    /**
     * The twenty-four equipment pieces, four per brother, in head/body/legs/weapon order. These are
     * the fully-repaired `barrows_<brother>_<slot>` types; the `_100`/`_75`/`_50`/`_25`/`_broken`
     * chain also ships in the cache but nothing degrades yet.
     */
    val equipment: Map<Brother, List<org.rsmod.game.type.obj.ObjType>> =
        Brother.entries.associateWith { brother ->
            val prefix = "barrows_${brother.name.lowercase()}"
            listOf(
                find("${prefix}_head"),
                find("${prefix}_body"),
                find("${prefix}_legs"),
                find("${prefix}_weapon"),
            )
        }
}

object BarrowsInterfaces : InterfaceReferences() {
    /**
     * The killcount HUD. Component 0 carries `onLoad=[clientscript,barrows_overlay_init, <its own
     * twelve components>]`, so it wires itself up: the server opens it and writes varbits, and
     * never touches a component.
     */
    val overlay = find("barrows_overlay")

    /**
     * The loot panel. Component 3 carries `onLoad=[clientscript,barrows_reward_init]`, and that
     * script names inv 141 (`trail_rewardinv`), draws from it with `inv_getobj` and registers
     * `if_setoninvtransmit` on it - so filling that inv and transmitting it is the whole of driving
     * this panel.
     */
    val reward = find("barrows_reward")
}

object BarrowsInvs : InvReferences() {
    /** Inv 141. Shared with clue scrolls, which is why it is not named for barrows. */
    val reward = find("trail_rewardinv")
}

object BarrowsVarBits : VarBitReferences() {
    val killed =
        mapOf(
            Brother.Ahrim to find("barrows_killed_ahrim"),
            Brother.Dharok to find("barrows_killed_dharok"),
            Brother.Guthan to find("barrows_killed_guthan"),
            Brother.Karil to find("barrows_killed_karil"),
            Brother.Torag to find("barrows_killed_torag"),
            Brother.Verac to find("barrows_killed_verac"),
        )

    /** Set while a crypt monster kill is being counted; the overlay reads it for its tooltip. */
    val killed_monster = find("barrows_killed_monster")

    /** Reward potential, and the number the overlay's `killcount` component prints. */
    val killed_count = find("barrows_killed_count")

    /** Drives the [BarrowsLocs.stone_chest] multiloc: 0 closed, 1 open. */
    val chest_open = find("barrows_chest_open")
}

object BarrowsSeqs : SeqReferences() {
    val dharok_attack = find("barrow_dharok_slash")
    val guthan_attack = find("barrows_war_spear_stab")
    val torag_attack = find("barrow_torag_crush")
    val verac_attack = find("barrows_quarterstaff_attack")
    val verac_defend = find("barrows_quarterstaff_defend")
    val guthan_defend = find("barrow_guthan_defend")
    val karil_attack = find("barrows_repeating_crossbow_fire")

    /** The four named specials. Ahrim has no plain cast seq, so his aura doubles as the attack. */
    val ahrim_special = find("barrows_ahrim_blighted_aura")
    val karil_special = find("barrows_karil_tainted_shot")
    val guthan_special = find("barrows_guthan_infestation")
    val torag_special = find("barrows_torag_corruption")
    val verac_special = find("barrows_verac_desolation")
}

object BarrowsSpotanims : SpotanimReferences() {
    /** Spelled `ahirm` in the cache. Not a typo here. */
    val ahrim_aura = find("barrows_ahirm_blighted_aura")
    val karil_shot = find("barrows_karil_tainted_shot")
    val guthan_effect = find("barrows_guthan_effect")
    val torag_effect = find("barrows_torag_effect")
    val verac_desolation = find("barrows_verac_desolation")
    val crossbow_bolt = find("barrows_repeating_crossbow_fire_spotanim")
}

object BarrowsProjAnims : ProjAnimReferences() {
    /** The cache names no barrows-specific projectile, so these are the generic pair. */
    val magic = find("magic_spell")
    val bolt = find("bolt")
}

object BarrowsTimers : TimerReferences() {
    /**
     * Ticks while the player is inside the crypt: drains prayer and keeps the overlay in step.
     *
     * A server-only type with no cache encoder, so it needs no `packCache` - but `find` will not
     * mint an id either, and it has to be hand-added to `.data/symbols/.local/timer.sym`.
     */
    val crypt_tick = find("barrows_crypt_tick")
}
