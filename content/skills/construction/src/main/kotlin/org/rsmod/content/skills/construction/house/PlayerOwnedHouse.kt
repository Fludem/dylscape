package org.rsmod.content.skills.construction.house

/**
 * A player's house as stored data: which room sits in each grid square, and what has been built
 * into each of that room's hotspots.
 *
 * The grid is [PohTemplate.GRID_LENGTH] squares each way on [PohTemplate.LEVEL_COUNT] levels. A
 * square either holds a [PlacedRoom] or nothing at all; nothing is void, exactly as an unbuilt
 * square is in OSRS.
 */
public class PlayerOwnedHouse(
    public var styleId: Int = 0,
    public var locationId: Int = HouseLocation.Rimmington.id,
    public var locked: Boolean = false,
) {
    private val rooms = HashMap<Int, PlacedRoom>()

    /** Where the exit portal stands, so the house always has a way out. */
    public var entrance: RoomKey? = null
        private set

    public val roomCount: Int
        get() = rooms.size

    public fun placed(): Map<RoomKey, PlacedRoom> =
        rooms.entries.associate { (packed, room) -> RoomKey.unpack(packed) to room }

    public operator fun get(key: RoomKey): PlacedRoom? = rooms[key.packed]

    public fun get(level: Int, gridX: Int, gridZ: Int): PlacedRoom? =
        rooms[RoomKey(level, gridX, gridZ).packed]

    public fun put(key: RoomKey, room: PlacedRoom) {
        rooms[key.packed] = room
        if (entrance == null) {
            entrance = key
        }
    }

    public fun remove(key: RoomKey): PlacedRoom? {
        val removed = rooms.remove(key.packed)
        if (entrance == key) {
            entrance = rooms.keys.firstOrNull()?.let(RoomKey::unpack)
        }
        return removed
    }

    public fun isEmpty(): Boolean = rooms.isEmpty()

    /** Restores the stored entrance, but only if that square still holds a room. */
    public fun restoreEntrance(key: RoomKey) {
        if (rooms.containsKey(key.packed)) {
            entrance = key
        }
    }

    /**
     * Lays down the room every new house starts with: a garden holding the exit portal, in the
     * middle of the ground floor.
     */
    public fun placeStarterGarden(gardenRoomType: Int) {
        val centre = PohTemplate.GRID_LENGTH / 2
        val key = RoomKey(PohTemplate.LEVEL_GROUND, centre, centre)
        put(key, PlacedRoom(gardenRoomType, rotation = 0))
        entrance = key
    }
}

/** A grid square, packed so it can key a map without allocating. */
@JvmInline
public value class RoomKey private constructor(public val packed: Int) {
    public constructor(
        level: Int,
        gridX: Int,
        gridZ: Int,
    ) : this((level shl 16) or (gridX shl 8) or gridZ)

    public val level: Int
        get() = (packed shr 16) and 0xFF

    public val gridX: Int
        get() = (packed shr 8) and 0xFF

    public val gridZ: Int
        get() = packed and 0xFF

    public fun translate(dx: Int, dz: Int): RoomKey = RoomKey(level, gridX + dx, gridZ + dz)

    override fun toString(): String = "RoomKey(level=$level, x=$gridX, z=$gridZ)"

    public companion object {
        public fun unpack(packed: Int): RoomKey = RoomKey(packed)
    }
}

/**
 * A room in the grid: which [roomType] it is, how far it has been turned, and what has been built
 * into each of its hotspot slots.
 *
 * [furniture] is indexed by hotspot slot -- the position of a hotspot in its room's
 * `poh_room:hotspot` list -- and holds the db row id of whatever is built there, or
 * [EMPTY_HOTSPOT].
 */
public class PlacedRoom(
    public val roomType: Int,
    public var rotation: Int,
    private val furniture: HashMap<Int, Int> = HashMap(),
) {
    public fun built(slot: Int): Int = furniture[slot] ?: EMPTY_HOTSPOT

    public fun isBuilt(slot: Int): Boolean = furniture.containsKey(slot)

    public fun build(slot: Int, furnitureRow: Int) {
        furniture[slot] = furnitureRow
    }

    public fun clear(slot: Int) {
        furniture.remove(slot)
    }

    public fun allBuilt(): Map<Int, Int> = furniture

    public companion object {
        public const val EMPTY_HOTSPOT: Int = -1
    }
}

/** The six world portals a house can be moved between. */
public enum class HouseLocation(
    public val id: Int,
    public val displayName: String,
    public val cost: Int,
) {
    Rimmington(0, "Rimmington", 5_000),
    Taverley(1, "Taverley", 5_000),
    Pollnivneach(2, "Pollnivneach", 7_500),
    Rellekka(3, "Rellekka", 10_000),
    Brimhaven(4, "Brimhaven", 15_000),
    Yanille(5, "Yanille", 25_000);

    public companion object {
        public fun of(id: Int): HouseLocation = entries.firstOrNull { it.id == id } ?: Rimmington
    }
}
