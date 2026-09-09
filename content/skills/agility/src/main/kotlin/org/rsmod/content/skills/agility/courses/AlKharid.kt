package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.RooftopCourse
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Al Kharid, the level 10 course.
 *
 * The wiki puts this at level 20; it is halved here to sit above [Draynor], which is open from
 * level 1 on this server.
 *
 * The only course that spends time below level 3: the zip line drops onto a level 1 roof, the
 * tropical tree keeps you there, and the roof top beams take you back up to 3 for the last
 * tightrope. Obstacle tiles and destinations were read from the decoded cache the same way as
 * [Draynor] - see `tools/agility/README.md`.
 *
 * `rooftops_kharid_tree` is named in the cache but placed nowhere in the world, so the tropical
 * tree here is `bamboo_tree_top`, which is what the map actually uses.
 *
 * Alone among the courses implemented so far this one has **no lap bonus**. The wiki is explicit
 * that Al Kharid's experience is "more evenly spread throughout the course, unlike other Agility
 * courses", and its eight obstacles already sum to the full 216.
 */
public object AlKharid {
    val course: RooftopCourse =
        RooftopCourse(
            name = "Al Kharid",
            level = 10,
            lapXp = 0.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3273, 3192, level = 3),
                    CoordGrid(3270, 3170, level = 3),
                    CoordGrid(3268, 3162, level = 3),
                    CoordGrid(3313, 3163, level = 1),
                    CoordGrid(3316, 3175, level = 1),
                    CoordGrid(3314, 3182, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.kharid_wallclimb,
                        xp = 12.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3273, 3192, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_tightrope_1,
                        xp = 36.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3272, 3172, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_rope_swing,
                        xp = 48.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3268, 3162, level = 3),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_slide_side,
                        xp = 48.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3313, 3163, level = 1),
                                anim = AgilitySeqs.zipline,
                                face = constants.em_face_east,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_bamboo_tree_top,
                        xp = 12.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3316, 3175, level = 1),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_north,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_wallclimb_2,
                        xp = 6.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3316, 3181, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_tightrope_4,
                        xp = 18.0,
                        ticks = 6,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3303, 3186, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 180,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.kharid_leapdown,
                        xp = 36.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3300, 3195, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
