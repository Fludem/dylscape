package org.rsmod.content.custom.barrows

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.configs.barrows_locs
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Keeps [BarrowsMap] honest against the real map.
 *
 * Every coordinate in it was read off a cache dump, and a wrong one fails quietly rather than
 * loudly: `telejump` puts the player wherever it is told, so a bad tile is a player inside a wall
 * and nothing else reports it. These tests are the thing that reports it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsMapTest {
    @Test
    fun GameTestState.`every authored coordinate is walkable`() =
        runInjectedGameTest(BarrowsCollisionDeps::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            val stuck = BarrowsMap.allCoords.filter { deps.collision[it] and blocked != 0 }
            assertTrue(stuck.isEmpty(), "Unwalkable barrows coordinates: $stuck")
        }

    @Test
    fun GameTestState.`the chest has room around it`() =
        runInjectedGameTest(BarrowsCollisionDeps::class) { deps ->
            // The chest is 2x2 and we place it ourselves, so it is entirely possible to seal a
            // corridor with it. A clear ring means there is always a way past.
            val chest = BarrowsMap.chest
            val blocked = CollisionFlag.BLOCK_WALK
            for (dx in -1..2) {
                for (dz in -1..2) {
                    val at = CoordGrid(chest.x + dx, chest.z + dz, chest.level)
                    assertTrue(deps.collision[at] and blocked == 0, "Chest surround blocked at $at")
                }
            }
        }

    @Test
    fun GameTestState.`each sarcophagus and staircase is where the table says`() =
        runAdvancedGameTest { advanced ->
            val registry = advanced.readOnly.locRegistry
            for (brother in Brother.all) {
                val sarcophagus = checkNotNull(BarrowsMap.sarcophagi[brother])
                val expected = checkNotNull(barrows_locs.sarcophagi[brother])
                assertTrue(
                    registry.findType(sarcophagus, expected.id) != null,
                    "No ${expected.internalName} at $sarcophagus",
                )

                val staircase = checkNotNull(BarrowsMap.staircases[brother])
                val stairsType = checkNotNull(barrows_locs.staircases[brother])
                assertTrue(
                    registry.findType(staircase, stairsType.id) != null,
                    "No ${stairsType.internalName} at $staircase",
                )
            }
        }

    @Test
    fun GameTestState.`the crypt entrance is beside its staircase`() = runBasicGameTest {
        for (brother in Brother.all) {
            val entrance = checkNotNull(BarrowsMap.cryptEntrances[brother])
            val staircase = checkNotNull(BarrowsMap.staircases[brother])
            val distance = maxOf(abs(entrance.x - staircase.x), abs(entrance.z - staircase.z))
            assertEquals(1, distance, "$brother lands $distance tiles from its staircase")
        }
    }

    @Test
    fun GameTestState.`every mound is on the surface and every crypt tile underground`() =
        runBasicGameTest {
            for ((brother, mound) in BarrowsMap.mounds) {
                assertEquals(0, mound.level, "$brother mound is not on the surface")
                assertTrue(BarrowsCrypt.inCrypt(mound).not(), "$brother mound reads as crypt")
            }
            val underground =
                BarrowsMap.sarcophagi.values +
                    BarrowsMap.staircases.values +
                    BarrowsMap.cryptEntrances.values +
                    listOf(BarrowsMap.chest, BarrowsMap.chestAmbushSpawn)
            for (coords in underground) {
                assertTrue(BarrowsCrypt.inCrypt(coords), "$coords is not inside the crypt box")
            }
        }

    /**
     * The tunnel level the plan originally assumed. Kept as a test so the assumption cannot creep
     * back: if a future cache ever ships the tunnels, this fails and tells somebody to revisit
     * `BarrowsMap`'s note about the crypt maze standing in for them.
     */
    @Test
    fun GameTestState.`the cache still has no barrows tunnel level`() =
        runAdvancedGameTest { advanced ->
            val registry = advanced.readOnly.locRegistry
            var locs = 0
            for (zoneX in UNDER_ZONE_X until UNDER_ZONE_X + ZONES_PER_SQUARE) {
                for (zoneZ in UNDER_ZONE_Z until UNDER_ZONE_Z + ZONES_PER_SQUARE) {
                    locs += registry.findAll(ZoneKey(zoneX, zoneZ, 0)).count()
                }
            }
            assertEquals(0, locs, "Mapsquare 55_151 level 0 now has locs; the tunnels may exist")
        }

    private companion object {
        const val UNDER_ZONE_X = 55 * 8
        const val UNDER_ZONE_Z = 151 * 8
        const val ZONES_PER_SQUARE = 8
    }
}

private class BarrowsCollisionDeps
@jakarta.inject.Inject
constructor(val collision: CollisionFlagMap)
