package org.rsmod.content.custom.leagues

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.cluechest.ClueTier
import org.rsmod.content.custom.cluechest.configs.ClueChestObjs
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.relics.DodgyDealsXp
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicPerkSource
import org.rsmod.content.custom.leagues.relics.effects.ClueCompassScript
import org.rsmod.content.custom.leagues.relics.effects.ClueKeyRewards
import org.rsmod.events.EventBus
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Clue Compass's keys and compass, and Dodgy Deals' additions. The bonus key rolls are driven
 * through a [ClueKeyRewards] built with a fixed [GameRandom], since the injected one cannot be told
 * to win.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ClueCompassTest {
    class Deps
    @Inject
    constructor(
        val eventBus: EventBus,
        val objTypes: ObjTypeList,
        val objRepo: ObjRepository,
        val objRegistry: ObjRegistry,
    )

    private val perks = Perks(setOf(RelicPerkSource()))

    @Test
    fun `combat levels choose the kill key's tier`() {
        val bands =
            mapOf(
                2 to ClueTier.Beginner,
                19 to ClueTier.Beginner,
                20 to ClueTier.Easy,
                49 to ClueTier.Easy,
                50 to ClueTier.Medium,
                99 to ClueTier.Medium,
                100 to ClueTier.Hard,
                199 to ClueTier.Hard,
                200 to ClueTier.Elite,
                1000 to ClueTier.Elite,
            )
        for ((level, tier) in bands) {
            assertEquals(tier, ClueKeyRewards.tierForCombatLevel(level), "Combat level $level")
        }
    }

    @Test
    fun `thieving levels choose the pocketed key's tier`() {
        val bands =
            mapOf(
                1 to ClueTier.Beginner,
                24 to ClueTier.Beginner,
                25 to ClueTier.Easy,
                39 to ClueTier.Easy,
                40 to ClueTier.Medium,
                64 to ClueTier.Medium,
                65 to ClueTier.Hard,
                79 to ClueTier.Hard,
                80 to ClueTier.Elite,
            )
        for ((level, tier) in bands) {
            assertEquals(tier, ClueKeyRewards.tierForThievingLevel(level), "Thieving level $level")
        }
    }

    @Test
    fun GameTestState.`clue compass grants the casket perks`() = runGameTest {
        assertFalse(perks.has(player, Perk.DoubleCaskets))
        player.setVarBit(league_varbits.relic_selection[2], Relic.ClueCompass.slot)
        assertTrue(perks.has(player, Perk.CasketUpgrade))
        assertTrue(perks.has(player, Perk.DoubleCaskets))
        assertFalse(perks.has(player, Perk.ClueChestAnyTier))
    }

    @Test
    fun GameTestState.`dodgy deals doubles thieving experience only`() = runGameTest {
        val xpMods = XpModifiers(setOf(DodgyDealsXp()))
        assertEquals(1.0, xpMods.get(player, stats.thieving), 1e-9)
        player.setVarBit(league_varbits.relic_selection[1], Relic.DodgyDeals.slot)
        assertEquals(2.0, xpMods.get(player, stats.thieving), 1e-9)
        assertEquals(1.0, xpMods.get(player, stats.mining), 1e-9)
    }

    @Test
    fun GameTestState.`a won key roll puts the key in the inventory`() =
        runInjectedGameTest(Deps::class) { deps ->
            val rewards = ClueKeyRewards(FixedRolls(0), deps.objRepo)
            player.clearInv()

            assertTrue(rewards.roll(player, 10, ClueTier.Hard, player.coords))
            assertEquals(1, player.count(ClueChestObjs.key_hard))
        }

    @Test
    fun GameTestState.`a lost key roll gives nothing`() =
        runInjectedGameTest(Deps::class) { deps ->
            val rewards = ClueKeyRewards(FixedRolls(1), deps.objRepo)
            player.clearInv()

            assertFalse(rewards.roll(player, 10, ClueTier.Hard, player.coords))
            assertEquals(0, player.count(ClueChestObjs.key_hard))
        }

    @Test
    fun GameTestState.`a key won with a full inventory lands on the floor`() =
        runInjectedGameTest(Deps::class) { deps ->
            val rewards = ClueKeyRewards(FixedRolls(0), deps.objRepo)
            // A floor drop is owned by its receiver's `observerUUID`, which production sets on
            // login and the harness player lacks.
            player.observerUUID = 1L
            for (slot in player.inv.indices) {
                player.inv[slot] = InvObj(league_objs.clue_compass)
            }
            val where = player.coords

            assertTrue(rewards.roll(player, 10, ClueTier.Medium, where))
            assertEquals(0, player.count(ClueChestObjs.key_medium))
            val dropped = deps.objRegistry.findAll(where)
            assertTrue(dropped.any { it.type == ClueChestObjs.key_medium.id })
            player.clearInv()
        }

    @Test
    fun GameTestState.`the compass opens every key carried`() =
        runInjectedGameTest(Deps::class, null, ClueCompassScript::class) { deps ->
            player.setVarBit(league_varbits.relic_selection[2], Relic.ClueCompass.slot)
            carryCompassAndKeys()

            opCompass(deps)
            advance(1)

            assertEquals(0, player.count(ClueChestObjs.key_easy))
            assertEquals(0, player.count(ClueChestObjs.key_beginner))
            assertEquals(1, player.count(league_objs.clue_compass))
        }

    @Test
    fun GameTestState.`the compass opens nothing without the relic`() =
        runInjectedGameTest(Deps::class, null, ClueCompassScript::class) { deps ->
            carryCompassAndKeys()

            opCompass(deps)
            advance(1)

            assertEquals(1, player.count(ClueChestObjs.key_easy))
            assertEquals(1, player.count(ClueChestObjs.key_beginner))
        }

    private fun GameTestScope.carryCompassAndKeys() {
        player.clearInv()
        player.inv[0] = InvObj(league_objs.clue_compass)
        player.inv[1] = InvObj(ClueChestObjs.key_easy)
        player.inv[2] = InvObj(ClueChestObjs.key_beginner)
    }

    /** op1, Open-keys, on the compass in slot 0. */
    private fun GameTestScope.opCompass(deps: Deps) {
        val obj = checkNotNull(player.inv[0])
        val type = deps.objTypes[obj]
        player.withProtectedAccess {
            deps.eventBus.publish(this, HeldObjEvents.Op1(0, obj, type, player.inv))
        }
    }

    /** Every bounded roll answers [value]; ranges answer their minimum. */
    private class FixedRolls(private val value: Int) : GameRandom {
        override fun of(maxExclusive: Int): Int = value

        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive

        override fun randomDouble(): Double = 0.0
    }
}
