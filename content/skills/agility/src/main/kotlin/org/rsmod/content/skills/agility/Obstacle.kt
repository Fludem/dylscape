package org.rsmod.content.skills.agility

import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid

/**
 * How the player physically gets across an obstacle.
 *
 * Four kinds cover every course. The distinction is not cosmetic: [Cross], [Through] and [Hop] are
 * visible slides the client interpolates, so the server's tick cost has to match the client cycles
 * they were given, while [Climb] is an instant relocation the player never sees themselves travel.
 */
public sealed interface Movement {
    /**
     * An `exactmove`: the client walks the player from where they stand to [dest] over [clientEnd]
     * client cycles while [anim] plays. This is every jump, gap, leap, swing, plank, tightrope,
     * ledge, log and set of monkey bars.
     *
     * [clientStart] is the cycle the player visually appears at their starting tile - almost always
     * 0. There are 30 client cycles to a game tick, but the engine takes these in the same units
     *    the protocol does, so a 2-tick obstacle wants roughly `clientEnd = 60`.
     */
    data class Cross(
        val dest: CoordGrid,
        val anim: SeqType,
        val face: Int,
        val clientStart: Int = 0,
        val clientEnd: Int = 60,
    ) : Movement

    /**
     * An animation, a pause, then a telejump. Wall climbs onto a roof and drops back to ground
     * level: the player is not shown travelling, they simply arrive. Same shape as
     * `content/generic/generic-locs/.../ladders/LadderScript.climb`.
     */
    data class Climb(val dest: CoordGrid, val anim: SeqType) : Movement

    /**
     * A two-ended obstacle that can be taken in either direction: pipes, and the low walls a player
     * climbs over. Each entry in [ends] is one passage; the player is moved from the end nearest
     * them to the other end of that same passage, so a pipe entered from the north comes out at the
     * south and a wall climbed from the east lands on the west.
     *
     * Several passages exist for the Gnome course, which has two parallel pipes sharing one
     * obstacle slot, and for Barbarian Outpost, whose three crumbling walls are one loc placed
     * three times.
     */
    data class Through(
        val ends: List<Pair<CoordGrid, CoordGrid>>,
        val anim: SeqType,
        val clientEnd: Int = 60,
    ) : Movement {
        init {
            require(ends.isNotEmpty()) { "Through needs at least one passage." }
        }

        /** The far end of the passage whose near end is closest to [from]. */
        fun exit(from: CoordGrid): CoordGrid {
            val (a, b) =
                ends.minBy { (a, b) -> minOf(from.chebyshevDistance(a), from.chebyshevDistance(b)) }
            return if (from.chebyshevDistance(a) <= from.chebyshevDistance(b)) b else a
        }
    }

    /**
     * A chain of short `exactmove`s, one per entry in [stops], each taking [ticksPerHop]. The
     * Wilderness stepping stones: one click, six hops, and only the last stone is ground a player
     * can stand on.
     */
    data class Hop(val stops: List<CoordGrid>, val anim: SeqType, val ticksPerHop: Int = 1) :
        Movement {
        init {
            require(stops.isNotEmpty()) { "Hop needs at least one stop." }
            require(ticksPerHop >= 1) { "Hop must take at least one tick per stop." }
        }
    }
}

/**
 * One obstacle on a course.
 *
 * Every obstacle on every course carries its interaction on **op1**, rooftop and ground alike, so
 * there is no per-obstacle op slot here - see
 * [org.rsmod.content.skills.agility.configs.AgilityLocs].
 *
 * @param loc the obstacle's loc type.
 * @param xp experience for clearing this obstacle alone, before the lap bonus and before any XP
 *   modifier. May be zero: Barbarian Outpost's ladder and the Wilderness rocks are links in the
 *   chain that pay nothing themselves.
 * @param ticks how long the whole traversal takes. Must be at least 1; a 0-tick obstacle would let
 *   a player spam-click it for free experience.
 * @param movement how the player crosses.
 * @param aliases other loc types that are the *same* obstacle: a corner ledge built from two
 *   pieces, a tree with two climbable trunks, a second pipe beside the first. Clicking any of them
 *   is one clear of this obstacle.
 * @param repeats how many consecutive clears a lap needs before it moves on. Barbarian Outpost's
 *   three crumbling walls are one loc placed three times; a lap has to climb all three.
 */
public data class Obstacle(
    val loc: LocType,
    val xp: Double,
    val ticks: Int,
    val movement: Movement,
    val aliases: List<LocType> = emptyList(),
    val repeats: Int = 1,
) {
    init {
        require(xp >= 0.0) { "Obstacle xp cannot be negative: $this" }
        require(ticks >= 1) { "Obstacle must take at least one tick: $this" }
        require(repeats >= 1) { "Obstacle must be cleared at least once: $this" }
    }

    /** The primary loc and every alias. */
    val locs: List<LocType>
        get() = listOf(loc) + aliases

    /** Every tile a traversal can leave the player standing on. */
    val dests: List<CoordGrid>
        get() =
            when (movement) {
                is Movement.Cross -> listOf(movement.dest)
                is Movement.Climb -> listOf(movement.dest)
                is Movement.Through -> movement.ends.flatMap { (a, b) -> listOf(a, b) }
                is Movement.Hop -> listOf(movement.stops.last())
            }

    /** Where a forward traversal ends: the first of [dests], which is enough for tests. */
    val dest: CoordGrid
        get() = dests.first()
}
