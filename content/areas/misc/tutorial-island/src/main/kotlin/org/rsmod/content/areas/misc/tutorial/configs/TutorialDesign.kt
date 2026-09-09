package org.rsmod.content.areas.misc.tutorial.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

object TutorialDesignInterfaces : InterfaceReferences() {
    /** The character-design screen, interface 679. @see [TutorialDesignComponents] */
    val player_design = find("player_design")
}

/**
 * The character-design screen's buttons.
 *
 * Unusually for this revision the interface names every one of its own components, so none of this
 * had to be guessed: each style row is a `_left`/`_right` pair carrying `Op1`, and the two gender
 * buttons and `confirm` carry `Op1` too. Because the buttons are server-driven `Op1`s rather than
 * clientscript-local ones, the screen has no client-side state at all -- every press comes to us
 * and the resulting look is pushed back out as appearance extended-info. That is why this works
 * without an incoming "design chosen" packet, which this revision's protocol does not have.
 */
object TutorialDesignComponents : ComponentReferences() {
    val head_left = find("player_design:head_left")
    val head_right = find("player_design:head_right")
    val jaw_left = find("player_design:jaw_left")
    val jaw_right = find("player_design:jaw_right")
    val torso_left = find("player_design:torso_left")
    val torso_right = find("player_design:torso_right")
    val arms_left = find("player_design:arms_left")
    val arms_right = find("player_design:arms_right")
    val hands_left = find("player_design:hands_left")
    val hands_right = find("player_design:hands_right")
    val legs_left = find("player_design:legs_left")
    val legs_right = find("player_design:legs_right")
    val feet_left = find("player_design:feet_left")
    val feet_right = find("player_design:feet_right")
    val hair_left = find("player_design:hair_left")
    val hair_right = find("player_design:hair_right")
    val torso_col_left = find("player_design:torso_col_left")
    val torso_col_right = find("player_design:torso_col_right")
    val legs_col_left = find("player_design:legs_col_left")
    val legs_col_right = find("player_design:legs_col_right")
    val feet_col_left = find("player_design:feet_col_left")
    val feet_col_right = find("player_design:feet_col_right")
    val skin_left = find("player_design:skin_left")
    val skin_right = find("player_design:skin_right")
    val gender_male = find("player_design:gender_male")
    val gender_female = find("player_design:gender_female")
    val confirm = find("player_design:confirm")
}

/**
 * What each slot of the appearance's identkit and colour arrays can hold.
 *
 * The style ids are **decoded from the cache's identkit config**, not typed from a wiki: identkit
 * opcode 1 is `bodypart`, 0-6 being the male slots and 7-13 the female ones, both in the order
 * hair, jaw, torso, arms, hands, legs, feet. Every id below is a kit the client already has a model
 * for in that slot and for that gender, so no combination this screen can produce is invalid.
 *
 * The lists are longer than live Tutorial Island's, which offers only each slot's original handful.
 * The later ids are perfectly good kits (they are what holiday and quest outfits swap in), so the
 * whole decoded set is offered rather than an arbitrary prefix of it.
 *
 * The **colour** counts are the client's own palette sizes. Those live in the client rather than
 * the cache, so unlike the styles they cannot be decoded here; they are the standard palettes and
 * are clamped by [ColourSlot.count] so an index can never run off the end of one.
 */
object TutorialDesignData {
    /** Index into `Appearance`'s identkit array, paired with the ids valid for it. */
    enum class StyleSlot(val index: Int, val male: List<Int>, val female: List<Int>) {
        HAIR(0, HAIR_MALE, HAIR_FEMALE),
        JAW(1, JAW_MALE, JAW_FEMALE),
        TORSO(2, TORSO_MALE, TORSO_FEMALE),
        ARMS(3, ARMS_MALE, ARMS_FEMALE),
        HANDS(4, HANDS_MALE, HANDS_FEMALE),
        LEGS(5, LEGS_MALE, LEGS_FEMALE),
        FEET(6, FEET_MALE, FEET_FEMALE);

        fun options(female: Boolean): List<Int> = if (female) this.female else male
    }

    /** Index into `Appearance`'s colour array, paired with that palette's size. */
    enum class ColourSlot(val index: Int, val count: Int) {
        HAIR(0, 25),
        TORSO(1, 29),
        LEGS(2, 29),
        FEET(3, 6),
        SKIN(4, 8),
    }
}

private val HAIR_MALE = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9) + (129..134) + (144..151) + (201..247)
private val JAW_MALE = (10..17).toList() + (111..117) + listOf(153)
private val TORSO_MALE = (18..25).toList() + (105..110) + (254..259)
private val ARMS_MALE = (26..32).toList() + (84..88) + (260..264)
private val HANDS_MALE = listOf(33, 34, 35)
private val LEGS_MALE = (36..41).toList() + (100..104) + (281..291)
private val FEET_MALE = listOf(42, 43, 44, 82)

private val HAIR_FEMALE = (45..55).toList() + (118..128) + (141..143) + listOf(152) + (154..200)
private val JAW_FEMALE = (292..306).toList()
private val TORSO_FEMALE = (56..60).toList() + (89..94) + (265..273)
private val ARMS_FEMALE = (61..66).toList() + (95..99) + (248..253)
private val HANDS_FEMALE = listOf(67, 68, 69)
private val LEGS_FEMALE = (70..78).toList() + (135..140) + (274..280)
private val FEET_FEMALE = listOf(79, 80, 81, 83)
