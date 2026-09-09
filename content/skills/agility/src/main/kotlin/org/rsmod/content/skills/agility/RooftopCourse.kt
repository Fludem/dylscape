package org.rsmod.content.skills.agility

import org.rsmod.map.CoordGrid

/**
 * A rooftop course: an ordered chain of obstacles plus the bonus for finishing the lap.
 *
 * Obstacles must be cleared **in order**. The engine does not enforce that geometrically - a player
 * who telejumps onto the middle of a roof could click obstacle five first - so
 * [org.rsmod.content.skills.agility.LapProgress] tracks position in the chain and silently restarts
 * the lap on anything out of sequence. Clearing an obstacle always pays its own experience; only
 * the lap bonus depends on having done the whole chain.
 *
 * @param name the course's display name, used in the "you need level N" refusal.
 * @param level the Agility level required to use *any* obstacle on the course.
 * @param lapXp the bonus paid on clearing the final obstacle, on top of that obstacle's own xp. The
 *   wiki does not publish this split: it lists one figure for the last obstacle that already has
 *   the completion bonus folded into it (Draynor's crate is 79, Seers' edge is 435). So the last
 *   obstacle keeps an own-xp in line with its neighbours and the remainder lands here, which makes
 *   `lapTotalXp` exactly the published per-lap figure while still giving the bonus something to
 *   withhold from a player who skipped the chain. Al Kharid is `0.0` on purpose - the wiki says its
 *   experience is "more evenly spread throughout the course, unlike other Agility courses", and its
 *   eight obstacles already sum to the full 216.
 * @param markChance the chance, as `1 in markChance`, that a lap spawns a Mark of grace. Rolled
 *   once when the player clears the first obstacle, so the mark is on the roof ahead of them for
 *   the rest of the lap - which is how the live game does it.
 * @param markTiles the tiles a Mark of grace may spawn on. Must all be reachable during a lap.
 * @param obstacles the chain, in the order a lap runs them.
 */
public class RooftopCourse(
    val name: String,
    val level: Int,
    val lapXp: Double,
    val markChance: Int,
    val markTiles: List<CoordGrid>,
    val obstacles: List<Obstacle>,
) {
    init {
        require(level in 1..99) { "$name: level must be in 1..99, was $level" }
        require(lapXp >= 0.0) { "$name: lap xp cannot be negative, was $lapXp" }
        require(markChance >= 1) { "$name: mark chance must be at least 1, was $markChance" }
        require(obstacles.size >= 2) { "$name: a course needs at least two obstacles" }
        require(markTiles.isNotEmpty()) { "$name: needs at least one mark tile" }

        val duplicate = obstacles.groupBy { it.loc.id }.filterValues { it.size > 1 }.keys
        require(duplicate.isEmpty()) { "$name: obstacle loc used twice: $duplicate" }
    }

    /** Total experience for one clean lap, including the bonus. */
    val lapTotalXp: Double
        get() = obstacles.sumOf { it.xp } + lapXp

    val start: Obstacle
        get() = obstacles.first()

    val finish: Obstacle
        get() = obstacles.last()
}
