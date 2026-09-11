package org.rsmod.content.custom.zulrah

import jakarta.inject.Inject
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.zulrah.configs.ZulrahShrine
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class ZulrahShrineDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * The shrine's coordinate table, checked against the real map rather than trusted.
 *
 * Every number in [ZulrahShrine] was read off `ZulrahDump`'s walkability render. These tests are
 * what keep them honest if the cache, the template or the table ever changes.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ZulrahShrineTest {
    @Test
    fun GameTestState.`the arrival tile and the whole platform are walkable`() =
        runInjectedGameTest(ZulrahShrineDeps::class) { deps ->
            for (tile in listOf(ZulrahShrine.arrival) + ZulrahShrine.platform) {
                val flags = deps.collision[tile]
                assertTrue(flags and CollisionFlag.BLOCK_WALK == 0) { "$tile is not walkable." }
            }
        }

    /**
     * Zulrah surfaces in the water, never on the platform: a spot overlapping a walkable tile would
     * let the player stand inside it.
     */
    @Test
    fun GameTestState.`every surfacing spot is open water`() =
        runInjectedGameTest(ZulrahShrineDeps::class) { deps ->
            for ((spot, corner) in ZulrahShrine.spots) {
                for (tile in footprint(corner)) {
                    val flags = deps.collision[tile]
                    assertTrue(flags and CollisionFlag.BLOCK_WALK != 0) {
                        "$spot overlaps the walkable tile $tile."
                    }
                }
            }
        }

    /** `StandardNpcAccess.telejump` silently does nothing into a zone the template did not copy. */
    @Test
    fun GameTestState.`every spot and every platform tile is inside the copied zones`() =
        runBasicGameTest {
            val tiles =
                ZulrahShrine.spots.values.flatMap(::footprint) +
                    ZulrahShrine.platform +
                    ZulrahShrine.arrival
            for (tile in tiles) {
                assertTrue(tile.x / ZONE in ZulrahShrine.copiedZonesX) { "$tile is outside x." }
                assertTrue(tile.z / ZONE in ZulrahShrine.copiedZonesZ) { "$tile is outside z." }
            }
        }

    /**
     * The harness binds an empty `AreaIndex`, so multiway cannot be asserted through `AreaChecker`.
     * What can be checked is the source it is packed from: the platform must sit inside the
     * "Zulrah" polygon, or snakelings and Zulrah could not both attack the player.
     */
    @Test
    fun GameTestState.`the platform is inside the zulrah multiway polygon`() = runBasicGameTest {
        val json = File(MULTIWAY_JSON).readText()
        val entry =
            Regex(
                    """"name"\s*:\s*"Zulrah".*?"vertices"\s*:\s*\[(.*?)]""",
                    RegexOption.DOT_MATCHES_ALL,
                )
                .find(json)
        val vertices =
            Regex(""""x"\s*:\s*(\d+),\s*"z"\s*:\s*(\d+)""")
                .findAll(checkNotNull(entry) { "No Zulrah polygon in $MULTIWAY_JSON" }.value)
                .map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
                .toList()
        val xs = vertices.map { it.first }
        val zs = vertices.map { it.second }
        for (tile in ZulrahShrine.platform) {
            assertTrue(tile.x in xs.min()..xs.max() && tile.z in zs.min()..zs.max()) {
                "$tile is outside the Zulrah multiway area."
            }
        }
    }

    private fun footprint(corner: CoordGrid): List<CoordGrid> =
        (0 until ZulrahShrine.ZULRAH_SIZE).flatMap { dx ->
            (0 until ZulrahShrine.ZULRAH_SIZE).map { dz -> corner.translate(dx, dz) }
        }

    private companion object {
        const val ZONE = 8

        const val MULTIWAY_JSON =
            "api/cache-enricher/src/main/resources/org/rsmod/api/cache/enricher/map/area/" +
                "multiway.json"
    }
}
