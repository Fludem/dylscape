package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varps
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.CornerCutterXp
import org.rsmod.content.custom.leagues.relics.EquilibriumXp
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicPerkSource

/**
 * Checks the relic-to-[Perk] wiring. The test injector installs no plugin modules, so the `Perks`
 * and `XpModifiers` here are built from the same classes `LeaguesModule` binds.
 */
@Execution(ExecutionMode.SAME_THREAD)
class RelicPerkSourceTest {
    private val perks = Perks(setOf(RelicPerkSource()))
    private val xpMods = XpModifiers(setOf(CornerCutterXp()))

    @Test
    fun `every relic is built`() {
        val unbuilt = Relic.entries.filterNot { it.implemented }
        assertTrue(unbuilt.isEmpty()) { "Unbuilt relics: $unbuilt" }
    }

    @Test
    fun GameTestState.`power miner's skilling perks need the pickaxe on you`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[0], Relic.PowerMiner.slot)

        assertTrue(perks.has(player, Perk.EchoPickaxe))
        assertFalse(perks.has(player, Perk.MiningToBank))

        player.withProtectedAccess { invAdd(inv, league_objs.echo_pickaxe) }
        assertTrue(perks.has(player, Perk.MiningToBank))
        assertTrue(perks.has(player, Perk.RockHoldsFourOres))
        assertFalse(perks.has(player, Perk.EchoAxe))
    }

    @Test
    fun GameTestState.`no relic means no perks`() = runGameTest {
        for (perk in Perk.entries) {
            assertFalse(perks.has(player, perk)) { "$perk granted with no relic" }
        }
    }

    @Test
    fun GameTestState.`dodgy deals grants every thieving perk`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[1], Relic.DodgyDeals.slot)
        assertTrue(perks.has(player, Perk.ThievingNeverFails))
        assertTrue(perks.has(player, Perk.PickpocketCrowd))
        assertTrue(perks.has(player, Perk.StallDoubleLoot))
        assertFalse(perks.has(player, Perk.CleanAllHerbs))
    }

    @Test
    fun GameTestState.`a reloaded tier one relic grants its perks`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[3], Relic.Reloaded.slot)
        player.setVarBit(league_varbits.relic_selection_other[0], Relic.Lumberjack.slot)
        assertTrue(perks.has(player, Perk.NeverFailFire))
    }

    @Test
    fun GameTestState.`a reloaded pick goes quiet when reloaded is swapped away`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[3], Relic.GoldenGod.slot)
        player.setVarBit(league_varbits.relic_selection_other[0], Relic.Lumberjack.slot)
        assertFalse(perks.has(player, Perk.NeverFailFire))
        assertTrue(perks.has(player, Perk.GoldenAlchemy))
    }

    @Test
    fun GameTestState.`corner cutter adds a quarter to agility experience only`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[1], Relic.CornerCutter.slot)
        assertEquals(1.25, xpMods.get(player, stats.agility), 1e-9)
        assertEquals(1.0, xpMods.get(player, stats.mining), 1e-9)
    }

    @Test
    fun GameTestState.`equilibrium triples the lowest skill and doubles the lagging ones`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            val equilibrium = XpModifiers(setOf(EquilibriumXp(deps.statTypes)))
            player.setVarBit(league_varbits.relic_selection[3], Relic.Equilibrium.slot)
            for (stat in deps.statTypes.values.filterNot { it.unreleased }) {
                player.setBaseLevel(stat, 50)
            }
            player.setBaseLevel(stats.cooking, 10)
            player.setBaseLevel(stats.fishing, 30)
            player.setBaseLevel(stats.mining, 99)

            assertEquals(3.0, equilibrium.get(player, stats.cooking), 1e-9)
            assertEquals(2.0, equilibrium.get(player, stats.fishing), 1e-9)
            assertEquals(1.1, equilibrium.get(player, stats.mining), 1e-9)
        }

    @Test
    fun GameTestState.`specialist caps every special at a fifth of the bar`() = runGameTest {
        val energy = SpecialAttackEnergy(perks)
        player.setVarBit(league_varbits.relic_selection[7], Relic.Specialist.slot)
        player.setVarp(varps.sa_energy, 1000)

        energy.takeSpecialEnergy(player, 1000)
        assertEquals(800, player.vars[varps.sa_energy])
        energy.takeSpecialEnergy(player, 100)
        assertEquals(700, player.vars[varps.sa_energy])
    }
}
