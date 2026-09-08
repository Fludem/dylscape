package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.ConstructionXp
import org.rsmod.content.skills.construction.data.FurnitureLocs

/**
 * Guards the cache-driven half of construction.
 *
 * None of this is transcribed data, so a cache update is the thing most likely to break it: a
 * renamed db table, a reshuffled column, a furniture row whose loc no longer shares its obj's
 * model. Each assertion below names something the skill genuinely depends on.
 */
class ConstructionDataTest {
    @Test
    fun GameTestState.`every room loads out of the cache`() = runBasicGameTest {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)

        assertEquals(29, tables.rooms.size) {
            "poh_room holds thirty rows, one of which is the null placeholder."
        }

        val parlour = tables.rooms.values.first { it.internalName == "poh_dummy_parlour" }
        assertEquals("parlour", parlour.name)
        assertEquals(1000, parlour.cost)
        assertEquals(1, parlour.levelRequirement)
        assertEquals(0, parlour.templateOffsetX)
        assertEquals(56, parlour.templateOffsetZ)
        assertEquals(7, parlour.hotspots.size) {
            "A parlour has three chairs, a rug, a bookcase, a fireplace and curtains."
        }

        val garden = tables.rooms.values.first { it.internalName == "poh_dummy_garden" }
        assertEquals(2, garden.floorRestriction) { "Gardens are ground floor only." }

        val blank = tables.blankRoom
        assertNotNull(blank) { "Without poh_dummy_null a house has no ground around it." }
        assertEquals("poh_dummy_null", blank!!.internalName)
        assertEquals(0, blank.templateOffsetX)
        assertEquals(0, blank.templateOffsetZ)
        assertTrue(blank.hotspots.isEmpty()) { "The blank square holds nothing to build." }
        assertTrue(blank.doors.isEmpty()) { "The blank square is not enterable." }
    }

    @Test
    fun GameTestState.`hotspots point at real furniture`() = runBasicGameTest {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val parlour = tables.rooms.values.first { it.internalName == "poh_dummy_parlour" }

        val chairs = tables.hotspot(parlour.hotspots[0])
        assertNotNull(chairs)
        assertEquals("armchair_hotspot", chairs!!.internalName)

        val crude = tables.furniture(chairs.options.first())
        assertNotNull(crude)
        assertEquals("Crude wooden chair", crude!!.name)
        assertEquals(1, crude.levelRequirement)
        assertEquals(2, crude.materials.size) { "Two planks and two nails." }
        assertEquals(960, crude.materials[0].obj) { "The first material is a plank." }
        assertEquals(2, crude.materials[0].count)
    }

    @Test
    fun GameTestState.`furniture resolves to the loc it builds`() = runBasicGameTest {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val locs = FurnitureLocs(cacheTypes.locs, cacheTypes.objs, tables)

        fun rowOf(name: String) = tables.furniture.values.first { it.internalName == name }.rowId

        assertEquals(
            "poh_chair1",
            cacheTypes.locs[locs.resolve(rowOf("poh_armchair_1"), variant = null)!!]?.internalName,
        )
        assertEquals(
            "poh_stove_1",
            cacheTypes.locs[locs.resolve(rowOf("poh_stove_1"), variant = null)!!]?.internalName,
        ) {
            "poh_stove_1 shares no model with its loc; only the exact-name rule finds it."
        }
        assertEquals(
            "poh_rugmiddle2",
            cacheTypes.locs[locs.resolve(rowOf("poh_rug_2"), variant = "middle")!!]?.internalName,
        ) {
            "A rug is laid a piece at a time; the middle piece has to follow the corner."
        }
        assertEquals(
            "poh_rugside2",
            cacheTypes.locs[locs.resolve(rowOf("poh_rug_2"), variant = "side")!!]?.internalName,
        )
    }

    @Test
    fun GameTestState.`most furniture can be placed`() = runBasicGameTest {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val locs = FurnitureLocs(cacheTypes.locs, cacheTypes.objs, tables)

        val reachable =
            tables.rooms.values
                .flatMap { it.hotspots }
                .mapNotNull(tables::hotspot)
                .flatMap { it.options }
                .distinct()
                .mapNotNull(tables::furniture)
                .filterNot { it.hidden }

        val placeable = reachable.count { locs.resolve(it.rowId, variant = null) != null }
        val ratio = placeable.toDouble() / reachable.size
        println("Furniture placeable: $placeable / ${reachable.size} (${(ratio * 100).toInt()}%)")
        assertTrue(ratio > 0.85) {
            "Only $placeable of ${reachable.size} furniture rows resolve to a loc; the naming " +
                "and model rules in FurnitureLocs have drifted from the cache."
        }
    }

    @Test
    fun GameTestState.`experience follows the materials`() = runBasicGameTest {
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)

        fun xpOf(name: String) =
            ConstructionXp.of(tables.furniture.values.first { it.internalName == name })

        assertEquals(58.0, xpOf("poh_armchair_1")) { "Crude wooden chair: two planks." }
        assertEquals(87.0, xpOf("poh_armchair_2")) { "Wooden chair: three planks." }
        assertEquals(180.0, xpOf("poh_armchair_5")) { "Oak armchair: three oak planks." }
        assertEquals(280.0, xpOf("poh_armchair_7")) { "Mahogany armchair: two mahogany planks." }
        assertEquals(60.0, xpOf("poh_rug_2")) { "Rug: four bolts of cloth." }
        assertEquals(360.0, xpOf("poh_rug_3")) { "Opulent rug: four bolts and a gold leaf." }
        assertEquals(500.0, xpOf("poh_fireplace_3")) { "Marble fireplace: one marble block." }
        assertEquals(115.0, xpOf("poh_bookcase_1")) { "Pinned: the material sum says 116." }
    }
}
