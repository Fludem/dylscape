package org.rsmod.content.skills.firemaking.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUContentEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.firemaking.configs.FiremakingContent
import org.rsmod.content.skills.firemaking.scripts.Firemaking.Companion.fireBurnTicks
import org.rsmod.content.skills.firemaking.scripts.Firemaking.Companion.fireRateHigh
import org.rsmod.content.skills.firemaking.scripts.Firemaking.Companion.fireRateLow
import org.rsmod.content.skills.firemaking.scripts.Firemaking.Companion.firemakingLevelReq
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: `integration-test-suite` sets
 * `junit.jupiter.execution.parallel.mode.default=concurrent`, so methods in one class share a
 * single game world. Tests that place locs or move items race each other otherwise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FiremakingTest {
    @Test
    fun GameTestState.`light a fire and consume the logs`() =
        runGameTest(Firemaking::class) {
            player.teleport(CoordGrid(0, 50, 50, 34, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.logs)
            player.inv[1] = InvObj(objs.tinderbox)
            player.stats[stats.firemaking] = 50
            val startXp = player.statMap.getXP(stats.firemaking)

            random.next = 0 // Guarantee the light succeeds on the first attempt.
            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    HeldUContentEvents.Type(
                        first = cacheTypes.objs.getValue(objs.logs.id),
                        firstSlot = 0,
                        second = cacheTypes.objs.getValue(objs.tinderbox.id),
                        secondSlot = 1,
                    ),
                )
            }
            advance(ticks = 6)

            assertEquals(0, player.count(objs.logs)) { "Logs were not consumed by a lit fire." }
            assertTrue(player.count(objs.tinderbox) == 1) { "The tinderbox must not be consumed." }
            assertTrue(player.statMap.getXP(stats.firemaking) > startXp) {
                "Firemaking XP did not advance."
            }
        }

    @Test
    fun GameTestState.`refuse logs above the player's level`() =
        runGameTest(Firemaking::class) {
            player.teleport(CoordGrid(0, 50, 50, 34, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.magic_logs)
            player.inv[1] = InvObj(objs.tinderbox)
            player.stats[stats.firemaking] = 1

            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    HeldUContentEvents.Type(
                        first = cacheTypes.objs.getValue(objs.magic_logs.id),
                        firstSlot = 0,
                        second = cacheTypes.objs.getValue(objs.tinderbox.id),
                        secondSlot = 1,
                    ),
                )
            }
            // The level check rejects synchronously, and the message buffer is cleared each
            // tick - so this must be asserted before advancing.
            assertMessageSent("You need a Firemaking level of 75 to light these logs.")
            assertEquals(1, player.count(objs.magic_logs)) { "Logs were consumed despite failing." }
        }

    @Test
    fun GameTestState.`every log is fully configured`() = runBasicGameTest {
        val logs =
            cacheTypes.objs.values.filter { it.isContentType(FiremakingContent.firemaking_logs) }
        assertTrue(logs.isNotEmpty()) { "No objs were tagged into the firemaking_logs group." }
        for (log in logs) {
            assertTrue(log.firemakingLevelReq in 1..99) {
                "'${log.internalName}' has an out-of-range level requirement."
            }
            // An inverted or zero rate pair would make the log impossible to light.
            assertTrue(log.fireRateLow in 1..log.fireRateHigh) {
                "'${log.internalName}' has a broken success range: " +
                    "${log.fireRateLow}..${log.fireRateHigh}"
            }
            assertTrue(log.fireBurnTicks > 0) { "'${log.internalName}' would burn for no time." }
        }
        assertTrue(logs.any { it.firemakingLevelReq == 1 }) { "No log is lightable at level 1." }
    }
}
