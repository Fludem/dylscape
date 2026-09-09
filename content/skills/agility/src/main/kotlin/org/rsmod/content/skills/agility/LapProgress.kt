package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.entity.Player

/**
 * How far each player is through their current lap.
 *
 * **Transient by design.** There is no rooftop progress varbit anywhere in the rev 233 cache - the
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
     */
    fun clear(player: Player, position: CourseRegistry.Position): Boolean {
        val lap = laps[player]
        val continues = lap != null && lap.course === position.course && lap.next == position.index
        if (!continues) {
            // Out of sequence. Only an actual first obstacle starts a countable lap.
            laps[player] =
                if (position.isFirst) {
                    Lap(position.course, next = 1)
                } else {
                    Lap(position.course, next = NOT_COUNTING)
                }
            return false
        }

        if (position.isLast) {
            laps.remove(player)
            return true
        }

        laps[player] = Lap(position.course, next = position.index + 1)
        return false
    }

    /** Whether [player] is on a clean lap of [course] that has not yet finished. */
    fun isRunning(player: Player, course: RooftopCourse): Boolean {
        val lap = laps[player] ?: return false
        return lap.course === course && lap.next != NOT_COUNTING
    }

    fun remove(player: Player) {
        laps.remove(player)
    }

    private data class Lap(val course: RooftopCourse, val next: Int)

    private companion object {
        /** A lap that started mid-course and can never pay the bonus. */
        const val NOT_COUNTING = -1
    }
}
