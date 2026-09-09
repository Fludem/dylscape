package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.commons.scripts.FrozenScript
import org.rsmod.map.CoordGrid

class FreezeDeps @Inject constructor(val freeze: FreezeManager)

/**
 * The freeze is a timer plus a re-armed walk trigger, so the things that can go wrong are the
 * trigger not stopping the step, the timer not releasing it, and immunity not refusing a refresh.
 * Every assertion lands on the tick its message is sent: the buffer clears each tick.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FreezeManagerTest {
    @Test
    fun GameTestState.`a frozen player cannot walk and is released when the timer ends`() =
        runInjectedGameTest(FreezeDeps::class, null, FrozenScript::class) { deps ->
            player.placeAt(START)
            assertTrue(deps.freeze.freeze(player, ticks = 3))
            assertTrue(deps.freeze.isFrozen(player))
            assertMessageSent("You have been frozen!")

            player.moveGameClick(START.translateX(3))
            advance(1)
            assertEquals(START, player.coords)
            assertMessageSent("A magical force stops you from moving.")

            // Still frozen on the second tick: the trigger was re-armed.
            player.moveGameClick(START.translateX(3))
            advance(1)
            assertEquals(START, player.coords)

            advance(2)
            assertFalse(deps.freeze.isFrozen(player))
            player.moveGameClick(START.translateX(3))
            advance(2)
            assertNotEquals(START, player.coords)
        }

    @Test
    fun GameTestState.`a player is immune for a few ticks after thawing`() =
        runInjectedGameTest(FreezeDeps::class, null, FrozenScript::class) { deps ->
            player.placeAt(START)
            assertTrue(deps.freeze.freeze(player, ticks = 2))
            assertFalse(deps.freeze.freeze(player, ticks = 2), "already frozen")
            advance(2)
            assertFalse(deps.freeze.isFrozen(player))
            assertTrue(deps.freeze.isImmune(player))
            assertFalse(deps.freeze.freeze(player, ticks = 2), "immune")
            advance(FreezeManager.IMMUNITY_TICKS)
            assertFalse(deps.freeze.isImmune(player))
            assertTrue(deps.freeze.freeze(player, ticks = 2))
        }

    @Test
    fun GameTestState.`a frozen npc stays where it is`() =
        runInjectedGameTest(FreezeDeps::class, null, FrozenScript::class) { deps ->
            val type = npcTypes.values.first { it.op[1] != null && it.hitpoints > 0 }
            val npc = spawnNpc(START.translateX(2), type)
            assertTrue(deps.freeze.freeze(npc, ticks = 3))
            assertTrue(deps.freeze.isFrozen(npc))
            npc.walk(START.translateX(5))
            advance(1)
            assertEquals(START.translateX(2), npc.coords)
            advance(3)
            assertFalse(deps.freeze.isFrozen(npc))
            assertTrue(deps.freeze.isImmune(npc))
        }

    private companion object {
        /** The open square north of the Edgeville bank. */
        val START = CoordGrid(3096, 3504, 0)
    }
}
