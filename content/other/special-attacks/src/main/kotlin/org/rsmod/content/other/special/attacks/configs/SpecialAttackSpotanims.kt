package org.rsmod.content.other.special.attacks.configs

import org.rsmod.api.type.refs.spot.SpotanimReferences

typealias special_spots = SpecialAttackSpotanims

object SpecialAttackSpotanims : SpotanimReferences() {
    val lumber_up_red = find("dragon_smallaxe_swoosh_spotanim", 37292951)
    val lumber_up_silver = find("crystal_smallaxe_swoosh_spotanim", 139193746)
    val fishstabber_silver = find("sp_attackglow_crystal", 8691321)
    val dragon_longsword = find("sp_attack_cleave_spotanim", 13013927)

    val dragon_dagger = find("sp_attack_puncture_spotanim")
    val abyssal_dagger = find("abyssal_dagger_special_spotanim")
    val abyssal_whip = find("sp_attack_abyssal_whip")
    val dragon_mace = find("sp_attack_shatter_spotanim")
    val dragon_scimitar = find("sp_attack_dragon_scimitar_trail_spotanim")
    val dragon_warhammer = find("dragon_warhammer_sa_spotanim")
    val dragon_claws = find("dragon_claws_spot")
    val armadyl_godsword = find("dh_sword_update_armadyl_special_spotanim")
    val armadyl_godsword_or = find("armadyl_special_spotanim_gold")
    val bandos_godsword = find("dh_sword_update_bandos_special_spotanim")
    val bandos_godsword_or = find("bandos_special_spotanim_gold")
    val saradomin_godsword = find("dh_sword_update_saradomin_special_spotanim")
    val saradomin_godsword_or = find("saradomin_special_spotanim_gold")
    val zamorak_godsword = find("dh_sword_update_zamorak_special_spotanim")
    val zamorak_godsword_or = find("zamorak_special_spotanim_gold")
    val abyssal_bludgeon = find("abyssal_miasma_spotanim_bludgeon")
    val saradomin_sword = find("dh_sword_update_saradomin_god_special_spotanim")
    val saradomin_sword_lightning = find("godwars_saradomin_magic_attack_spotanim")
    val granite_maul = find("sp_attack_maul_spotanim")
    val magic_shortbow = find("sp_attack_snapshot_spotanim")
    val glow_arrow_launch = find("sp_attack_glow_arrow_launch")
    val glow_arrow_travel = find("sp_attack_glow_arrow_travel")
}
