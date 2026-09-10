package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.CornerCutterXp
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicPerkSource
import org.rsmod.content.custom.leagues.relics.hasRelic

/**
 * Checks the relic-to-[Perk] wiring. The test injector installs no plugin modules, so the `Perks`
 * and `XpModifiers` here are built from the same classes `LeaguesModule` binds.
 */
@Execution(ExecutionMode.SAME_THREAD)
class RelicPerkSourceTest {
    private val perks = Perks(setOf(RelicPerkSource()))
    private val xpMods = XpModifiers(setOf(CornerCutterXp()))

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
    fun GameTestState.`an unbuilt relic in the varbit grants nothing`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[1], Relic.FriendlyForager.slot)
        assertFalse(player.hasRelic(Relic.FriendlyForager))
    }

    @Test
    fun GameTestState.`corner cutter adds a quarter to agility experience only`() = runGameTest {
        player.setVarBit(league_varbits.relic_selection[1], Relic.CornerCutter.slot)
        assertEquals(1.25, xpMods.get(player, stats.agility), 1e-9)
        assertEquals(1.0, xpMods.get(player, stats.mining), 1e-9)
    }
}
