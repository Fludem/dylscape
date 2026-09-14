package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Pollnivneach, the level 70 course.
 *
 * Runs on levels 1 and 2 rather than 3: the basket puts the player on a level 1 roof and the rough
 * wall takes them to 2, where the monkey bars, tree top and drying line finish the lap.
 *
 * The wiki's 540 for the drying line is the completion bonus folded in; split here into 40 for the
 * jump and 500 for the lap, for the published 890. The Desert diary bonus does not exist here.
 */
public object Pollnivneach {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Pollnivneach",
            level = 70,
            lapXp = 500.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3349, 2965, level = 1),
                    CoordGrid(3354, 2975, level = 1),
                    CoordGrid(3361, 2978, level = 1),
                    CoordGrid(3367, 2975, level = 1),
                    CoordGrid(3356, 2983, level = 2),
                    CoordGrid(3358, 2993, level = 2),
                    CoordGrid(3357, 3002, level = 2),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_basket,
                        xp = 10.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3349, 2964, level = 1),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_marketstall,
                        xp = 45.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3352, 2974, level = 1),
                                anim = AgilitySeqs.hurdle,
                                face = constants.em_face_northeast,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_hangingbanner,
                        xp = 65.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3360, 2978, level = 1),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_gap,
                        xp = 35.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3366, 2976, level = 1),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_tree,
                        xp = 75.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3366, 2982, level = 1),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_north,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_wallclimb,
                        xp = 5.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3365, 2983, level = 2),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_monkeybars_start,
                        xp = 55.0,
                        ticks = 6,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3358, 2992, level = 2),
                                anim = AgilitySeqs.monkeybars_walk,
                                face = constants.em_face_north,
                                clientEnd = 180,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_treetop,
                        xp = 60.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3359, 3000, level = 2),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_north,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.pollnivneach_line,
                        xp = 40.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3363, 3003, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
