package org.rsmod.content.custom.tollgate.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal typealias tollgate_locs = TollGateLocs

internal typealias tollgate_npcs = TollGateNpcs

internal typealias tollgate_varps = TollGateVarps

/**
 * The Al Kharid toll gate is two wall locs. The map carries the `closed` pair, which are multilocs
 * keyed on [TollGateVarps.princequest]: values `0..99` resolve to the `_2op` pair (`Open` and
 * `Pay-toll(10gp)`), anything higher to the `_1op` pair (`Open` only). Op scripts bind to the
 * resolved variants, so the varp check for "is the toll still owed" happens in the cache data
 * rather than in code.
 *
 * All four closed variants and the two open ones share the same models (1509/1511), so an opened
 * toll gate is simply the plain `metalgateopen` leaf rotated onto the next tile.
 */
object TollGateLocs : LocReferences() {
    val closed_left = find("kharidmetalgateclosedl")
    val closed_right = find("kharidmetalgateclosedr")
    val toll_left = find("kharidmetalgateclosedl_2op")
    val toll_right = find("kharidmetalgateclosedr_2op")
    val free_left = find("kharidmetalgateclosedl_1op")
    val free_right = find("kharidmetalgateclosedr_1op")
    val open_left = find("metalgateopenl")
    val open_right = find("metalgateopenr")
}

object TollGateNpcs : NpcReferences() {
    /** Stands on the Lumbridge side of the gate. */
    val borderguard_lumbridge = find("borderguard1")
    /** Stands on the Al Kharid side of the gate. */
    val borderguard_alkharid = find("borderguard2")
}

/**
 * Pins the border guards to the gate.
 *
 * Out of the cache both guards keep the default `Wander` behaviour with a non-zero range, so they
 * stroll away from the gate they are meant to be manning. `wanderRange = 0` makes the wander
 * processor skip them, keeping each on its spawn tile. This is a type edit, so the server's
 * boot-time config sync applies it — no `packCache` needed.
 */
internal object TollGateNpcEditor : NpcEditor() {
    init {
        edit(tollgate_npcs.borderguard_lumbridge) { wanderRange = 0 }
        edit(tollgate_npcs.borderguard_alkharid) { wanderRange = 0 }
    }
}

object TollGateVarps : VarpReferences() {
    val princequest = find("princequest")
}
