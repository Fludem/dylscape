package org.rsmod.content.custom.vorkath.configs

import org.rsmod.map.CoordGrid

/**
 * Every coordinate the module knows, in one place.
 *
 * All of these are checked against the live collision map by `VorkathArenaTest`, and were read off
 * the cache's tile and loc placement data rather than the wiki. Ungael is mapsquare 35,63 and its
 * crater is an oval of open floor from x 2261 to 2283 and z 4054 to 4076, ringed by ice. In the
 * middle of it sits a blocked 7x7 - x 2269..2275, z 4062..4068 - which is exactly Vorkath's
 * footprint: the map reserves the platform for it.
 */
public object VorkathArena {
    /**
     * The south-west tile of the sleeping dragon's 7x7. This is the same coordinate `map/npcs.toml`
     * packs, and the test holds the two in step.
     */
    public val spawn: CoordGrid = CoordGrid(2269, 4062, 0)

    public const val VORKATH_SIZE: Int = 7

    /** The loc a player climbs over to get in: the south lip of the crater. */
    public val craterEntrance: CoordGrid = CoordGrid(2272, 4053, 0)

    /** Where the entrance climb lands, on either side of the lip. */
    public val craterOutsideSouth: CoordGrid = CoordGrid(2272, 4052, 0)
    public val craterInsideSouth: CoordGrid = CoordGrid(2272, 4054, 0)

    /**
     * Where Torfinn's boat lands you on the island: the strip of shore north of the placed
     * `ungael_boat`, which is the only open ground on the south coast.
     */
    public val ungaelArrival: CoordGrid = CoordGrid(2277, 4037, 0)

    /** Torfinn's own tile on the island, one off the arrival so he is never stood on. */
    public val torfinnUngael: CoordGrid = CoordGrid(2276, 4036, 0)

    /**
     * The Rellekka pier is two tiles wide, x 2640..2641, running north from the shore at z 3687 to
     * z 3699; the placed `fremennik_boat_ungael` sits in the water just west of it. The map's own
     * tile flags call the whole pier water - the planks are loc-provided - so this was read off the
     * real collision map in `VorkathDump`, not the tile dump.
     */
    public val rellekkaArrival: CoordGrid = CoordGrid(2640, 3695, 0)

    public val torfinnRellekka: CoordGrid = CoordGrid(2640, 3696, 0)

    /** The teleport-panel destination: outside the crater, two tiles south of the lip. */
    public val teleport: CoordGrid = CoordGrid(2272, 4051, 0)

    /** Every tile a player is put on, for the collision sweep. */
    public val standable: Map<String, CoordGrid> =
        mapOf(
            "crater outside south" to craterOutsideSouth,
            "crater inside south" to craterInsideSouth,
            "Ungael arrival" to ungaelArrival,
            "Torfinn (Ungael)" to torfinnUngael,
            "Rellekka arrival" to rellekkaArrival,
            "Torfinn (Rellekka)" to torfinnRellekka,
            "teleport" to teleport,
        )
}
