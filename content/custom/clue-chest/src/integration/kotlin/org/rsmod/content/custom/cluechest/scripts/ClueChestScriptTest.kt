package org.rsmod.content.custom.cluechest.scripts

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.inv.LocUOpScript
import org.rsmod.api.player.events.interact.LocTDefaultEvents
import org.rsmod.api.player.interact.LocTInteractions
import org.rsmod.api.player.interact.LocUInteractions
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.cluechest.configs.ClueChestInvs
import org.rsmod.content.custom.cluechest.configs.ClueChestLocs
import org.rsmod.content.custom.cluechest.configs.ClueChestObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid

/**
 * The chest against a placed copy of the Edgeville "Loot Chest" multiloc.
 *
 * The harness has no map locs, so the chest is placed by hand; each test gets its own tile because
 * methods share one world. Using an item on the chest goes through the real two-step dispatch, as
 * `FarmingScriptTest` does: `LocTInteractions` resolves the multiloc face, then `LocUInteractions`
 * is handed what `LocUOpScript` would hand it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ClueChestScriptTest {
    class Deps
    @Inject
    constructor(
        val invTypes: InvTypeList,
        val objTypes: ObjTypeList,
        val locT: LocTInteractions,
        val locU: LocUInteractions,
    )

    @Test
    fun GameTestState.`looting with a medium key spends it on a medium casket`() =
        runInjectedGameTest(Deps::class, null, ClueChestScript::class) { deps ->
            val chest = placeMapLoc(tile(0), locTypes[ClueChestLocs.loot_chest])
            player.teleport(chest.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(ClueChestObjs.key_medium)

            player.opLoc1(chest)
            advance(ticks = 3)

            assertEquals(0, player.count(ClueChestObjs.key_medium), "The key should be spent.")
            val filled = player.reward(deps).count { it != null }
            assertTrue(filled in 1..5, "A medium casket rolls 3-5 times; reward held $filled.")
        }

    @Test
    fun GameTestState.`looting spends the highest tier key held`() =
        runInjectedGameTest(Deps::class, null, ClueChestScript::class) { deps ->
            val chest = placeMapLoc(tile(1), locTypes[ClueChestLocs.loot_chest])
            player.teleport(chest.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(ClueChestObjs.key_beginner)
            player.inv[1] = InvObj(ClueChestObjs.key_elite)
            player.inv[2] = InvObj(ClueChestObjs.key_easy)

            player.opLoc1(chest)
            advance(ticks = 3)

            assertEquals(0, player.count(ClueChestObjs.key_elite))
            assertEquals(1, player.count(ClueChestObjs.key_beginner))
            assertEquals(1, player.count(ClueChestObjs.key_easy))
            assertTrue(player.reward(deps).any { it != null })
        }

    @Test
    fun GameTestState.`looting without a key takes nothing`() =
        runInjectedGameTest(Deps::class, null, ClueChestScript::class) { deps ->
            val chest = placeMapLoc(tile(2), locTypes[ClueChestLocs.loot_chest])
            player.teleport(chest.coords.translateX(-1))
            player.clearInv()

            player.opLoc1(chest)
            advance(ticks = 3)

            assertTrue(player.reward(deps).all { it == null })
        }

    @Test
    fun GameTestState.`an elite key used on the chest opens an elite casket`() =
        runInjectedGameTest(Deps::class, null, ClueChestScript::class, LocUOpScript::class) { deps
            ->
            val chest = placeMapLoc(tile(3), locTypes[ClueChestLocs.loot_chest])
            player.teleport(chest.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(ClueChestObjs.key_elite)

            useOnChest(deps, chest, ClueChestObjs.key_elite)
            advance(ticks = 3)

            assertEquals(0, player.count(ClueChestObjs.key_elite))
            val filled = player.reward(deps).count { it != null }
            assertTrue(filled in 1..6, "An elite casket rolls 4-6 times; reward held $filled.")
        }

    @Test
    fun GameTestState.`a leftover reward is handed back before another key is spent`() =
        runInjectedGameTest(Deps::class, null, ClueChestScript::class, LocUOpScript::class) { deps
            ->
            val chest = placeMapLoc(tile(4), locTypes[ClueChestLocs.loot_chest])
            player.teleport(chest.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(ClueChestObjs.key_hard)
            val reward = player.reward(deps)
            reward[0] = InvObj(ClueChestObjs.key_medium)

            useOnChest(deps, chest, ClueChestObjs.key_hard)
            advance(ticks = 3)

            assertEquals(1, player.count(ClueChestObjs.key_hard), "The key should be kept.")
            assertEquals(1, reward.count { it != null }, "Nothing new should be rolled.")
            reward[0] = null
        }

    private fun Player.reward(deps: Deps): Inventory =
        invMap.getOrPut(deps.invTypes[ClueChestInvs.reward])

    private fun GameTestScope.useOnChest(deps: Deps, chest: BoundLocInfo, key: ObjType) {
        val obj = deps.objTypes[key]
        val click = deps.locT.opTrigger(player, chest, obj, components.inv_items, comsub = 0)
        check(click is LocTDefaultEvents.Op) { "The engine's inventory-on-loc script owns this." }
        player.withProtectedAccess {
            deps.locU.interactOp(this, click.vis, click.loc, click.type, obj, inv, invSlot = 0)
        }
    }

    /** Open grass east of Lumbridge castle, one tile apart per test. */
    private fun tile(index: Int): CoordGrid = CoordGrid(3226, 3212 + index * 3, 0)
}
