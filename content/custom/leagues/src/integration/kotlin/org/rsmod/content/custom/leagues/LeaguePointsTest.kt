package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.custom.leagues.relics.LeaguePoints
import org.rsmod.content.custom.leagues.relics.Relic

class LeaguePointsTest {
    @Test
    fun `each tier opens exactly at its total level`() {
        for (tier in 0 until Relic.TIER_COUNT) {
            val level = LeaguePoints.totalLevelFor(tier)
            assertEquals(Relic.TIER_POINTS[tier], LeaguePoints.forTotalLevel(level), "tier $tier")
            if (tier > 0) {
                assertTrue(LeaguePoints.forTotalLevel(level - 1) < Relic.TIER_POINTS[tier]) {
                    "tier $tier is already open one total level early"
                }
            }
        }
    }

    @Test
    fun `points never fall as total level rises`() {
        val points = (0..MAX_TOTAL_LEVEL).map(LeaguePoints::forTotalLevel)
        assertTrue(points.zipWithNext().all { (a, b) -> a <= b })
    }

    @Test
    fun `points stop at the last tier`() {
        assertEquals(LeaguePoints.MAX, LeaguePoints.forTotalLevel(MAX_TOTAL_LEVEL))
        assertEquals(Relic.TIER_POINTS.last(), LeaguePoints.MAX)
    }

    @Test
    fun `a fresh account can pick from tier one and nothing more`() {
        val points = LeaguePoints.forTotalLevel(FRESH_TOTAL_LEVEL)
        assertTrue(points >= Relic.TIER_POINTS[0])
        assertTrue(points < Relic.TIER_POINTS[1])
    }

    private companion object {
        const val MAX_TOTAL_LEVEL = 2277
        const val FRESH_TOTAL_LEVEL = 32
    }
}
