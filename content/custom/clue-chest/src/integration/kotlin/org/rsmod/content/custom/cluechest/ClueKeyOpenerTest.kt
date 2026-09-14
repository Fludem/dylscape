package org.rsmod.content.custom.cluechest

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.PerkSource
import org.rsmod.api.perks.Perks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.cluechest.configs.ClueChestInvs
import org.rsmod.content.custom.cluechest.configs.ClueChestObjs
import org.rsmod.content.custom.droptables.DropTableRoller
import org.rsmod.content.custom.droptables.RolledDrop
import org.rsmod.content.custom.droptables.data.DropTableResourceLoader
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList

/**
 * [ClueKeyOpener]'s perks, against an opener built by hand: the test injector binds no perk
 * sources, and a fixed [GameRandom] makes every casket roll - and the upgrade roll - repeatable, so
 * the expected reward is simply the same casket rolled directly.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ClueKeyOpenerTest {
    class Deps
    @Inject
    constructor(
        val loader: DropTableResourceLoader,
        val invTypes: InvTypeList,
        val objTypes: ObjTypeList,
        val objRepo: ObjRepository,
    )

    @Test
    fun GameTestState.`a key with no perks opens its own tier once`() =
        runInjectedGameTest(Deps::class) { deps ->
            val (opener, casket) = build(deps)
            useKey(deps, opener, ClueChestObjs.key_easy)

            assertEquals(0, player.count(ClueChestObjs.key_easy))
            assertEquals(totals(casket.roll(ClueTier.Easy)), rewardTotals(deps))
        }

    @Test
    fun GameTestState.`double caskets roll the reward twice`() =
        runInjectedGameTest(Deps::class) { deps ->
            val (opener, casket) = build(deps, Perk.DoubleCaskets)
            useKey(deps, opener, ClueChestObjs.key_easy)

            val once = totals(casket.roll(ClueTier.Easy))
            assertEquals(once.mapValues { it.value * 2 }, rewardTotals(deps))
        }

    @Test
    fun GameTestState.`a casket upgrade opens the next tier`() =
        runInjectedGameTest(Deps::class) { deps ->
            val (opener, casket) = build(deps, Perk.CasketUpgrade)
            useKey(deps, opener, ClueChestObjs.key_easy)

            assertEquals(totals(casket.roll(ClueTier.Medium)), rewardTotals(deps))
        }

    @Test
    fun GameTestState.`an elite key has no tier to upgrade to`() =
        runInjectedGameTest(Deps::class) { deps ->
            val (opener, casket) = build(deps, Perk.CasketUpgrade)
            useKey(deps, opener, ClueChestObjs.key_elite)

            assertEquals(totals(casket.roll(ClueTier.Elite)), rewardTotals(deps))
        }

    @Test
    fun GameTestState.`opening every key spends them all into one reward`() =
        runInjectedGameTest(Deps::class) { deps ->
            val (opener, casket) = build(deps)
            player.clearInv()
            player.reward(deps).clearAll()
            player.inv[0] = InvObj(ClueChestObjs.key_beginner)
            player.inv[1] = InvObj(ClueChestObjs.key_medium)
            player.inv[2] = InvObj(ClueChestObjs.key_easy)

            player.withProtectedAccess { opener.openAll(this) }

            assertEquals(0, player.count(ClueChestObjs.key_beginner))
            assertEquals(0, player.count(ClueChestObjs.key_easy))
            assertEquals(0, player.count(ClueChestObjs.key_medium))
            val expected =
                totals(
                    ClueTier.entries.take(3).flatMap {
                        checkNotNull(casket.roll(it)) { "No table for $it." }
                    }
                )
            assertEquals(expected, rewardTotals(deps))
            assertTrue(expected.isNotEmpty())
            player.reward(deps).clearAll()
        }

    private fun GameTestScope.useKey(deps: Deps, opener: ClueKeyOpener, key: ObjType) {
        player.clearInv()
        player.reward(deps).clearAll()
        player.inv[0] = InvObj(key)
        player.withProtectedAccess { opener.useKey(this, deps.objTypes[key], slot = 0) }
    }

    private fun build(deps: Deps, vararg granted: Perk): Pair<ClueKeyOpener, ClueCasket> {
        val rolls = FixedRolls()
        val casket = ClueCasket(rolls, DropTableRoller(rolls), deps.loader)
        val perks = Perks(setOf(Granted(granted.toSet())))
        val opener = ClueKeyOpener(casket, deps.invTypes, deps.objTypes, deps.objRepo, perks, rolls)
        return opener to casket
    }

    private fun GameTestScope.rewardTotals(deps: Deps): Map<Int, Int> {
        val inv = player.reward(deps)
        val totals = HashMap<Int, Int>()
        for (slot in inv.indices) {
            val obj = inv[slot] ?: continue
            totals.merge(obj.id, obj.count, Int::plus)
        }
        inv.clearAll()
        return totals
    }

    private fun totals(drops: List<RolledDrop>?): Map<Int, Int> {
        val totals = HashMap<Int, Int>()
        for (drop in checkNotNull(drops) { "A casket table did not load." }) {
            totals.merge(drop.obj.id, drop.count, Int::plus)
        }
        return totals
    }

    private fun Player.reward(deps: Deps): Inventory =
        invMap.getOrPut(deps.invTypes[ClueChestInvs.reward])

    private fun Inventory.clearAll() {
        for (slot in indices) {
            this[slot] = null
        }
    }

    private class Granted(private val granted: Set<Perk>) : PerkSource {
        override fun Player.has(perk: Perk): Boolean = perk in granted
    }

    /** Always the lowest answer: casket roll counts at their minimum, every table on slot one. */
    private class FixedRolls : GameRandom {
        override fun of(maxExclusive: Int): Int = 0

        override fun of(minInclusive: Int, maxInclusive: Int): Int = minInclusive

        override fun randomDouble(): Double = 0.0
    }
}
