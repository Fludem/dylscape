package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Rellekka, the level 80 course.
 *
 * Every obstacle is placed at level 3 and nothing under them carries the bridge flag, so the whole
 * lap runs on level 3 - even though the `rooftops_rellekka_tightrope_end` markers for the ropes are
 * placed a level *lower*, at 2. The markers are scenery under the rope ends, not landing spots; the
 * landings are the level-3 roof pockets the collision map shows beyond them.
 *
 * The wiki's 475 for the pile of fish is the completion bonus folded in; split here into 75 for the
 * jump and 400 for the lap, for the published 780. The Fremennik diary bonus does not exist here.
 */
public object Rellekka {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Rellekka",
            level = 80,
            lapXp = 400.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(2623, 3673, level = 3),
                    CoordGrid(2620, 3665, level = 3),
                    CoordGrid(2629, 3652, level = 3),
                    CoordGrid(2634, 3663, level = 3),
                    CoordGrid(2641, 3652, level = 3),
                    CoordGrid(2648, 3660, level = 3),
                    CoordGrid(2656, 3674, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.rellekka_wallclimb,
                        xp = 20.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2625, 3675, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_gap_1,
                        xp = 30.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2622, 3667, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_tightrope_1,
                        xp = 40.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2627, 3654, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_southeast,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_gap_2,
                        xp = 85.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2630, 3660, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_north,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_gap_3,
                        xp = 25.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2643, 3657, level = 3),
                                anim = AgilitySeqs.hurdle,
                                face = constants.em_face_north,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_tightrope_3,
                        xp = 105.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2655, 3670, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_northeast,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.rellekka_dropoff,
                        xp = 75.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2652, 3676, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
