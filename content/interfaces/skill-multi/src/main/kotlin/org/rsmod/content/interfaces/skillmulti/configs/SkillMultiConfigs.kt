package org.rsmod.content.interfaces.skillmulti.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.type.comp.ComponentType

object SkillMultiInterfaces : InterfaceReferences() {
    /** The vanilla "make" menu. @see [SkillMultiComponents] */
    val skillmulti = find("skillmulti")
}

/**
 * The vanilla make-menu, interface 270.
 *
 * The interface draws itself from a single call to `clientscript,skillmulti_setup` (2046), whose
 * signature is not in the symbol tables -- only "13 ints and a string". It was recovered by
 * decoding the script's own bytecode out of the cache (`ClientScriptTypeDecoder` already exposes
 * the opcode stream), and reads:
 * ```
 * skillmulti_setup(
 *     int    type,          // picks the op verb out of enum 1809; 13 is "Smelt"
 *     int    maxQuantity,   // clamped by the client to 1..28
 *     int    obj0 .. obj9,  // the ten item slots, -1 for an empty one
 *     int    quantity,      // the initially selected quantity, clamped to 1..maxQuantity
 *     string title,
 * )
 * ```
 *
 * The proof of the slot layout is in the script itself: it tests `obj1` through `obj9` against -1
 * in turn to count how many buttons to draw, and bails out with "You can't think of any options."
 * when `obj0` is -1.
 *
 * **The click comes back with the quantity in the subcomponent.** The item buttons carry no static
 * events in the cache, so nothing reaches the server until we call `if_setevents` on them. When one
 * is pressed the client does not send the button directly -- `proc,skillmulti_itembutton_triggered`
 * re-targets it at the chosen quantity first, exactly the way the chatbox option menu carries its
 * option index. So the button we receive names the item, and its `comsub` is how many to make.
 */
object SkillMultiComponents : ComponentReferences() {
    val slot_a = find("skillmulti:a")
    val slot_b = find("skillmulti:b")
    val slot_c = find("skillmulti:c")
    val slot_d = find("skillmulti:d")
    val slot_e = find("skillmulti:e")
    val slot_f = find("skillmulti:f")
    val slot_g = find("skillmulti:g")
    val slot_h = find("skillmulti:h")
    val slot_i = find("skillmulti:i")
    val slot_j = find("skillmulti:j")

    /**
     * The ten item buttons, in the order [org.rsmod.content.interfaces.skillmulti.SkillMulti] fills
     * them.
     */
    val slots: List<ComponentType> =
        listOf(slot_a, slot_b, slot_c, slot_d, slot_e, slot_f, slot_g, slot_h, slot_i, slot_j)
}
