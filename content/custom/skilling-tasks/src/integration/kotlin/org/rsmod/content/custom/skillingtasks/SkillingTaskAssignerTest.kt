package org.rsmod.content.custom.skillingtasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.testing.GameTestState

@Execution(ExecutionMode.SAME_THREAD)
class SkillingTaskAssignerTest {
    @Test
    fun GameTestState.`a level-99 woodcutter is only offered the top three log tiers`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            player.setBaseLevel(stats.woodcutting, 99)
            val offered = deps.assigner.candidates(player, TaskKind.Chop).getValue(TaskKind.Chop)
            assertEquals(
                setOf("chop_maple_logs", "chop_yew_logs", "chop_magic_logs"),
                offered.map { it.key }.toSet(),
            )
        }

    @Test
    fun GameTestState.`a fresh account is only offered what it can do`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            for (kind in TaskKind.entries) {
                player.setBaseLevel(kind.stat, 1)
            }
            val candidates = deps.assigner.candidates(player)
            for ((kind, rows) in candidates) {
                assertTrue(rows.all { it.level <= 1 }) { "$kind offered ${rows.map { it.key }}" }
            }
            // Herblore starts at 3, so a level-1 account gets no potion task at all.
            assertNull(candidates[TaskKind.Potion])
            assertNotNull(candidates[TaskKind.Chop])
            repeat(20) {
                val task = deps.assigner.roll(player)
                assertNotNull(task)
                assertEquals(1, task!!.level, task.key)
            }
        }

    @Test
    fun GameTestState.`the previous task is never rolled again and last-tier keeps the skill`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            player.setBaseLevel(stats.woodcutting, 99)
            repeat(30) {
                val task = deps.assigner.roll(player, TaskKind.Chop, exclude = "chop_yew_logs")
                assertNotNull(task)
                assertEquals(TaskKind.Chop, task!!.kind)
                assertTrue(task.key != "chop_yew_logs")
            }
        }

    @Test
    fun GameTestState.`amounts are 20 to 40 in fives`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            repeat(50) {
                val amount = deps.assigner.amount()
                assertTrue(amount in 20..40, "$amount")
                assertEquals(0, amount % 5)
            }
        }
}
