package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.agility.courses.AgilityCourses
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Prints the walkable ground of every course, per level, from the **live collision map**.
 *
 * This is the tool for authoring a course's destinations, and it exists because the standalone
 * terrain dumper in `tools/agility` cannot answer the question on its own. That dumper reads map
 * tile settings, so it sees the roof but not the scenery standing on it - Varrock's ruins are
 * unblocked terrain covered in blocking locs, and a destination read off terrain alone landed
 * inside them.
 *
 * Each course prints one map per level it uses, covering the bounding box of its destinations and
 * mark tiles plus a margin. `X` is a destination, `M` a mark tile, `#` blocked, `.` walkable.
 *
 * Gradle does not print test stdout: read `system-out` out of
 * `content/skills/agility/build/test-results/integration/TEST-*.xml`. Run with `--tests
 * '*CourseCollisionDump*' --rerun-tasks`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CourseCollisionDump {
    @Test
    fun GameTestState.`dump walkable ground for every course`() =
        runInjectedGameTest(AgilityCollisionDeps::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            for (course in AgilityCourses.all) {
                val dests = (course.obstacles + course.entrances).flatMap { it.dests }.toSet()
                val marks = course.markTiles.toSet()
                val tiles = dests + marks
                val levels = tiles.map { it.level }.toSortedSet()
                val minX = tiles.minOf { it.x } - MARGIN
                val maxX = tiles.maxOf { it.x } + MARGIN
                val minZ = tiles.minOf { it.z } - MARGIN
                val maxZ = tiles.maxOf { it.z } + MARGIN
                for (level in levels) {
                    println("=== ${course.name} level $level  x=$minX..$maxX z=$maxZ..$minZ ===")
                    println("     " + (minX..maxX).joinToString("") { (it / 10 % 10).toString() })
                    println("     " + (minX..maxX).joinToString("") { (it % 10).toString() })
                    for (z in maxZ downTo minZ) {
                        val row = buildString {
                            for (x in minX..maxX) {
                                val coord = CoordGrid(x, z, level)
                                val flags = deps.collision[x, z, level]
                                append(
                                    when {
                                        coord in dests -> 'X'
                                        coord in marks -> 'M'
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
        const val MARGIN = 4
    }
}
