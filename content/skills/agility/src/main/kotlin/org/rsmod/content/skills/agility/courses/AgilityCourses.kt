package org.rsmod.content.skills.agility.courses

import org.rsmod.content.skills.agility.AgilityCourse

/** Every course, lowest level first. */
public object AgilityCourses {
    val all: List<AgilityCourse> =
        listOf(
            GnomeStronghold.course,
            Draynor.course,
            AlKharid.course,
            Varrock.course,
            BarbarianOutpost.course,
            Canifis.course,
            Falador.course,
            Wilderness.course,
            SeersVillage.course,
            Pollnivneach.course,
            Rellekka.course,
            Ardougne.course,
        )

    val lowestLevel: Int = all.minOf { it.level }
}
