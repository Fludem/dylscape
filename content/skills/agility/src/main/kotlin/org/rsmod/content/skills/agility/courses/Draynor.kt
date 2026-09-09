package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.RooftopCourse
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Draynor Village, the entry course, usable from level 1.
 *
 * The wiki puts this at level 10; here it is the first thing a new account can train, so it is open
 * from the start.
 *
 * Obstacle tiles come from the cache: every `l{x}_{z}` mapsquare was decoded and every
 * `rooftops_draynor_*` placement read off it (`tools/agility/DumpLocPlacements.java`). Destinations
 * were then read off the decoded terrain (`DumpMapTiles.java`) rather than guessed - the rooftop
 * walkways show up as unblocked corridors surrounded by blocked void, so the landing platform for
 * each obstacle is directly visible.
 *
 * One thing that catches you out: the **tightrope end markers are on blocked terrain**. The ropes
 * span a void, so `rooftops_draynor_tightrope_end` at (3090, 3277) is not somewhere a player can
 * stand - the real landing is the platform one tile south. `AgilityCourseTest` asserts every
 * destination here is walkable in the live collision map, which is what caught that.
 *
 * Experience is the wiki's per-obstacle table: 5, 8, 7, 7, 10, 4, and 79 for the crate at the end.
 * That last figure is the completion bonus folded into the final obstacle, so it is split here into
 * 4 for the climb down and 75 for the lap - 120 per lap either way.
 */
public object Draynor {
    val course: RooftopCourse =
        RooftopCourse(
            name = "Draynor Village",
            level = 1,
            lapXp = 75.0,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(3101, 3279, level = 3),
                    CoordGrid(3089, 3275, level = 3),
                    CoordGrid(3093, 3266, level = 3),
                    CoordGrid(3088, 3258, level = 3),
                    CoordGrid(3092, 3255, level = 3),
                    CoordGrid(3099, 3260, level = 3),
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.draynor_wallclimb,
                        xp = 5.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3102, 3279, level = 3),
                                anim = AgilitySeqs.climb_up,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_tightrope_1,
                        xp = 8.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3090, 3276, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_west,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_tightrope_2,
                        xp = 7.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3092, 3266, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_wallcrossing,
                        xp = 7.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3088, 3261, level = 3),
                                anim = AgilitySeqs.balance,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_wallscramble,
                        xp = 10.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3088, 3255, level = 3),
                                anim = AgilitySeqs.wall_scramble,
                                face = constants.em_face_south,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_leapdown,
                        xp = 4.0,
                        ticks = 2,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(3096, 3256, level = 3),
                                anim = AgilitySeqs.jump_gap,
                                face = constants.em_face_east,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.draynor_crate,
                        xp = 4.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(3103, 3261, level = 0),
                                anim = AgilitySeqs.climb_down,
                            ),
                    ),
                ),
        )
}
