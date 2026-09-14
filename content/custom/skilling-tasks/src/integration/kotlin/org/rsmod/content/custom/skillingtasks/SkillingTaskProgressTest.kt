package org.rsmod.content.custom.skillingtasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.skillingtasks.configs.SkillingTaskObjs as skilling_task_objs
import org.rsmod.content.custom.skillingtasks.scripts.SkillingTaskProgressScript
import org.rsmod.content.skills.cooking.scripts.CookedFood
import org.rsmod.content.skills.fishing.scripts.Fishing
import org.rsmod.content.skills.smithing.configs.SmithingProductObjs
import org.rsmod.content.skills.smithing.scripts.Smithed
import org.rsmod.content.skills.woodcutting.scripts.Woodcutting

/**
 * Each case drives the script with the event the real skill publishes. Payout and messages land on
 * the same tick as the event, so nothing here advances the clock.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SkillingTaskProgressTest {
    @Test
    fun GameTestState.`only the assigned product advances the task`() =
        runInjectedGameTest(SkillingTaskTestDeps::class, null, SkillingTaskProgressScript::class) {
            deps ->
            val task = SkillingTasks.find("chop_yew_logs")!!
            deps.state.assign(player, task, 100)
            val tree = placeMapLoc(player.coords, locTypes.values.first())

            eventBus.publish(
                Woodcutting.CutLogs(player, tree, objTypes[skilling_task_objs.maple_logs])
            )
            assertEquals(0, deps.state[player].progress)

            eventBus.publish(
                Woodcutting.CutLogs(player, tree, objTypes[skilling_task_objs.yew_logs])
            )
            assertEquals(1, deps.state[player].progress)

            // A yew caught rather than cut is the wrong kind, even though it is the right obj.
            val spot = spawnNpc(player.coords, npcTypes.values.first())
            eventBus.publish(
                Fishing.CaughtFish(player, spot, objTypes[skilling_task_objs.yew_logs])
            )
            assertEquals(1, deps.state[player].progress)
        }

    @Test
    fun GameTestState.`burnt food does not count`() =
        runInjectedGameTest(SkillingTaskTestDeps::class, null, SkillingTaskProgressScript::class) {
            deps ->
            deps.state.assign(player, SkillingTasks.find("cook_shark")!!, 100)
            val raw = objTypes[skilling_task_objs.raw_shark]
            val shark = objTypes[skilling_task_objs.shark]
            eventBus.publish(CookedFood(player, raw, shark, burnt = true))
            assertEquals(0, deps.state[player].progress)
            eventBus.publish(CookedFood(player, raw, shark, burnt = false))
            assertEquals(1, deps.state[player].progress)
        }

    @Test
    fun GameTestState.`a batch advances by its count and any item of the tier counts`() =
        runInjectedGameTest(SkillingTaskTestDeps::class, null, SkillingTaskProgressScript::class) {
            deps ->
            deps.state.assign(player, SkillingTasks.find("smith_steel_items")!!, 100)
            eventBus.publish(Smithed(player, objTypes[SmithingProductObjs.steel_platebody], 5))
            eventBus.publish(Smithed(player, objTypes[SmithingProductObjs.steel_dagger], 1))
            eventBus.publish(Smithed(player, objTypes[SmithingProductObjs.iron_dagger], 1))
            assertEquals(6, deps.state[player].progress)
        }

    @Test
    fun GameTestState.`reaching the target pays out, bumps the streak and clears the task`() =
        runInjectedGameTest(SkillingTaskTestDeps::class, null, SkillingTaskProgressScript::class) {
            deps ->
            player.setBaseLevel(stats.woodcutting, 60)
            val task = SkillingTasks.find("chop_yew_logs")!!
            deps.state.assign(player, task, 100)
            val tree = placeMapLoc(player.coords, locTypes.values.first())
            val yew = objTypes[skilling_task_objs.yew_logs]
            val xpBefore = player.statMap.getFineXP(stats.woodcutting)

            repeat(99) { eventBus.publish(Woodcutting.CutLogs(player, tree, yew)) }
            assertEquals(99, deps.state[player].progress)
            assertTrue(deps.state[player].hasTask)

            eventBus.publish(Woodcutting.CutLogs(player, tree, yew))
            val tasks = deps.state[player]
            assertFalse(tasks.hasTask)
            assertNull(tasks.taskKey)
            assertEquals("chop_yew_logs", tasks.lastTaskKey)
            assertEquals(1, tasks.streak)
            assertEquals(1, tasks.completed)
            assertEquals(SkillingTaskState.pointsFor(task, 1), tasks.points)
            assertTrue(player.statMap.getFineXP(stats.woodcutting) > xpBefore, "bonus xp paid")

            // Nothing further is counted without a task.
            eventBus.publish(Woodcutting.CutLogs(player, tree, yew))
            assertEquals(0, deps.state[player].progress)
            assertTrue(player.statBase(stats.woodcutting) >= 60)
        }

    @Test
    fun GameTestState.`cancelling clears the task and the streak but keeps the points`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            deps.state.assign(player, SkillingTasks.find("chop_logs")!!, 100)
            deps.state.forceComplete(player)
            val points = deps.state[player].points
            assertTrue(points > 0)
            deps.state.assign(player, SkillingTasks.find("chop_oak_logs")!!, 100)
            deps.state.cancel(player)
            val tasks = deps.state[player]
            assertFalse(tasks.hasTask)
            assertEquals(0, tasks.streak)
            assertEquals(points, tasks.points)
            assertEquals("chop_oak_logs", tasks.lastTaskKey)
        }

    @Test
    fun GameTestState.`points are spent only when affordable`() =
        runInjectedGameTest(SkillingTaskTestDeps::class) { deps ->
            deps.state.addPoints(player, 30)
            assertFalse(deps.state.spendPoints(player, 31))
            assertTrue(deps.state.spendPoints(player, 25))
            assertEquals(5, deps.state.points(player))
        }
}
