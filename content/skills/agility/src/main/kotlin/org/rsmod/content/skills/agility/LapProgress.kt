package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

/**
 * How far each player is through their current lap.
 *
 * **Transient by design.** There is no course progress varbit anywhere in the rev 233 cache - the
 * live game keeps this server-side and drops it on logout, and so do we. That also avoids minting a
 * varbit, which is a real cache type and would drag in a `packCache` for something no client ever
 * reads.
 *
 * Cleared on `SessionStateEvent.Delete` rather than `Logout`, because logout fires *before* the
 * account save and anything holding a `Player` past that point leaks it.
 */
@Singleton
public class LapProgress @Inject constructor() {
    private val laps = HashMap<Player, Lap>()

    /**
     * Records that [player] just cleared the obstacle at [position].
     *
     * Returns `true` if that completed a full lap in order. Anything out of sequence quietly starts
     * a new lap from this obstacle instead - so a player who wanders onto the middle of a course
     * still gets each obstacle's own experience, they just do not get the lap bonus until they run
     * a clean one.
     *
     * An obstacle with [Obstacle.repeats] greater than one holds the lap on the same index until it
     * has been cleared that many times in a row. An entrance never touches the lap at all.
     */
    fun clear(player: Player, position: CourseRegistry.Position): Boolean {
        if (position.isEntrance) {
            return false
        }

        val lap = laps[player]
        val continues = lap != null && lap.course === position.course && lap.next == position.index
        if (!continues) {
            // Out of sequence. Only an actual first obstacle starts a countable lap.
            laps[player] =
                if (position.isFirst) {
                    Lap(position.course, next = 0, cleared = 0).advance(position)
                } else {
                    Lap(position.course, next = NOT_COUNTING, cleared = 0)
                }
            return false
        }

        val advanced = lap!!.advance(position)
        if (advanced.next == position.index) {
            // Still on a repeated obstacle.
            laps[player] = advanced
            return false
        }

        if (position.isLast) {
            laps.remove(player)
            return true
        }

        laps[player] = advanced
        return false
    }

    /** Whether [player] is on a clean lap of [course] that has not yet finished. */
    fun isRunning(player: Player, course: AgilityCourse): Boolean {
        val lap = laps[player] ?: return false
        return lap.course === course && lap.next != NOT_COUNTING
    }

    fun remove(player: Player) {
        laps.remove(player)
    }

    /**
     * @param next the index of the obstacle the lap expects next.
     * @param cleared how many times [next] has been cleared so far, for repeated obstacles.
     */
    private data class Lap(val course: AgilityCourse, val next: Int, val cleared: Int) {
        /** The lap after one more clear of [position], which must be the obstacle at [next]. */
        fun advance(position: CourseRegistry.Position): Lap {
            val done = cleared + 1
            return if (done < position.obstacle.repeats) {
                copy(cleared = done)
            } else {
                copy(next = position.index + 1, cleared = 0)
            }
        }
    }

    private companion object {
        /** A lap that started mid-course and can never pay the bonus. */
        const val NOT_COUNTING = -1
    }
}
