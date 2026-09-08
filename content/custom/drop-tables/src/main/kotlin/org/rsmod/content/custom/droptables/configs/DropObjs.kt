package org.rsmod.content.custom.droptables.configs

import org.rsmod.api.type.refs.obj.ObjReferences

internal typealias drop_objs = DropObjs

/**
 * Objs referenced by our drop tables that [org.rsmod.api.config.refs.BaseObjs] does not already
 * expose. Anything already in `objs` should be used from there instead of redeclared here.
 */
object DropObjs : ObjReferences() {
    val feather = find("feather")
    val raw_chicken = find("raw_chicken")
    val raw_beef = find("raw_beef")
    val cow_hide = find("cow_hide")
    val raw_rat_meat = find("raw_rat_meat")

    val bronze_bolts = find("bolt")
    val bronze_med_helm = find("bronze_med_helm")
    val bronze_sq_shield = find("bronze_sq_shield")
    val bronze_spear = find("bronze_spear")
    val iron_dagger = find("iron_dagger")
    val goblin_mail = find("goblin_armour")

    val bodyrune = find("bodyrune")

    val chefs_hat = find("chefs_hat")
    val brass_necklace = find("brass_necklace")
    val air_talisman = find("air_talisman")
    val earth_talisman = find("earth_talisman")
    val fishing_bait = find("fishing_bait")
    val copper_ore = find("copper_ore")
}
