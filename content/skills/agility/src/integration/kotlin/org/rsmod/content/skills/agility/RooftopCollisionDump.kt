package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.agility.courses.RooftopCourses
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Prints the walkable ground around every authored obstacle, from the **live collision map**.
 *
 * This is the tool for authoring a new course's destinations, and it exists because the standalone
 * terrain dumper in `tools/agility` cannot answer the question on its own. That dumper reads map
 * tile settings, so it sees the roof but not the scenery standing on it - Varrock's ruins are
 * unblocked terrain covered in blocking locs, and a destination read off terrain alone landed
 * inside them.
 *
 * Gradle does not print test stdout: read `system-out` out of
 * `content/skills/agility/build/test-results/integration/TEST-*.xml`. Run with `--tests
 * '*RooftopCollisionDump*' --rerun-tasks`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class RooftopCollisionDump {
    @Test
    fun GameTestState.`dump walkable ground around every obstacle`() =
        runInjectedGameTest(AgilityCollisionDeps::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            for (course in RooftopCourses.all) {
                println("=== ${course.name} ===")
                for (obstacle in course.obstacles) {
                    val dest = obstacle.dest
                    println("--- loc ${obstacle.loc.id} dest ${dest.toConventionalString()}")
                    for (z in dest.z + RADIUS downTo dest.z - RADIUS) {
                        val row = buildString {
                            for (x in dest.x - RADIUS..dest.x + RADIUS) {
                                val flags = deps.collision[x, z, dest.level]
                                append(
                                    when {
                                        x == dest.x && z == dest.z -> 'X'
                                        flags and blocked != 0 -> '#'
                                        else -> '.'
                                    }
                                )
                            }
                        }
                        println("$z $row")
                    }
                }
            }
        }

    private companion object {
        const val RADIUS = 6
    }
}
