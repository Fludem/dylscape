package org.rsmod.content.skills.agility.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.agility.CourseRegistry
import org.rsmod.content.skills.agility.LapProgress
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.RooftopCourse
import org.rsmod.content.skills.agility.configs.AgilityObjs
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Every rooftop obstacle on every course.
 *
 * All of them bind `onOpLoc1`: the op *text* differs per obstacle ("Climb", "Cross", "Balance",
 * "Jump-up", "Teeth-grip", "Swing-across", "Vault", "Grab", "Walk-on", "Jump-in"...) but the slot
 * is op1 without exception across all nine courses. That was verified by decoding every loc type
 * straight out of `.data/cache/game`, not assumed - loc ops are famously not uniform in this cache.
 *
 * There is deliberately **no failure chance**. Every obstacle always succeeds.
 */
class RooftopCourseScript
@Inject
constructor(
    private val progress: LapProgress,
    private val xpMods: XpModifiers,
    private val objRepo: ObjRepository,
    private val random: GameRandom,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (position in CourseRegistry.all) {
            onOpLoc1(position.obstacle.loc) { traverse(it.loc) }
        }
        onEvent<SessionStateEvent.Delete> { progress.remove(player) }
    }

    private suspend fun ProtectedAccess.traverse(loc: BoundLocInfo) {
        val position = CourseRegistry[loc.id] ?: return
        val course = position.course

        if (player.agilityLvl < course.level) {
            mes("You need an Agility level of ${course.level} to use this course.")
            return
        }

        faceLoc(loc)
        cross(position.obstacle)

        // `cross` suspends, so nothing above this line can be trusted any more: the player may have
        // been moved, or the whole course reloaded, between the animation and here.
        statAdvance(stats.agility, position.obstacle.xp * xpMods.get(player, stats.agility))

        val lapComplete = progress.clear(player, position)
        if (position.isFirst) {
            spawnMarksOfGrace(course)
        }
        if (lapComplete) {
            statAdvance(stats.agility, course.lapXp * xpMods.get(player, stats.agility))
        }
    }

    private suspend fun ProtectedAccess.cross(obstacle: Obstacle) {
        when (val movement = obstacle.movement) {
            is Movement.Cross -> {
                anim(movement.anim)
                exactMove(
                    start = player.coords,
                    end = movement.dest,
                    delay1 = movement.clientStart,
                    delay2 = movement.clientEnd,
                    dir = movement.face,
                )
                delay(obstacle.ticks)
                resetAnim()
            }
            is Movement.Climb -> {
                anim(movement.anim)
                delay(obstacle.ticks)
                resetAnim()
                telejump(movement.dest)
            }
        }
    }

    /**
     * Spawns this lap's Marks of grace, at the start of the lap.
     *
     * Every lap pays [RooftopCourse.MARKS_PER_LAP] of them, and [RooftopCourse.markChance] rolls
     * for one more on top - so a lap is never a dry run, which is the whole point of the rate here.
     *
     * Each mark gets its own tile: two objs of the same type on one tile would merge into a single
     * stack, and a stack of five reads as one pickup rather than a roof strewn with marks. Tiles
     * are drawn without replacement from the course's list, which [RooftopCourse] guarantees is
     * longer than a lap's worth of marks.
     *
     * The marks are spawned **private to this player** - `ObjRepository.add` takes a `receiver`,
     * which is how the live game shows one person a mark on a roof that nobody else can see. They
     * are placed ahead of the player rather than handed over, so they still have to be picked up
     * during the lap.
     */
    private fun ProtectedAccess.spawnMarksOfGrace(course: RooftopCourse) {
        val bonus = if (random.randomBoolean(course.markChance)) 1 else 0
        val marks = (RooftopCourse.MARKS_PER_LAP + bonus).coerceAtMost(course.markTiles.size)
        val remaining = course.markTiles.toMutableList()
        repeat(marks) {
            val tile = remaining.removeAt(random.of(remaining.size))
            objRepo.add(
                coords = tile,
                type = AgilityObjs.mark_of_grace,
                count = 1,
                duration = MARK_DURATION,
                receiver = player,
            )
        }
    }

    private companion object {
        /** Long enough to finish a lap of the slowest course and come back for it. */
        const val MARK_DURATION = 400
    }
}
