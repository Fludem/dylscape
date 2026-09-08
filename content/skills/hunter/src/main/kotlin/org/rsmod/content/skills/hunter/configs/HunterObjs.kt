package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.refs.obj.ObjReferences

/**
 * Trap items and the things trapping produces.
 *
 * Both laid traps carry **"Lay" on iop1** — decoded from the cache, not assumed, so `onOpHeld1`
 * binds them with no op edit needed. `hunting_snare` ("Rabbit snare") carries the same op and is
 * included for a later pass; nothing binds it yet.
 *
 * The kebbit products are the reason deadfall works at all: each of the four deadfall creatures has
 * its own drop, and the loc chain is keyed by the same four names
 * (`claw`/`barbed`/`spike`/`sabre`).
 */
object HunterObjs : ObjReferences() {
    // Traps you lay.
    val bird_snare = find("hunting_ojibway_bird_snare")
    val box_trap = find("hunting_box_trap")

    // Net trap components.
    val rope = find("rope")
    val small_fishing_net = find("net")

    // Deadfall components.
    val knife = find("knife")
    val logs = find("logs")

    // Products.
    val raw_bird_meat = find("spit_raw_bird_meat")
    val feather = find("feather")
    val chinchompa = find("chinchompa_captured")
    val red_chinchompa = find("chinchompa_big_captured")
    val kebbit_claws = find("huntingbeast_claws")
    val kebbit_spike = find("huntingbeast_spike")
    val long_kebbit_spike = find("huntingbeast_bigspike")
    val kebbit_teeth = find("huntingbeast_sabreteeth")
}
