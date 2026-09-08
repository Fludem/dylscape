package org.rsmod.content.custom.teleports

import org.rsmod.map.CoordGrid

/**
 * Wilderness level for [coord], or `0` when the tile is not in the Wilderness.
 *
 * The repo has no Wilderness system yet: `.data/symbols/area.sym` names only `lumbridge`,
 * `singles_plus` and `multiway`, and `isInWilderness()` in the npc hunt processor is a stub. This
 * is the standard bounds-and-arithmetic form rather than an area lookup, kept module-local until
 * something else needs it.
 *
 * The `x` bound matters: plenty of non-Wilderness surface sits above `z` 3520 (Fremennik and
 * Trollheim among them), so testing `z` alone would refuse teleports from perfectly safe ground.
 */
fun wildernessLevel(coord: CoordGrid): Int {
    if (coord.x !in WILDERNESS_X) return 0
    val base =
        when (coord.z) {
            in SURFACE_Z -> SURFACE_Z.first
            in UNDERGROUND_Z -> UNDERGROUND_Z.first
            else -> return 0
        }
    return ((coord.z - base) / LEVEL_HEIGHT) + 1
}

/** Teleports are refused above this level, matching the standard 20-Wilderness cut-off. */
const val MAX_WILDERNESS_LEVEL: Int = 20

private const val LEVEL_HEIGHT = 8

private val WILDERNESS_X = 2944..3392
private val SURFACE_Z = 3520..3968

/** The Wilderness caves mirror the surface band 6400 tiles north. */
private val UNDERGROUND_Z = 9920..10368
