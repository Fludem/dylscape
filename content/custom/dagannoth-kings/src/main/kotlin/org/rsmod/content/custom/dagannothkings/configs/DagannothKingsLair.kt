package org.rsmod.content.custom.dagannothkings.configs

import org.rsmod.content.custom.dagannothkings.DagannothKing
import org.rsmod.map.CoordGrid

/**
 * Every coordinate the module knows, in one place.
 *
 * All of these are verified against the live collision map by `DagannothKingsLairTest`, and were
 * read off `DagannothKingsDump` rather than guessed. The lair is a walled oval roughly 30 tiles
 * across, and - unlike the Rogues' Den, whose floor sits on level 1 with level 0 solid - mapsquare
 * 45,69 carries terrain on **level 0 only**. There is no floor at all on levels 1 to 3, so a tile
 * here that is not level 0 is a bug.
 */
public object DagannothKingsLair {
    /**
     * Where the three kings stand. Set by `world-spawns`, not by this module; mirrored for tests.
     */
    public val spawns: Map<DagannothKing, CoordGrid> =
        mapOf(
            DagannothKing.Supreme to CoordGrid(2906, 4448, 0),
            DagannothKing.Prime to CoordGrid(2911, 4451, 0),
            DagannothKing.Rex to CoordGrid(2913, 4445, 0),
        )

    /**
     * The lair side of the ladder. The ladder tile itself is blocked, so this is one east of it.
     */
    public val lairArrival: CoordGrid = CoordGrid(2900, 4449, 0)

    /**
     * The Waterbirth side. Also the teleport destination, so a player arrives at the *entrance* and
     * climbs down the same ladder they would on the real route from Rellekka.
     */
    public val antechamberArrival: CoordGrid = CoordGrid(1912, 4367, 0)
}
