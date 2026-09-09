package org.rsmod.content.skills.agility

import org.rsmod.content.skills.agility.courses.RooftopCourses

/**
 * Resolves an obstacle from the loc the player clicked.
 *
 * **Keyed on the raw `Int` loc id, deliberately.** `LocReferences.find` hands back a
 * `HashedLocType` while the runtime gives scripts an `UnpackedLocType`, and the two never compare
 * equal - a map keyed on `LocType` would build fine, boot fine, and then never match anything.
 */
public object CourseRegistry {
    /** Where an obstacle sits: which course, and how far along the chain. */
    data class Position(val course: RooftopCourse, val index: Int) {
        val obstacle: Obstacle
            get() = course.obstacles[index]

        val isFirst: Boolean
            get() = index == 0

        val isLast: Boolean
            get() = index == course.obstacles.lastIndex
    }

    private val byLocId: Map<Int, Position> = buildMap {
        for (course in RooftopCourses.all) {
            for ((index, obstacle) in course.obstacles.withIndex()) {
                val previous = put(obstacle.loc.id, Position(course, index))
                check(previous == null) {
                    "Loc ${obstacle.loc.id} is on both ${previous?.course?.name} and ${course.name}."
                }
            }
        }
    }

    operator fun get(locId: Int): Position? = byLocId[locId]

    val all: Collection<Position>
        get() = byLocId.values
}
