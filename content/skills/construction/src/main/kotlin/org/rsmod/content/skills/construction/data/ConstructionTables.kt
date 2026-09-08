package org.rsmod.content.skills.construction.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.dbrow.DbRowTypeList
import org.rsmod.game.type.dbrow.UnpackedDbRowType
import org.rsmod.game.type.dbtable.DbTableTypeList

/**
 * Construction is almost entirely a data-driven skill, and rev 233 ships every byte of that data in
 * the cache already: three db tables hold the rooms, the hotspots each room exposes, and the
 * furniture that can go in each hotspot.
 *
 * ```
 * poh_room     name, cost, room_type, level_requirement, source_offset,
 *              door_locations, hotspot[], floor_restriction, has_roof, room_obj, button
 * poh_hotspot  builddata[]        -- the furniture rows this hotspot offers, cheapest first
 * furniture    model_obj, name, material_cost[], level_requirement, ...
 * ```
 *
 * Reading them here rather than transcribing them into Kotlin means all thirty rooms and all 512
 * furniture entries are available without an authored table, and a cache update carries straight
 * through. The one thing the cache does *not* hold is experience -- see [ConstructionXp].
 */
@Singleton
public class ConstructionTables
@Inject
constructor(dbTables: DbTableTypeList, dbRows: DbRowTypeList) {
    /** Every buildable room, keyed by its `room_type` -- the id the house grid stores. */
    public val rooms: Map<Int, RoomData>

    /** Every hotspot, keyed by its db row id, as referenced from [RoomData.hotspots]. */
    public val hotspots: Map<Int, HotspotData>

    /**
     * Every piece of furniture, keyed by its db row id, as referenced from [HotspotData.options].
     */
    public val furniture: Map<Int, FurnitureData>

    /**
     * The blank square, `poh_dummy_null`: room type `-1`, no name, no doors, no hotspots, and the
     * first zone of the template grid. It is what a house's *unbuilt* squares are made of -- copy
     * it into them and the house stands on ground, leave them out and it hangs in an unmapped void.
     */
    public val blankRoom: RoomData?

    init {
        val roomTable = dbTables.requireTable(ROOM_TABLE)
        val hotspotTable = dbTables.requireTable(HOTSPOT_TABLE)
        val furnitureTable = dbTables.requireTable(FURNITURE_TABLE)

        val byTable = dbRows.values.groupBy { it.table }

        furniture = byTable[furnitureTable].orEmpty().associate { it.id to it.toFurniture() }
        hotspots = byTable[hotspotTable].orEmpty().associate { it.id to it.toHotspot() }
        val allRooms = byTable[roomTable].orEmpty().mapNotNull { it.toRoom() }
        rooms = allRooms.filter { it.roomType >= 0 }.associateBy { it.roomType }
        blankRoom = allRooms.firstOrNull { it.roomType < 0 }
    }

    public fun hotspot(rowId: Int): HotspotData? = hotspots[rowId]

    public fun furniture(rowId: Int): FurnitureData? = furniture[rowId]

    private fun DbTableTypeList.requireTable(name: String): Int =
        values.firstOrNull { it.internalName == name }?.id
            ?: error("Cache is missing db table '$name'; construction cannot start.")

    private fun UnpackedDbRowType.toRoom(): RoomData? {
        val roomType = intAt(ROOM_TYPE) ?: return null
        val offset = data[ROOM_SOURCE_OFFSET]?.filterIsInstance<Int>() ?: return null
        if (offset.size < 2) return null
        return RoomData(
            rowId = id,
            internalName = internalName,
            name = stringAt(ROOM_NAME) ?: return null,
            displayName = stringAt(ROOM_NAME_UPPER) ?: return null,
            roomType = roomType,
            cost = intAt(ROOM_COST) ?: 0,
            levelRequirement = statLevelAt(ROOM_LEVEL_REQ),
            templateOffsetX = offset[0],
            templateOffsetZ = offset[1],
            doors = data[ROOM_DOORS]?.filterIsInstance<Int>()?.toSet().orEmpty(),
            hotspots = data[ROOM_HOTSPOTS]?.filterIsInstance<Int>().orEmpty(),
            floorRestriction = intAt(ROOM_FLOOR_RESTRICTION),
            roomObj = intAt(ROOM_OBJ),
        )
    }

    private fun UnpackedDbRowType.toHotspot(): HotspotData =
        HotspotData(
            rowId = id,
            internalName = internalName,
            options = data[HOTSPOT_BUILDDATA]?.filterIsInstance<Int>().orEmpty(),
        )

    private fun UnpackedDbRowType.toFurniture(): FurnitureData {
        // `material_cost` is a flat run of alternating obj id and count.
        val costs = data[FURNITURE_MATERIAL_COST]?.filterIsInstance<Int>().orEmpty()
        val materials =
            costs.chunked(2).mapNotNull { pair ->
                if (pair.size == 2) Material(pair[0], pair[1]) else null
            }
        return FurnitureData(
            rowId = id,
            internalName = internalName,
            name = stringAt(FURNITURE_NAME) ?: "",
            modelObj = intAt(FURNITURE_MODEL_OBJ) ?: -1,
            levelRequirement = statLevelAt(FURNITURE_LEVEL_REQ),
            materials = materials,
            hidden = intAt(FURNITURE_HIDDEN) == 1,
        )
    }

    private fun UnpackedDbRowType.intAt(column: Int): Int? = data[column]?.firstOrNull() as? Int

    private fun UnpackedDbRowType.stringAt(column: Int): String? =
        data[column]?.firstOrNull() as? String

    /**
     * A `level_requirement` cell is a `(stat, level)` pair. Every construction row names stat 22,
     * so only the level is kept.
     */
    private fun UnpackedDbRowType.statLevelAt(column: Int): Int {
        val pair = data[column]?.filterIsInstance<Int>() ?: return 1
        return pair.getOrNull(1) ?: 1
    }

    private companion object {
        const val ROOM_TABLE = "poh_room"
        const val HOTSPOT_TABLE = "poh_hotspot"
        const val FURNITURE_TABLE = "furniture"

        const val ROOM_NAME = 0
        const val ROOM_NAME_UPPER = 1
        const val ROOM_COST = 2
        const val ROOM_TYPE = 3
        const val ROOM_LEVEL_REQ = 4
        const val ROOM_SOURCE_OFFSET = 5
        const val ROOM_DOORS = 6
        const val ROOM_HOTSPOTS = 7
        const val ROOM_FLOOR_RESTRICTION = 8
        const val ROOM_OBJ = 10

        const val HOTSPOT_BUILDDATA = 0

        const val FURNITURE_MODEL_OBJ = 0
        const val FURNITURE_NAME = 1
        const val FURNITURE_MATERIAL_COST = 2
        const val FURNITURE_LEVEL_REQ = 3
        const val FURNITURE_HIDDEN = 5
    }
}

/**
 * One buildable room.
 *
 * [templateOffsetX] and [templateOffsetZ] are tile offsets into the construction template map,
 * whose south-west corner sits at [PohTemplate.BASE_X], [PohTemplate.BASE_Z]. Every room occupies
 * exactly one 8x8 zone there, which is why a house can be assembled by copying zones.
 *
 * [floorRestriction] is `1` for the rooms that only exist in the dungeon, `2` for the ones that
 * only exist on the ground floor (the gardens, the throne room), and `null` for everything else.
 */
public data class RoomData(
    public val rowId: Int,
    public val internalName: String?,
    public val name: String,
    public val displayName: String,
    public val roomType: Int,
    public val cost: Int,
    public val levelRequirement: Int,
    public val templateOffsetX: Int,
    public val templateOffsetZ: Int,
    public val doors: Set<Int>,
    public val hotspots: List<Int>,
    public val floorRestriction: Int?,
    public val roomObj: Int?,
)

/** One hotspot in a room, and the furniture rows it offers. */
public data class HotspotData(
    public val rowId: Int,
    public val internalName: String?,
    public val options: List<Int>,
)

/** One piece of furniture, as the build menu offers it. */
public data class FurnitureData(
    public val rowId: Int,
    public val internalName: String?,
    public val name: String,
    public val modelObj: Int,
    public val levelRequirement: Int,
    public val materials: List<Material>,
    public val hidden: Boolean,
)

/** An obj and how many of it a piece of furniture consumes. */
public data class Material(public val obj: Int, public val count: Int)
