package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.RooftopCourse
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Seers' Village, the level 60 course, and the shortest of the nine at six obstacles.
 *
 * It alternates between levels 2 and 3 the whole way down, which the decoded terrain makes obvious:
 * the roof the wall drops you onto only exists at level 3, the tightrope platform only at level 2,
 * and the corridor after the first gap only at level 3 again.
 *
 * `rooftops_seers_crate` is left unbound. It sits beside the tightrope at (2707, 3488) and carries
 * `Search`, not a traversal op - it is scenery on the course, not part of the lap.
 *
 * The wiki's 435 for the final edge is almost entirely completion bonus - the five obstacles before
 * it pay 135 between them. Split here into 45 for the jump (matching the opening wall) and 390 for
 * the lap, for the published 570.
 */
public object SeersVillage {
    val course: RooftopCourse =
        RooftopCourse(
            name = "Seers' Village",
            level = 60,
            lapXp = 390.0,
            markChance = 4,
            markTiles =
                listOf(
                    CoordGrid(2725, 3494, level = 3),
                    CoordGrid(2713, 3493, level = 2),
                    CoordGrid(2710, 3479, level = 2),
                    CoordGrid(2710, 3472, level = 3),
                    CoordGrid(2702, 3472, level = 3),
                    CoordGrid(2700, 3465, level = 2),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.seers_wallclimb,
                        xp = 45.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2728, 3492, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.seers_jump,
                        xp = 20.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2713, 3493, level = 2),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_west,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.seers_tightrope,
                        xp = 20.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2711, 3481, level = 2),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.seers_jump_1,
                        xp = 35.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2710, 3472, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.seers_jump_2,
                        xp = 15.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2700, 3465, level = 2),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.seers_leapdown,
                        xp = 45.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2704, 3461, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
