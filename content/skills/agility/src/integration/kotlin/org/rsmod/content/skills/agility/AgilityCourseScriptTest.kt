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
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilityObjs
import org.rsmod.content.skills.agility.courses.AlKharid
import org.rsmod.content.skills.agility.courses.BarbarianOutpost
import org.rsmod.content.skills.agility.courses.Draynor
import org.rsmod.content.skills.agility.courses.GnomeStronghold
import org.rsmod.content.skills.agility.courses.Wilderness
import org.rsmod.content.skills.agility.scripts.AgilityCourseScript
import org.rsmod.content.skills.agility.scripts.WildernessGateScript
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class AgilityObjDeps @Inject constructor(val objRepo: ObjRepository)

@Execution(ExecutionMode.SAME_THREAD)
class AgilityCourseScriptTest {
    /**
     * Draynor is open from level 1 and so can never refuse, which makes Al Kharid the lowest course
     * that has a level to be under.
     */
    @Test
    fun GameTestState.`an obstacle refuses below the course level`() =
        runGameTest(AgilityCourseScript::class) {
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
        runGameTest(AgilityCourseScript::class) {
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
        runGameTest(AgilityCourseScript::class) {
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
        runGameTest(AgilityCourseScript::class) {
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
        runInjectedGameTest(AgilityObjDeps::class, null, AgilityCourseScript::class) { deps ->
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
            assertTrue(perTile.sum() >= AgilityCourse.MARKS_PER_LAP) {
                "A lap spawned ${perTile.sum()} marks, expected at least " +
                    "${AgilityCourse.MARKS_PER_LAP}."
            }
            assertTrue(perTile.all { it <= 1 }) {
                "Marks stacked on a tile instead of taking one each: $perTile"
            }
        }

    /**
     * The Wilderness stepping stones are one loc and one click since the 2024 rework, so a single
     * op has to carry the player across every stone and pay once.
     */
    @Test
    fun GameTestState.`a hop crosses every stone on one click and pays once`() =
        runGameTest(AgilityCourseScript::class) {
            val course = Wilderness.course
            val stones = course.obstacles.single { it.movement is Movement.Hop }
            val hop = stones.movement as Movement.Hop
            val start = hop.stops.first().translateX(1)
            val placed = placeMapLoc(hop.stops.first(), locTypes[stones.loc])
            player.teleport(start)
            player.setAgility(level = 99)
            val startXp = player.statMap.getXP(stats.agility)

            player.opLoc1(placed)
            advance(ticks = stones.ticks + 1)

            assertEquals(hop.stops.last(), player.coords) {
                "The hop did not end on the last stone."
            }
            val gained = player.statMap.getXP(stats.agility) - startXp
            assertEquals(stones.xp.toInt(), gained.toInt()) { "The stones paid $gained." }
        }

    /** A pipe entered from either end comes out at the other end of that same pipe. */
    @Test
    fun GameTestState.`a two-ended obstacle exits at the far end from either side`() =
        runGameTest(AgilityCourseScript::class) {
            val course = GnomeStronghold.course
            val pipes = course.obstacles.single { it.movement is Movement.Through }
            val through = pipes.movement as Movement.Through
            val (south, north) = through.ends.last()
            player.setAgility(level = 99)

            // Clicking the alias loc from the south end of the second pipe.
            val southEnd = placeMapLoc(south.translateZ(1), locTypes[pipes.aliases.single()])
            player.teleport(south)
            player.opLoc1(southEnd)
            advance(ticks = pipes.ticks + 1)
            assertEquals(north, player.coords) { "Squeezing north did not exit at the north end." }

            val northEnd = placeMapLoc(north.translateZ(-1), locTypes[pipes.aliases.single()])
            player.opLoc1(northEnd)
            advance(ticks = pipes.ticks + 1)
            assertEquals(south, player.coords) { "Squeezing back did not exit at the south end." }
        }

    /**
     * Barbarian Outpost's three walls are one loc with `repeats = 3`, and its ladder pays nothing.
     * A lap that climbs only two walls must not pay the bonus; all three must, plus Strength.
     */
    @Test
    fun GameTestState.`a repeated obstacle needs every repeat before the lap completes`() =
        runGameTest(AgilityCourseScript::class) {
            val course = BarbarianOutpost.course
            val walls = course.finish
            val through = walls.movement as Movement.Through
            player.setAgility(level = 99)
            val startAgility = player.statMap.getXP(stats.agility)
            val startStrength = player.statMap.getXP(stats.strength)

            for (obstacle in course.obstacles.dropLast(1)) {
                val placed = placeMapLoc(obstacle.dest, locTypes[obstacle.loc])
                player.teleport(obstacle.dest)
                player.opLoc1(placed)
                advance(ticks = obstacle.ticks + 1)
            }
            val beforeWalls = player.statMap.getXP(stats.agility) - startAgility
            val chainBeforeWalls = course.obstacles.dropLast(1).sumOf { it.xp }
            assertEquals(chainBeforeWalls.toInt(), beforeWalls.toInt()) {
                "The chain up to the walls paid $beforeWalls; the ladder should pay nothing."
            }

            for ((index, passage) in through.ends.withIndex()) {
                val (west, east) = passage
                val placed = placeMapLoc(west.translateX(1), locTypes[walls.loc])
                player.teleport(west)
                player.opLoc1(placed)
                advance(ticks = walls.ticks + 1)
                assertEquals(east, player.coords) { "Wall $index did not put the player east." }
                if (index < through.ends.lastIndex) {
                    val soFar = player.statMap.getXP(stats.agility) - startAgility
                    assertTrue(soFar < course.lapTotalXp - 1) {
                        "The lap bonus paid after only ${index + 1} walls."
                    }
                }
            }

            val gained = player.statMap.getXP(stats.agility) - startAgility
            assertEquals(course.lapTotalXp.toInt(), gained.toInt()) {
                "A full Barbarian lap paid $gained, expected ${course.lapTotalXp}."
            }
            val strength = player.statMap.getXP(stats.strength) - startStrength
            assertEquals(course.lapStrengthXp.toInt(), strength.toInt()) {
                "The lap paid $strength Strength, expected ${course.lapStrengthXp}."
            }
        }

    /** The entrance pipe is level-gated like the course but never touches the lap. */
    @Test
    fun GameTestState.`an entrance obstacle neither starts nor breaks a lap`() =
        runGameTest(AgilityCourseScript::class) {
            val course = BarbarianOutpost.course
            val pipe = course.entrances.single()
            val (south, north) = (pipe.movement as Movement.Through).ends.single()
            player.setAgility(level = 99)
            val startXp = player.statMap.getXP(stats.agility)

            // Start the lap, squeeze through the entrance mid-lap, then finish it.
            val first = course.start
            val placedFirst = placeMapLoc(first.dest, locTypes[first.loc])
            player.teleport(first.dest)
            player.opLoc1(placedFirst)
            advance(ticks = first.ticks + 1)

            val placedPipe = placeMapLoc(south.translateZ(1), locTypes[pipe.loc])
            player.teleport(south)
            player.opLoc1(placedPipe)
            advance(ticks = pipe.ticks + 1)
            assertEquals(north, player.coords) { "The entrance pipe did not move the player." }

            for (obstacle in course.obstacles.drop(1)) {
                val movement = obstacle.movement
                val passages = if (movement is Movement.Through) movement.ends else null
                repeat(obstacle.repeats) { i ->
                    val (from, to) =
                        passages?.get(i) ?: (obstacle.dest.translateX(-1) to obstacle.dest)
                    val placed = placeMapLoc(to.translateX(-1), locTypes[obstacle.loc])
                    player.teleport(from)
                    player.opLoc1(placed)
                    advance(ticks = obstacle.ticks + 1)
                }
            }

            val gained = player.statMap.getXP(stats.agility) - startXp
            assertEquals(course.lapTotalXp.toInt(), gained.toInt()) {
                "Passing the entrance mid-lap broke the lap: paid $gained."
            }
        }

    @Test
    fun GameTestState.`the wilderness gate refuses below the course level`() =
        runGameTest(WildernessGateScript::class) {
            val level = Wilderness.course.level
            val gateTile = CoordGrid(2998, 3917, 0)
            val gate =
                placeMapLoc(
                    gateTile,
                    locTypes[AgilityLocs.wilderness_gate_outer],
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.teleport(gateTile.translateZ(-1))
            player.setAgility(level = level - 1)

            player.opLoc1(gate)
            advance(ticks = 1)

            assertMessageSent("You need an Agility level of $level to use this course.")
            assertEquals(gateTile.translateZ(-1), player.coords)
        }

    @Test
    fun GameTestState.`the wilderness gate lets a player through at the course level`() =
        runGameTest(WildernessGateScript::class) {
            val gateTile = CoordGrid(2998, 3917, 0)
            val gate =
                placeMapLoc(
                    gateTile,
                    locTypes[AgilityLocs.wilderness_gate_outer],
                    LocShape.WallStraight,
                    LocAngle.South,
                )
            player.teleport(gateTile.translateZ(-1))
            player.setAgility(level = Wilderness.course.level)

            player.opLoc1(gate)
            advance(ticks = 2)
            assertEquals(gateTile, player.coords) { "Opening from the south did not step north." }

            player.opLoc1(gate)
            advance(ticks = 2)
            assertEquals(gateTile.translateZ(-1), player.coords) {
                "Opening from the north did not step back south."
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
