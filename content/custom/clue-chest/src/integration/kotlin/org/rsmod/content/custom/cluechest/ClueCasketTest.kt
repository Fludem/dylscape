package org.rsmod.content.custom.cluechest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.random.GameRandom
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.droptables.DropSlot
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.data.DropTableResourceLoader

/**
 * The generated casket tables, and the roll count around them.
 *
 * `caskets.toml` is written by `tools/drop-tables/caskets.py`, and [ClueCasket] quietly leaves out
 * a table the loader rejects. This is where that leniency is paid for.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ClueCasketTest {
    @Test
    fun GameTestState.`every casket table loads`() =
        runInjectedGameTest(DropTableResourceLoader::class) { loader ->
            val scripted = ScriptedRolls(1)
            val casket = ClueCasket(scripted, DropTableRoller(scripted), loader)
            assertTrue(
                casket.errors.isEmpty(),
                "caskets.toml had ${casket.errors.size} bad rows. First 10:\n" +
                    casket.errors.take(10).joinToString("\n"),
            )
            for (tier in ClueTier.entries) {
                val table = checkNotNull(casket.table(tier)) { "No table for $tier." }
                val weighted = table.tables.single()
                assertEquals(weighted.denominator, weighted.slots.sumOf(DropSlot::weight))
                // The wiki's per-roll rates sum to one roll; anything empty is rounding.
                val empty = weighted.slots.filterIsInstance<DropSlot.Empty>().sumOf { it.weight }
                assertTrue(
                    empty * 1000 < weighted.denominator,
                    "$tier rolls nothing ${empty}/${weighted.denominator} of the time.",
                )
            }
        }

    @Test
    fun GameTestState.`a casket rolls its table once per vanilla roll`() =
        runInjectedGameTest(DropTableResourceLoader::class) { loader ->
            for (tier in ClueTier.entries) {
                for (rolls in listOf(tier.rolls.first, tier.rolls.last)) {
                    // First draw is the roll count; every later draw lands on the table's first
                    // slot, which is never the empty remainder.
                    val scripted = ScriptedRolls(rolls)
                    val casket = ClueCasket(scripted, DropTableRoller(scripted), loader)
                    val drops = checkNotNull(casket.roll(tier)) { "No table for $tier." }
                    assertEquals(rolls, drops.size, "$tier with $rolls rolls.")
                }
            }
        }

    /** Answers [first] once, then zero forever. */
    private class ScriptedRolls(private val first: Int) : GameRandom {
        private var used = false

        override fun of(maxExclusive: Int): Int = next()

        override fun of(minInclusive: Int, maxInclusive: Int): Int =
            if (used) minInclusive else next()

        override fun randomDouble(): Double = 0.0

        private fun next(): Int =
            if (used) {
                0
            } else {
                used = true
                first
            }
    }
}
