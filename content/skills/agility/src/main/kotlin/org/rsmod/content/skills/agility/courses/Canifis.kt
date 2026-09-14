package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Canifis, the level 40 course.
 *
 * The live game plays this course on plane 2, and the reason is in the map data: the tiles under
 * the obstacles carry the bridge flag on level 1. This server does **not** shift a level-3 loc for
 * a flag on level 1 - `GameMapDecoder.putLocs` only looks at the loc's own level and the one
 * above - so here the obstacles, the roofs and the player are all on level 3, which is also where
 * the collision map has the walkable roof pockets. If the client draws the roofs a level lower than
 * the player, that is the place to look.
 *
 * The cache's `rooftops_canifis_jump_5` is the *third* gap of the lap, not the last - the numbering
 * is the order the locs were made, not the order they are run.
 *
 * The wiki's 175 for the final gap is the completion bonus folded in; split here into 15 for the
 * jump and 160 for the lap, for the published 240. Canifis spawns marks at roughly double the rate
 * of other courses, so [markChance] is tighter here.
 */
public object Canifis {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Canifis",
            level = 40,
            lapXp = 160.0,
            markChance = 3,
            markTiles =
                listOf(
                    CoordGrid(3507, 3494, level = 3),
                    CoordGrid(3500, 3505, level = 3),
                    CoordGrid(3491, 3502, level = 3),
                    CoordGrid(3477, 3495, level = 3),
                    CoordGrid(3480, 3486, level = 3),
                    CoordGrid(3495, 3475, level = 3),
                    CoordGrid(3512, 3480, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.canifis_start_tree,
                        xp = 10.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3507, 3492, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_jump,
                        xp = 8.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3503, 3505, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_north,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_jump_2,
                        xp = 8.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3492, 3503, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_west,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_jump_5,
                        xp = 10.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3479, 3498, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_west,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_jump_3,
                        xp = 8.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3478, 3487, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_polevault,
                        xp = 10.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3490, 3477, level = 3),
                                anim = AgilitySeqs.pole_vault,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_jump_4,
                        xp = 11.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3510, 3478, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.canifis_leapdown,
                        xp = 15.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3510, 3485, level = 0),
                                anim = AgilitySeqs.wall_drop,
                            ),
                    ),
                ),
        )
}
