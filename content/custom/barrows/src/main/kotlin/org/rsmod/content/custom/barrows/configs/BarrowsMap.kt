package org.rsmod.content.custom.barrows.configs

import org.rsmod.content.custom.barrows.Brother
import org.rsmod.map.CoordGrid

/**
 * Every coordinate Barrows needs, read off the cache rather than off the wiki.
 *
 * `BarrowsDump.dump barrows loc placements` printed the sarcophagi and staircases; `render the
 * barrows underground square` printed the walkable geometry the rest was chosen against.
 * `BarrowsMapTest` re-checks all of it against the live map, so a wrong tile here fails a test
 * rather than teleporting somebody into a wall.
 *
 * ### The shape of the place, which is not quite OSRS's
 *
 * In live OSRS the six crypts are isolated rooms and a separate tunnel maze sits below them with
 * the reward chest at its centre. **This cache has no tunnel level.** Mapsquare 55_151 carries locs
 * on level 3 only - one connected maze holding all six sarcophagi and all six staircases - while
 * level 0, where the tunnels belong, has no locs and not one walkable tile. A whole-map scan finds
 * zero placements of any barrows door, ladder, chest or rockslide, so there is nothing to walk into
 * and nothing to loot until we place it.
 *
 * So the crypt maze *is* the tunnels here: one connected space, no doors to unlock, and the chest
 * goes at its centre. That happens to be exactly the "core loop, no maze" this module was asked
 * for.
 */
object BarrowsMap {
    /** Level of the crypt maze. Levels 0-2 of the mapsquare are empty. */
    const val CRYPT_LEVEL: Int = 3

    /**
     * Where you dig. The mounds are bare terrain - no loc is placed on any of them - so entry is a
     * spade dig on a coordinate, the way OSRS does it, and not an op on a staircase.
     */
    val mounds: Map<Brother, CoordGrid> =
        mapOf(
            Brother.Ahrim to CoordGrid(3565, 3288, 0),
            Brother.Dharok to CoordGrid(3575, 3298, 0),
            Brother.Guthan to CoordGrid(3577, 3281, 0),
            Brother.Karil to CoordGrid(3565, 3276, 0),
            Brother.Torag to CoordGrid(3554, 3283, 0),
            Brother.Verac to CoordGrid(3557, 3298, 0),
        )

    /** The `barrows_stairs_*` tiles inside the maze. Climbing one returns you to its mound. */
    val staircases: Map<Brother, CoordGrid> =
        mapOf(
            Brother.Ahrim to CoordGrid(3558, 9703, CRYPT_LEVEL),
            Brother.Dharok to CoordGrid(3557, 9718, CRYPT_LEVEL),
            Brother.Guthan to CoordGrid(3534, 9705, CRYPT_LEVEL),
            Brother.Karil to CoordGrid(3546, 9685, CRYPT_LEVEL),
            Brother.Torag to CoordGrid(3565, 9683, CRYPT_LEVEL),
            Brother.Verac to CoordGrid(3578, 9703, CRYPT_LEVEL),
        )

    /**
     * Where a dig drops you: one tile clear of the staircase, on the open side of it. Landing on
     * the staircase itself would work - the tile is walkable - but stepping off it reads better and
     * keeps the climb-up op in front of the player.
     */
    val cryptEntrances: Map<Brother, CoordGrid> =
        mapOf(
            Brother.Ahrim to CoordGrid(3557, 9703, CRYPT_LEVEL),
            Brother.Dharok to CoordGrid(3556, 9718, CRYPT_LEVEL),
            Brother.Guthan to CoordGrid(3535, 9705, CRYPT_LEVEL),
            Brother.Karil to CoordGrid(3547, 9685, CRYPT_LEVEL),
            Brother.Torag to CoordGrid(3566, 9683, CRYPT_LEVEL),
            Brother.Verac to CoordGrid(3577, 9703, CRYPT_LEVEL),
        )

    /** The `barrow_*_sarcophagus` tiles. Searching one raises its brother. */
    val sarcophagi: Map<Brother, CoordGrid> =
        mapOf(
            Brother.Ahrim to CoordGrid(3555, 9698, CRYPT_LEVEL),
            Brother.Dharok to CoordGrid(3554, 9714, CRYPT_LEVEL),
            Brother.Guthan to CoordGrid(3538, 9703, CRYPT_LEVEL),
            Brother.Karil to CoordGrid(3550, 9682, CRYPT_LEVEL),
            Brother.Torag to CoordGrid(3569, 9685, CRYPT_LEVEL),
            Brother.Verac to CoordGrid(3573, 9705, CRYPT_LEVEL),
        )

    /**
     * The reward chest, which we place because the map does not.
     *
     * Dead centre of the maze, chosen as the tile nearest the middle whose 2x2 footprint is clear
     * *and* which keeps a walkable ring around it, so dropping a chest here cannot seal a corridor.
     * It is more than four tiles from every sarcophagus and staircase.
     */
    val chest: CoordGrid = CoordGrid(3551, 9695, CRYPT_LEVEL)

    /** Where the brother raised from a sarcophagus appears: on the sarcophagus's own tile. */
    fun brotherSpawn(brother: Brother): CoordGrid = checkNotNull(sarcophagi[brother])

    /** Where the ambush brother appears when the chest is looted early. */
    val chestAmbushSpawn: CoordGrid = CoordGrid(3552, 9693, CRYPT_LEVEL)

    /** Every tile the module owns, for the map test to sweep in one pass. */
    val allCoords: List<CoordGrid>
        get() =
            mounds.values +
                staircases.values +
                cryptEntrances.values +
                sarcophagi.values +
                listOf(chest, chestAmbushSpawn)
}
