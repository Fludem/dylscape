package org.rsmod.content.skills.construction.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.game.type.comp.ComponentType

public object ConstructionInterfaces : InterfaceReferences() {
    /** The vanilla build menu. @see [ConstructionComponents] */
    public val furniture_creation: org.rsmod.game.type.interf.InterfaceType =
        find("poh_furniture_creation")
}

/**
 * The vanilla build menu, interface 458.
 *
 * The interface arrives empty: its thirty-one slots are bare layers with no ops, no events and no
 * children. Two clientscripts fill it, and neither is in the symbol tables by a useful name, so
 * both were read off their own bytecode.
 *
 * `clientscript,poh_furniture_creation_entry` (1404) draws one slot:
 * ```
 * poh_furniture_creation_entry(
 *     int    slot,        // 1..31; keyed through enum 1461 to component 458:(3 + slot)
 *     int    furniture,   // a `furniture` db row id -- the script reads the row itself
 *     int    level,       // level requirement, shown as "Level N"; a NEGATIVE value empties the slot
 *     int    buildable,   // 1 hides the "you cannot build this" marker, 0 shows it
 *     string materials,   // the cost line, split across two rows by the client
 * )
 * ```
 *
 * The proof that argument 2 is a db row rather than an obj is in the script: it passes that value
 * to `db_getfield` with the column constants `110 << 12` (`furniture:model_obj`, used for the item
 * model) and `110 << 12 | 1 << 4` (`furniture:name`, used for the op text).
 *
 * Script 1406 then lays the filled slots out and hides the rest; it takes the entry count (clamped
 * to thirty-one) and a flag. It reads each slot with `cc_find(component, 0)` -- child `0` being the
 * hidden sort key `entry` writes -- which is why **every entry must be drawn before the layout
 * runs**.
 *
 * Each slot gets `Build` as op 1 and `Examine` as op 10. Examine is answered by the client itself,
 * so only op 1 needs enabling, and it presses with **no subcomponent**.
 */
public object ConstructionComponents : ComponentReferences() {
    /**
     * The thirty-one furniture slots, in the order
     * [org.rsmod.content.skills.construction.scripts.BuildMenu] fills them. `slots[0]` is the
     * script's slot `1`.
     */
    public val slots: List<ComponentType> =
        (1..31).map { find("poh_furniture_creation:${it.toString().padStart(2, '0')}") }
}
