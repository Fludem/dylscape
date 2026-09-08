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
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.CoordGrid

/**
 * Behaviour of the pickpocket loop.
 *
 * `SequenceRandom.randomDouble` is `queuedValue / 100.0` and `statRandom` succeeds when `rate >
 * randomDouble()`, so queuing **0** forces a success and **100** forces a failure. Values are
 * consumed in order, one per roll, plus one more for the damage roll on a failure — which is why
 * the failure tests queue two.
 *
 * Single-threaded because `integration-test-suite` runs methods concurrently against one world.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PickpocketingTest {
    @Test
    fun GameTestState.`a successful pickpocket pays a coin pouch`() =
        runGameTest(Pickpocketing::class) {
            val man = spawnMan()
            startThieving(level = 1)
            random.next = 0

            player.opNpc3(man)
            advance(ticks = 1)
            assertMessageSent("You attempt to pick the Man's pocket.")
            assertEquals(0, player.inv.count(ThievingObjs.pouch_citizen))

            val startXp = player.statMap.getXP(stats.thieving)
            advance(ticks = PICKPOCKET_DELAY)
            assertMessageSent("You pick the Man's pocket.")
            assertContains(player.inv, ThievingObjs.pouch_citizen)
            assertTrue(player.statMap.getXP(stats.thieving) > startXp) {
                "Thieving xp did not advance after a successful pickpocket."
            }
        }

    /**
     * The AFK promise, and the most important assertion in the module: one click, three pouches, no
     * further input.
     */
    @Test
    fun GameTestState.`the loop keeps stealing without a second click`() =
        runGameTest(Pickpocketing::class) {
            val man = spawnMan()
            startThieving(level = 1)
            repeat(4) { random.then = 0 }

            player.opNpc3(man)
            advance(ticks = PICKPOCKET_PERIOD * 3)

            assertEquals(3, player.inv.count(ThievingObjs.pouch_citizen)) {
                "Expected the loop to keep stealing on its own."
            }
        }

    /**
     * Pins the two things that make the stun correct: the damage lands *on the failure tick* rather
     * than being deferred (which is what would happen with a strong-queued hit while access is
     * protected), and the loop comes back by itself afterwards.
     */
    @Test
    fun GameTestState.`a failed pickpocket stuns, damages, and then resumes`() =
        runGameTest(Pickpocketing::class) {
            val man = spawnMan()
            startThieving(level = 1)
            player.stats[stats.hitpoints] = 50
            // Roll: fail. Then the damage roll. Then a success for the resumed attempt.
            random.next = 100
            random.then = 1
            repeat(3) { random.then = 0 }

            player.opNpc3(man)
            advance(ticks = 1 + PICKPOCKET_DELAY)
            assertMessageSent("You fail to pick the Man's pocket.")
            assertTrue(player.stats[stats.hitpoints] < 50) {
                "Damage did not land on the tick the pickpocket failed."
            }
            assertEquals(0, player.inv.count(ThievingObjs.pouch_citizen))

            // No further input: the stun expires and the loop picks itself back up.
            advance(ticks = STUN_TICKS + PICKPOCKET_DELAY + 1)
            assertContains(player.inv, ThievingObjs.pouch_citizen)
        }

    /** The hard half of the death guard: a stun can never be the killing blow. */
    @Test
    fun GameTestState.`a stun can never kill`() =
        runGameTest(Pickpocketing::class) {
            val man = spawnMan()
            startThieving(level = 1)
            player.stats[stats.hitpoints] = 1
            random.next = 100
            random.then = 99

            player.opNpc3(man)
            // Asserted on the very first tick: the message buffer is cleared every tick, so
            // overshooting here would lose the assertion rather than fail it.
            advance(ticks = 1)

            // The health floor refuses the attempt outright, well before any damage is rolled.
            assertMessageSent("You are too badly hurt to carry on thieving.")
            advance(ticks = PICKPOCKET_PERIOD)
            assertEquals(1, player.stats[stats.hitpoints])
        }

    @Test
    fun GameTestState.`refuse a target above the player's level`() =
        runGameTest(Pickpocketing::class) {
            val hero = spawnTarget("hero")
            startThieving(level = 79)
            random.next = 0

            player.opNpc3(hero)
            advance(ticks = 1)
            assertMessageSent("You need a Thieving level of 80 to pickpocket the Hero.")
            assertEquals(0, player.inv.count(ThievingObjs.pouch_hero))
        }

    private fun GameTestScope.spawnMan(): Npc = spawnTarget("man")

    private fun GameTestScope.spawnTarget(name: String): Npc {
        val type: UnpackedNpcType = npcTypes.values.single { it.internalName == name }
        val npc = spawnNpc(TARGET_COORDS, type)
        player.teleport(TARGET_COORDS.translateX(-1))
        return npc
    }

    private fun GameTestScope.startThieving(level: Int) {
        player.clearInv()
        player.actionDelay = -1
        player.skillAnimDelay = -1
        player.stats[stats.thieving] = level
        player.stats[stats.hitpoints] = 50
    }

    private companion object {
        val TARGET_COORDS = CoordGrid(0, 50, 50, 34, 31)

        /** Mirrors `Pickpocketing.PICKPOCKET_DELAY`. */
        const val PICKPOCKET_DELAY = 3

        /**
         * Ticks from one roll to the next. One more than the delay, because the tick *after* a roll
         * lands takes the "start a fresh attempt" branch and pushes `actionDelay` out again — the
         * same reason fishing describes its four-tick delay as "five ticks end to end".
         */
        const val PICKPOCKET_PERIOD = PICKPOCKET_DELAY + 1

        /** Mirrors the default in `ThievingTargets`. */
        const val STUN_TICKS = 3
    }
}
