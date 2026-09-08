package org.rsmod.content.custom.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.map.CoordGrid

/**
 * Guards the two things the destination table cannot check for itself: that the component name we
 * bind is one the cache actually has, and that the Wilderness arithmetic matches the bounds it
 * claims to model.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TeleportDestinationsTest {
    @Test
    fun GameTestState.`home teleport button is a real component on the spellbook`() =
        runBasicGameTest {
            val component =
                cacheTypes.components.values.firstOrNull {
                    it.internalName == "magic_spellbook:teleport_home_standard"
                }
            assertNotNull(component) { "The spellbook has no `teleport_home_standard` component." }
            assertEquals(218, component!!.interfaceId)
            assertEquals(7, component.component)
        }

    @Test
    fun `every category has entries and no coordinate is reused`() {
        for (category in TeleportCategory.entries) {
            val destinations = TeleportDestinations[category]
            assert(destinations.isNotEmpty()) { "Category $category is empty." }
        }
        val coords = TeleportDestinations.all.map(TeleportDestination::dest)
        assertEquals(coords.size, coords.toSet().size, "Two destinations share a coordinate.")
    }

    @Test
    fun `no destination sits in the wilderness`() {
        // A Wilderness destination would teleport the player somewhere they could not teleport
        // back out of, and would need the arrival side checked too.
        for (destination in TeleportDestinations.all) {
            assertEquals(
                0,
                wildernessLevel(destination.dest),
                "${destination.label} is in the Wilderness.",
            )
        }
    }

    @Test
    fun `wilderness level is zero outside the band and climbs every eight tiles inside it`() {
        // Lumbridge and Varrock: well inside the x band, well south of the z band.
        assertEquals(0, wildernessLevel(CoordGrid(0, 50, 50, 21, 18)))
        assertEquals(0, wildernessLevel(CoordGrid(0, 50, 53, 13, 32)))
        // Level 1 starts at the first tile of the band, and each level is eight tiles deep.
        assertEquals(1, wildernessLevel(CoordGrid(3100, 3520)))
        assertEquals(1, wildernessLevel(CoordGrid(3100, 3527)))
        assertEquals(2, wildernessLevel(CoordGrid(3100, 3528)))
        assertEquals(21, wildernessLevel(CoordGrid(3100, 3680)))
        // The caves mirror the surface band 6400 tiles north.
        assertEquals(1, wildernessLevel(CoordGrid(3100, 9920)))
        assertEquals(21, wildernessLevel(CoordGrid(3100, 10080)))
    }

    @Test
    fun `northern non-wilderness ground is not treated as wilderness`() {
        // Far enough north to be inside the z band, but east and west of the x bounds. Testing z
        // alone would refuse teleports from safe ground such as Fremennik or Piscatoris.
        assertEquals(0, wildernessLevel(CoordGrid(2700, 3700)))
        assertEquals(0, wildernessLevel(CoordGrid(3500, 3700)))
    }
}
