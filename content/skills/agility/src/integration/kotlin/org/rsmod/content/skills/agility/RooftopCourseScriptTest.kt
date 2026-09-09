package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.agility.configs.AgilityObjs
import org.rsmod.content.skills.agility.courses.AlKharid
import org.rsmod.content.skills.agility.courses.Draynor
import org.rsmod.content.skills.agility.scripts.RooftopCourseScript
import org.rsmod.game.loc.BoundLocInfo

class AgilityObjDeps @Inject constructor(val objRepo: ObjRepository)

@Execution(ExecutionMode.SAME_THREAD)
class RooftopCourseScriptTest {
    /**
     * Draynor is open from level 1 and so can never refuse, which makes Al Kharid the lowest course
     * that has a level to be under.
     */
    @Test
    fun GameTestState.`an obstacle refuses below the course level`() =
        runGameTest(RooftopCourseScript::class) {
            val course = AlKharid.course
            val obstacle = course.start
            val coords = obstacle.dest.translateZ(2)
            val placed = placeMapLoc(coords, locTypes[obstacle.loc])
            player.teleport(coords.translateX(-1))
            player.setAgility(level = course.level - 1)

            player.opLoc1(placed)
            advance(ticks = 1)

            // The message buffer clears every tick, so this has to be asserted on the tick the
            // refusal lands, not after the obstacle's own duration.
            assertMessageSent("You need an Agility level of ${course.level} to use this course.")
        }

    @Test
    fun GameTestState.`clearing an obstacle pays its experience and moves the player`() =
        runGameTest(RooftopCourseScript::class) {
            val wall = placeFirstObstacle()
            player.setAgility(level = Draynor.course.level)
            val startXp = player.statMap.getXP(stats.agility)

            player.opLoc1(wall)
            advance(ticks = Draynor.course.start.ticks + 1)

            assertTrue(player.statMap.getXP(stats.agility) > startXp) {
                "Agility xp did not advance after clearing the rough wall."
            }
            assertEquals(Draynor.course.start.dest, player.coords) {
                "The rough wall did not put the player on the roof."
            }
        }

    /**
     * The lap bonus is the whole reason [LapProgress] exists, so it gets driven end to end rather
     * than unit-tested through the tracker.
     *
     * Each obstacle is placed and clicked in turn. The player is teleported to each obstacle first
     * because the test world has no map locs and therefore no roof to walk along between them -
     * what is being asserted is the *chain*, not the pathing.
     */
    @Test
    fun GameTestState.`a full lap in order pays the completion bonus`() =
        runGameTest(RooftopCourseScript::class) {
            val course = Draynor.course
            player.setAgility(level = 99)
            val startXp = player.statMap.getXP(stats.agility)

            for (obstacle in course.obstacles) {
                val placed = placeMapLoc(obstacle.dest, locTypes[obstacle.loc])
                player.teleport(obstacle.dest)
                player.opLoc1(placed)
                advance(ticks = obstacle.ticks + 1)
            }

            val gained = player.statMap.getXP(stats.agility) - startXp
            // Obstacles alone would pay `sumOf { xp }`; the bonus is what takes it to `lapTotalXp`.
            assertEquals(course.lapTotalXp.toInt(), gained.toInt()) {
                "A clean lap paid $gained, expected ${course.lapTotalXp} including the bonus."
            }
        }

    @Test
    fun GameTestState.`starting mid-course pays no lap bonus`() =
        runGameTest(RooftopCourseScript::class) {
            val course = Draynor.course
            player.setAgility(level = 99)
            val startXp = player.statMap.getXP(stats.agility)

            // Skip the rough wall: every obstacle still pays, but the lap never counts.
            for (obstacle in course.obstacles.drop(1)) {
                val placed = placeMapLoc(obstacle.dest, locTypes[obstacle.loc])
                player.teleport(obstacle.dest)
                player.opLoc1(placed)
                advance(ticks = obstacle.ticks + 1)
            }

            val gained = player.statMap.getXP(stats.agility) - startXp
            val obstaclesOnly = course.obstacles.drop(1).sumOf { it.xp }
            assertEquals(obstaclesOnly.toInt(), gained.toInt()) {
                "An out-of-order lap paid $gained; the completion bonus should not apply."
            }
        }

    /**
     * The marks are spawned on the first obstacle of the lap, so one click is a whole lap's worth
     * of them. They are also spawned one per tile: a second mark on an occupied tile would merge
     * into a stack of two and the roof would show a single pickup instead of five.
     */
    @Test
    fun GameTestState.`clearing the first obstacle spawns a lap of marks of grace`() =
        runInjectedGameTest(AgilityObjDeps::class, null, RooftopCourseScript::class) { deps ->
            val course = Draynor.course
            val wall = placeFirstObstacle()
            player.setAgility(level = 99)

            player.opLoc1(wall)
            advance(ticks = course.start.ticks + 1)

            val perTile =
                course.markTiles.map { tile ->
                    deps.objRepo
                        .findAll(tile)
                        .filter { it.type == AgilityObjs.mark_of_grace.id }
                        .sumOf { it.count }
                }
            assertTrue(perTile.sum() >= RooftopCourse.MARKS_PER_LAP) {
                "A lap spawned ${perTile.sum()} marks, expected at least " +
                    "${RooftopCourse.MARKS_PER_LAP}."
            }
            assertTrue(perTile.all { it <= 1 }) {
                "Marks stacked on a tile instead of taking one each: $perTile"
            }
        }

    private fun GameTestScope.placeFirstObstacle(): BoundLocInfo {
        val obstacle = Draynor.course.start
        val coords = obstacle.dest.translateZ(2)
        val placed = placeMapLoc(coords, locTypes[obstacle.loc])
        player.teleport(coords.translateX(-1))
        return placed
    }

    /**
     * Sets the level and gives the player an observer id.
     *
     * A Mark of grace is spawned *private to the player*, and `Obj.fromOwner` hard-errors without
     * an `observerUUID`. Production always has one - `CharacterAccountApplier` sets it on login -
     * but a bare test player does not, so the mark roll would take the whole lap down with it.
     */
    private fun org.rsmod.game.entity.Player.setAgility(level: Int) {
        observerUUID = 1
        statMap.setBaseLevel(stats.agility, level.toByte())
        statMap.setCurrentLevel(stats.agility, level.toByte())
    }
}
