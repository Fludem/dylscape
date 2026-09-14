package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * The Gnome Stronghold course, open from level 1 and the first ground course.
 *
 * Three cache quirks shape this file. The log is placed at level 1 in the map files but the bridge
 * flag under it drops it to the ground. The tree down has two loc types (`climbing_tree` and
 * `climbing_tree2`, three trunks between them) and any of them is the same obstacle. And the pipes
 * are two parallel pipes, one loc type each, each placed at both of its ends - so the last obstacle
 * is a [Movement.Through] with two passages, and a player can squeeze back north through either.
 *
 * The wiki lists the 50 completion bonus as its own row here rather than folding it into the pipe,
 * so `lapXp` is exactly that figure and the obstacles sum to the other 60.5.
 */
public object GnomeStronghold {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Gnome Stronghold",
            level = 1,
            lapXp = 50.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(2474, 3429, level = 0),
                    CoordGrid(2473, 3423, level = 1),
                    CoordGrid(2476, 3420, level = 2),
                    CoordGrid(2483, 3420, level = 2),
                    CoordGrid(2487, 3422, level = 0),
                    CoordGrid(2485, 3428, level = 0),
                    CoordGrid(2484, 3438, level = 0),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.gnome_log,
                        xp = 10.0,
                        ticks = 7,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2474, 3429, level = 0),
                                anim = AgilitySeqs.log_balance,
                                face = constants.em_face_south,
                                clientEnd = 210,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_net_1,
                        xp = 10.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2473, 3423, level = 1),
                                anim = AgilitySeqs.net,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_branch_up,
                        xp = 6.5,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2473, 3420, level = 2),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_rope,
                        xp = 10.0,
                        ticks = 5,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2483, 3420, level = 2),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_east,
                                clientEnd = 150,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_tree_down,
                        aliases = listOf(AgilityLocs.gnome_tree_down_2),
                        xp = 6.5,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2487, 3421, level = 0),
                                anim = AgilitySeqs.climb_down,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_net_2,
                        xp = 10.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2485, 3428, level = 0),
                                anim = AgilitySeqs.net,
                                face = constants.em_face_north,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.gnome_pipe_1,
                        aliases = listOf(AgilityLocs.gnome_pipe_2),
                        xp = 7.5,
                        ticks = 6,
                        movement =
                            Movement.Through(
                                ends =
                                    listOf(
                                        CoordGrid(2484, 3430, level = 0) to
                                            CoordGrid(2484, 3437, level = 0),
                                        CoordGrid(2487, 3430, level = 0) to
                                            CoordGrid(2487, 3437, level = 0),
                                    ),
                                anim = AgilitySeqs.pipe,
                                clientEnd = 180,
                            ),
                    ),
                ),
        )
}
