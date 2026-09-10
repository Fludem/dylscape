package org.rsmod.content.custom.leagues.relics

/**
 * League Points, derived from total level.
 *
 * Nothing on this server awards points (there are no league tasks), so tiers unlock by total level
 * instead. The client still gates each tier on the vanilla points thresholds in
 * [Relic.TIER_POINTS], so total level is translated into points rather than the other way round:
 * each knot below puts one tier's threshold on a total level, and the points bar fills smoothly
 * between them.
 */
object LeaguePoints {
    /** `(total level, points)`, one knot per tier threshold. */
    private val KNOTS =
        listOf(
            0 to 0,
            300 to 750,
            500 to 1500,
            750 to 2500,
            1000 to 5000,
            1250 to 8000,
            1500 to 16000,
            1750 to 25000,
        )

    /** The points at the top knot; total levels past it earn nothing more. */
    val MAX: Int = KNOTS.last().second

    fun forTotalLevel(totalLevel: Int): Int {
        if (totalLevel <= KNOTS.first().first) {
            return KNOTS.first().second
        }
        for (i in 1 until KNOTS.size) {
            val (levelHigh, pointsHigh) = KNOTS[i]
            if (totalLevel <= levelHigh) {
                val (levelLow, pointsLow) = KNOTS[i - 1]
                val progress = (totalLevel - levelLow).toLong() * (pointsHigh - pointsLow)
                return pointsLow + (progress / (levelHigh - levelLow)).toInt()
            }
        }
        return MAX
    }

    /** The total level at which [tier] unlocks. */
    fun totalLevelFor(tier: Int): Int = KNOTS[tier].first
}
