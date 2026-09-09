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
     * The diggable top of each mound.
     *
     * The mounds are bare terrain - no loc is placed on any of them, and the `barrows_short_grass_*`
     * scenery that covers them is generic dressing used all over the world, so neither is a marker
     * we can test against. What *does* mark a mound is the terrain height: the `m55_51` height bytes
     * put each mound top on a plateau eight to seventeen units above the surrounding grass, and a
     * flood fill from each peak down to `peak - 4` gives the six blobs boxed here. `BarrowsMapTest`
     * re-derives nothing, but it does check the boxes are disjoint and that each [mounds] tile sits
     * inside its own.
     *
     * Boxing the plateau rather than testing one tile is the whole point: a single coordinate means
     * a player standing anywhere else on the hill gets "You find nothing but earth" and no hint that
     * they are two tiles from the entrance.
     */
    val moundTops: Map<Brother, MoundTop> =
        mapOf(
            Brother.Ahrim to MoundTop(3563..3568, 3287..3291),
            Brother.Dharok to MoundTop(3573..3577, 3296..3300),
            Brother.Guthan to MoundTop(3576..3580, 3280..3286),
            Brother.Karil to MoundTop(3564..3568, 3273..3278),
            Brother.Torag to MoundTop(3552..3555, 3281..3285),
            Brother.Verac to MoundTop(3554..3560, 3295..3301),
        )

    /** Which mound, if any, [coords] is standing on. */
    fun moundAt(coords: CoordGrid): Brother? =
        moundTops.entries.firstOrNull { coords in it.value }?.key

    /**
     * The peak of each mound: where a dig sends you *back* to when you climb out of its crypt, and
     * the tile the walkability check cares about. Digging works anywhere in [moundTops].
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

    /** A rectangle of surface tiles. Levels above 0 are never part of a mound. */
    data class MoundTop(val x: IntRange, val z: IntRange) {
        val tiles: List<CoordGrid>
            get() = x.flatMap { tileX -> z.map { tileZ -> CoordGrid(tileX, tileZ, 0) } }

        operator fun contains(coords: CoordGrid): Boolean =
            coords.level == 0 && coords.x in x && coords.z in z
    }
}
