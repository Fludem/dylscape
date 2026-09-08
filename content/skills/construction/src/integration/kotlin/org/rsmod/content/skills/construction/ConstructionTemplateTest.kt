package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.house.Direction
import org.rsmod.content.skills.construction.house.PohTemplate
import org.rsmod.content.skills.construction.house.TemplateIndex

/**
 * Checks the hotspot scan against the map the server actually loads.
 *
 * `runGameTest` builds its own empty world, so the construction templates are not in it; the
 * advanced scope's registry is the one view of the fully decoded map a test can reach, which is why
 * these run through [GameTestState.runAdvancedGameTest] and construct [TemplateIndex] by hand.
 */
class ConstructionTemplateTest {
    @Test
    fun GameTestState.`the parlour template yields seven hotspot slots`() = runAdvancedGameTest {
        val index = index(it.readOnly.locRegistry)
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val parlour = tables.rooms.values.first { room -> room.internalName == "poh_dummy_parlour" }

        val template = index.room(parlour.roomType)
        assertNotNull(template)
        assertEquals(listOf(0, 1, 2, 3, 4, 5, 6), template!!.slots) {
            "The parlour's hotspot locs are poh_parlour_1 through _7, so slots 0..6."
        }

        val chairs = template.pieces(slot = 0)
        assertEquals(1, chairs.size) { "The first chair space is a single tile." }
        assertEquals("poh_parlour_1", cacheTypes.locs[chairs.first().locId]?.internalName)

        val rug = template.pieces(slot = 3)
        assertTrue(rug.size > 1) { "A rug is laid out of middle, side and corner pieces." }
        assertTrue(rug.any { piece -> piece.variant == "middle" })
        assertTrue(rug.any { piece -> piece.variant == "corner" })
    }

    @Test
    fun GameTestState.`every room template is found and has a doorway`() = runAdvancedGameTest {
        val index = index(it.readOnly.locRegistry)
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)

        val withHotspots =
            tables.rooms.values.count { room ->
                index.room(room.roomType)?.hotspots?.isNotEmpty() == true
            }
        val withDoors =
            tables.rooms.values.count { room ->
                index.room(room.roomType)?.doors?.isNotEmpty() == true
            }
        val missing =
            tables.rooms.values.filter { room ->
                index.room(room.roomType)?.hotspots?.isEmpty() != false
            }
        println("Rooms without hotspots: ${missing.map { room -> room.internalName }}")
        println("Rooms with hotspots: $withHotspots / ${tables.rooms.size}")
        println("Rooms with doorways: $withDoors / ${tables.rooms.size}")

        assertEquals(tables.rooms.size, withHotspots) {
            "Only $withHotspots of ${tables.rooms.size} room templates yielded hotspots " +
                "(${missing.map { room -> room.internalName }}); the naming the scan relies on " +
                "has drifted."
        }
        assertEquals(tables.rooms.size, withDoors) {
            "Only $withDoors of ${tables.rooms.size} room templates have a doorway; rooms with " +
                "none can never be reached."
        }
    }

    @Test
    fun GameTestState.`the garden opens on all four sides`() = runAdvancedGameTest {
        val index = index(it.readOnly.locRegistry)
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val garden = tables.rooms.values.first { room -> room.internalName == "poh_dummy_garden" }

        val template = index.room(garden.roomType)
        assertNotNull(template)
        assertEquals(
            setOf(Direction.North, Direction.East, Direction.South, Direction.West),
            template!!.doors,
        ) {
            "The starter garden has to be extendable in every direction."
        }
        for (direction in Direction.entries) {
            assertTrue(template.rotationFacing(direction) >= 0)
        }
    }

    @Test
    fun GameTestState.`the template grid sits where the room offsets say it does`() =
        runAdvancedGameTest {
            val zone = PohTemplate.templateZone(styleId = 0, offsetX = 0, offsetZ = 56)
            assertEquals(PohTemplate.BASE_X, zone.x * 8) { "Parlour column." }
            assertEquals(PohTemplate.BASE_Z + 56, zone.z * 8) { "Parlour row." }
            assertEquals(0, zone.level) { "Style 0 is drawn on map level 0." }

            // Style 5 is the sixth theme: second group of four, second level within it.
            val fancy = PohTemplate.templateZone(styleId = 5, offsetX = 0, offsetZ = 56)
            assertEquals(PohTemplate.BASE_X + 64, fancy.x * 8)
            assertEquals(1, fancy.level)
        }

    private fun GameTestState.index(
        locRegistry: org.rsmod.api.registry.loc.LocRegistry
    ): TemplateIndex {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        return TemplateIndex(locRegistry, cacheTypes.locs, tables).apply { index() }
    }
}
