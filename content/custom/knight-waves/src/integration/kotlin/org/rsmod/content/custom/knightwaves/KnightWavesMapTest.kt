package org.rsmod.content.custom.knightwaves

import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.knightwaves.configs.KnightWavesArena
import org.rsmod.content.custom.knightwaves.map.KnightWavesNpcSpawns
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * The half of this module that lives on the map rather than in the cache.
 *
 * A spawn file is not a cache type, so nothing else in the suite would notice a Squire authored
 * into a wall, a typo'd npc name, or an arena tile that turns out to be scenery. These read the
 * packed resource and the real collision map back.
 */
class KnightWavesMapTest {
    @Test
    fun GameTestState.`the squire spawn names an npc the cache knows`() = runBasicGameTest {
        val known = cacheTypes.npcs.values.mapTo(hashSetOf()) { it.internalName }
        val unknown = parseSpawns().filter { it.npc !in known }
        assertTrue(unknown.isEmpty()) {
            "`packCache` hard-errors on these; they are typos in the spawn file: $unknown"
        }
    }

    @Test
    fun GameTestState.`the squire stands somewhere a player can reach him`() = runBasicGameTest {
        val spawns = parseSpawns()
        assertTrue(spawns.isNotEmpty()) { "The Squire is not authored anywhere - no entrance." }
        val blocked = spawns.filter { blocksStanding(it.coords) }
        assertTrue(blocked.isEmpty()) {
            "The Squire is authored onto a tile nothing can stand on: $blocked"
        }
    }

    /**
     * The arena is a copy of Camelot's top floor, so these two tiles have to be open floor in the
     * source map or the trial starts with the player and the knight embedded in the scenery.
     */
    @Test
    fun GameTestState.`the arena tiles are open floor`() = runBasicGameTest {
        assertTrue(!blocksStanding(KnightWavesArena.entrance)) {
            "The arena entrance ${KnightWavesArena.entrance} is not standable."
        }
        assertTrue(!blocksStanding(KnightWavesArena.knightSpawn)) {
            "The knight tile ${KnightWavesArena.knightSpawn} is not standable."
        }
    }

    /** A knight spawning out of hunt range would leave the player waiting on a fight. */
    @Test
    fun GameTestState.`the knight spawns within charging distance of the entrance`() =
        runBasicGameTest {
            val entrance = KnightWavesArena.entrance
            val knight = KnightWavesArena.knightSpawn
            val distance = maxOf(abs(entrance.x - knight.x), abs(entrance.z - knight.z))
            assertTrue(distance in 1..MAX_HUNT_RANGE) {
                "The knight appears $distance tiles from the player; hunt range is $MAX_HUNT_RANGE."
            }
            assertTrue(entrance.level == knight.level) {
                "The two arena tiles are on other floors."
            }
        }

    @Test
    fun GameTestState.`the arena template covers both arena tiles`() = runGameTest {
        val region = createRegion(KnightWavesArena.template)
        // `normal[...]` throws when the coordinate is not part of a copied zone, which is the
        // failure this guards: a template block that misses the tiles the trial actually uses.
        assertNotNull(region.normal[KnightWavesArena.entrance])
        assertNotNull(region.normal[KnightWavesArena.knightSpawn])
    }

    private fun GameTestState.blocksStanding(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKS_STANDING != 0

    private data class Spawn(val npc: String, val coords: CoordGrid)

    /** Parsed straight out of the packed resource, so the test and the packer cannot drift. */
    private fun parseSpawns(): List<Spawn> {
        val text =
            KnightWavesNpcSpawns::class
                .java
                .getResourceAsStream("npcs.toml")
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("`npcs.toml` is not on the classpath next to KnightWavesNpcSpawns.")
        return SPAWN_PATTERN.findAll(text)
            .map { match ->
                val (npc, level, msqX, msqZ, localX, localZ) = match.destructured
                Spawn(
                    npc,
                    CoordGrid(
                        level.toInt(),
                        msqX.toInt(),
                        msqZ.toInt(),
                        localX.toInt(),
                        localZ.toInt(),
                    ),
                )
            }
            .toList()
    }

    private companion object {
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        /** Matches the `huntRange` given to the knights in `KnightWavesNpcEditor`. */
        const val MAX_HUNT_RANGE = 10

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
