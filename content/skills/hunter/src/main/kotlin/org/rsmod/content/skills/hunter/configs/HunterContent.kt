package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.refs.content.ContentReferences

/**
 * Content groups for hunter.
 *
 * Traps are driven entirely off these rather than off per-type bindings, because every trap kind
 * shares one lifecycle: `hunter_trap_armed` carries the "Dismantle"/"Investigate" states and
 * `hunter_trap_sprung` the "Check"/"Reset" ones, so two op bindings cover all four methods and
 * every creature variant within them.
 *
 * `hunter_trap_npc` is the important one: it is the key the AI npc-on-npc event is dispatched
 * under, so tagging all four armed trap npcs into it lets a single handler catch every creature
 * walking into every kind of trap.
 */
object HunterContent : ContentReferences() {
    val hunter_trap_creature = find("hunter_trap_creature")
    val hunter_trap_npc = find("hunter_trap_npc")
    val hunter_trap_armed = find("hunter_trap_armed")
    val hunter_trap_sprung = find("hunter_trap_sprung")
    val hunter_trap_obj = find("hunter_trap_obj")
    val hunter_young_tree = find("hunter_young_tree")
    val hunter_roped_tree = find("hunter_roped_tree")
    val hunter_deadfall_boulder = find("hunter_deadfall_boulder")
}
