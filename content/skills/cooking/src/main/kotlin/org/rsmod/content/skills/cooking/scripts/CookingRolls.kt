package org.rsmod.content.skills.cooking.scripts

import org.rsmod.api.random.GameRandom

/**
 * Whether a cook burns.
 *
 * Every food has a level at which it stops burning altogether -- that part is the live game's own
 * number, held per food and per heat source. Below it, the chance of success climbs in a straight
 * line from [BASE_SUCCESS] at the food's level requirement up to certainty at the stop-burn level.
 *
 * That line is the one **tuned** number in the module. It puts a brand-new cook's shrimps at
 * roughly a one-in-three burn, which is the right neighbourhood, and it keeps the ordering that
 * matters: a food you have only just unlocked burns often, one you are about to master hardly ever.
 * The live game's exact curve is not public and has not been transcribed.
 */
object CookingRolls {
    /** Out of [ROLL]: the success weight at the food's level requirement. */
    const val BASE_SUCCESS: Int = 166

    const val ROLL: Int = 256

    fun burns(level: Int, levelReq: Int, stopBurn: Int, random: GameRandom): Boolean {
        if (level >= stopBurn) {
            return false
        }
        val span = (stopBurn - levelReq).coerceAtLeast(1)
        val progress = (level - levelReq).coerceIn(0, span)
        val success = BASE_SUCCESS + (ROLL - BASE_SUCCESS) * progress / span
        return random.of(maxExclusive = ROLL) >= success
    }
}
