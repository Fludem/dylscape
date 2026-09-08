package org.rsmod.content.skills.thieving.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.thieving.configs.ThievingObjs
import org.rsmod.content.skills.thieving.configs.ThievingRates
import org.rsmod.content.skills.thieving.configs.ThievingStallLocs
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class StallThievingTest {
    @Test
    fun GameTestState.`stealing pays noted loot and never empties the stall`() =
        runGameTest(StallThieving::class) {
            val stall = placeBakery()
            startThieving(level = 5)
            // `random.pick` reads the sequence raw, so zero selects the first loot entry (cake).
            repeat(4) { random.then = 0 }

            val startXp = player.statMap.getXP(stats.thieving)
            player.opLoc2(stall)
            advance(ticks = 1)
            assertMessageSent("You attempt to steal from the baker's stall.")

            advance(ticks = STALL_TICKS)
            assertMessageSent("You steal from the baker's stall.")
            assertTrue(player.statMap.getXP(stats.thieving) > startXp) {
                "Thieving xp did not advance after stealing from a stall."
            }

            // Paid noted, which is what keeps the 5x haul from filling the bag. The noted obj is
            // the cert link of the product, not the product itself.
            assertEquals(0, player.inv.count(ThievingObjs.cake)) {
                "Stall loot arrived loose; it should be bank-noted."
            }
            assertEquals(ThievingRates.LOOT_RATE, player.inv.count(notedCake())) {
                "Expected a noted stack of ${ThievingRates.LOOT_RATE}."
            }

            // The whole point of the AFK stall: it is still standing, and still stealable.
            assertExists(stall)
        }

    @Test
    fun GameTestState.`the loop keeps stealing without a second click`() =
        runGameTest(StallThieving::class) {
            val stall = placeBakery()
            startThieving(level = 5)
            repeat(6) { random.then = 0 }

            player.opLoc2(stall)
            advance(ticks = STALL_PERIOD * 3)

            assertEquals(3 * ThievingRates.LOOT_RATE, player.inv.count(notedCake())) {
                "Expected the stall loop to keep going on its own."
            }
        }

    @Test
    fun GameTestState.`refuse a stall above the player's level`() =
        runGameTest(StallThieving::class) {
            val stall = placeStall(ThievingStallLocs.gem)
            startThieving(level = 74)

            player.opLoc2(stall)
            advance(ticks = 1)
            assertMessageSent("You need a Thieving level of 75 to steal from the gem stall.")
        }

    private fun GameTestScope.placeBakery(): BoundLocInfo = placeStall(ThievingStallLocs.bakery)

    /** The bank-noted form of an obj is its cert link, which is what the stall actually pays. */
    private fun GameTestScope.notedCake(): UnpackedObjType =
        checkNotNull(objTypes[objTypes[ThievingObjs.cake].certlink])

    private fun GameTestScope.placeStall(loc: LocType): BoundLocInfo {
        val type = locTypes[loc]
        val placed = placeMapLoc(STALL_COORDS, type)
        player.teleport(STALL_COORDS.translateX(-1))
        return placed
    }

    private fun GameTestScope.startThieving(level: Int) {
        player.clearInv()
        player.actionDelay = -1
        player.skillAnimDelay = -1
        player.stats[stats.thieving] = level
    }

    private companion object {
        val STALL_COORDS = CoordGrid(0, 50, 50, 34, 31)

        /** The bakery's `Stall.ticks`. */
        const val STALL_TICKS = 3

        /** One more than the delay; see `PickpocketingTest.PICKPOCKET_PERIOD` for why. */
        const val STALL_PERIOD = STALL_TICKS + 1
    }
}
