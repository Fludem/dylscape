package org.rsmod.content.skills.construction.house

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.registry.loc.LocRegistry
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType

/**
 * What each room template actually contains, read off the loaded map rather than transcribed.
 *
 * The server already decodes every static loc in the world, the construction templates included, so
 * the layout of a room -- where its hotspots are, which walls have doorways -- can simply be looked
 * up. Two things are recovered per room:
 * - **hotspot slots.** Most hotspot locs are named `poh_<room>_<n>`, optionally with a piece suffix
 *   (`poh_parlour_4_middle`). `n` is 1-based and lines up with the room's `poh_room:hotspot` list,
 *   so `poh_parlour_1` is slot 0 and takes its furniture from the first entry of that list. The
 *   rooms added since -- the costume room, the menagerie, the superior garden, the achievement
 *   gallery -- dropped that convention and name themselves after the hotspot instead
 *   (`poh_cos_room_cape_rack_hotspot` against the `cape_rack_hotspot` db row), so the row names are
 *   matched as a fallback.
 * - **doorways.** Door hotspots are named `poh_hotspot_door[lr]_<style>`. Which wall of the 8x8
 *   template they sit against is the direction a neighbouring room can be added in.
 *
 * The scan runs against style 0 only: comparing the twelve style templates shows they differ in
 * their wall and door locs alone, never in where the hotspots sit.
 *
 * It reads [LocRegistry] rather than the repository because that is all it needs, and because the
 * registry is the one piece of the loaded map an integration test can reach -- see
 * `ConstructionTemplateTest`.
 */
@Singleton
public class TemplateIndex
@Inject
constructor(
    private val locRegistry: LocRegistry,
    private val locTypes: LocTypeList,
    private val tables: ConstructionTables,
) {
    private val rooms = HashMap<Int, RoomTemplateInfo>()

    /** Every door hotspot loc id, across all twelve styles, so one lookup recognises them all. */
    public val doorHotspotLocs: Set<Int> by lazy {
        locTypes.values
            .filter { it.internalName?.startsWith(DOOR_HOTSPOT_PREFIX) == true }
            .map { it.id }
            .toSet()
    }

    /** Hotspot loc id -> the room and slot it belongs to. Filled by [index]. */
    private val hotspotLocs = HashMap<Int, HotspotSlot>()

    public fun index() {
        if (rooms.isNotEmpty()) {
            return
        }
        for (room in tables.rooms.values) {
            val info = scan(room.roomType, room.templateOffsetX, room.templateOffsetZ)
            rooms[room.roomType] = info
            for (hotspot in info.hotspots) {
                hotspotLocs[hotspot.locId] = HotspotSlot(room.roomType, hotspot.slot)
            }
        }
    }

    public fun room(roomType: Int): RoomTemplateInfo? = rooms[roomType]

    public fun isDoorHotspot(locId: Int): Boolean = locId in doorHotspotLocs

    /** Which room and hotspot slot a `Build` loc belongs to, or `null` if it is not a hotspot. */
    public fun slotOf(locId: Int): HotspotSlot? = hotspotLocs[locId]

    private fun scan(roomType: Int, offsetX: Int, offsetZ: Int): RoomTemplateInfo {
        val slotsByRowName = slotsByRowName(roomType)
        val zone = PohTemplate.templateZone(styleId = 0, offsetX = offsetX, offsetZ = offsetZ)
        val southWestX = zone.x * PohTemplate.ROOM_LENGTH
        val southWestZ = zone.z * PohTemplate.ROOM_LENGTH

        val hotspots = ArrayList<TemplateHotspot>()
        val doors = HashSet<Direction>()

        for (loc in locRegistry.findAll(zone)) {
            val type = locTypes[loc.id] ?: continue
            val name = type.internalName ?: continue
            val localX = loc.coords.x - southWestX
            val localZ = loc.coords.z - southWestZ

            if (name.startsWith(DOOR_HOTSPOT_PREFIX)) {
                Direction.ofWall(localX, localZ)?.let(doors::add)
                continue
            }

            if (!type.hasBuildOp()) {
                continue
            }

            val parsed = SLOT_PATTERN.find(name)
            val slot =
                parsed?.groupValues?.get(1)?.toIntOrNull()?.minus(1)
                    ?: slotsByRowName.entries
                        .firstOrNull { (rowName, _) -> name.contains(rowName) }
                        ?.value
                    ?: continue
            hotspots +=
                TemplateHotspot(
                    slot = slot,
                    locId = loc.id,
                    variant = parsed?.groupValues?.get(2)?.takeIf { it.isNotEmpty() },
                    localX = localX,
                    localZ = localZ,
                    shape = loc.shapeId,
                    angle = loc.angleId,
                )
        }

        return RoomTemplateInfo(roomType, hotspots, doors)
    }

    private fun UnpackedLocType.hasBuildOp(): Boolean = op.any { it == BUILD_OP }

    /**
     * The room's hotspot rows keyed by the distinctive part of their name, longest first so
     * `pet_combatring_hotspot` is tried before `petlist_hotspot` would match a shorter substring.
     */
    private fun slotsByRowName(roomType: Int): Map<String, Int> {
        val room = tables.rooms[roomType] ?: return emptyMap()
        return room.hotspots
            .withIndex()
            .mapNotNull { (slot, rowId) ->
                val name = tables.hotspot(rowId)?.internalName ?: return@mapNotNull null
                if (name == NULL_HOTSPOT) return@mapNotNull null
                name.removeSuffix(HOTSPOT_SUFFIX) to slot
            }
            .sortedByDescending { it.first.length }
            .toMap()
    }

    private companion object {
        const val DOOR_HOTSPOT_PREFIX = "poh_hotspot_door"
        const val HOTSPOT_SUFFIX = "_hotspot"
        const val NULL_HOTSPOT = "null_hotspot"
        const val BUILD_OP = "Build"

        /** `poh_parlour_4_middle` -> slot `4`, variant `middle`. */
        val SLOT_PATTERN = Regex("_(\\d+)(?:_([a-z0-9]+))?$")
    }
}

/** One room template's recovered layout. */
public data class RoomTemplateInfo(
    public val roomType: Int,
    public val hotspots: List<TemplateHotspot>,
    public val doors: Set<Direction>,
) {
    /** Every template loc belonging to [slot]; a rug or a run of windows has more than one. */
    public fun pieces(slot: Int): List<TemplateHotspot> = hotspots.filter { it.slot == slot }

    public val slots: List<Int>
        get() = hotspots.map { it.slot }.distinct().sorted()

    /**
     * The quarter-turn that puts one of this room's doorways on [wanted], or `-1` when none of the
     * four rotations does. A room reached from the south has to have a door facing south once it is
     * placed, or the player would seal themselves out of it.
     */
    public fun rotationFacing(wanted: Direction): Int {
        for (rotation in 0..3) {
            if (doors.any { it.rotate(rotation) == wanted }) {
                return rotation
            }
        }
        return -1
    }
}

/** One hotspot loc in a room template, positioned relative to the room's south-west corner. */
public data class TemplateHotspot(
    public val slot: Int,
    public val locId: Int,
    public val variant: String?,
    public val localX: Int,
    public val localZ: Int,
    public val shape: Int,
    public val angle: Int,
)

/** Which room and hotspot slot a `Build` loc identifies. */
public data class HotspotSlot(public val roomType: Int, public val slot: Int)

/** A wall of a room, and so the direction a neighbouring room sits in. */
public enum class Direction(public val deltaX: Int, public val deltaZ: Int) {
    South(0, -1),
    West(-1, 0),
    North(0, 1),
    East(1, 0);

    public fun opposite(): Direction =
        when (this) {
            South -> North
            West -> East
            North -> South
            East -> West
        }

    /** Rotating a room turns its doorways with it, a quarter turn clockwise at a time. */
    public fun rotate(rotation: Int): Direction = entries[(ordinal + rotation) and 3]

    public companion object {
        private const val LAST_TILE = PohTemplate.ROOM_LENGTH - 1

        public fun ofWall(localX: Int, localZ: Int): Direction? =
            when {
                localZ == 0 -> South
                localZ == LAST_TILE -> North
                localX == 0 -> West
                localX == LAST_TILE -> East
                else -> null
            }
    }
}
