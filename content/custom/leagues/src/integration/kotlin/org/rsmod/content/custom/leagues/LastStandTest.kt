package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.queues
import org.rsmod.api.config.refs.stats
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.effects.LastStandScript
import org.rsmod.game.hit.HitType

/**
 * Last Stand hangs off `PlayerHitEvents.Impact`, which the standard hit processor publishes after
 * subtracting the damage but before it checks for 0 hp. These drive a real queued hit through that
 * processor, so they prove the ordering the relic relies on rather than assuming it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class LastStandTest {
    @Test
    fun GameTestState.`a killing blow leaves last stand on one hitpoint`() =
        runGameTest(LastStandScript::class, PlayerHitScript::class) {
            player.setVarBit(league_varbits.relic_selection[7], Relic.LastStand.slot)
            player.setCurrentLevel(stats.hitpoints, 5)

            player.queueHit(delay = 1, type = HitType.Melee, damage = 50)
            advance(2)

            assertEquals(1, player.hitpoints)
            assertFalse(queues.death in player.queueList) { "death was queued anyway" }
            assertTrue(player.stat(stats.attack) > player.statBase(stats.attack) + 15) {
                "combat stats were not boosted"
            }
        }

    @Test
    fun GameTestState.`without the relic the same blow kills`() =
        runGameTest(LastStandScript::class, PlayerHitScript::class) {
            player.setCurrentLevel(stats.hitpoints, 5)

            player.queueHit(delay = 1, type = HitType.Melee, damage = 50)
            advance(2)

            assertEquals(0, player.hitpoints)
        }
}
