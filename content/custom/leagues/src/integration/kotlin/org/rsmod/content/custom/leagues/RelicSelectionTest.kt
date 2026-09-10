package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.scripts.RelicScreenScript

/**
 * The pick flow, driven through the same two presses the client sends: a relic's View
 * (`clickzones`, comsub = its index) and then Confirm. Everything between them - Select, the
 * confirm popup, Cancel - is client-side and never reaches the server.
 *
 * A green run proves the server half only. Whether the expanded view draws, and which relic each
 * View press names, needs a look in the real client.
 */
@Execution(ExecutionMode.SAME_THREAD)
class RelicSelectionTest {
    @Test
    fun GameTestState.`a first pick sets the tier's varbit and hands over the relic item`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            openScreen(deps)
            pick(Relic.Lumberjack)

            assertEquals(Relic.Lumberjack.slot, selection(0))
            assertEquals(1, player.count(league_objs.echo_axe))
        }

    @Test
    fun GameTestState.`a tier stays shut below its points`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            player.setVarBit(league_varbits.relic_selection[0], Relic.PowerMiner.slot)
            openScreen(deps)
            pick(Relic.CornerCutter)

            assertEquals(0, selection(1))
        }

    @Test
    fun GameTestState.`a tier stays shut while the previous one is empty`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            deps.points.override(player, Relic.TIER_POINTS.last())
            openScreen(deps)
            pick(Relic.CornerCutter)

            assertEquals(0, selection(1))
        }

    @Test
    fun GameTestState.`with reloaded held a lower tier pick becomes the reloaded relic`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            deps.points.override(player, Relic.TIER_POINTS[3])
            pickFirstFour(Relic.Reloaded)
            openScreen(deps)
            pick(Relic.Lumberjack)

            assertEquals(Relic.PowerMiner.slot, selection(0))
            assertEquals(Relic.Lumberjack.slot, other(0))
            assertEquals(1, player.count(league_objs.echo_axe))
        }

    @Test
    fun GameTestState.`changing the reloaded pick costs the tier four price`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            deps.points.override(player, Relic.TIER_POINTS[3])
            pickFirstFour(Relic.Reloaded)
            player.setVarBit(league_varbits.relic_selection_other[0], Relic.Lumberjack.slot)
            player.withProtectedAccess { invAdd(inv, objs.coins, Relic.REPICK_COSTS[3]) }
            openScreen(deps)
            pick(Relic.FriendlyForager)

            assertEquals(0, other(0))
            assertEquals(Relic.FriendlyForager.slot, other(1))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`swapping reloaded away clears the reloaded pick`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            deps.points.override(player, Relic.TIER_POINTS[3])
            pickFirstFour(Relic.Reloaded)
            player.setVarBit(league_varbits.relic_selection_other[0], Relic.Lumberjack.slot)
            player.withProtectedAccess { invAdd(inv, objs.coins, Relic.REPICK_COSTS[3]) }
            openScreen(deps)
            pick(Relic.GoldenGod)

            assertEquals(Relic.GoldenGod.slot, selection(3))
            assertEquals(0, other(0))
        }

    /** Picks the first relic in tiers 1-3 and [tierFour] in tier 4, straight into the varbits. */
    private fun GameTestScope.pickFirstFour(tierFour: Relic) {
        for (tier in 0 until 3) {
            player.setVarBit(league_varbits.relic_selection[tier], Relic.inTier(tier)[0].slot)
        }
        player.setVarBit(league_varbits.relic_selection[3], tierFour.slot)
    }

    private fun GameTestScope.other(tier: Int): Int =
        player.vars[league_varbits.relic_selection_other[tier]]

    @Test
    fun GameTestState.`swapping a tier one relic costs its price`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            player.setVarBit(league_varbits.relic_selection[0], Relic.PowerMiner.slot)
            player.withProtectedAccess { invAdd(inv, objs.coins, Relic.REPICK_COSTS[0]) }
            openScreen(deps)
            pick(Relic.Lumberjack)

            assertEquals(Relic.Lumberjack.slot, selection(0))
            assertEquals(0, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`a swap is refused one coin short`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            val short = Relic.REPICK_COSTS[0] - 1
            player.setVarBit(league_varbits.relic_selection[0], Relic.PowerMiner.slot)
            player.withProtectedAccess { invAdd(inv, objs.coins, short) }
            openScreen(deps)
            pick(Relic.Lumberjack)

            assertEquals(Relic.PowerMiner.slot, selection(0))
            assertEquals(short, player.count(objs.coins))
        }

    @Test
    fun GameTestState.`the tier eight swap takes its price out of the bank`() =
        runInjectedGameTest(LeagueTestDeps::class, null, RelicScreenScript::class) { deps ->
            deps.points.override(player, Relic.TIER_POINTS.last())
            for (tier in 0 until Relic.TIER_COUNT) {
                player.setVarBit(league_varbits.relic_selection[tier], Relic.inTier(tier)[0].slot)
            }
            player.withProtectedAccess { invAdd(bank, objs.coins, Relic.REPICK_COSTS[7]) }
            openScreen(deps)
            pick(Relic.LastStand)

            assertEquals(Relic.LastStand.slot, selection(7))
            player.withProtectedAccess { assertEquals(0, invTotal(bank, objs.coins)) }
        }

    private fun GameTestScope.openScreen(deps: LeagueTestDeps) {
        player.withProtectedAccess { deps.screen.open(this) }
        advance(1)
    }

    private fun GameTestScope.pick(relic: Relic) {
        player.ifButton(league_components.relics_clickzones, comsub = relic.ordinal)
        advance(1)
        player.ifButton(league_components.relics_confirm_button, comsub = 0)
        advance(1)
    }

    private fun GameTestScope.selection(tier: Int): Int =
        player.vars[league_varbits.relic_selection[tier]]
}
