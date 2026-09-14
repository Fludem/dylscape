package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.agility.courses.AgilityCourses
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
                AgilityCourses.all.flatMap { course ->
                    (course.obstacles + course.entrances)
                        .flatMap { it.locs }
                        .mapNotNull { loc ->
                            val type = cacheTypes.locs[loc.id]
                            assertNotNull(type) { "Loc ${loc.id} is not in the cache." }
                            val op1 = type!!.op.getOrNull(0)
                            if (op1.isNullOrBlank()) "${course.name}: ${type.internalName}"
                            else null
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
                AgilityCourses.all.flatMap { course ->
                    (course.obstacles + course.entrances).flatMap { obstacle ->
                        obstacle.dests
                            .filter { deps.collision.isBlocked(it) }
                            .map {
                                "${course.name} -> ${obstacle.loc.id} @ ${it.toConventionalString()}"
                            }
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
                AgilityCourses.all.flatMap { course ->
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
                AgilityCourses.all.flatMap { course ->
                    (course.obstacles + course.entrances).flatMap { obstacle ->
                        obstacle.dests
                            .filter { d ->
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
                                "${course.name} -> ${obstacle.loc.id} @ ${it.toConventionalString()}"
                            }
                    }
                }
            assertTrue(stranded.isEmpty()) { "Destinations with no way off them: $stranded" }
        }

    @Test
    fun GameTestState.`courses are internally consistent`() = runBasicGameTest {
        for (course in AgilityCourses.all) {
            assertTrue(course.obstacles.size >= 2) { "${course.name} has too few obstacles." }
            // A loc on two courses would make `CourseRegistry` ambiguous; it already throws at
            // class-load, but assert it so the failure names the course rather than a lambda.
            val ids = (course.obstacles + course.entrances).flatMap { it.locs }.map { it.id }
            assertEquals(ids.size, ids.toSet().size) { "${course.name} repeats an obstacle." }
        }
        val everyLoc =
            AgilityCourses.all
                .flatMap { it.obstacles + it.entrances }
                .flatMap { it.locs }
                .map { it.id }
        assertEquals(everyLoc.size, everyLoc.toSet().size) { "An obstacle is on two courses." }
        // Every loc a course claims must be bound, aliases and entrances included.
        assertEquals(everyLoc.toSet(), CourseRegistry.locs.map { it.id }.toSet())
    }

    @Test
    fun `every course pays the experience per lap the live game pays`() {
        // Transcribed from the per-obstacle tables on the OSRS wiki, which are the only published
        // source for these. They are worth pinning: the split between obstacle xp and the lap
        // bonus is a judgement call (see `AgilityCourse.lapXp`), but the *total* is not, and a
        // course that quietly drifts off it is the kind of bug nobody notices for months.
        val published =
            mapOf(
                "Gnome Stronghold" to 110.5,
                "Draynor Village" to 120.0,
                "Al Kharid" to 216.0,
                // 269.7 in the table; the wiki rounds it to 270 in prose.
                "Varrock" to 269.7,
                "Barbarian Outpost" to 153.3,
                "Canifis" to 240.0,
                "Falador" to 586.0,
                "Wilderness" to 571.4,
                "Seers' Village" to 570.0,
                "Pollnivneach" to 890.0,
                "Rellekka" to 780.0,
                "Ardougne" to 889.0,
            )
        for (course in AgilityCourses.all) {
            val expected = published[course.name]
            assertNotNull(expected) { "No published lap total recorded for ${course.name}." }
            assertEquals(expected!!, course.lapTotalXp, 0.001, "${course.name} lap experience")
        }
        // Barbarian Outpost is the only course that pays Strength, and only on the lap.
        val barbarian = AgilityCourses.all.single { it.name == "Barbarian Outpost" }
        assertEquals(41.3, barbarian.lapStrengthXp, 0.001)
        assertTrue(AgilityCourses.all.filter { it !== barbarian }.all { it.lapStrengthXp == 0.0 })
    }

    private fun CollisionFlagMap.isBlocked(coords: CoordGrid): Boolean {
        val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
        return this[coords.x, coords.z, coords.level] and blocked != 0
    }
}
