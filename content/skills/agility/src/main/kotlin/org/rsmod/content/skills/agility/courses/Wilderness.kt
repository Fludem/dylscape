package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * The Wilderness course, level 52.
 *
 * Rev 233 has the 2024 rework of this course: the stepping stones are a single loc and a single
 * click that hops every stone ([Movement.Through] would not do - the stones are a chain, not a
 * passage), and the climbing rocks at the end are one loc across three tiles. The pipe is placed at
 * both ends and can be squeezed either way.
 *
 * The wiki is explicit that the rocks pay 498.9 *only* when the rest of the lap was done and 0
 * otherwise, which is exactly [AgilityCourse.lapXp]; the rocks' own xp is zero. The dispenser
 * pillar, its tickets and the 150k payment are not implemented: marks of grace spawn here like on
 * every other course. The gates are handled by
 * [org.rsmod.content.skills.agility.scripts.WildernessGateScript].
 */
public object Wilderness {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Wilderness",
            level = 52,
            lapXp = 498.9,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3004, 3951, level = 0),
                    CoordGrid(3005, 3958, level = 0),
                    CoordGrid(3003, 3958, level = 0),
                    CoordGrid(2996, 3960, level = 0),
                    CoordGrid(3000, 3949, level = 0),
                    CoordGrid(2994, 3945, level = 0),
                    CoordGrid(2995, 3938, level = 0),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.wilderness_pipe,
                        xp = 12.5,
                        ticks = 8,
                        movement =
                            Movement.Through(
                                ends =
                                    listOf(
                                        CoordGrid(3004, 3937, level = 0) to
                                            CoordGrid(3004, 3950, level = 0)
                                    ),
                                anim = AgilitySeqs.pipe,
                                clientEnd = 240,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.wilderness_ropeswing,
                        xp = 20.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3005, 3952, level = 0),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.wilderness_stones,
                        xp = 20.0,
                        ticks = 6,
                        movement =
                            Movement.Hop(
                                stops =
                                    listOf(
                                        CoordGrid(3001, 3960, level = 0),
                                        CoordGrid(3000, 3960, level = 0),
                                        CoordGrid(2999, 3960, level = 0),
                                        CoordGrid(2998, 3960, level = 0),
                                        CoordGrid(2997, 3960, level = 0),
                                        CoordGrid(2996, 3960, level = 0),
                                    ),
                                anim = AgilitySeqs.stepping_stone,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.wilderness_log,
                        xp = 20.0,
                        ticks = 8,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2994, 3945, level = 0),
                                anim = AgilitySeqs.log_balance,
                                face = constants.em_face_west,
                                clientEnd = 240,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.wilderness_rocks,
                        xp = 0.0,
                        ticks = 3,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2995, 3933, level = 0),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                ),
        )
}
