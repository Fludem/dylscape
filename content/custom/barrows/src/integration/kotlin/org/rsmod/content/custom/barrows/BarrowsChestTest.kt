package org.rsmod.content.custom.barrows

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.barrows.configs.BarrowsRewards
import org.rsmod.content.custom.droptables.DropTableRoller

/**
 * The chest arithmetic, pinned against a scripted random rather than a live world.
 *
 * These are the numbers a player judges the minigame by, and they are easy to get subtly wrong in a
 * way no ordinary test would notice - a chest that pays out slightly too often looks fine for a
 * week and then everybody has a full set.
 *
 * An integration test rather than a plain unit test only because `BarrowsRewards` names cache
 * types, and `find` needs the type system up. The randomness is still scripted, so nothing here
 * depends on a world.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsChestTest {
    @Test
    fun GameTestState.`roll count is one per brother plus one`() = runBasicGameTest {
        assertEquals(1, BarrowsRewards.rollCount(0))
        assertEquals(4, BarrowsRewards.rollCount(3))
        assertEquals(7, BarrowsRewards.rollCount(6))
    }

    @Test
    fun GameTestState.`zero reward potential can never reach the equipment table`() =
        runBasicGameTest {
            // The draw is `random.of(space) < potential`, so at zero potential no draw can win -
            // not
            // even the smallest. A chest opened having killed nothing pays consolation only.
            val chest = chest(rolls = 0)
            assertTrue(!chest.rollsEquipment(potential = 0))
        }

    @Test
    fun GameTestState.`full reward potential reaches the equipment table once in RARE_SCALE`() =
        runBasicGameTest {
            val potential = BarrowsProgress.MAX_REWARD_POTENTIAL
            val space = potential * BarrowsRewards.RARE_SCALE

            // The last winning draw and the first losing one, either side of the boundary.
            assertTrue(chest(rolls = potential - 1).rollsEquipment(potential))
            assertTrue(!chest(rolls = potential).rollsEquipment(potential))

            // And that the boundary really is one in RARE_SCALE of the whole draw space.
            assertEquals(BarrowsRewards.RARE_SCALE, space / potential)
        }

    @Test
    fun GameTestState.`a full clear rolls seven times and every roll pays something`() =
        runBasicGameTest {
            // Every equipment draw wins, so the shape of the result is seven equipment pieces.
            val potential = BarrowsProgress.MAX_REWARD_POTENTIAL
            val scripted = ScriptedRolls(*IntArray(14) { 0 })
            val drops = BarrowsChest(scripted, DropTableRoller(scripted)).roll(6, potential)
            assertEquals(7, drops.size)
            assertTrue(drops.all { it.obj in BarrowsRewards.equipment })
        }

    @Test
    fun GameTestState.`a chest opened at zero potential still pays out once`() = runBasicGameTest {
        val scripted = ScriptedRolls(*IntArray(8) { 0 })
        val drops = BarrowsChest(scripted, DropTableRoller(scripted)).roll(0, potential = 0)
        assertEquals(1, drops.size)
        assertTrue(drops.none { it.obj in BarrowsRewards.equipment })
    }

    private fun chest(rolls: Int) =
        ScriptedRolls(*IntArray(64) { rolls }).let { BarrowsChest(it, DropTableRoller(it)) }

    private class ScriptedRolls(private vararg val values: Int) : GameRandom {
        private var index = 0

        override fun of(maxExclusive: Int): Int = next()

        override fun of(minInclusive: Int, maxInclusive: Int): Int = next()

        override fun randomDouble(): Double = 0.0

        private fun next(): Int {
            check(index < values.size) { "ScriptedRolls ran out of values." }
            return values[index++]
        }
    }
}
