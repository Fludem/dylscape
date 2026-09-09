package org.rsmod.content.skills.magic.combat.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.api.type.refs.synth.SynthReferences

public typealias combat_seqs = CombatSpellSeqs

public typealias combat_spotanims = CombatSpellSpotanims

public typealias combat_synths = CombatSpellSynths

public typealias combat_objs = CombatSpellObjs

public object CombatSpellSeqs : SeqReferences() {
    val confuse = find("human_castconfuse")
    val confuse_staff = find("human_castconfuse_staff")
    val weaken = find("human_castweaken")
    val weaken_staff = find("human_castweaken_staff")
    val curse = find("human_castcurse")
    val curse_staff = find("human_castcurse_staff")
    val enfeeble = find("human_castenfeeble")
    val enfeeble_staff = find("human_castenfeeble_staff")
    val stun = find("human_caststun")
    val stun_staff = find("human_caststun_staff")
    val bind = find("human_castentangle")
    val bind_staff = find("human_castentangle_staff")
    val crumble_undead = find("human_castcrumbleundead")
    val crumble_undead_staff = find("human_castcrumbleundead_staff")
    val iban_blast = find("human_castibanblast")
    val magic_dart = find("slayer_magicdart_cast")
    val god_spell = find("human_casting")
    /** Rush and Blitz. */
    val ancient_single = find("zaros_casting")
    /** Burst and Barrage. */
    val ancient_multi = find("zaros_vertical_casting")
}

public object CombatSpellSpotanims : SpotanimReferences() {
    val confuse_casting = find("confuse_casting")
    val confuse_travel = find("confuse_travel")
    val confuse_impact = find("confuse_impact")
    val weaken_casting = find("weaken_casting")
    val weaken_travel = find("weaken_travel")
    val weaken_impact = find("weaken_impact")
    val curse_casting = find("curse_casting")
    val curse_travel = find("curse_travel")
    val curse_impact = find("curse_impact")
    val vulnerability_casting = find("vulnerability_casting")
    val vulnerability_travel = find("vulnerability_travel")
    val vulnerability_impact = find("vulnerability_impact")
    val enfeeble_casting = find("enfeeble_casting")
    val enfeeble_travel = find("enfeeble_travel")
    val enfeeble_impact = find("enfeeble_impact")
    val stun_casting = find("stun_casting")
    val stun_travel = find("stun_travel")
    val stun_impact = find("stun_impact")
    val bind_casting = find("entangle_casting")
    val bind_travel = find("entangle_travel")
    val bind_impact = find("bind_impact")
    val snare_impact = find("snare_impact")
    val entangle_impact = find("entangle_impact")
    val crumble_undead_casting = find("crumbleundead_casting")
    val crumble_undead_travel = find("crumbleundead_travel")
    val crumble_undead_impact = find("crumbleundead_impact")
    val iban_blast_casting = find("ibanblast_casting")
    val iban_blast_travel = find("ibanblast_travel")
    val iban_blast_impact = find("ibanblast_impact")
    val magic_dart_travel = find("slayer_magicdart_travel")
    val magic_dart_impact = find("slayer_magicdart_impact")
    val saradomin_strike = find("saradomin_lightning")
    val claws_of_guthix = find("gunthix_claw")
    val flames_of_zamorak = find("zamorak_flame")

    val ice_rush_travel = find("ice_rush_travel")
    val ice_rush_impact = find("ice_rush_impact")
    val ice_burst_travel = find("ice_burst_travel")
    val ice_burst_impact = find("ice_burst_impact")
    val ice_blitz_travel = find("ice_blitz_travel")
    val ice_blitz_impact = find("ice_blitz_impact")
    val ice_barrage_travel = find("ice_barrage_travel")
    val ice_barrage_impact = find("ice_barrage_impact")
    val blood_rush_travel = find("blood_rush_travel")
    val blood_rush_impact = find("blood_rush_impact")
    val blood_burst_impact = find("spell_blood_burst_impact")
    val blood_blitz_travel = find("blood_blitz_travel")
    val blood_blitz_impact = find("blood_blitz_impact")
    val blood_barrage_impact = find("spell_blood_barrage_impact")
    val shadow_rush_travel = find("shadow_rush_travel")
    val shadow_rush_impact = find("shadow_rush_impact")
    val shadow_burst_impact = find("shadow_burst_impact")
    val shadow_blitz_travel = find("shadow_blitz_travel")
    val shadow_blitz_impact = find("shadow_blitz_impact")
    val shadow_barrage_impact = find("shadow_barrage_impact")
    val smoke_rush_travel = find("smoke_rush_travel")
    val smoke_rush_impact = find("smoke_rush_impact")
    val smoke_burst_travel = find("smoke_burst_travel")
    val smoke_burst_impact = find("smoke_burst_impact")
    val smoke_blitz_travel = find("smoke_blitz_travel")
    val smoke_blitz_impact = find("smoke_blitz_impact")
    val smoke_barrage_travel = find("smoke_barrage_travel")
    val smoke_barrage_impact = find("smoke_barrage_impact")
}

/** God spells and Iban Blast have no named sound in this cache and are silent on purpose. */
public object CombatSpellSynths : SynthReferences() {
    val confuse_cast = find("confuse_cast_and_fire")
    val confuse_hit = find("confuse_hit")
    val weaken_all = find("weaken_all")
    val curse_cast = find("curse_cast_and_fire")
    val curse_hit = find("curse_hit")
    val vulnerability_all = find("vulnerability_all")
    val enfeeble_cast = find("enfeeble_cast_and_fire")
    val enfeeble_hit = find("enfeeble_hit")
    val stun_all = find("stun_all")
    val bind_cast = find("bind_cast")
    val bind_impact = find("bind_impact")
    val snare_all = find("snare_all")
    val entangle_cast = find("entangle_cast_and_fire")
    val entangle_hit = find("entangle_hit")
    val crumble_cast = find("crumble_cast_and_fire")
    val crumble_hit = find("crumble_hit")
    val magic_dart_hit = find("magic_dart_hit")

    val ice_cast = find("ice_cast")
    val blood_cast = find("blood_cast")
    val shadow_cast = find("shadow_cast")
    val smoke_cast = find("smoke_cast")
    val ice_rush_impact = find("ice_rush_impact")
    val ice_burst_impact = find("ice_burst_impact")
    val ice_blitz_impact = find("ice_blitz_impact")
    val ice_barrage_impact = find("ice_barrage_impact")
    val blood_rush_impact = find("blood_rush_impact")
    val blood_burst_impact = find("blood_burst_impact")
    val blood_blitz_impact = find("blood_blitz_impact")
    val blood_barrage_impact = find("blood_barrage_impact")
    val shadow_rush_impact = find("shadow_rush_impact")
    val shadow_burst_impact = find("shadow_burst_impact")
    val shadow_blitz_impact = find("shadow_blitz_impact")
    val shadow_barrage_impact = find("shadow_barrage_impact")
    val smoke_rush_impact = find("smoke_rush_impact")
    val smoke_burst_impact = find("smoke_burst_impact")
    val smoke_blitz_impact = find("smoke_blitz_impact")
    val smoke_barrage_impact = find("smoke_barrage_impact")
}

public object CombatSpellObjs : ObjReferences() {
    val saradomin_cape = find("saradomin_cape")
    val guthix_cape = find("guthix_cape")
    val zamorak_cape = find("zamorak_cape")
}
