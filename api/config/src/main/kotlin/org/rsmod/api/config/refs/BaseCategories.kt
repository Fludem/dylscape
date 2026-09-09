@file:Suppress("SpellCheckingInspection", "unused")

package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.category.CategoryReferences

typealias categories = BaseCategories

object BaseCategories : CategoryReferences() {
    val staff = find("staff")
    val throwing_weapon = find("throwing_weapon")
    val spear = find("spear")
    val crossbow = find("crossbow")
    val rack_bolts = find("rack_bolts")
    val arrows = find("arrows")
    val crossbow_bolt = find("crossbow_bolt")
    val bow = find("bow")
    val halberd = find("halberd")
    val ogre_arrows = find("ogre_arrows")
    val chargebow = find("chargebow")
    val rune = find("rune")
    val vampyres = find("vampyres")
    val arrows_training = find("arrows_training")
    val ballista = find("ballista")
    val chinchompa = find("chinchompa")
    val kebbit_bolts = find("kebbit_bolts")
    val javelin = find("javelin")
    val dragon_arrow = find("dragon_arrow")
    val dinhs_bulwark = find("dinhs_bulwark")
    val atlatl_dart = find("atlatl_dart")

    val attacktype_stab = find("attacktype_stab")
    val attacktype_slash = find("attacktype_slash")
    val attacktype_crush = find("attacktype_crush")

    // The other two thirds of OSRS's npc `attacktype` domain. Upstream authored only the melee
    // three because `NvPCombat` could only build a melee attack; these are what let an npc say it
    // shoots or casts. Their ids live in `.data/symbols/.local/category.sym`.
    val attacktype_ranged = find("attacktype_ranged")
    val attacktype_magic = find("attacktype_magic")
}
