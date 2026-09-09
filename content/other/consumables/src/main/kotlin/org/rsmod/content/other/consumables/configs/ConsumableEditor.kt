package org.rsmod.content.other.consumables.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.obj.ObjEditor

/**
 * Tags every consumable into the `food` content group.
 *
 * This is the one fact about eating that has to reach the cache. `HeldInteractions.opHeld1`
 * dispatches on an obj's content group, so the tag is what lets a single hook cover several hundred
 * objs; and the bank reads the same group to decide whether to offer its bankside `Eat` extra-op,
 * which means tagging lights up eating from the bank interface for free.
 *
 * Derived from [Consumables] rather than written out again, so the tag set and the table cannot
 * drift apart.
 *
 * Everything goes into `food`, drinks included, and nothing into `potion`. An obj carries exactly
 * one content group and a group edit is additive once packed, so claiming `potion` for a jug of
 * wine today would take the slot away from the Herblore module that will want it. `potion` is left
 * empty and unclaimed.
 *
 * Note that this is additive in the cache: deleting a row later will not untag the obj. Comment a
 * row out rather than removing it if one ever needs to go.
 */
internal object ConsumableEditor : ObjEditor() {
    init {
        for (row in Consumables.rows) {
            edit(row.obj) { contentGroup = content.food }
        }
    }
}
