package org.rsmod.content.skills.agility

import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.map.CoordGrid

/**
 * How the player physically gets across an obstacle.
 *
 * Two kinds cover all nine rooftop courses. The distinction is not cosmetic: [Cross] is a visible
 * slide the client interpolates, so the server's tick cost has to match the client cycles it was
 * given, while [Climb] is an instant relocation the player never sees themselves travel.
 */
public sealed interface Movement {
    /**
     * An `exactmove`: the client walks the player from where they stand to [dest] over [clientEnd]
     * client cycles while [anim] plays. This is every jump, gap, leap, swing, plank, tightrope,
     * ledge and set of monkey bars.
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
}

/**
 * One obstacle on a rooftop course.
 *
 * Every rooftop obstacle in the cache carries its interaction on **op1**, so there is no per-
 * obstacle op slot here - see [org.rsmod.content.skills.agility.configs.AgilityLocs].
 *
 * @param loc the obstacle's loc type.
 * @param xp experience for clearing this obstacle alone, before the lap bonus and before any XP
 *   modifier.
 * @param ticks how long the whole traversal takes. Must be at least 1; a 0-tick obstacle would let
 *   a player spam-click it for free experience.
 * @param movement how the player crosses.
 */
public data class Obstacle(
    val loc: LocType,
    val xp: Double,
    val ticks: Int,
    val movement: Movement,
) {
    init {
        require(xp > 0.0) { "Obstacle xp must be positive: $this" }
        require(ticks >= 1) { "Obstacle must take at least one tick: $this" }
    }

    val dest: CoordGrid
        get() =
            when (movement) {
                is Movement.Cross -> movement.dest
                is Movement.Climb -> movement.dest
            }
}
