package org.rsmod.content.custom.dagannothkings

import jakarta.inject.Inject
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.dagannothkings.configs.DagannothKingsLair
import org.rsmod.content.custom.dagannothkings.configs.dk_locs
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class DagannothLairDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * Guards the coordinates, which is the half of this module a compiler cannot check.
 *
 * `telejump` does not fail on a bad destination -- it puts the player inside a wall, or on a tile
 * that does not exist, and reports nothing. The same is true of a spawn tile. Since every game test
 * is handed the real collision map and the real area index, the whole table is swept here.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsLairTest {
    @Test
    fun GameTestState.`every king has a clear three by three footprint`() =
        runInjectedGameTest(DagannothLairDeps::class) { deps ->
            // Checking only the spawn tile is not enough: these are size-3 npcs, so a coordinate
            // near the wall passes a single-tile check and then fails to place at all.
            for ((king, coords) in DagannothKingsLair.spawns) {
                for (dx in 0 until DagannothKingsLair.KING_SIZE) {
                    for (dz in 0 until DagannothKingsLair.KING_SIZE) {
                        val x = coords.x + dx
                        val z = coords.z + dz
                        val flags = deps.collision[x, z, coords.level]
                        assertEquals(
                            0,
                            flags and BLOCKS_STANDING,
                            "${king.displayName} overlaps a blocked tile at ($x, $z): flags $flags.",
                        )
                    }
                }
            }
        }

    @Test
    fun GameTestState.`the kings are spread out and clear of the entrance`() = runBasicGameTest {
        // The whole point of moving them off void's coordinates. If a future edit drags them back
        // together, or parks one on top of the ladder, the fight stops being separable.
        val arrival = DagannothKingsLair.lairArrival
        for ((king, coords) in DagannothKingsLair.spawns) {
            val fromEntrance = chebyshev(coords, arrival)
            assertTrue(
                fromEntrance >= MIN_ENTRANCE_DISTANCE,
                "${king.displayName} is $fromEntrance tiles from the ladder; it would aggro on arrival.",
            )
        }
        for ((a, b) in DagannothKingsLair.spawns.entries.pairs()) {
            val apart = chebyshev(a.value, b.value)
            assertTrue(
                apart >= MIN_SEPARATION,
                "${a.key.displayName} and ${b.key.displayName} are only $apart tiles apart.",
            )
        }
    }

    @Test
    fun GameTestState.`both arrival tiles are standable and on level zero`() =
        runInjectedGameTest(DagannothLairDeps::class) { deps ->
            val arrivals =
                mapOf(
                    "lair" to DagannothKingsLair.lairArrival,
                    "antechamber" to DagannothKingsLair.antechamberArrival,
                )
            for ((name, coords) in arrivals) {
                val flags = deps.collision[coords.x, coords.z, coords.level]
                assertEquals(
                    0,
                    flags and BLOCKS_STANDING,
                    "The $name arrival is blocked: $coords (flags $flags).",
                )
                // Explicit, because the obvious mistake here is to reason from the Rogues' Den,
                // where the underground floor sits on level 1. Mapsquare 45,69 carries terrain on
                // level 0 only -- there is no floor at all above it.
                assertEquals(0, coords.level, "The $name arrival drifted off level 0.")
            }
        }

    // Deliberately not tested here: that the lair is multiway.
    //
    // It is -- `api/cache-enricher/.../map/area/multiway.json` declares a "Dagannoth kings" polygon
    // spanning x 2880-2943, z 4352-4479, which contains all three spawns -- but it cannot be
    // asserted from a game test. `GameTestScope.TestModule` binds a fresh, empty `AreaIndex`, so
    // `AreaChecker.inArea` returns false for every tile in the test world, including famously
    // multi ones like the King Black Dragon lair. An assertion here would only be testing the
    // harness.
    //
    // Worth knowing for deployment: area data is written by `MapAreaEncoder`, which lives in
    // `TypeUpdaterResources`, and the only caller of that is `GameServerCachePacker`. So multiway
    // reaches `.data/cache/game` on a `./gradlew packCache`, never on a normal boot -- unlike the
    // npc editor in this module, which applies through config sync.

    @Test
    fun GameTestState.`both ladders are on the map where the script binds them`() =
        runAdvancedGameTest { advanced ->
            val registry = advanced.readOnly.locRegistry

            val upLoc =
                registry.findAll(ZoneKey.from(LAIR_LADDER)).firstOrNull { it.coords == LAIR_LADDER }
            assertTrue(upLoc != null, "No loc at all on the lair ladder tile $LAIR_LADDER.")
            assertEquals(
                dk_locs.boss_ladder_up.id,
                upLoc!!.id,
                "The lair ladder is not `dagexp_bossroomladder_up`.",
            )

            val downLoc =
                registry.findAll(ZoneKey.from(ANTE_LADDER)).firstOrNull { it.coords == ANTE_LADDER }
            assertTrue(
                downLoc != null,
                "No loc at all on the antechamber ladder tile $ANTE_LADDER.",
            )
            // The placed loc is the multiloc parent; `LocInteractions` resolves it before
            // dispatching, which is why the script binds the resolved child instead.
            val placed = cacheTypes.locs.getValue(downLoc!!.id)
            assertEquals(
                dk_locs.boss_ladder_down.id,
                placed.multiLocDefault,
                "The antechamber ladder no longer resolves to `dagexp_bossroomladder_down_normal`.",
            )
        }

    private fun chebyshev(a: CoordGrid, b: CoordGrid): Int = maxOf(abs(a.x - b.x), abs(a.z - b.z))

    private fun <T> Collection<T>.pairs(): List<Pair<T, T>> =
        toList().let { list ->
            list.indices.flatMap { i -> (i + 1 until list.size).map { j -> list[i] to list[j] } }
        }

    private companion object {
        /** Rex hunts 10 tiles and the other two 12, so this clears the widest of them. */
        const val MIN_ENTRANCE_DISTANCE = 13

        /** Far enough that fighting one does not automatically drag in the next. */
        const val MIN_SEPARATION = 12

        /** Stricter than a walk check: a ladder or a rock on the tile counts as unusable. */
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or
                CollisionFlag.BLOCK_PLAYERS or
                CollisionFlag.LOC or
                CollisionFlag.GROUND_DECOR

        val LAIR_LADDER = CoordGrid(2899, 4449, 0)
        val ANTE_LADDER = CoordGrid(1911, 4367, 0)
    }
}
