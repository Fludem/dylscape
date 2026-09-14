@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.custom.skillingtasks.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences

internal typealias skilling_task_npcs = SkillingTaskNpcs

/**
 * The Taskmaster borrows `con_contractor_3op`, Mahogany Homes' Amy. Nothing on this server spawns
 * her, and the cache already gives her `Talk-to` on op1, `Contract` on op3, `Last-tier contract` on
 * op4 and `Rewards` on op5 - the same shape as a slayer master, so the right-click menu offers a
 * task and the shop without a dialogue. `SkillingTaskSpawnTest` pins those slots.
 */
object SkillingTaskNpcs : NpcReferences() {
    val taskmaster = find("con_contractor_3op")
}

/**
 * Pins the Taskmaster to her tile and renames her. The cache never writes RSMod's `wanderRange`
 * opcode, so without this she would roam the five-tile default box off her post. Both are type
 * edits applied by the boot-time config sync, not `packCache`.
 */
internal object SkillingTaskNpcEditor : NpcEditor() {
    init {
        edit(SkillingTaskNpcs.taskmaster) {
            name = "Taskmaster"
            wanderRange = 0
        }
    }
}
