package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.game.movement.BlockWalk

/**
 * Pins the invisible attractor npcs to the trap tile they are spawned on.
 *
 * Out of the cache these inherit the same defaults as every other npc — `wanderRange = 5`,
 * `maxRange = 7`, `defaultMode = Wander` — which means a trap's own attractor would stroll off the
 * trap and drag the creatures it is pulling in with it. Every one of them is therefore locked down
 * three ways: `nomove` so it cannot physically step, `defaultMode = none` so no wander AI runs at
 * all, and `wanderRange = 0` so the wander processor skips it even if the mode is ever changed.
 *
 * `blockWalk = BlockWalk.None` matters separately: a creature has to be able to walk *onto* the
 * trap tile to reach the attractor, and the vanilla npcs block movement by default.
 *
 * `huntRange = 0` disables hunting on the attractor itself — the hunt relationship runs the other
 * way, from creature to trap.
 *
 * These are type edits, so the boot config sync applies them; no `packCache` needed.
 */
internal object HunterTrapNpcEditor : NpcEditor() {
    init {
        for (npc in HunterNpcs.armedTrapNpcs) {
            edit(npc) {
                contentGroup = HunterContent.hunter_trap_npc
                moveRestrict = nomove
                defaultMode = none
                blockWalk = BlockWalk.None
                wanderRange = 0
                maxRange = 1
                huntRange = 0
            }
        }
        // The disarmed twins attract nothing, so they get no content group -- but they still need
        // pinning, or a sprung trap's marker walks away before the trap is cleaned up.
        for (npc in HunterNpcs.disarmedTrapNpcs) {
            edit(npc) {
                moveRestrict = nomove
                defaultMode = none
                blockWalk = BlockWalk.None
                wanderRange = 0
                maxRange = 1
                huntRange = 0
            }
        }
    }
}
