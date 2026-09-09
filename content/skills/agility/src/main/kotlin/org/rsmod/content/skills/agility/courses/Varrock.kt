package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.RooftopCourse
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Varrock, the level 30 course, and the longest chain of the three at nine obstacles.
 *
 * The roof between the rough wall and the clothes line is genuinely awkward: the decoded terrain
 * shows a single-tile column at x=3219 that the wall drops you onto, then a dog-leg west along
 * z=3411 and north up x=3214 to reach the line. Landing on (3219, 3414) rather than beside the wall
 * is deliberate for that reason - (3220, 3414) is blocked.
 *
 * The middle of the course threads a series of one-tile-wide columns (x=3202, 3208, 3218, 3232, all
 * running z=3398..3403), which is why several destinations here look oddly specific.
 *
 * The leap to the ruins really is a ten-tile jump. Terrain alone suggests landing around x=3197,
 * but the *collision* map - terrain plus locs - has x=3192..3200 blocked solid by the ruin scenery,
 * so the only ground on the far side is x=3186..3191. `AgilityCourseTest` caught that; a terrain
 * dump cannot see it, which is the whole reason that test sweeps the live collision map.
 *
 * The wiki's table ends on 143.7 for the edge, which is the completion bonus folded into the last
 * jump; it is split here into 3.7 for the jump and 140 for the lap. 269.7 per lap, which the wiki
 * rounds to 270 in prose.
 */
public object Varrock {
    val course: RooftopCourse =
        RooftopCourse(
            name = "Varrock",
            level = 30,
            lapXp = 140.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3219, 3414, level = 3),
                    CoordGrid(3208, 3414, level = 3),
                    CoordGrid(3191, 3416, level = 3),
                    CoordGrid(3193, 3405, level = 3),
                    CoordGrid(3193, 3398, level = 3),
                    CoordGrid(3218, 3397, level = 3),
                    CoordGrid(3236, 3403, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.varrock_wallclimb,
                        xp = 13.5,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3219, 3414, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_clothesline,
                        xp = 23.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3208, 3414, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_leaptoruins,
                        xp = 19.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3191, 3416, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_west,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_wallswing,
                        xp = 28.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3193, 3405, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_wallscramble,
                        xp = 10.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3193, 3398, level = 3),
                                anim = AgilitySeqs.wall_scramble,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_leaptobalcony,
                        xp = 24.5,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3218, 3397, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_leapdown,
                        xp = 4.5,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3236, 3403, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_stepuproof,
                        xp = 3.5,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3236, 3412, level = 3),
                                anim = AgilitySeqs.hurdle,
                                face = constants.em_face_north,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.varrock_finish,
                        xp = 3.7,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3236, 3418, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
