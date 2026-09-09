package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.type.editors.obj.ObjEditor

/**
 * Tags every dose in [Potions] into the `potion` content group.
 *
 * This is the one fact about drinking that has to reach the cache. `HeldInteractions.opHeld1`
 * dispatches on an obj's content group, so the tag is what lets a single hook cover two hundred
 * objs; and the bank reads the same group -- `BankInvScript` and `PlayerExtensions` both treat
 * `content.potion` exactly as they treat `content.food` -- which means tagging lights up drinking
 * from the bank interface for free.
 *
 * Derived from [Potions] rather than written out again, so the tag set and the table cannot drift
 * apart. Never derived from a cache-wide filter over dosed objs: such a filter would sweep in the
 * Nightmare Zone, Castle Wars, Chambers, Deadman and Trailblazer copies, and -- worse -- the ale
 * kegs, which `ConsumableEditor` has already claimed for `food`. An obj carries exactly **one**
 * content group, so a collision there would silently break drinking or eating for whichever editor
 * lost the race. `PotionCoverageTest` asserts the two tables never intersect.
 *
 * `ConsumableEditor` deliberately left this group empty for us; `HerbloreDump` confirms the
 * installed cache has nothing in it, so this tags rather than overwrites.
 *
 * **Note that this is additive in the cache: deleting a row later will not untag the obj.** Comment
 * a row out rather than removing it if one ever needs to go, and read the id list out of
 * `HerbloreDump` before this ships anywhere real -- a wrong tag here cannot be taken back.
 */
internal object PotionEditor : ObjEditor() {
    init {
        for (row in Potions.rows) {
            edit(row.obj) { contentGroup = content.potion }
        }
    }
}
