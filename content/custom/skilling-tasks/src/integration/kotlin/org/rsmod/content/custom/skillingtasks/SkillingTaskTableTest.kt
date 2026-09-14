package org.rsmod.content.custom.skillingtasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.skillingtasks.configs.SkillingRewards

class SkillingTaskTableTest {
    @Test
    fun `keys are unique slugs and every row is sane`() {
        val keys = SkillingTasks.all.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "duplicate keys")
        for (task in SkillingTasks.all) {
            assertTrue(task.level in 1..99, task.key)
            assertTrue(task.xpEach > 0.0, task.key)
            assertTrue(task.products.isNotEmpty(), task.key)
            assertTrue(task.name.isNotBlank(), task.key)
        }
    }

    /** Whatever a fresh account has trained, something is on offer in every skill soon. */
    @Test
    fun `every kind has a low-level row`() {
        for (kind in TaskKind.entries) {
            val lowest = SkillingTasks.all.filter { it.kind == kind }.minOf { it.level }
            assertTrue(lowest <= 5) { "$kind's easiest task needs level $lowest" }
        }
    }

    @Test
    fun `amounts stay inside the promised range`() {
        assertEquals(20, SkillingTaskAssigner.MIN_AMOUNT)
        assertEquals(40, SkillingTaskAssigner.MAX_AMOUNT)
    }

    @Test
    fun GameTestState.`every product resolves to an obj in the cache`() = runBasicGameTest {
        for (task in SkillingTasks.all) {
            for (product in task.products) {
                val resolved = cacheTypes.objs[product]
                assertTrue(resolved.name.isNotBlank()) {
                    "${task.key}: ${product.internalNameValue}"
                }
            }
        }
    }

    @Test
    fun GameTestState.`every reward resolves and is priced`() = runBasicGameTest {
        val objs = SkillingRewards.all.map { it.obj.id }
        assertEquals(objs.size, objs.toSet().size, "an obj is on the shelf twice")
        for (reward in SkillingRewards.all) {
            val resolved = cacheTypes.objs[reward.obj]
            assertTrue(resolved.name.isNotBlank()) { reward.obj.internalNameValue.orEmpty() }
            assertTrue(reward.points > 0) { resolved.name }
            assertEquals(reward.points, SkillingRewards.pointsByObj[reward.obj.id])
        }
    }

    @Test
    fun `payout formulas grow with level and streak`() {
        val logs = SkillingTasks.find("chop_logs")!!
        val yews = SkillingTasks.find("chop_yew_logs")!!
        assertTrue(
            SkillingTaskState.bonusXp(yews, 150, 0) > SkillingTaskState.bonusXp(logs, 150, 0)
        )
        assertTrue(
            SkillingTaskState.bonusXp(yews, 150, 5) > SkillingTaskState.bonusXp(yews, 150, 0)
        )
        assertEquals(
            SkillingTaskState.bonusXp(yews, 150, 10),
            SkillingTaskState.bonusXp(yews, 150, 50),
            "streak bonus caps",
        )
        assertEquals(1, SkillingTaskState.pointsFor(logs, 1))
        assertEquals(7, SkillingTaskState.pointsFor(yews, 1))
        assertEquals(12, SkillingTaskState.pointsFor(yews, 5))
        assertEquals(17, SkillingTaskState.pointsFor(yews, 10))
    }
}
