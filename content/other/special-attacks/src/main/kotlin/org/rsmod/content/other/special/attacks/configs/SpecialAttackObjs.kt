package org.rsmod.content.other.special.attacks.configs

import org.rsmod.api.type.refs.obj.ObjReferences

typealias special_objs = SpecialAttackObjs

/** Special attack weapons that `BaseObjs` does not already name. */
object SpecialAttackObjs : ObjReferences() {
    val dragon_dagger = find("dragon_dagger")
    val dragon_dagger_p = find("dragon_dagger_p")
    val dragon_dagger_p_plus = find("dragon_dagger_p+")
    val dragon_dagger_p_plus_plus = find("dragon_dagger_p++")

    val abyssal_dagger = find("abyssal_dagger")
    val abyssal_dagger_p = find("abyssal_dagger_p")
    val abyssal_dagger_p_plus = find("abyssal_dagger_p+")
    val abyssal_dagger_p_plus_plus = find("abyssal_dagger_p++")

    val abyssal_whip_lava = find("abyssal_whip_lava")
    val abyssal_whip_ice = find("abyssal_whip_ice")
    val abyssal_tentacle = find("abyssal_tentacle")

    val dragon_mace = find("dragon_mace")
    val dragon_scimitar = find("dragon_scimitar")
    val dragon_scimitar_or = find("dragon_scimitar_ornament")
    val dragon_warhammer = find("dragon_warhammer")
    val dragon_warhammer_or = find("dragon_warhammer_ornament")
    val dragon_claws_or = find("dragon_claws_ornament")
    val dragon_battleaxe = find("dragon_battleaxe")

    val armadyl_godsword_or = find("agsg")
    val bandos_godsword = find("bgs")
    val bandos_godsword_or = find("bgsg")
    val saradomin_godsword = find("sgs")
    val saradomin_godsword_or = find("sgsg")
    val zamorak_godsword = find("zgs")
    val zamorak_godsword_or = find("zgsg")

    val abyssal_bludgeon = find("abyssal_bludgeon")
    val saradomin_sword = find("saradomin_sword")

    val granite_maul = find("granite_maul")
    val granite_maul_or = find("granite_maul_pretty")
    val granite_maul_plus = find("granite_maul_plus")
    val granite_maul_or_plus = find("granite_maul_pretty_plus")

    val magic_shortbow_i = find("magic_shortbow_i")
    val magic_longbow = find("magic_longbow")
}
