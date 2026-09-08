package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.builders.controller.ControllerBuilder
import org.rsmod.api.type.builders.varcon.VarConBuilder

/**
 * One controller per laid trap, keyed on the trap's tile.
 *
 * A controller is the right home for trap state because traps *are* tile-keyed and need a tick of
 * their own: `conRepo.findExact(coords, type)` finds the trap under any op, `onAiConTimer` gives it
 * a self-rearming per-trap tick without a global scan, and `Controller.duration` is a free backstop
 * that removes the trap even if the state machine ever wedges. There is no world-tick script hook
 * to hang a singleton registry off anyway.
 *
 * Controllers and their vars are name-only server types with no cache encoder, so adding these
 * costs nothing and needs no `packCache`.
 */
object HunterControllers : ControllerBuilder() {
    val hunter_trap = build("hunter_trap")
}

object HunterVarCons : VarConBuilder() {
    /** `PlayerUid.packed` of whoever laid the trap. Only they can check or dismantle it. */
    val trap_owner = build("hunter_trap_owner")

    /** Which of the four methods this is; see `TrapKind`. */
    val trap_kind = build("hunter_trap_kind")

    /** Where in the lifecycle the trap is; see `TrapState`. */
    val trap_state = build("hunter_trap_state")

    /** Npc type id of whatever walked in, or 0 while the trap is still empty. */
    val trap_creature = build("hunter_trap_creature")

    /** `mapClock` cycle the current state was entered on, for timing the transient states. */
    val trap_stage_tick = build("hunter_trap_stage_tick")

    /** `NpcUid.packed` of the invisible attractor standing on the trap. */
    val trap_npc_uid = build("hunter_trap_npc_uid")

    /** Loc type id currently placed, so the engine can replace it without re-deriving. */
    val trap_loc = build("hunter_trap_loc")

    /** Consecutive ticks the owner has been absent; a trap collapses once this runs out. */
    val trap_abandoned = build("hunter_trap_abandoned")
}
