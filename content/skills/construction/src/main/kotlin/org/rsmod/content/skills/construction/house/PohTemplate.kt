package org.rsmod.content.skills.construction.house

import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Where the room templates live in the world map, and how a house style picks between them.
 *
 * The construction template map is a grid of 8x8 rooms in map squares 28-31 x 89-90. A room's
 * `poh_room:source_offset` is a tile offset into that grid from [BASE_X], [BASE_Z], which
 * [templateZone] turns back into the zone the house copies.
 *
 * The twelve house styles are stacked *both* ways: every 64 tiles east is a new group of four, and
 * within a group the four map levels are four different themes. Reading the door hotspot each
 * template places gives the order below -- level 0 of the first column is Rimmington, level 1 is
 * Lumbridge, and so on through Canifis.
 *
 * The house itself is assembled into a small region (16x16 zones), of which the south-west
 * [GRID_LENGTH] x [GRID_LENGTH] zones are the buildable grid.
 */
public object PohTemplate {
    public const val BASE_X: Int = 1856
    public const val BASE_Z: Int = 5696

    /** How far east the next group of four styles sits. */
    private const val STYLE_GROUP_STRIDE: Int = 64

    /** Rooms are one zone square, which is what makes zone-copying work at all. */
    public const val ROOM_LENGTH: Int = 8

    /** OSRS caps a house at 13x13 rooms; the region has room for 16. */
    public const val GRID_LENGTH: Int = 13

    /** Dungeon, ground floor, upper floor. */
    public const val LEVEL_DUNGEON: Int = 0
    public const val LEVEL_GROUND: Int = 1
    public const val LEVEL_UPPER: Int = 2
    public const val LEVEL_COUNT: Int = 3

    /** Rooms with this `floor_restriction` may only be built in the dungeon. */
    public const val RESTRICT_DUNGEON: Int = 1

    /** Rooms with this `floor_restriction` may only be built on the ground floor. */
    public const val RESTRICT_GROUND: Int = 2

    public val styles: List<HouseStyle> =
        listOf(
            HouseStyle(0, "Basic wood", "rimmington"),
            HouseStyle(1, "Basic stone", "lumbridge"),
            HouseStyle(2, "Whitewashed stone", "pollnivneach"),
            HouseStyle(3, "Fremennik-style wood", "rellekka"),
            HouseStyle(4, "Tropical wood", "brimhaven"),
            HouseStyle(5, "Fancy stone", "yanille"),
            HouseStyle(6, "Deathly mansion", "deathly"),
            HouseStyle(7, "Twisted", "twisted"),
            HouseStyle(8, "Hosidius", "hosidius"),
            HouseStyle(9, "Festive", "xmas"),
            HouseStyle(10, "Civitas illa Fortis", "civitas"),
            HouseStyle(11, "Canifis", "canifis"),
        )

    public fun style(id: Int): HouseStyle = styles.getOrElse(id) { styles[0] }

    /** The map level a style's templates are drawn on. */
    public fun templateLevel(styleId: Int): Int = style(styleId).id % 4

    /** The south-west tile of the template grid a style's templates are drawn in. */
    public fun templateBaseX(styleId: Int): Int =
        BASE_X + (style(styleId).id / 4) * STYLE_GROUP_STRIDE

    /** The zone holding [room]'s template, decorated in [styleId]. */
    public fun templateZone(styleId: Int, offsetX: Int, offsetZ: Int): ZoneKey =
        ZoneKey.fromAbsolute(
            templateBaseX(styleId) + offsetX,
            BASE_Z + offsetZ,
            templateLevel(styleId),
        )

    /** The south-west tile of the zone [gridX], [gridZ] occupies inside [regionSouthWest]. */
    public fun roomSouthWest(
        regionSouthWest: CoordGrid,
        gridX: Int,
        gridZ: Int,
        level: Int,
    ): CoordGrid =
        CoordGrid(
            level = level,
            x = regionSouthWest.x + gridX * ROOM_LENGTH,
            z = regionSouthWest.z + gridZ * ROOM_LENGTH,
        )

    public fun isInsideGrid(gridX: Int, gridZ: Int): Boolean =
        gridX in 0 until GRID_LENGTH && gridZ in 0 until GRID_LENGTH
}

/**
 * A wall-and-door theme. [doorSuffix] is the tail of the door hotspot loc the templates place --
 * `poh_hotspot_doorl_rimmington` and friends -- which is what identified the order in the first
 * place.
 */
public data class HouseStyle(
    public val id: Int,
    public val displayName: String,
    public val doorSuffix: String,
)
