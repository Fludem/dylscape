package org.rsmod.content.custom.skillingtasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.skillingtasks.configs.SkillingTaskNpcs
import org.rsmod.content.custom.skillingtasks.map.SkillingTaskNpcSpawns
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Holds the npc and the map together: the spawn names the type the script binds, the tile is one
 * something can stand on, and the op slots the script binds are the ones the cache gives her.
 */
class SkillingTaskSpawnTest {
    @Test
    fun GameTestState.`the spawn names the taskmaster on a walkable tile`() = runBasicGameTest {
        val spawns = parseSpawns()
        assertEquals(1, spawns.size)
        val spawn = spawns.single()
        assertEquals(SkillingTaskNpcs.taskmaster.internalNameValue, spawn.npc)
        assertEquals(CoordGrid(0, 48, 54, 16, 49), spawn.coords)
        assertEquals(CoordGrid(3088, 3505), spawn.coords)

        val flags = collision[spawn.coords.x, spawn.coords.z, spawn.coords.level]
        assertTrue(flags and BLOCKS_STANDING == 0) { "Nothing can stand on ${spawn.coords}" }
    }

    @Test
    fun GameTestState.`the taskmaster carries the ops the script binds and stays put`() =
        runBasicGameTest {
            val npc = cacheTypes.npcs[SkillingTaskNpcs.taskmaster]
            assertEquals("Talk-to", npc.op.getOrNull(0), npc.op.toList().toString())
            assertEquals("Contract", npc.op.getOrNull(2), npc.op.toList().toString())
            assertEquals("Last-tier contract", npc.op.getOrNull(3), npc.op.toList().toString())
            assertEquals("Rewards", npc.op.getOrNull(4), npc.op.toList().toString())
            assertEquals(0, npc.wanderRange, "wanderRange must be pinned by the editor")
            assertEquals("Taskmaster", npc.name)
        }

    private data class Spawn(val npc: String, val coords: CoordGrid)

    private fun parseSpawns(): List<Spawn> {
        val text =
            SkillingTaskNpcSpawns::class
                .java
                .getResourceAsStream(SkillingTaskNpcSpawns.FILE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("`${SkillingTaskNpcSpawns.FILE}` is not on the classpath.")
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

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
