package org.rsmod.content.custom.vorkath

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.vorkath.configs.vorkath_npcs
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class VorkathDumpDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * What the cache already says about Vorkath and Torfinn, and what the ground looks like.
 *
 * The editor sets only what the cache leaves out, and that is only safe if "what the cache leaves
 * out" is read rather than assumed. The coordinate table is sourced from the map, and the map's
 * tile flags alone can mislead (a pier reads as water), so the real collision map is printed too.
 *
 * Gradle does not print test stdout: read `system-out` out of
 * `content/custom/vorkath/build/test-results/integration/TEST-*.xml`. Run with `--tests
 * '*VorkathDump*' --rerun-tasks`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class VorkathDump {
    @Test
    fun GameTestState.`dump what the cache already knows`() = runBasicGameTest {
        val refs =
            listOf(
                vorkath_npcs.sleeping,
                vorkath_npcs.awake,
                vorkath_npcs.torfinn_rellekka,
                vorkath_npcs.torfinn_ungael,
            )
        for (ref in refs) {
            val type = cacheTypes.npcs.getValue(ref.id)
            val ops = type.op.withIndex().filter { it.value != null }
            println(
                "NPC ${type.id} sym=${type.internalName} name='${type.name}' " +
                    "size=${type.size} vislevel=${type.vislevel} " +
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
    fun GameTestState.`dump the crater and the two shores`() =
        runInjectedGameTest(VorkathDumpDeps::class) { deps ->
            dump(deps.collision, "Ungael crater and shore", 2252..2292, 4028..4082)
            dump(deps.collision, "Rellekka pier", 2628..2660, 3686..3704)
        }

    private fun dump(collision: CollisionFlagMap, title: String, xs: IntRange, zs: IntRange) {
        val blocked =
            CollisionFlag.BLOCK_WALK or
                CollisionFlag.BLOCK_PLAYERS or
                CollisionFlag.LOC or
                CollisionFlag.GROUND_DECOR
        println("=== $title, level 0: x ${xs.first}..${xs.last} ===")
        for (z in zs.reversed()) {
            val row = buildString {
                for (x in xs) {
                    val flags = collision[x, z, 0]
                    append(if (flags and blocked != 0) '#' else '.')
                }
            }
            println("$z $row")
        }
    }
}
