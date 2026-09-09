package org.rsmod.content.custom.dagannothkings

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.dagannothkings.configs.dk_npcs
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class DagannothDumpDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * What the cache already says about the three kings, and what the lair floor looks like.
 *
 * The editor is deliberately thin -- it sets only what the cache leaves out -- and that is only
 * safe if "what the cache leaves out" is read rather than assumed. Every combat level, defence
 * bonus and op below is a vanilla or RSMod extension opcode that may or may not be present, and
 * re-declaring one that is already there just creates a second place to be wrong, since type edits
 * are additive once packed.
 *
 * Gradle does not print test stdout: read `system-out` out of
 * `content/custom/dagannoth-kings/build/test-results/integration/TEST-*.xml`. Run with `--tests
 * '*DagannothKingsDump*' --rerun-tasks`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsDump {
    @Test
    fun GameTestState.`dump what the cache already knows about each king`() = runBasicGameTest {
        for ((king, ref) in dk_npcs.byKing) {
            val type = cacheTypes.npcs.getValue(ref.id)
            val ops = type.op.withIndex().filter { it.value != null }
            println(
                "NPC ${type.id} sym=${type.internalName} name='${type.name}' " +
                    "king=$king size=${type.size} vislevel=${type.vislevel} " +
                    "hp=${type.hitpoints} att=${type.attack} str=${type.strength} " +
                    "def=${type.defence} rng=${type.ranged} magic=${type.magic} " +
                    "wander=${type.wanderRange} maxRange=${type.maxRange} " +
                    "attackRange=${type.attackRange} huntRange=${type.huntRange} " +
                    "huntMode=${type.huntMode} defaultMode=${type.defaultMode} " +
                    "respawn=${type.respawnRate} moveRestrict=${type.moveRestrict} " +
                    "readyAnim=${type.readyAnim} walkAnim=${type.walkAnim} " +
                    "ops=${ops.map { "op${it.index + 1}='${it.value}'" }}"
            )
            val params = type.paramMap
            if (params == null) {
                println("  params=NONE")
            } else {
                for ((key, value) in params.primitiveMap) {
                    println(
                        "  param $key (${cacheTypes.params[key]?.internalName ?: "?"}) = $value"
                    )
                }
            }
        }
    }

    @Test
    fun GameTestState.`dump the lair floor and both arrival tiles`() =
        runInjectedGameTest(DagannothDumpDeps::class) { deps ->
            val blocked =
                CollisionFlag.BLOCK_WALK or
                    CollisionFlag.BLOCK_PLAYERS or
                    CollisionFlag.LOC or
                    CollisionFlag.GROUND_DECOR
            for (level in 0..3) {
                println("=== lair, level $level ===")
                for (z in LAIR_MAX_Z downTo LAIR_MIN_Z) {
                    val row = buildString {
                        for (x in LAIR_MIN_X..LAIR_MAX_X) {
                            val flags = deps.collision[x, z, level]
                            append(if (flags and blocked != 0) '#' else '.')
                        }
                    }
                    println("$z $row")
                }
            }
            println("=== antechamber, level 0 ===")
            for (z in ANTE_Z + RADIUS downTo ANTE_Z - RADIUS) {
                val row = buildString {
                    for (x in ANTE_X - RADIUS..ANTE_X + RADIUS) {
                        val flags = deps.collision[x, z, 0]
                        append(if (flags and blocked != 0) '#' else '.')
                    }
                }
                println("$z $row")
            }
            for (candidate in CANDIDATES) {
                val flags = deps.collision[candidate.x, candidate.z, candidate.level]
                println(
                    "TILE ${candidate.toConventionalString()} flags=$flags blocked=${flags and blocked != 0}"
                )
            }
        }

    private companion object {
        const val LAIR_MIN_X = 2895
        const val LAIR_MAX_X = 2932
        const val LAIR_MIN_Z = 4432
        const val LAIR_MAX_Z = 4466
        const val ANTE_X = 1911
        const val ANTE_Z = 4367
        const val RADIUS = 8

        val CANDIDATES =
            listOf(
                CoordGrid(2906, 4448, 0), // Supreme's spawn
                CoordGrid(2911, 4451, 0), // Prime's spawn
                CoordGrid(2913, 4445, 0), // Rex's spawn
                CoordGrid(2899, 4449, 0), // the up-ladder itself
                CoordGrid(2900, 4449, 0), // proposed lair arrival
                CoordGrid(1911, 4367, 0), // the down-ladder itself
                CoordGrid(1912, 4367, 0), // proposed antechamber arrival
            )
    }
}
