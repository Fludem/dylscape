package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.agility.courses.RooftopCourses
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class AgilityCollisionDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * Guards the two things a hand-authored coordinate table cannot check for itself.
 *
 * Neither `exactMove` nor `telejump` fails loudly on a bad destination - they put the player inside
 * a wall or on a roof tile that does not exist, and nothing reports it. Since every `GameTestScope`
 * is handed a copy of the *real* game collision map, the whole table can be swept here instead.
 */
@Execution(ExecutionMode.SAME_THREAD)
class AgilityCourseTest {
    @Test
    fun GameTestState.`every obstacle loc resolves in the cache and carries op1`() =
        runBasicGameTest {
            val missingOp =
                RooftopCourses.all.flatMap { course ->
                    course.obstacles.mapNotNull { obstacle ->
                        val type = cacheTypes.locs[obstacle.loc.id]
                        assertNotNull(type) { "Loc ${obstacle.loc.id} is not in the cache." }
                        val op1 = type!!.op.getOrNull(0)
                        if (op1.isNullOrBlank()) "${course.name}: ${type.internalName}" else null
                    }
                }
            // An op with no cache text is never dispatched -- `OpLocHandler` drops it silently, so
            // the obstacle would simply do nothing in game with no error anywhere.
            assertTrue(missingOp.isEmpty()) { "Obstacles with no op1 text: $missingOp" }
        }

    @Test
    fun GameTestState.`every destination lands on a tile a player can stand on`() =
        runInjectedGameTest(AgilityCollisionDeps::class) { deps ->
            val failures =
                RooftopCourses.all.flatMap { course ->
                    course.obstacles
                        .filter { deps.collision.isBlocked(it.dest) }
                        .map {
                            "${course.name} -> ${it.loc.id} @ ${it.dest.toConventionalString()}"
                        }
                }
            assertTrue(failures.isEmpty()) {
                "Obstacle destinations on unwalkable tiles: $failures"
            }
        }

    @Test
    fun GameTestState.`every mark of grace tile is reachable ground`() =
        runInjectedGameTest(AgilityCollisionDeps::class) { deps ->
            val failures =
                RooftopCourses.all.flatMap { course ->
                    course.markTiles
                        .filter { deps.collision.isBlocked(it) }
                        .map { "${course.name} @ ${it.toConventionalString()}" }
                }
            assertTrue(failures.isEmpty()) { "Mark tiles on unwalkable tiles: $failures" }
        }

    /**
     * The failure mode that actually strands a player.
     *
     * A destination can be perfectly walkable and still be a one-tile island in the middle of a
     * roof - `exactMove` and `telejump` will happily put someone there, and then there is no way
     * off it except logging out. Requiring at least one walkable orthogonal neighbour is a cheap
     * guard against authoring a landing on the wrong side of a gap.
     */
    @Test
    fun GameTestState.`no destination is an isolated tile`() =
        runInjectedGameTest(AgilityCollisionDeps::class) { deps ->
            val stranded =
                RooftopCourses.all.flatMap { course ->
                    course.obstacles
                        .filter { obstacle ->
                            val d = obstacle.dest
                            val neighbours =
                                listOf(
                                    d.translateX(1),
                                    d.translateX(-1),
                                    d.translateZ(1),
                                    d.translateZ(-1),
                                )
                            neighbours.none { !deps.collision.isBlocked(it) }
                        }
                        .map {
                            "${course.name} -> ${it.loc.id} @ ${it.dest.toConventionalString()}"
                        }
                }
            assertTrue(stranded.isEmpty()) { "Destinations with no way off them: $stranded" }
        }

    @Test
    fun GameTestState.`courses are internally consistent`() = runBasicGameTest {
        for (course in RooftopCourses.all) {
            assertTrue(course.obstacles.size >= 2) { "${course.name} has too few obstacles." }
            // A loc on two courses would make `CourseRegistry` ambiguous; it already throws at
            // class-load, but assert it so the failure names the course rather than a lambda.
            val ids = course.obstacles.map { it.loc.id }
            assertEquals(ids.size, ids.toSet().size) { "${course.name} repeats an obstacle." }
        }
        val everyLoc = RooftopCourses.all.flatMap { it.obstacles }.map { it.loc.id }
        assertEquals(everyLoc.size, everyLoc.toSet().size) { "An obstacle is on two courses." }
    }

    private fun CollisionFlagMap.isBlocked(coords: CoordGrid): Boolean {
        val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
        return this[coords.x, coords.z, coords.level] and blocked != 0
    }
}
