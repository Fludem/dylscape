package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Falador, the level 50 course and the longest chain in the game at thirteen obstacles.
 *
 * The wiki lists four ledges; the cache has five ledge locs. `rooftops_falador_ledge_3a` and `_3b`
 * are the two halves of the corner ledge between the second and fourth, so `3b` is an alias of `3a`
 * rather than a fourteenth obstacle.
 *
 * The hand holds run north up the *side* of the building at level 2 while the player starts and
 * finishes on level 3; only the start loc carries an op.
 *
 * The wiki's 241 for the edge is the completion bonus folded in; split here into 41 for the jump
 * and 200 for the lap, for the published 586.
 */
public object Falador {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Falador",
            level = 50,
            lapXp = 200.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3048, 3343, level = 3),
                    CoordGrid(3048, 3357, level = 3),
                    CoordGrid(3041, 3361, level = 3),
                    CoordGrid(3027, 3354, level = 3),
                    CoordGrid(3020, 3353, level = 3),
                    CoordGrid(3016, 3349, level = 3),
                    CoordGrid(3021, 3332, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.falador_wallclimb,
                        xp = 11.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3036, 3343, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_tightrope_1,
                        xp = 22.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3048, 3343, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_east,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_handholds_start,
                        xp = 61.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3048, 3357, level = 3),
                                anim = AgilitySeqs.handholds,
                                face = constants.em_face_north,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_gap_1,
                        xp = 27.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3048, 3361, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_north,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_gap_2,
                        xp = 26.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3041, 3361, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_west,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_tightrope_2,
                        xp = 61.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3027, 3354, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_southwest,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_tightrope_3,
                        xp = 53.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3020, 3353, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_gap_3,
                        xp = 30.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3016, 3349, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_ledge_1,
                        xp = 14.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3013, 3345, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_ledge_2,
                        xp = 13.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3012, 3339, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_ledge_3a,
                        aliases = listOf(AgilityLocs.falador_ledge_3b),
                        xp = 13.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3016, 3333, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_ledge_4,
                        xp = 14.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3021, 3332, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.falador_edge,
                        xp = 41.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3029, 3334, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
