package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.relics.effects.SmallXpLamp

@Execution(ExecutionMode.SAME_THREAD)
class SmallXpLampTest {
    @Test
    fun `the table matches the wiki at its landmarks`() {
        assertEquals(66.3, SmallXpLamp.xp(1), 1e-9)
        assertEquals(1_218.1, SmallXpLamp.xp(50), 1e-9)
        assertEquals(8_872.1, SmallXpLamp.xp(99), 1e-9)
    }

    @Test
    fun `every level pays more than the one below it`() {
        for (level in 2..SmallXpLamp.MAX_LEVEL) {
            assertTrue(SmallXpLamp.xp(level) > SmallXpLamp.xp(level - 1)) { "level $level" }
        }
    }

    @Test
    fun `levels outside 1 to 99 clamp`() {
        assertEquals(SmallXpLamp.xp(1), SmallXpLamp.xp(0), 1e-9)
        assertEquals(SmallXpLamp.xp(99), SmallXpLamp.xp(120), 1e-9)
    }

    @Test
    fun GameTestState.`a payout is multiplied by the player's xp rate`() = runGameTest {
        player.xpRate = 16.0
        player.statAdvance(stats.agility, SmallXpLamp.xp(1))
        // 66.3 x 16 = 1,060.8, within one stored tenth of truncation.
        assertEquals(10_608.0, player.statMap.getFineXP(stats.agility).toDouble(), 1.0)
    }
}
