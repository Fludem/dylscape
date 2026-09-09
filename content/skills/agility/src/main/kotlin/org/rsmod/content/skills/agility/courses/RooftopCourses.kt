package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.RooftopCourse

/** Every rooftop course, lowest level first. */
public object RooftopCourses {
    val all: List<RooftopCourse> = listOf(Draynor.course)

    val lowestLevel: Int = all.minOf { it.level }
}
