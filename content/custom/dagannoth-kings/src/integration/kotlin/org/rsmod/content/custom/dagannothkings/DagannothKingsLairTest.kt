package org.rsmod.content.custom.dagannothkings

import jakarta.inject.Inject
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
    fun GameTestState.`every king stands on open floor`() =
        runInjectedGameTest(DagannothLairDeps::class) { deps ->
            for ((king, coords) in DagannothKingsLair.spawns) {
                val flags = deps.collision[coords.x, coords.z, coords.level]
                assertEquals(
                    0,
                    flags and BLOCKS_STANDING,
                    "${king.displayName} spawns on a blocked tile: $coords (flags $flags).",
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

    private companion object {
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
