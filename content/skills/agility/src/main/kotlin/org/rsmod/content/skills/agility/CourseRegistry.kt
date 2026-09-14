package org.rsmod.content.skills.agility

import org.rsmod.content.skills.agility.courses.AgilityCourses
import org.rsmod.game.type.loc.LocType

/**
 * Resolves an obstacle from the loc the player clicked.
 *
 * **Keyed on the raw `Int` loc id, deliberately.** `LocReferences.find` hands back a
 * `HashedLocType` while the runtime gives scripts an `UnpackedLocType`, and the two never compare
 * equal - a map keyed on `LocType` would build fine, boot fine, and then never match anything.
 */
public object CourseRegistry {
    /**
     * Where an obstacle sits: which course, and how far along the chain. [index] is [ENTRANCE] for
     * an obstacle that is on the course but not in the lap.
     */
    data class Position(val course: AgilityCourse, val index: Int, val obstacle: Obstacle) {
        val isEntrance: Boolean
            get() = index == ENTRANCE

        val isFirst: Boolean
            get() = index == 0

        val isLast: Boolean
            get() = index == course.obstacles.lastIndex
    }

    const val ENTRANCE: Int = -1

    private val byLocId: Map<Int, Position> = buildMap {
        for (course in AgilityCourses.all) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                register(Position(course, index, obstacle))
            }
            for (obstacle in course.entrances) {
                register(Position(course, ENTRANCE, obstacle))
            }
        }
    }

    private fun MutableMap<Int, Position>.register(position: Position) {
        for (loc in position.obstacle.locs) {
            val previous = put(loc.id, position)
            check(previous == null) {
                "Loc ${loc.id} is on both ${previous?.course?.name} and ${position.course.name}."
            }
        }
    }

    operator fun get(locId: Int): Position? = byLocId[locId]

    /** Every loc that belongs to a course, aliases included, each exactly once. */
    val locs: List<LocType>
        get() = AgilityCourses.all.flatMap { c -> (c.obstacles + c.entrances).flatMap { it.locs } }
}
