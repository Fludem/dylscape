package org.rsmod.content.skills.thieving.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldContentEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.thieving.configs.ThievingObjs
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class CoinPouchTest {
    @Test
    fun GameTestState.`opening a stack pays a coin per pouch`() =
        runGameTest(CoinPouches::class) {
            player.clearInv()
            player.inv[0] = InvObj(ThievingObjs.pouch_citizen, count = 10)
            // One value consumed per pouch opened.
            repeat(10) { random.then = 2 }

            fireHeldOp(op = 1)

            assertEquals(0, player.inv.count(ThievingObjs.pouch_citizen)) {
                "Open-all should consume the whole stack."
            }
            // 10 pouches x 2 coins x the 5x loot multiplier.
            assertEquals(100, player.inv.count(ThievingObjs.coins))
        }

    @Test
    fun GameTestState.`open takes a single pouch from the stack`() =
        runGameTest(CoinPouches::class) {
            player.clearInv()
            player.inv[0] = InvObj(ThievingObjs.pouch_citizen, count = 10)
            random.next = 2

            fireHeldOp(op = 2)

            assertEquals(9, player.inv.count(ThievingObjs.pouch_citizen))
            assertEquals(10, player.inv.count(ThievingObjs.coins))
        }

    /**
     * The inversion of OSRS that makes the skill AFK: 28 pouches would normally *block* further
     * pickpocketing, so instead the stack opens itself and the loop carries on.
     */
    @Test
    fun GameTestState.`a full stack opens itself mid-session`() =
        runGameTest(Pickpocketing::class, CoinPouches::class) {
            val man = spawnMan()
            player.clearInv()
            player.actionDelay = -1
            player.skillAnimDelay = -1
            player.stats[stats.thieving] = 1
            player.stats[stats.hitpoints] = 50
            player.inv[0] = InvObj(ThievingObjs.pouch_citizen, count = 27)

            random.next = 0
            repeat(28) { random.then = 2 }

            player.opNpc3(man)
            advance(ticks = 1 + PICKPOCKET_DELAY)

            assertEquals(0, player.inv.count(ThievingObjs.pouch_citizen)) {
                "The 28th pouch should have triggered an auto-open."
            }
            assertTrue(player.inv.count(ThievingObjs.coins) > 0) {
                "Auto-opening the stack paid no coins."
            }
        }

    /**
     * There is no `opHeld` helper on the test scope, so the content event is published directly —
     * the same route `FiremakingTest` uses for its use-on event.
     */
    private fun GameTestScope.fireHeldOp(op: Int, slot: Int = 0) {
        val obj = checkNotNull(player.inv[slot])
        val type = objTypes[obj]
        player.withProtectedAccess {
            val event =
                when (op) {
                    1 -> HeldContentEvents.Op1(slot, obj, type, player.inv)
                    else -> HeldContentEvents.Op2(slot, obj, type, player.inv)
                }
            eventBus.publish(this, event)
        }
    }

    private fun GameTestScope.spawnMan(): Npc {
        val type: UnpackedNpcType = npcTypes.values.single { it.internalName == "man" }
        val npc = spawnNpc(TARGET_COORDS, type)
        player.teleport(TARGET_COORDS.translateX(-1))
        return npc
    }

    private companion object {
        val TARGET_COORDS = CoordGrid(0, 50, 50, 34, 31)
        const val PICKPOCKET_DELAY = 3
    }
}
