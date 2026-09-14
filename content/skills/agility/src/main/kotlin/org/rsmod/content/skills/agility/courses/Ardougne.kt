package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Ardougne, the level 90 course.
 *
 * Seven obstacles, the last of which jumps straight from the roof to the ground beside the starting
 * wall. Every loc here is also placed a second time at 3374,5934 - a copy of the market quarter
 * used by an instanced area - which is harmless because the script binds by type.
 *
 * The wiki's 625 for the final gap is the completion bonus folded in; split here into 65 for the
 * jump (matching the first gap) and 560 for the lap, for the published 889. The live game spawns
 * Ardougne's marks only at the end of a lap; here they follow the same rule as every other course.
 */
public object Ardougne {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Ardougne",
            level = 90,
            lapXp = 560.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(2671, 3303, level = 3),
                    CoordGrid(2671, 3308, level = 3),
                    CoordGrid(2663, 3318, level = 3),
                    CoordGrid(2655, 3318, level = 3),
                    CoordGrid(2653, 3312, level = 3),
                    CoordGrid(2651, 3307, level = 3),
                    CoordGrid(2650, 3298, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.ardy_wallclimb,
                        xp = 43.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2671, 3299, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_jump,
                        xp = 65.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2665, 3318, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_north,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_plank,
                        xp = 50.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2656, 3318, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_jump_2,
                        xp = 21.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2653, 3314, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_jump_3,
                        xp = 28.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2653, 3304, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_wallcrossing,
                        xp = 57.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2653, 3297, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.ardy_jump_4,
                        xp = 65.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2668, 3297, level = 0),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                ),
        )
}
