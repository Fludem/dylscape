package org.rsmod.content.skills.construction.house

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.skills.construction.configs.ConstructionLocs
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.FurnitureLocs
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.region.util.RegionRotations
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.loc.LocLayerConstants

/**
 * Turns a stored house into a region a player can stand in, and keeps track of who is inside which.
 *
 * A house is assembled the way the client expects: one copied zone per room, taken from the
 * construction template map, rotated to face the way the owner turned it. Everything the template
 * cannot express is then layered on top:
 * - built furniture replaces its hotspot, piece by piece;
 * - unbuilt hotspots and door hotspots are deleted outside building mode, so a finished house does
 *   not show its scaffolding;
 * - the exit portal is placed in the room the house was started from.
 *
 * Regions are reclaimed by [RegionRepository] once nobody is standing in them, so leaving a house
 * only has to move the player out.
 */
@Singleton
public class HouseManager
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val locRepo: LocRepository,
    private val locTypes: LocTypeList,
    private val tables: ConstructionTables,
    private val templates: TemplateIndex,
    private val furnitureLocs: FurnitureLocs,
) {
    private val sessions = HashMap<Player, HouseSession>()

    public fun session(player: Player): HouseSession? = sessions[player]

    public fun isInside(player: Player): Boolean = sessions.containsKey(player)

    public fun forget(player: Player) {
        sessions.remove(player)
    }

    /**
     * Builds [house] into a fresh region and returns where the player should be put down, or `null`
     * when no region could be allocated.
     */
    public fun open(player: Player, house: PlayerOwnedHouse, buildMode: Boolean): CoordGrid? {
        val entrance = house.entrance ?: return null
        val region = regionRepo.add(house.toTemplate()) ?: return null

        val session = HouseSession(house, region, buildMode)
        sessions[player] = session

        decorate(session)

        val southWest =
            PohTemplate.roomSouthWest(
                region.southWest,
                entrance.gridX,
                entrance.gridZ,
                entrance.level,
            )
        return southWest.translate(ENTRANCE_OFFSET, ENTRANCE_OFFSET, 0)
    }

    /**
     * Re-lays a house that has changed shape. Rooms are copied zones, so adding or removing one
     * means a new region: the player is moved into it and the old one falls away on its own.
     */
    public fun rebuild(player: Player, standingIn: RoomKey?): CoordGrid? {
        val session = sessions[player] ?: return null
        val house = session.house
        val region = regionRepo.add(house.toTemplate()) ?: return null

        val rebuilt = HouseSession(house, region, session.buildMode)
        sessions[player] = rebuilt
        decorate(rebuilt)

        val landing = standingIn?.takeIf { house[it] != null } ?: house.entrance ?: return null
        val southWest =
            PohTemplate.roomSouthWest(region.southWest, landing.gridX, landing.gridZ, landing.level)
        return southWest.translate(ENTRANCE_OFFSET, ENTRANCE_OFFSET, 0)
    }

    /** The grid square [coords] falls in, for a player standing inside [session]'s region. */
    public fun roomAt(session: HouseSession, coords: CoordGrid): RoomKey? {
        val gridX = (coords.x - session.region.southWest.x) / PohTemplate.ROOM_LENGTH
        val gridZ = (coords.z - session.region.southWest.z) / PohTemplate.ROOM_LENGTH
        if (!PohTemplate.isInsideGrid(gridX, gridZ)) {
            return null
        }
        return RoomKey(coords.level, gridX, gridZ)
    }

    private fun PlayerOwnedHouse.toTemplate(): RegionTemplate {
        val placements = placed()
        val style = styleId
        return RegionTemplate.create {
            // Unbuilt squares are not nothing: the cache has a blank chunk for them, and without it
            // the house hangs in an unmapped void with no ground around it. Only the ground floor
            // gets one -- filling the upper floors would float grass in mid-air, and the dungeon is
            // meant to be walled in by rock.
            val blank = tables.blankRoom
            if (blank != null) {
                val blankZone =
                    PohTemplate.templateZone(style, blank.templateOffsetX, blank.templateOffsetZ)
                for (gridX in 0 until PohTemplate.GRID_LENGTH) {
                    for (gridZ in 0 until PohTemplate.GRID_LENGTH) {
                        val key = RoomKey(PohTemplate.LEVEL_GROUND, gridX, gridZ)
                        if (placements.containsKey(key)) continue
                        this[gridX, gridZ, PohTemplate.LEVEL_GROUND] = blankZone
                    }
                }
            }

            for ((key, room) in placements) {
                val data = tables.rooms[room.roomType] ?: continue
                val zone =
                    PohTemplate.templateZone(style, data.templateOffsetX, data.templateOffsetZ)
                when (room.rotation) {
                    1 -> this[key.gridX, key.gridZ, key.level] = zone.rotate90()
                    2 -> this[key.gridX, key.gridZ, key.level] = zone.rotate180()
                    3 -> this[key.gridX, key.gridZ, key.level] = zone.rotate270()
                    else -> this[key.gridX, key.gridZ, key.level] = zone
                }
            }
        }
    }

    /**
     * Lays the exit portal, the built furniture, and -- outside build mode -- hides the hotspots.
     */
    private fun decorate(session: HouseSession) {
        for ((key, room) in session.house.placed()) {
            val template = templates.room(room.roomType) ?: continue
            for (slot in template.slots) {
                val pieces = template.pieces(slot)
                val furnitureRow = room.built(slot)
                if (furnitureRow == PlacedRoom.EMPTY_HOTSPOT) {
                    if (!session.buildMode) {
                        pieces.forEach { deletePiece(session, key, room, it) }
                    }
                    continue
                }
                pieces.forEach { piece ->
                    deletePiece(session, key, room, piece)
                    val locId = furnitureLocs.resolve(furnitureRow, piece.variant) ?: return@forEach
                    addPiece(session, key, room, piece, locId)
                }
            }
            if (!session.buildMode) {
                hideDoorHotspots(session, key)
            }
        }
        placeExitPortal(session)
    }

    private fun placeExitPortal(session: HouseSession) {
        val entrance = session.house.entrance ?: return
        val southWest =
            PohTemplate.roomSouthWest(
                session.region.southWest,
                entrance.gridX,
                entrance.gridZ,
                entrance.level,
            )
        val portal = locTypes[ConstructionLocs.exit_portal.id] ?: return
        val coords = southWest.translate(PORTAL_OFFSET, PORTAL_OFFSET, 0)
        locRepo.add(
            coords = coords,
            type = portal,
            duration = Int.MAX_VALUE,
            angle = LocAngle.West,
            shape = LocShape.CentrepieceStraight,
        )
    }

    private fun hideDoorHotspots(session: HouseSession, key: RoomKey) {
        val zone = roomZone(session, key)
        for (loc in locRepo.findAll(zone).toList()) {
            if (templates.isDoorHotspot(loc.id)) {
                locRepo.del(loc, duration = Int.MAX_VALUE)
            }
        }
    }

    private fun deletePiece(
        session: HouseSession,
        key: RoomKey,
        room: PlacedRoom,
        piece: TemplateHotspot,
    ) {
        val existing = pieceLoc(session, key, room, piece, piece.locId) ?: return
        locRepo.del(existing, duration = Int.MAX_VALUE)
    }

    private fun addPiece(
        session: HouseSession,
        key: RoomKey,
        room: PlacedRoom,
        piece: TemplateHotspot,
        locId: Int,
    ) {
        val loc = pieceLoc(session, key, room, piece, locId) ?: return
        locRepo.add(loc, duration = Int.MAX_VALUE)
    }

    /** Where a template hotspot ends up once its room has been placed and turned. */
    public fun pieceLoc(
        session: HouseSession,
        key: RoomKey,
        room: PlacedRoom,
        piece: TemplateHotspot,
        locId: Int,
    ): LocInfo? {
        val type = locTypes[locId] ?: return null
        val angle = (piece.angle + room.rotation) and ANGLE_MASK
        val swapped = piece.angle and 1 == 1
        val width = if (swapped) type.length else type.width
        val length = if (swapped) type.width else type.length
        val translation =
            RegionRotations.translateLoc(
                regionRot = room.rotation,
                locSwGrid = org.rsmod.map.zone.ZoneGrid(piece.localX, piece.localZ),
                locAdjustedWidth = width,
                locAdjustedLength = length,
            )
        val southWest =
            PohTemplate.roomSouthWest(session.region.southWest, key.gridX, key.gridZ, key.level)
        val coords = southWest.translate(translation.x, translation.z, 0)
        val entity = LocEntity(id = locId, shape = piece.shape, angle = angle)
        return LocInfo(LocLayerConstants.of(piece.shape), coords, entity)
    }

    /**
     * Places a loc that will stand until the region is thrown away, reporting whether it took. A
     * region rejects locs in zones it does not cover, so this can legitimately fail.
     */
    public fun addLoc(loc: LocInfo): Boolean = locRepo.add(loc, duration = Int.MAX_VALUE)

    /**
     * Removes a loc for as long as the region lives, reporting whether it took. The registry
     * refuses a delete whose id, shape and angle do not match what is actually on the tile, so a
     * `false` here means the caller built the wrong [LocInfo], not that the tile was empty.
     */
    public fun deleteLoc(loc: LocInfo): Boolean = locRepo.del(loc, duration = Int.MAX_VALUE)

    private fun roomZone(session: HouseSession, key: RoomKey) =
        org.rsmod.map.zone.ZoneKey.from(
            PohTemplate.roomSouthWest(session.region.southWest, key.gridX, key.gridZ, key.level)
        )

    private companion object {
        const val ANGLE_MASK = 0x3

        /** Players land, and the exit portal stands, in the middle of a room. */
        const val ENTRANCE_OFFSET = PohTemplate.ROOM_LENGTH / 2
        const val PORTAL_OFFSET = PohTemplate.ROOM_LENGTH / 2
    }
}

/** A player standing in a built house. */
public class HouseSession(
    public val house: PlayerOwnedHouse,
    public val region: Region,
    public var buildMode: Boolean,
)
