package org.rsmod.content.skills.fishing.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.fishing.configs.FishingObjs
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: `integration-test-suite` sets
 * `junit.jupiter.execution.parallel.mode.default=concurrent`, so methods in one class share a
 * single game world. Tests that spawn npcs or move items race each other otherwise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FishingScriptTest {
    @Test
    fun GameTestState.`catch a shrimp with a small net`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_49_saltfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.net)
            player.stats[stats.fishing] = 1
            val startXp = player.statMap.getXP(stats.fishing)

            player.opNpc1(spot)
            advance(ticks = 1)
            assertMessageSent("You cast out your net...")
            assertDoesNotContain(player.inv, FishingObjs.raw_shrimp)

            // The catch lands exactly four ticks later. The message buffer is cleared every tick,
            // so overshooting here would silently lose the assertion rather than fail it.
            advance(ticks = 4)
            assertMessageSent("You catch some shrimps.")
            assertContains(player.inv, FishingObjs.raw_shrimp)
            check(player.statMap.getXP(stats.fishing) > startXp) {
                "Fishing XP did not advance after a successful catch."
            }
        }

    @Test
    fun GameTestState.`refuse to fish without the right tool`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_49_saltfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.stats[stats.fishing] = 99

            // A rod is the wrong tool for op1 even though it is the right tool for op3, so this
            // also pins that the two options resolve separately.
            player.inv[0] = InvObj(FishingObjs.fishing_rod)
            player.opNpc1(spot)
            advance(ticks = 1)
            assertMessageSent("You need a small fishing net to catch these fish.")
            assertDoesNotContain(player.inv, FishingObjs.raw_shrimp)
        }

    @Test
    fun GameTestState.`refuse fish above the player's level`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_50_freshfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.fly_fishing_rod)
            player.inv[1] = InvObj(FishingObjs.feather, count = 10)
            player.stats[stats.fishing] = 19

            player.opNpc1(spot)
            advance(ticks = 1)
            assertMessageSent("You need a Fishing level of 20 to fish here.")
            assertDoesNotContain(player.inv, FishingObjs.raw_trout)
        }

    @Test
    fun GameTestState.`lure fishing burns one feather per catch`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_50_freshfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.fly_fishing_rod)
            player.inv[1] = InvObj(FishingObjs.feather, count = 10)
            player.stats[stats.fishing] = 20

            player.opNpc1(spot)
            advance(ticks = 5)
            assertMessageSent("You catch a trout.")
            assertEquals(9, player.count(FishingObjs.feather)) {
                "Exactly one feather should be spent per catch."
            }
            assertEquals(1, player.count(FishingObjs.fly_fishing_rod)) {
                "The rod must not be consumed."
            }
        }

    @Test
    fun GameTestState.`a higher level lands the better fish`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_50_freshfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.fly_fishing_rod)
            player.inv[1] = InvObj(FishingObjs.feather, count = 10)
            player.stats[stats.fishing] = 30

            // Both salmon and trout are in range now, and each is rolled in turn best-first, so a
            // player who can catch salmon must not be handed a trout when both rolls succeed.
            player.opNpc1(spot)
            advance(ticks = 5)
            assertContains(player.inv, FishingObjs.raw_salmon)
            assertDoesNotContain(player.inv, FishingObjs.raw_trout)
        }

    @Test
    fun GameTestState.`refuse to bait fish without bait`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_50_49_saltfish")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.fishing_rod)
            player.stats[stats.fishing] = 99

            player.opNpc3(spot)
            advance(ticks = 1)
            assertMessageSent("You don't have any fishing bait left.")
        }

    @Test
    fun GameTestState.`the tutorial spot yields the tutorial's shrimps`() =
        runGameTest(Fishing::class) {
            val spot = spawnSpot("0_48_48_newbiefishing")
            player.teleport(spot.coords.translateX(-1))
            player.clearInv()
            player.actionDelay = -1
            player.inv[0] = InvObj(FishingObjs.net)
            player.stats[stats.fishing] = 1

            player.opNpc1(spot)
            advance(ticks = 5)
            assertContains(player.inv, FishingObjs.newbie_raw_shrimp)
            assertDoesNotContain(player.inv, FishingObjs.raw_shrimp)
        }

    /**
     * Spots are npcs, so unlike mining's rocks they are spawned rather than placed. Every test uses
     * the same empty tile; the class is single-threaded, so they cannot collide there.
     */
    private fun GameTestScope.spawnSpot(name: String): Npc {
        val type: UnpackedNpcType = npcTypes.values.single { it.internalName == name }
        return spawnNpc(SPOT_COORDS, type)
    }

    private companion object {
        val SPOT_COORDS = CoordGrid(0, 50, 50, 34, 31)
    }
}
