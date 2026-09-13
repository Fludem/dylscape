package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.player.events.interact.HeldEquipEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.leagues.configs.league_task_objs
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.tasks.GatherKind
import org.rsmod.content.custom.leagues.tasks.LeagueTask
import org.rsmod.content.custom.leagues.tasks.LeagueTaskScript
import org.rsmod.content.custom.leagues.tasks.LeagueTasks
import org.rsmod.content.custom.leagues.tasks.Trigger
import org.rsmod.content.interfaces.levelup.LevelUpScript
import org.rsmod.content.skills.mining.scripts.Mining
import org.rsmod.content.skills.smithing.scripts.Smithed
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.Wearpos

/**
 * One case per trigger kind, each driven by publishing the event the real content publishes. The
 * completion message and the points write land on the same tick as the event, so nothing here
 * advances the clock except the visit case, which waits for the soft timer.
 */
@Execution(ExecutionMode.SAME_THREAD)
class LeagueTaskTriggerTest {
    @Test
    fun GameTestState.`reaching a level completes the level task on the level-up event`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task {
                it is Trigger.SkillLevel && it.stat == stats.mining && it.level == 20
            }
            player.setBaseLevel(stats.mining, 19)
            eventBus.publish(LevelUpScript.StatLevelUp(player, stats.mining))
            assertFalse(deps.progress.isDone(player, task))

            player.setBaseLevel(stats.mining, 20)
            eventBus.publish(LevelUpScript.StatLevelUp(player, stats.mining))
            assertCompleted(deps, task)
        }

    @Test
    fun GameTestState.`a kill task counts every npc with the name and completes at its target`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task { it is Trigger.KillNpc && "Chicken" in it.names }
            val chicken = npcTypes.values.first { it.name == "Chicken" }
            val npc = spawnNpc(player.coords, chicken)
            repeat(task.target - 1) { eventBus.publish(NpcDeathEvents.Killed(npc, player)) }
            assertEquals(task.target - 1, deps.progress.count(player, task))
            assertFalse(deps.progress.isDone(player, task))

            eventBus.publish(NpcDeathEvents.Killed(npc, player))
            assertCompleted(deps, task)
        }

    @Test
    fun GameTestState.`a kill with no killer counts for nobody`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task { it is Trigger.KillNpc && "Chicken" in it.names }
            val chicken = npcTypes.values.first { it.name == "Chicken" }
            val npc = spawnNpc(player.coords, chicken)
            eventBus.publish(NpcDeathEvents.Killed(npc, null))
            assertEquals(0, deps.progress.count(player, task))
        }

    @Test
    fun GameTestState.`a gather task with a product ignores other products`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val any = task {
                it is Trigger.Gather && it.kind == GatherKind.Mine && it.products.isEmpty()
            }
            val iron = task {
                it is Trigger.Gather &&
                    it.kind == GatherKind.Mine &&
                    it.count == 10 &&
                    league_task_objs.iron_ore in it.products
            }
            val rock = placeMapLoc(player.coords, locTypes.values.first())
            val coal = objTypes[league_task_objs.coal]
            eventBus.publish(Mining.MinedOre(player, rock, coal))
            assertCompleted(deps, any)
            assertEquals(0, deps.progress.count(player, iron))

            val ironOre = objTypes[league_task_objs.iron_ore]
            repeat(10) { eventBus.publish(Mining.MinedOre(player, rock, ironOre)) }
            assertCompleted(deps, iron)
        }

    @Test
    fun GameTestState.`a batch advances a counter by its count`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task {
                it is Trigger.Gather && it.kind == GatherKind.Smith && it.count == 100
            }
            eventBus.publish(Smithed(player, objTypes[league_task_objs.bronze_dagger], 15))
            assertEquals(15, deps.progress.count(player, task))
        }

    @Test
    fun GameTestState.`equipping the named item completes the task, wearing nothing does not`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task { it is Trigger.Equip && league_task_objs.iron_scimitar in it.objs }
            val scimitar = objTypes[league_task_objs.iron_scimitar]
            eventBus.publish(HeldEquipEvents.WearposChange(player, Wearpos.RightHand, scimitar))
            assertFalse(deps.progress.isDone(player, task), "not worn yet")

            player.worn[Wearpos.RightHand.slot] = InvObj(scimitar)
            eventBus.publish(HeldEquipEvents.WearposChange(player, Wearpos.RightHand, scimitar))
            assertCompleted(deps, task)
        }

    @Test
    fun GameTestState.`unlocking a relic completes the any-relic task and its tier's task`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val any = task { it is Trigger.UnlockRelic && it.minTier == null }
            val tier2 = task { it is Trigger.UnlockRelic && it.minTier == 1 }
            eventBus.publish(RelicUnlocked(player, Relic.PowerMiner, null))
            assertCompleted(deps, any)
            assertFalse(deps.progress.isDone(player, tier2))

            eventBus.publish(RelicUnlocked(player, Relic.CornerCutter, null))
            assertCompleted(deps, tier2)
        }

    @Test
    fun GameTestState.`standing in a visit box completes it on the next timer`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = task { it is Trigger.VisitArea && it.box.x1 == 3200 && it.box.z1 == 3418 }
            val box = (task.trigger as Trigger.VisitArea).box
            player.placeAt(box.centre)
            advance(1)
            assertFalse(deps.progress.isDone(player, task), "login timer has not fired yet")
            advanceUntil({ deps.progress.isDone(player, task) }, timeoutTicks = 10)
            assertTrue(deps.progress.isDone(player, task))
        }

    @Test
    fun GameTestState.`a completed task is never completed twice`() =
        runInjectedGameTest(LeagueTestDeps::class, null, LeagueTaskScript::class) { deps ->
            val task = LeagueTasks.all.first { it.trigger is Trigger.TotalLevel }
            assertTrue(deps.progress.complete(player, task))
            assertFalse(deps.progress.complete(player, task))
            assertEquals(task.points, deps.progress.points(player))
        }

    private fun task(predicate: (Trigger) -> Boolean): LeagueTask =
        LeagueTasks.all.first { predicate(it.trigger) }

    private fun GameTestScope.assertCompleted(deps: LeagueTestDeps, task: LeagueTask) {
        assertTrue(deps.progress.isDone(player, task)) { "${task.name} should be complete" }
        assertMessageSent(
            "<col=00c8ff>League task completed:</col> ${task.name} " +
                "<col=00c8ff>(+${task.points} points)</col>"
        )
        assertEquals(deps.points.points(player), player.vars[league_varps.points_claimed])
    }
}
