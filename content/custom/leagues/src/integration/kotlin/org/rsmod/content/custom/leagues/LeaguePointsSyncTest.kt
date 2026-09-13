package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier
import org.rsmod.content.custom.leagues.tasks.LeagueTasks

@Execution(ExecutionMode.SAME_THREAD)
class LeaguePointsSyncTest {
    @Test
    fun GameTestState.`points are the sum of completed tasks and land in every varp`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            val easy = LeagueTasks.all.first { it.tier == LeagueTaskTier.Easy }
            val hard = LeagueTasks.all.first { it.tier == LeagueTaskTier.Hard }
            deps.progress.complete(player, easy)
            deps.progress.complete(player, hard)
            deps.points.sync(player)

            val expected = easy.points + hard.points
            assertEquals(expected, deps.points.points(player))
            assertEquals(expected, player.vars[league_varps.points_claimed])
            assertEquals(expected, player.vars[league_varps.points_currency])
            assertEquals(expected, player.vars[league_varps.points_completed])
            assertEquals(2, player.vars[league_varbits.total_tasks_completed])
        }

    @Test
    fun GameTestState.`an override wins until forgotten`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            deps.progress.complete(player, LeagueTasks.all.first())
            deps.points.override(player, 4321)
            assertEquals(4321, deps.points.points(player))
            assertEquals(4321, player.vars[league_varps.points_claimed])

            deps.points.forget(player)
            deps.points.sync(player)
            assertEquals(LeagueTasks.all.first().points, player.vars[league_varps.points_claimed])
        }

    @Test
    fun GameTestState.`a reset clears every bit and the points with it`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            for (task in LeagueTasks.all.take(40)) {
                deps.progress.complete(player, task)
            }
            deps.progress.reset(player)
            deps.points.sync(player)
            assertEquals(0, deps.progress.completedCount(player))
            assertEquals(0, player.vars[league_varps.points_claimed])
        }
}
