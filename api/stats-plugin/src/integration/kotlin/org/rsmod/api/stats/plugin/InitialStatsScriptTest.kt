package org.rsmod.api.stats.plugin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent

@Execution(ExecutionMode.SAME_THREAD)
class InitialStatsScriptTest {
    private var Player.newAccount by boolVarBit(varbits.new_player_account)

    @Test
    fun GameTestState.`a new account starts at herblore 3`() =
        runGameTest(InitialStatsScript::class) {
            player.newAccount = true

            eventBus.publish(SessionStateEvent.Initialize(player))

            assertEquals(HERBLORE_START_LVL, player.statBase(stats.herblore))
            assertEquals(HERBLORE_START_LVL, player.stat(stats.herblore))
            assertEquals(HERBLORE_START_XP, player.statMap.getXP(stats.herblore))
        }

    /**
     * Herblore is topped up outside the first-login gate, unlike hitpoints: Druidic Ritual is not
     * implemented, so a character created before this existed would otherwise be locked out of the
     * skill for good.
     */
    @Test
    fun GameTestState.`an existing character is topped up to herblore 3`() =
        runGameTest(InitialStatsScript::class) {
            player.newAccount = false

            eventBus.publish(SessionStateEvent.Initialize(player))

            assertEquals(HERBLORE_START_LVL, player.statBase(stats.herblore))
        }

    @Test
    fun GameTestState.`herblore progress past the floor is left alone`() =
        runGameTest(InitialStatsScript::class) {
            player.newAccount = false
            player.statMap.setXP(stats.herblore, LEVEL_20_XP)
            player.statMap.setBaseLevel(stats.herblore, 20)
            player.statMap.setCurrentLevel(stats.herblore, 20)

            eventBus.publish(SessionStateEvent.Initialize(player))

            assertEquals(20, player.statBase(stats.herblore))
            assertEquals(LEVEL_20_XP, player.statMap.getXP(stats.herblore))
        }

    private companion object {
        const val HERBLORE_START_LVL = 3
        /** The level-3 threshold, granted outright rather than as Druidic Ritual's 250 xp. */
        const val HERBLORE_START_XP = 174
        const val LEVEL_20_XP = 4470
    }
}
