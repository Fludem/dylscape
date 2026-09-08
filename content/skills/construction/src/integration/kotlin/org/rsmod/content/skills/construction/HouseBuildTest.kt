package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.FurnitureLocs
import org.rsmod.content.skills.construction.house.HouseManager
import org.rsmod.content.skills.construction.house.PlacedRoom
import org.rsmod.content.skills.construction.house.PlayerOwnedHouse
import org.rsmod.content.skills.construction.house.PohTemplate
import org.rsmod.content.skills.construction.house.RoomKey
import org.rsmod.content.skills.construction.house.TemplateIndex
import org.rsmod.game.region.zone.RegionZoneCopy
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey

/**
 * Drives a house the way the script does, without the script.
 *
 * `runGameTest` builds its own empty world, so the construction templates are not in it -- which is
 * why the script's own test can only cover entering and leaving. Running with **no scripts** means
 * nothing has indexed the templates yet, so a stand-in parlour can be placed at the template
 * coordinates first and [TemplateIndex] pointed at it afterwards. That is enough to exercise the
 * part that cannot otherwise be reached: assembling the region, finding the hotspot inside it, and
 * swapping that hotspot for the furniture's loc.
 */
@Execution(ExecutionMode.SAME_THREAD)
class HouseBuildTest {
    class Deps
    @Inject
    constructor(
        val houses: HouseManager,
        val templates: TemplateIndex,
        val tables: ConstructionTables,
        val furnitureLocs: FurnitureLocs,
        val regions: RegionRegistry,
    )

    @Test
    fun GameTestState.`a hotspot inside a built house can be swapped for its furniture`() =
        runInjectedGameTest(Deps::class) { deps ->
            val parlour = deps.tables.rooms.values.first { it.internalName == "poh_dummy_parlour" }
            seedParlour(parlour.templateOffsetX, parlour.templateOffsetZ)
            deps.templates.index()

            val template = deps.templates.room(parlour.roomType)
            assertNotNull(template) { "The seeded parlour template was not picked up." }
            assertEquals(listOf(0), template!!.slots) { "Only the first chair space was seeded." }

            val house = PlayerOwnedHouse()
            val key = RoomKey(PohTemplate.LEVEL_GROUND, 6, 6)
            house.put(key, PlacedRoom(parlour.roomType, rotation = 0))

            val landing = deps.houses.open(player, house, buildMode = true)
            assertNotNull(landing) { "No region could be allocated for the house." }
            player.teleport(landing!!)

            val session = deps.houses.session(player)!!
            val room = house[key]!!
            val piece = template.pieces(slot = 0).single()

            // The hotspot has to be reachable inside the region before anything can replace it.
            val hotspot = deps.houses.pieceLoc(session, key, room, piece, piece.locId)
            assertNotNull(hotspot)
            assertTrue(findLocs(hotspot!!.coords).any { it.id == piece.locId }) {
                "The chair space did not survive the zone copy into the region; it was expected " +
                    "at ${hotspot.coords} but the tile holds " +
                    "${findLocs(hotspot.coords).map { locTypes[it.id]?.internalName }.toList()}."
            }

            // Every square the house does not occupy on the ground floor is still mapped, so the
            // house stands on ground rather than hanging in an unmapped void.
            for (corner in listOf(0 to 0, 0 to 12, 12 to 0, 12 to 12)) {
                val (gridX, gridZ) = corner
                val zone =
                    ZoneKey.from(
                        PohTemplate.roomSouthWest(
                            session.region.southWest,
                            gridX,
                            gridZ,
                            PohTemplate.LEVEL_GROUND,
                        )
                    )
                assertTrue(deps.regions[zone] != RegionZoneCopy.NULL) {
                    "Grid square $gridX,$gridZ is unmapped; the house will render in a void."
                }
            }

            // Now the swap the build path performs.
            val chairRow =
                deps.tables.furniture.values.first { it.internalName == "poh_armchair_1" }.rowId
            val chairLocId = deps.furnitureLocs.resolve(chairRow, piece.variant)
            assertNotNull(chairLocId) { "Crude wooden chair resolved to no loc." }

            deps.houses.deleteLoc(hotspot)
            val chair = deps.houses.pieceLoc(session, key, room, piece, chairLocId!!)
            assertNotNull(chair)
            assertTrue(deps.houses.addLoc(chair!!)) {
                "The region refused the chair at ${chair.coords}."
            }
            assertTrue(findLocs(chair.coords).any { it.id == chairLocId }) {
                "The chair was accepted but is not on the tile."
            }

            // ...and the swap the removal path performs, which has to undo it exactly.
            deps.houses.deleteLoc(chair)
            val restored = deps.houses.pieceLoc(session, key, room, piece, piece.locId)
            assertNotNull(restored)
            deps.houses.addLoc(restored!!)

            assertFalse(findLocs(chair.coords).any { it.id == chairLocId }) {
                "The chair survived removal; the tile still holds " +
                    "${findLocs(chair.coords).map { locTypes[it.id]?.internalName }.toList()}."
            }
            assertTrue(findLocs(restored.coords).any { it.id == piece.locId }) {
                "The chair space did not come back after the chair was removed."
            }
        }

    /** Places a single chair space in the parlour's template zone, plus a floor to hang it on. */
    private fun GameTestScope.seedParlour(offsetX: Int, offsetZ: Int) {
        val chairSpace = findLocTypes { it.internalName == "poh_parlour_1" }.first()
        val x = PohTemplate.BASE_X + offsetX + 2
        val z = PohTemplate.BASE_Z + offsetZ + 4
        placeMapLoc(CoordGrid(0, x / 64, z / 64, x % 64, z % 64), chairSpace)
    }
}
