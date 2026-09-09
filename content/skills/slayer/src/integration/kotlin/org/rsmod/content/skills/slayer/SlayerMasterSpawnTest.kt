package org.rsmod.content.skills.slayer

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.slayer.configs.SlayerNpcs
import org.rsmod.content.skills.slayer.map.SlayerNpcSpawns
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Holds the two halves of a master together: the npc type has to be one that can be clicked, and it
 * has to stand somewhere reachable.
 *
 * Both halves were broken before this module existed. Turael, Mazchna and Duradel were on the map
 * as `wgs_heroes_*` quest actors carrying no ops at all, so they were unclickable; Nieve was not on
 * the map in any form. A test that only checked "is a slayer master spawned" would have passed on
 * the statues, which is why the op assertion is here too.
 */
class SlayerMasterSpawnTest {
    @Test
    fun GameTestState.`every master spawn names an npc the cache knows`() = runBasicGameTest {
        val known = cacheTypes.npcs.values.mapTo(hashSetOf()) { it.internalName }
        val unknown = parseSpawns().filter { it.npc !in known }
        assertTrue(unknown.isEmpty()) {
            "`packCache` hard-errors on these; they are typos in the spawn file: $unknown"
        }
    }

    @Test
    fun GameTestState.`every master stands on a walkable tile`() = runBasicGameTest {
        val blocked =
            parseSpawns().filter { spawn ->
                collision[spawn.coords.x, spawn.coords.z, spawn.coords.level] and BLOCKS_STANDING !=
                    0
            }
        assertTrue(blocked.isEmpty()) {
            "These masters are authored onto tiles nothing can stand on:\n" +
                blocked.joinToString("\n") { "  ${it.npc} at ${it.coords}" }
        }
    }

    /**
     * The one that would have caught the statues. A master with no `Talk-to` and no `Assignment` op
     * cannot hand out a task no matter how correct everything else is.
     */
    @Test
    fun GameTestState.`every master carries the ops this module binds`() = runBasicGameTest {
        val missing =
            SlayerNpcs.byMasterId.values.mapNotNull { master ->
                val type = cacheTypes.npcs[master]
                val ops = type.op
                val talk = ops.getOrNull(TALK_OP - 1)
                val assignment = ops.getOrNull(ASSIGNMENT_OP - 1)
                when {
                    !talk.equals("Talk-to", ignoreCase = true) ->
                        "${type.internalName}: op$TALK_OP is '$talk', expected 'Talk-to'"
                    !assignment.equals("Assignment", ignoreCase = true) ->
                        "${type.internalName}: op$ASSIGNMENT_OP is '$assignment', expected " +
                            "'Assignment'"
                    else -> null
                }
            }
        assertTrue(missing.isEmpty()) {
            "These masters do not carry the ops `SlayerMasterScript` binds:\n" +
                missing.joinToString("\n") { "  $it" }
        }
    }

    @Test
    fun `the masters this module places are the ones expected`() {
        val spawnedHere = parseSpawns().map { it.npc }.toSet()
        val expectedHere =
            setOf(
                "slayer_master_1_tureal",
                "slayer_master_2_mazchna",
                "slayer_master_5_duradel",
                "slayer_master_nieve",
                // Chaeldar appears only as an Edgeville hub copy; her Zanaris spawn is in
                // `world-spawns`.
                "slayer_master_4",
            )
        assertTrue(spawnedHere == expectedHere) {
            "Vannaka is spawned by `world-spawns` with his real type and is deliberately absent " +
                "here; everyone else must be in this file. Got: $spawnedHere"
        }
    }

    /**
     * Two npcs authored onto one tile leaves one standing inside the other. Worth checking now that
     * three masters are placed shoulder to shoulder in the Edgeville hub.
     */
    @Test
    fun `no two masters are authored onto the same tile`() {
        val stacked = parseSpawns().groupBy { it.coords }.filter { it.value.size > 1 }
        assertTrue(stacked.isEmpty()) {
            "Two masters on one tile:\n" +
                stacked.entries.joinToString("\n") { (coords, at) ->
                    "  $coords <- ${at.map(Spawn::npc)}"
                }
        }
    }

    private data class Spawn(val npc: String, val coords: CoordGrid)

    private fun parseSpawns(): List<Spawn> {
        val text =
            SlayerNpcSpawns::class
                .java
                .getResourceAsStream(SlayerNpcSpawns.FILE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("`${SlayerNpcSpawns.FILE}` is not on the classpath.")
        val spawns =
            SPAWN_PATTERN.findAll(text)
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
        assertTrue(spawns.isNotEmpty()) { "Parsed no spawns out of `${SlayerNpcSpawns.FILE}`." }
        return spawns
    }

    private companion object {
        const val TALK_OP = 1
        const val ASSIGNMENT_OP = 3

        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
