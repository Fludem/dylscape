package org.rsmod.content.custom.barrows

import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * "Is this player in the crypt?", and the timer that follows from the answer.
 *
 * A coordinate test rather than an `onArea` binding. An area would need a `MapAreaBuilder`, which
 * only takes effect after `packCache` with the server stopped, and the crypt is exactly one level
 * of exactly one mapsquare - a box check answers it precisely and costs nothing.
 */
object BarrowsCrypt {
    /** Mapsquare 55_151, level 3. Levels 0-2 of that square hold nothing. */
    private const val BASE_X = 55 * 64
    private const val BASE_Z = 151 * 64
    private const val SQUARE_TILES = 64

    /** How often the crypt tick fires: prayer drain and the overlay's upkeep. */
    const val TICK_INTERVAL: Int = 10

    fun inCrypt(coords: CoordGrid): Boolean =
        coords.level == BarrowsMap.CRYPT_LEVEL &&
            coords.x in BASE_X until BASE_X + SQUARE_TILES &&
            coords.z in BASE_Z until BASE_Z + SQUARE_TILES

    val Player.inCrypt: Boolean
        get() = inCrypt(coords)
}
