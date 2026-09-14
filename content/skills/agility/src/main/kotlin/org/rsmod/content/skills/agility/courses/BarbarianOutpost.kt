package org.rsmod.content.skills.agility.courses

import org.rsmod.api.config.constants
import org.rsmod.content.skills.agility.AgilityCourse
import org.rsmod.content.skills.agility.Movement
import org.rsmod.content.skills.agility.Obstacle
import org.rsmod.content.skills.agility.configs.AgilityLocs
import org.rsmod.content.skills.agility.configs.AgilitySeqs
import org.rsmod.map.CoordGrid

/**
 * Barbarian Outpost, the level 35 course, and the one that pays Strength as well.
 *
 * The lap climbs the net to a level 1 platform, walks the ledge, and comes back down the ladder -
 * `barbarian_laddertop_norim`, which upstream's ladder script does not know about - before the
 * three crumbling walls. The ladder is a link in the chain that pays nothing, exactly as the wiki
 * lists it.
 *
 * The three walls are **one loc placed three times**, so they are one [Obstacle] with three
 * [Movement.Through] passages and `repeats = 3`: a lap has to climb all three, each paying its own
 * 13.7, and any of them can be climbed back westward.
 *
 * The entrance pipe is the outpost's level gate and not part of the lap, so it lives in
 * [AgilityCourse.entrances]. The barcrawl miniquest the live game asks for does not exist here.
 *
 * The wiki lists the completion bonus as its own row: 46.3 Agility and 41.3 Strength.
 */
public object BarbarianOutpost {
    val course: AgilityCourse =
        AgilityCourse(
            name = "Barbarian Outpost",
            level = 35,
            lapXp = 46.3,
            lapStrengthXp = 41.3,
            markChance = 5,
            markTiles =
                listOf(
                    CoordGrid(2551, 3549, level = 0),
                    CoordGrid(2546, 3546, level = 0),
                    CoordGrid(2541, 3546, level = 0),
                    CoordGrid(2535, 3547, level = 1),
                    CoordGrid(2534, 3551, level = 0),
                    CoordGrid(2538, 3553, level = 0),
                    CoordGrid(2544, 3553, level = 0),
                ),
            entrances =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.barbarian_pipe,
                        xp = 0.0,
                        ticks = 3,
                        movement =
                            Movement.Through(
                                ends =
                                    listOf(
                                        CoordGrid(2552, 3558, level = 0) to
                                            CoordGrid(2552, 3561, level = 0)
                                    ),
                                anim = AgilitySeqs.pipe,
                                clientEnd = 90,
                            ),
                    )
                ),
            obstacles =
                listOf(
                    Obstacle(
                        loc = AgilityLocs.barbarian_ropeswing,
                        xp = 22.0,
                        ticks = 3,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2551, 3549, level = 0),
                                anim = AgilitySeqs.rope_swing,
                                face = constants.em_face_south,
                                clientEnd = 90,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.barbarian_log,
                        xp = 13.7,
                        ticks = 8,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2541, 3546, level = 0),
                                anim = AgilitySeqs.log_balance,
                                face = constants.em_face_west,
                                clientEnd = 240,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.barbarian_net,
                        xp = 8.2,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2537, 3546, level = 1),
                                anim = AgilitySeqs.net,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.barbarian_ledge,
                        xp = 22.0,
                        ticks = 4,
                        movement =
                            Movement.Cross(
                                dest = CoordGrid(2532, 3547, level = 1),
                                anim = AgilitySeqs.ledge,
                                face = constants.em_face_west,
                                clientEnd = 120,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.barbarian_ladder_down,
                        xp = 0.0,
                        ticks = 2,
                        movement =
                            Movement.Climb(
                                dest = CoordGrid(2532, 3546, level = 0),
                                anim = AgilitySeqs.climb_down,
                            ),
                    ),
                    Obstacle(
                        loc = AgilityLocs.barbarian_wall,
                        xp = 13.7,
                        ticks = 2,
                        repeats = 3,
                        movement =
                            Movement.Through(
                                ends =
                                    listOf(
                                        CoordGrid(2535, 3553, level = 0) to
                                            CoordGrid(2537, 3553, level = 0),
                                        CoordGrid(2538, 3553, level = 0) to
                                            CoordGrid(2540, 3553, level = 0),
                                        CoordGrid(2541, 3553, level = 0) to
                                            CoordGrid(2543, 3553, level = 0),
                                    ),
                                anim = AgilitySeqs.wall_scramble,
                            ),
                    ),
                ),
        )
}
