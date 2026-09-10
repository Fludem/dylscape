package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.relics.Relic

/**
 * Re-walks the cache's own relic chain and checks [Relic] against it.
 *
 * [Relic] is a hand-written mirror of what `league_relics` draws: its declaration order is the
 * clickzone numbering, its slots are what the selection varbits store, and its thresholds must
 * match the ones the client greys tiers out by. A cache upgrade that moves, renames or re-prices a
 * relic would silently hand players the wrong one; this is what makes that fail instead.
 *
 * The ids are the raw chain, as decoded in `LeagueRelicsDump`: `enum 2670[league_type]` -> league
 * struct, param 870 -> tier enum, each tier's param 877 (points) and param 878 (1-based relic
 * enum).
 */
class RelicTableTest {
    @Test
    fun GameTestState.`relic table matches the cache`() = runBasicGameTest {
        fun enumMap(id: Int): Map<Int, Int> =
            cacheTypes.enums.values
                .first { it.id == id }
                .primitiveMap
                .entries
                .associate { (it.key as Int) to (it.value as Int) }

        fun structParams(id: Int): Map<Int, Any> =
            checkNotNull(cacheTypes.structs.values.first { it.id == id }.paramMap)
                .primitiveMap
                .mapKeys { it.key }

        val league = checkNotNull(enumMap(LEAGUE_ENUM)[LEAGUE_TYPE]) { "No league for type 5" }
        val tiers = enumMap(structParams(league)[TIERS_PARAM] as Int)
        assertEquals(Relic.TIER_COUNT, tiers.size, "tier count")

        for (tier in 0 until Relic.TIER_COUNT) {
            val tierParams = structParams(checkNotNull(tiers[tier]))
            assertEquals(Relic.TIER_POINTS[tier], tierParams[POINTS_PARAM], "tier $tier points")

            val cacheRelics = enumMap(tierParams[RELICS_PARAM] as Int)
            val ourRelics = Relic.inTier(tier).associate { it.slot to it.struct.id }
            assertEquals(cacheRelics, ourRelics, "tier $tier relics (slot -> struct)")

            for (relic in Relic.inTier(tier)) {
                val name = structParams(relic.struct.id)[NAME_PARAM]
                assertEquals(relic.displayName, name, "name of $relic")
            }
        }
    }

    @Test
    fun `declaration order is tier by tier, slot by slot`() {
        val expected = Relic.entries.sortedWith(compareBy({ it.tier }, { it.slot }))
        assertEquals(expected, Relic.entries.toList())
        for ((index, relic) in Relic.entries.withIndex()) {
            assertEquals(relic, Relic.forComsub(index))
        }
    }

    private companion object {
        const val LEAGUE_ENUM = 2670
        const val LEAGUE_TYPE = 5
        const val TIERS_PARAM = 870
        const val POINTS_PARAM = 877
        const val RELICS_PARAM = 878
        const val NAME_PARAM = 879
    }
}
