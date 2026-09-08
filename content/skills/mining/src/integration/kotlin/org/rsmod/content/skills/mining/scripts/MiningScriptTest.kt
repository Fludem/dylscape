package org.rsmod.content.skills.mining.scripts

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.righthand
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.configs.MiningContent
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockLevelReq
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockOre
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockRespawnTime
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockSpent
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: `integration-test-suite` sets
 * `junit.jupiter.execution.parallel.mode.default=concurrent`, so methods in one class share a
 * single game world. Tests that place locs or move items race each other otherwise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class MiningScriptTest {
    @Test
    fun GameTestState.`require a usable pickaxe`() =
        runGameTest(Mining::class) {
            val type = findLocType(MiningContent.mining_rock) { it.rockLevelReq == 1 }
            val rock = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(rock.coords.translateX(-1))
            player.clearInv()

            // Holding a pickaxe the player has no level for must read as having none at all,
            // rather than silently mining with it.
            player.righthand = InvObj(objs.crystal_pickaxe)
            player.stats[stats.mining] = 1
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessagesSent(
                "You need a pickaxe to mine this rock.",
                "You do not have a pickaxe which you have the Mining level to use.",
            )

            player.actionDelay = -1
            player.stats[stats.mining] = 99
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pickaxe at the rock.")
        }

    @Test
    fun GameTestState.`require the rock's mining level`() =
        runGameTest(Mining::class) {
            val type = findLocType(MiningContent.mining_rock) { it.rockLevelReq > 1 }
            val rock = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(rock.coords.translateX(-1))
            player.clearInv()

            player.righthand = InvObj(objs.bronze_pickaxe)
            player.stats[stats.mining] = type.rockLevelReq - 1
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You need a Mining level of ${type.rockLevelReq} to mine this rock.")

            player.stats[stats.mining] = type.rockLevelReq
            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pickaxe at the rock.")
        }

    @Test
    fun GameTestState.`mine a rock through to respawn`() =
        runGameTest(Mining::class) {
            val type = findLocType(MiningContent.mining_rock) { it.rockLevelReq == 1 }
            val rock = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            val ore = type.rockOre
            player.teleport(rock.coords.translateX(-1))
            player.clearInv()

            player.righthand = InvObj(objs.bronze_pickaxe)
            player.stats[stats.mining] = type.rockLevelReq
            val startXp = player.statMap.getXP(stats.mining)

            player.opLoc1(rock)
            advance(ticks = 1)
            assertMessageSent("You swing your pickaxe at the rock.")
            assertDoesNotContain(player.inv, ore)

            // Swings that have not landed yet must not produce ore or deplete the rock.
            advance(ticks = 1)
            assertDoesNotContain(player.inv, ore)
            assertExists(rock)

            advance(ticks = 1)
            assertDoesNotContain(player.inv, ore)
            assertExists(rock)

            random.next = 0 // Guarantee the success roll.
            advance(ticks = 1)
            assertContains(player.inv, ore)
            assertExists(rock.coords, type.rockSpent)
            assertDoesNotExist(rock)

            // Unlike trees, an ore rock always depletes on a success, so XP must have moved
            // exactly once.
            check(player.statMap.getXP(stats.mining) > startXp) {
                "Mining XP did not advance after a successful swing."
            }

            advance(ticks = type.rockRespawnTime)
            assertDoesNotExist(rock.coords, type.rockSpent)
            assertExists(rock)
        }
}
