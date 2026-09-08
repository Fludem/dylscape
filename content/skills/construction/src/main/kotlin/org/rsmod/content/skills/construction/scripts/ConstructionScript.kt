package org.rsmod.content.skills.construction.scripts

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.constructionLvl
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc3
import org.rsmod.api.script.onOpLoc5
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.construction.configs.ConstructionLocs
import org.rsmod.content.skills.construction.configs.ConstructionObjs
import org.rsmod.content.skills.construction.configs.ConstructionSeqs
import org.rsmod.content.skills.construction.configs.ConstructionVarBits
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.ConstructionXp
import org.rsmod.content.skills.construction.data.FurnitureData
import org.rsmod.content.skills.construction.data.FurnitureLocs
import org.rsmod.content.skills.construction.data.RoomData
import org.rsmod.content.skills.construction.house.Direction
import org.rsmod.content.skills.construction.house.HouseManager
import org.rsmod.content.skills.construction.house.HouseRegistry
import org.rsmod.content.skills.construction.house.HouseSession
import org.rsmod.content.skills.construction.house.PlacedRoom
import org.rsmod.content.skills.construction.house.PohTemplate
import org.rsmod.content.skills.construction.house.RoomKey
import org.rsmod.content.skills.construction.house.TemplateIndex
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Construction: the player-owned house, and everything that goes into one.
 *
 * The shape of the skill is entirely cache-driven -- see
 * [org.rsmod.content.skills.construction.data.ConstructionTables] -- so this script is only the
 * behaviour around it:
 * - the six world portals let the owner in, either normally or in building mode;
 * - a door hotspot in building mode offers the rooms that will fit there, charging the room's cost;
 * - a furniture hotspot in building mode offers what that hotspot takes, charging the materials and
 *   paying the experience;
 * - a built piece offers `Remove`, which gives nothing back, exactly as OSRS does.
 *
 * Furniture is chosen through the real build menu -- interface 458, see [BuildMenu]. Rooms are
 * chosen through the vanilla list modal, which suits a plain list of thirty names better than a
 * grid of item models would.
 *
 * The make-menu was tried for furniture first and cannot work: `skillmulti_itembutton_triggered`
 * only forwards a press after `cc_find(component, quantity)` succeeds, and for the
 * quantity-suppressed types -- the only ones that suit building one thing at a time -- setup never
 * creates that child, so the click dies inside the client with no packet sent.
 *
 * Two things are worth knowing about how it hangs together. First, **every hotspot loc is bound
 * individually** rather than through a content group: hotspot locs number in the hundreds and are
 * discovered from the templates at startup, and binding them by type keeps a content group free for
 * anything upstream might want to tag them with. Second, **adding or removing a room rebuilds the
 * region**, because rooms are copied zones and a region's zone map is fixed once built; furniture,
 * which is only ever locs on top, is added and removed in place.
 */
class ConstructionScript
@Inject
constructor(
    private val tables: ConstructionTables,
    private val templates: TemplateIndex,
    private val furnitureLocs: FurnitureLocs,
    private val houses: HouseManager,
    private val buildMenu: BuildMenu,
    private val registry: HouseRegistry,
    private val locTypes: LocTypeList,
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
) : PluginScript() {
    /**
     * Where each player stood when they stepped through their portal, so `Enter` can undo itself.
     */
    private val returnCoords = HashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        templates.index()

        for (portal in ConstructionLocs.world_portals) {
            onOpLoc1(portal) { enterHouse(buildMode = false) }
            onOpLoc3(portal) { enterHouse(buildMode = true) }
        }

        onOpLoc1(ConstructionLocs.exit_portal) { leaveHouse() }

        for (locId in templates.doorHotspotLocs) {
            val type = locTypes[locId] ?: continue
            onOpLoc5(type) { offerRooms(it.loc) }
        }

        for (locId in hotspotLocIds()) {
            val type = locTypes[locId] ?: continue
            onOpLoc5(type) { offerFurniture(it.loc, type) }
        }

        for (locId in builtFurnitureLocIds()) {
            val type = locTypes[locId] ?: continue
            onOpLoc5(type) { removeFurniture(it.loc) }
        }

        onPlayerLogout { player.evictFromHouse() }
        onEvent<SessionStateEvent.Delete> { registry.remove(player) }
    }

    /* Entering and leaving */

    private fun ProtectedAccess.enterHouse(buildMode: Boolean) {
        val garden = gardenRoomType()
        if (garden == null) {
            mes("Something has gone wrong with your house. Please report this.")
            return
        }
        val house = registry.getOrCreate(player, garden)
        val landing = houses.open(player, house, buildMode)
        if (landing == null) {
            mes("Your house is unavailable right now. Try again in a few seconds.")
            return
        }
        returnCoords[player] = player.coords
        vars[ConstructionVarBits.building_mode] = if (buildMode) 1 else 0
        vars[ConstructionVarBits.house_style] = house.styleId
        vars[ConstructionVarBits.house_location] = house.locationId
        teleport(landing)
        if (buildMode) {
            mes("Welcome to your house, in building mode.")
        } else {
            mes("Welcome to your house.")
        }
    }

    private fun ProtectedAccess.leaveHouse() {
        val destination = returnCoords.remove(player)
        houses.forget(player)
        vars[ConstructionVarBits.building_mode] = 0
        if (destination == null) {
            mes("You have nowhere to return to.")
            return
        }
        teleport(destination)
    }

    /**
     * A player who logs out inside their house would come back to a region that no longer exists,
     * so their coordinates are moved back outside before the save runs.
     *
     * The house itself stays in the registry: logout fires *before* the account save is queued, and
     * dropping it here left [org.rsmod.content.skills.construction.house.CharacterHousePipeline]
     * with nothing to write. It is dropped on [SessionStateEvent.Delete] instead, which runs once
     * the save has been through.
     */
    private fun Player.evictFromHouse() {
        val destination = returnCoords.remove(this)
        houses.forget(this)
        if (destination != null) {
            coords = destination
        }
    }

    /* Rooms */

    private suspend fun ProtectedAccess.offerRooms(door: BoundLocInfo) {
        val session = requireBuildMode() ?: return
        val standing = houses.roomAt(session, door.coords) ?: return stuck("tile ${door.coords}")
        val direction =
            doorDirection(session, standing, door) ?: return stuck("doorway ${door.coords}")
        val target = standing.translate(direction.deltaX, direction.deltaZ)

        if (!PohTemplate.isInsideGrid(target.gridX, target.gridZ)) {
            mes("You can't build any further out than this.")
            return
        }
        if (session.house[target] != null) {
            mes("There is already a room there.")
            return
        }

        val buildable =
            tables.rooms.values
                .filter { it.isAllowedOn(target.level) }
                .sortedBy { it.levelRequirement }
        if (buildable.isEmpty()) {
            mes("You can't build anything here.")
            return
        }

        val choice = menu("Choose a room", hotkeys = false, choices = buildable.map { it.label() })
        val room = buildable.getOrNull(choice) ?: return
        buildRoom(session, target, room, direction)
    }

    private fun ProtectedAccess.buildRoom(
        session: HouseSession,
        target: RoomKey,
        room: RoomData,
        approach: Direction,
    ) {
        if (player.constructionLvl < room.levelRequirement) {
            mes(
                "You need a Construction level of ${room.levelRequirement} to build a ${room.name}."
            )
            return
        }
        if (invTotal(inv, objs.coins) < room.cost) {
            mes("You need ${room.cost} coins to build a ${room.name}.")
            return
        }
        val template = templates.room(room.roomType)
        // The new room has to open back onto the one it was reached from, or it would be sealed
        // off.
        val rotation = template?.rotationFacing(approach.opposite()) ?: 0
        if (template != null && rotation == NO_ROTATION_FITS) {
            mes("A ${room.name} doesn't have a door on that side.")
            return
        }

        val deleted = invDel(inv, objs.coins, room.cost)
        if (!deleted.success) {
            return
        }

        session.house.put(target, PlacedRoom(room.roomType, rotation))
        val landing = houses.rebuild(player, target)
        if (landing == null) {
            mes("Your house is unavailable right now. Try again in a few seconds.")
            return
        }
        teleport(landing)
        mes("You build a ${room.name}.")
    }

    /* Furniture */

    private suspend fun ProtectedAccess.offerFurniture(loc: BoundLocInfo, type: UnpackedLocType) {
        val session = requireBuildMode() ?: return
        if (!hasTools()) {
            return
        }
        val slot = templates.slotOf(type.id) ?: return stuck("hotspot ${type.internalName}")
        val key = houses.roomAt(session, loc.coords) ?: return stuck("tile ${loc.coords}")
        val room = session.house[key] ?: return stuck("no room at $key")
        val roomData = tables.rooms[room.roomType] ?: return stuck("room ${room.roomType}")
        val hotspotRow = roomData.hotspots.getOrNull(slot.slot) ?: return stuck("slot ${slot.slot}")
        val hotspot = tables.hotspot(hotspotRow) ?: return stuck("hotspot row $hotspotRow")

        val variants =
            templates.room(room.roomType)?.pieces(slot.slot)?.map { it.variant }?.distinct()
                ?: return stuck("no template pieces for slot ${slot.slot}")

        val options =
            hotspot.options
                .distinct()
                .mapNotNull(tables::furniture)
                .filterNot { it.hidden }
                .filter { furnitureLocs.canPlace(it.rowId, variants) }

        if (options.isEmpty()) {
            mes("You can't build anything here yet.")
            return
        }

        val rows = options.map { BuildOption(it, canBuildNow(it), it.cost()) }
        buildMenu.open(this, rows) { furniture -> buildFurniture(key, slot.slot, furniture) }
    }

    /**
     * Whether the player could build this right now. The menu marks the ones they cannot rather
     * than hiding them, so the list doubles as the shopping list it is in OSRS.
     */
    private fun ProtectedAccess.canBuildNow(furniture: FurnitureData): Boolean {
        if (player.constructionLvl < furniture.levelRequirement) {
            return false
        }
        return furniture.materials.all { material ->
            val type = objTypes[material.obj] ?: return@all false
            invTotal(inv, type) >= material.count
        }
    }

    private suspend fun ProtectedAccess.buildFurniture(
        key: RoomKey,
        slot: Int,
        furniture: FurnitureData,
    ) {
        val session = requireBuildMode() ?: return
        val room = session.house[key] ?: return stuck("no room at $key")
        if (room.isBuilt(slot)) {
            mes("There is already something built there.")
            return
        }
        if (player.constructionLvl < furniture.levelRequirement) {
            mes(
                "You need a Construction level of ${furniture.levelRequirement} to build " +
                    "${furniture.name.lowercase()}."
            )
            return
        }
        if (!hasTools()) {
            return
        }
        for (material in furniture.materials) {
            val type = objTypes[material.obj] ?: continue
            if (invTotal(inv, type) < material.count) {
                mes("You need ${material.count} x ${type.name} to build that.")
                return
            }
        }

        anim(ConstructionSeqs.build)
        delay(BUILD_TICKS)

        // Re-read everything after the delay: the player may have moved, or the region may have
        // been rebuilt underneath them.
        val current = houses.session(player) ?: return stuck("left the house mid-build")
        val standing = current.house[key] ?: return stuck("no room at $key")
        if (standing.isBuilt(slot)) {
            return stuck("slot $slot filled mid-build")
        }
        for (material in furniture.materials) {
            val type = objTypes[material.obj] ?: continue
            val deleted = invDel(inv, type, material.count)
            if (!deleted.success) {
                return stuck("could not take ${material.count} x ${type.name}")
            }
        }

        standing.build(slot, furniture.rowId)
        val placed = placePieces(current, key, standing, slot, furniture.rowId)
        statAdvance(
            stats.construction,
            ConstructionXp.of(furniture) * xpMods.get(player, stats.construction),
        )
        if (placed == 0) {
            stuck("built ${furniture.name} but could place none of its scenery")
            return
        }
        mes("You build ${furniture.name.lowercase().withArticle()}.")
    }

    private suspend fun ProtectedAccess.removeFurniture(loc: BoundLocInfo) {
        val session = requireBuildMode() ?: return
        val key = houses.roomAt(session, loc.coords) ?: return stuck("tile ${loc.coords}")
        val room = session.house[key] ?: return stuck("no room at $key")
        val template = templates.room(room.roomType) ?: return stuck("room ${room.roomType}")

        val built =
            room.allBuilt().entries.firstOrNull { (slot, furnitureRow) ->
                template.pieces(slot).any { piece ->
                    furnitureLocs.resolve(furnitureRow, piece.variant) == loc.id
                }
            } ?: return stuck("nothing built at ${loc.coords}")
        val slot = built.key
        val furnitureRow = built.value

        val furniture = tables.furniture(furnitureRow) ?: return stuck("row $furnitureRow")
        // The chatbox dialogue rather than the centre-screen list: it is what OSRS uses to confirm
        // a removal, it does not cover the room, and its options answer to the number keys.
        val confirmed =
            choice2(
                "Yes, I don't need it any more.",
                true,
                "No, I'll keep it.",
                false,
                title = "Really remove the ${furniture.name.lowercase()}?",
            )
        if (!confirmed) {
            // Silence here reads as a broken click, and this menu is reached by clicking a piece
            // of furniture that is already built -- which is easy to mistake for a failed build.
            mes("You leave the ${furniture.name.lowercase()} where it is.")
            return
        }

        anim(ConstructionSeqs.build)
        delay(BUILD_TICKS)

        val current = houses.session(player) ?: return stuck("left the house mid-removal")
        val standing = current.house[key] ?: return stuck("no room at $key")
        if (standing.built(slot) != furnitureRow) {
            return stuck("slot $slot changed mid-removal")
        }
        val removed = removePieces(current, key, standing, slot, furnitureRow)
        if (removed == 0) {
            stuck("could not take down the ${furniture.name.lowercase()}")
            return
        }
        standing.clear(slot)
        mes("You remove the ${furniture.name.lowercase()}.")
    }

    /* Helpers */

    /** Swaps a hotspot's template locs for the furniture's, returning how many went down. */
    private fun placePieces(
        session: HouseSession,
        key: RoomKey,
        room: PlacedRoom,
        slot: Int,
        furnitureRow: Int,
    ): Int {
        val template = templates.room(room.roomType) ?: return 0
        var placed = 0
        for (piece in template.pieces(slot)) {
            houses.pieceLoc(session, key, room, piece, piece.locId)?.let { houses.deleteLoc(it) }
            val locId = furnitureLocs.resolve(furnitureRow, piece.variant) ?: continue
            val loc = houses.pieceLoc(session, key, room, piece, locId) ?: continue
            if (houses.addLoc(loc)) {
                placed++
            }
        }
        return placed
    }

    /** Takes a hotspot's furniture back down, returning how many of its pieces actually went. */
    private fun removePieces(
        session: HouseSession,
        key: RoomKey,
        room: PlacedRoom,
        slot: Int,
        furnitureRow: Int,
    ): Int {
        val template = templates.room(room.roomType) ?: return 0
        var removed = 0
        for (piece in template.pieces(slot)) {
            val locId = furnitureLocs.resolve(furnitureRow, piece.variant) ?: continue
            val built = houses.pieceLoc(session, key, room, piece, locId) ?: continue
            if (houses.deleteLoc(built)) {
                removed++
            }
            // Building mode is the only way to get here, so the hotspot goes straight back.
            houses.pieceLoc(session, key, room, piece, piece.locId)?.let { houses.addLoc(it) }
        }
        return removed
    }

    /**
     * Reports a build that stopped for a reason the player cannot see.
     *
     * Every one of these was a bare `return` to begin with, which made a house that would not build
     * indistinguishable from a click that never arrived. None of them should happen in a healthy
     * house, so they say what went wrong rather than staying quiet.
     */
    private fun ProtectedAccess.stuck(reason: String) {
        mes("You can't build that here. ($reason)")
        logger.warn { "Construction stopped for ${player.displayName}: $reason" }
    }

    /** The session, but only when the player is standing in their own house in building mode. */
    private fun ProtectedAccess.requireBuildMode(): HouseSession? {
        val session = houses.session(player)
        if (session == null) {
            mes("You can only do that inside your own house.")
            return null
        }
        if (!session.buildMode) {
            mes("You need to be in building mode to do that.")
            return null
        }
        return session
    }

    private fun ProtectedAccess.hasTools(): Boolean {
        if (invTotal(inv, ConstructionObjs.saw) <= 0) {
            mes("You need a saw to build furniture.")
            return false
        }
        if (invTotal(inv, ConstructionObjs.hammer) <= 0) {
            mes("You need a hammer to build furniture.")
            return false
        }
        return true
    }

    private fun gardenRoomType(): Int? =
        tables.rooms.values.firstOrNull { it.internalName == STARTER_ROOM }?.roomType

    private fun RoomData.isAllowedOn(level: Int): Boolean =
        when (floorRestriction) {
            PohTemplate.RESTRICT_DUNGEON -> level == PohTemplate.LEVEL_DUNGEON
            PohTemplate.RESTRICT_GROUND -> level == PohTemplate.LEVEL_GROUND
            else -> level != PohTemplate.LEVEL_DUNGEON
        }

    private fun String.withArticle(): String =
        if (isNotEmpty() && first() in "aeiou") "an $this" else "a $this"

    private fun RoomData.label(): String = "$displayName (level $levelRequirement, $cost coins)"

    /** The cost line the build menu shows under a piece of furniture. */
    private fun FurnitureData.cost(): String =
        materials.joinToString(", ") { material ->
            "${material.count} x ${objTypes[material.obj]?.name ?: "?"}"
        }

    /** Which wall of the room the player is standing in this door hotspot sits against. */
    private fun doorDirection(
        session: HouseSession,
        standing: RoomKey,
        door: BoundLocInfo,
    ): Direction? {
        val southWest =
            PohTemplate.roomSouthWest(
                session.region.southWest,
                standing.gridX,
                standing.gridZ,
                standing.level,
            )
        return Direction.ofWall(door.coords.x - southWest.x, door.coords.z - southWest.z)
    }

    private fun hotspotLocIds(): Set<Int> = buildSet {
        for (room in tables.rooms.values) {
            val template = templates.room(room.roomType) ?: continue
            template.hotspots.forEach { add(it.locId) }
        }
    }

    private fun builtFurnitureLocIds(): Set<Int> = buildSet {
        for (room in tables.rooms.values) {
            val template = templates.room(room.roomType) ?: continue
            val data = tables.rooms[room.roomType] ?: continue
            for (slot in template.slots) {
                val hotspot = data.hotspots.getOrNull(slot)?.let(tables::hotspot) ?: continue
                val variants = template.pieces(slot).map { it.variant }.distinct()
                for (furnitureRow in hotspot.options) {
                    for (variant in variants) {
                        furnitureLocs.resolve(furnitureRow, variant)?.let(::add)
                    }
                }
            }
        }
    }

    private companion object {
        private val logger = InlineLogger()

        const val BUILD_TICKS = 3
        const val NO_ROTATION_FITS = -1
        const val STARTER_ROOM = "poh_dummy_garden"
    }
}
