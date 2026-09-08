package org.rsmod.content.custom.droptables.configs

import org.rsmod.api.type.refs.npc.NpcReferences

internal typealias drop_npcs = DropNpcs

/**
 * Npcs we assign drop tables to. Scoped to what a new character actually meets around Lumbridge;
 * [org.rsmod.api.config.refs.BaseNpcs] already exposes the `man`/`woman` variants.
 */
object DropNpcs : NpcReferences() {
    val chicken = find("chicken")
    val chicken_brown = find("chicken_brown")

    val cow = find("cow")
    val cow2 = find("cow2")
    val cow3 = find("cow3")
    val cow_beef = find("cow_beef")
    val cow2_calf = find("cow2_calf")
    val cow3_calf = find("cow3_calf")

    val rat = find("rat")
    val giantrat = find("giantrat")
    val giantrat1 = find("giantrat1")
    val giantrat2 = find("giantrat2")
    val giantrat3 = find("giantrat3")
    val giantrat1_2 = find("giantrat1_2")
    val giantrat1_3 = find("giantrat1_3")

    val man4 = find("man4")

    val goblin = find("goblin")
    val goblin_unarmed_melee_1 = find("goblin_unarmed_melee_1")
    val goblin_unarmed_melee_2 = find("goblin_unarmed_melee_2")
    val goblin_unarmed_melee_3 = find("goblin_unarmed_melee_3")
    val goblin_unarmed_melee_4 = find("goblin_unarmed_melee_4")
    val goblin_unarmed_melee_5 = find("goblin_unarmed_melee_5")
    val goblin_unarmed_melee_6 = find("goblin_unarmed_melee_6")
    val goblin_unarmed_melee_7 = find("goblin_unarmed_melee_7")
    val goblin_unarmed_melee_8 = find("goblin_unarmed_melee_8")
    val goblin_armed_melee_1 = find("goblin_armed_melee_1")
    val goblin_armed_melee_2 = find("goblin_armed_melee_2")
    val goblin_armed_melee_3 = find("goblin_armed_melee_3")
    val goblin_armed_melee_4 = find("goblin_armed_melee_4")
}
