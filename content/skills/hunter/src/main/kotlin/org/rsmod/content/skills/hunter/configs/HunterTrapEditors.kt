package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.content.skills.hunter.TrapKind
import org.rsmod.game.type.loc.LocType

/**
 * Tags the trap items so one `onOpHeld1` binding covers both.
 *
 * Neither item needs an op edit: the dump confirms "Lay" already sits on **iop1** for the bird
 * snare, the box trap and the (unimplemented) rabbit snare. All that is added here is the content
 * group and which method the item belongs to.
 */
internal object HunterTrapObjEditor : ObjEditor() {
    init {
        edit(HunterObjs.bird_snare) {
            contentGroup = HunterContent.hunter_trap_obj
            param[HunterParams.trap_kind] = TrapKind.SNARE
        }
        edit(HunterObjs.box_trap) {
            contentGroup = HunterContent.hunter_trap_obj
            param[HunterParams.trap_kind] = TrapKind.BOX
        }
    }
}

/**
 * Groups the trap locs so the check/dismantle bindings do not need one entry per creature variant.
 *
 * The split is by what a player can *do*, not by method: `hunter_trap_armed` is every state
 * carrying "Dismantle"/"Investigate", `hunter_trap_sprung` every state carrying "Check"/"Reset".
 * Two bindings then cover four methods and sixteen creatures.
 *
 * The transient `trapping`/`failing` states are deliberately left ungrouped — they carry no ops in
 * the cache, so there is nothing to bind and no way for a player to interact mid-animation.
 */
internal object HunterTrapLocEditor : LocEditor() {
    init {
        for (loc in HunterLocs.armedStates) {
            tag(loc, HunterContent.hunter_trap_armed)
        }
        for (loc in HunterLocs.sprungStates) {
            tag(loc, HunterContent.hunter_trap_sprung)
        }
        for (loc in HunterLocs.youngTrees) {
            tag(loc, HunterContent.hunter_young_tree)
        }
        for (loc in HunterLocs.ropedTrees) {
            tag(loc, HunterContent.hunter_roped_tree)
        }
        tag(HunterLocs.deadfall_boulder, HunterContent.hunter_deadfall_boulder)
    }

    private fun tag(type: LocType, group: org.rsmod.game.type.content.ContentGroupType) {
        edit(type) { contentGroup = group }
    }
}
