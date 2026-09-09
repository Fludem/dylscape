package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

/**
 * The kit waiting in a player's bank the moment they step off Tutorial Island.
 *
 * This is a house rule, not anything the live game does: it exists so a new account on a private
 * server can go straight to the content rather than spend its first hours grinding a starter set.
 * It lands in the **bank** rather than the inventory both because it does not fit in 28 slots and
 * because the Account Guide has just explained what a bank is for.
 *
 * Everything here is a plain, freely-tradeable item, so nothing in it can desync a player's state
 * if it is later dropped, sold or banked somewhere else.
 */
object TutorialKitObjs : ObjReferences() {
    // Melee: a full iron set and every scimitar up to rune.
    val iron_full_helm = find("iron_full_helm")
    val iron_platebody = find("iron_platebody")
    val iron_platelegs = find("iron_platelegs")
    val iron_kiteshield = find("iron_kiteshield")
    val bronze_scimitar = find("bronze_scimitar")
    val iron_scimitar = find("iron_scimitar")
    val steel_scimitar = find("steel_scimitar")
    val black_scimitar = find("black_scimitar")
    val mithril_scimitar = find("mithril_scimitar")
    val adamant_scimitar = find("adamant_scimitar")
    val rune_scimitar = find("rune_scimitar")

    // Ranged.
    val shortbow = find("shortbow")
    val bronze_arrow = find("bronze_arrow")
    val leather_body = find("leather_armour")
    val leather_chaps = find("leather_chaps")

    // Magic: the gold-trimmed wizard set, a staff of air and the elemental runes.
    val wizard_hat_g = find("bluewizhat_trim_gold")
    val wizard_robe_g = find("wizards_robe_trim_gold")
    val wizard_bottoms_g = find("blue_skirt_trim_gold")
    val staff_of_air = find("staff_of_air")
    val air_rune = find("airrune")
    val water_rune = find("waterrune")
    val earth_rune = find("earthrune")
    val fire_rune = find("firerune")
    val mind_rune = find("mindrune")

    val coins = find("coins")

    /** The kit as it is banked: every entry is `obj to count`. */
    val all: List<Pair<ObjType, Int>> =
        listOf(
            iron_full_helm to 1,
            iron_platebody to 1,
            iron_platelegs to 1,
            iron_kiteshield to 1,
            bronze_scimitar to 1,
            iron_scimitar to 1,
            steel_scimitar to 1,
            black_scimitar to 1,
            mithril_scimitar to 1,
            adamant_scimitar to 1,
            rune_scimitar to 1,
            shortbow to 1,
            bronze_arrow to 100,
            leather_body to 1,
            leather_chaps to 1,
            wizard_hat_g to 1,
            wizard_robe_g to 1,
            wizard_bottoms_g to 1,
            staff_of_air to 1,
            air_rune to 200,
            water_rune to 200,
            earth_rune to 200,
            fire_rune to 200,
            mind_rune to 200,
            coins to 10_000,
        )
}
