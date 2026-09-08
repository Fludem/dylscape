package org.rsmod.content.skills.cooking.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocContentEvents
import org.rsmod.api.player.events.interact.LocUContentEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiInterfaces
import org.rsmod.content.skills.cooking.configs.CookingContent
import org.rsmod.content.skills.cooking.configs.CookingObjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: `integration-test-suite` runs the methods of one class
 * concurrently against a single shared world, and these place locs and move inventory items.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CookingScriptTest {
    @Test
    fun GameTestState.`cook a shrimp by using it on a range`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 34)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 99
            val startXp = player.statMap.getXP(stats.cooking)

            useOnLoc(range, type, CookingObjs.raw_shrimp)
            advance(ticks = 6)

            assertEquals(1, player.count(CookingObjs.shrimp)) { "The shrimps were not cooked." }
            assertEquals(0, player.count(CookingObjs.raw_shrimp)) { "Raw shrimps survived." }
            assertTrue(player.statMap.getXP(stats.cooking) > startXp) { "No Cooking xp." }
        }

    @Test
    fun GameTestState.`a new cook can burn shrimps`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 36)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 1
            val startXp = player.statMap.getXP(stats.cooking)

            // The roll is out of 256 and the top of the range always burns below the stop level.
            random.next = 255
            useOnLoc(range, type, CookingObjs.raw_shrimp)
            advance(ticks = 6)

            assertEquals(1, player.count(CookingObjs.burnt_shrimp)) { "The shrimps did not burn." }
            assertEquals(0, player.count(CookingObjs.shrimp)) { "Burnt shrimps also cooked." }
            assertEquals(startXp, player.statMap.getXP(stats.cooking)) { "Burning gave xp." }
        }

    @Test
    fun GameTestState.`a master cook never burns shrimps`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 38)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 34

            // At or above the stop-burn level the dice are never rolled at all.
            random.next = 255
            useOnLoc(range, type, CookingObjs.raw_shrimp)
            advance(ticks = 6)

            assertEquals(1, player.count(CookingObjs.shrimp)) { "Shrimps burnt above stop level." }
        }

    @Test
    fun GameTestState.`refuse a shark below level 80`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 40)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shark)
            player.stats[stats.cooking] = 1

            useOnLoc(range, type, CookingObjs.raw_shark)
            // The level check rejects before any delay, and the harness clears its message
            // buffer every tick - so this has to be asserted without advancing.
            assertMessageSent("You need a Cooking level of 80 to cook the shark.")
            assertEquals(1, player.count(CookingObjs.raw_shark)) { "The shark was consumed." }
        }

    @Test
    fun GameTestState.`bread dough will not bake on a fire`() =
        runGameTest(Cooking::class) {
            val type = findLocType(CookingContent.cooking_fire) { it.internalName == "fire" }
            val fire = placeMapLoc(CoordGrid(0, 50, 50, 42, 33), type)
            fresh()
            player.teleport(fire.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.bread_dough)
            player.stats[stats.cooking] = 99

            useOnLoc(fire, type, CookingObjs.bread_dough)
            assertMessageSent("This needs to be cooked on a range.")
            assertEquals(1, player.count(CookingObjs.bread_dough)) { "The dough was consumed." }
        }

    @Test
    fun GameTestState.`shrimps cook on a fire`() =
        runGameTest(Cooking::class) {
            val type = findLocType(CookingContent.cooking_fire) { it.internalName == "fire" }
            val fire = placeMapLoc(CoordGrid(0, 50, 50, 44, 33), type)
            fresh()
            player.teleport(fire.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 99

            useOnLoc(fire, type, CookingObjs.raw_shrimp)
            advance(ticks = 6)

            assertEquals(1, player.count(CookingObjs.shrimp)) { "Nothing cooked on the fire." }
        }

    @Test
    fun GameTestState.`clicking a range with nothing to cook says so`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 46)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.shrimp)

            clickCook(range, type)

            assertMessageSent("You have nothing to cook.")
            assertFalse(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "A menu opened with nothing on it."
            }
        }

    @Test
    fun GameTestState.`clicking a range with food opens the make-menu`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 48)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.inv[1] = InvObj(CookingObjs.raw_beef)

            clickCook(range, type)

            assertTrue(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The make-menu was not opened."
            }
            for (slot in SkillMultiComponents.slots) {
                assertTrue(player.ui.hasEvent(slot, 1, IfEvent.PauseButton)) {
                    "$slot: no pause button at 1."
                }
                assertTrue(player.ui.hasEvent(slot, 28, IfEvent.PauseButton)) {
                    "$slot: no pause button at 28."
                }
            }
        }

    @Test
    fun GameTestState.`picking from the menu cooks that many`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 50)
            fresh()
            player.teleport(range.coords.translateX(-1))
            for (slot in 0 until 5) {
                player.inv[slot] = InvObj(CookingObjs.raw_shrimp)
            }
            player.stats[stats.cooking] = 99

            clickCook(range, type)
            // Delivered as the client would: through the button handler, with the menu still
            // open. The quantity rides in the subcomponent.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 3)
            advance(ticks = 16)

            assertEquals(3, player.count(CookingObjs.shrimp)) { "Quantity was not honoured." }
            assertEquals(2, player.count(CookingObjs.raw_shrimp)) { "Too many shrimps cooked." }
            assertFalse(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The menu stayed open after a pick."
            }
        }

    @Test
    fun GameTestState.`using several of one food asks how many`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 52)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.inv[1] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 99

            useOnLoc(range, type, CookingObjs.raw_shrimp)
            advance(ticks = 1)

            assertTrue(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "Two raw shrimps should open the how-many menu rather than cook at once."
            }
            assertEquals(2, player.count(CookingObjs.raw_shrimp)) { "Cooked before asking." }
        }

    @Test
    fun GameTestState.`a menu the player closed does not cook later`() =
        runGameTest(Cooking::class) {
            val (range, type) = placeRange(x = 54)
            fresh()
            player.teleport(range.coords.translateX(-1))
            player.inv[0] = InvObj(CookingObjs.raw_shrimp)
            player.stats[stats.cooking] = 99

            clickCook(range, type)
            player.ifClose()
            advance(ticks = 1)

            // The press only resumes a coroutine while the interface it names is still open, so a
            // late click on a menu the player walked away from has nothing to land on.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(0, player.count(CookingObjs.shrimp)) { "A stale menu still cooked." }
            assertEquals(1, player.count(CookingObjs.raw_shrimp))
        }

    private fun GameTestScope.placeRange(x: Int): Pair<BoundLocInfo, UnpackedLocType> {
        val type = findLocType(CookingContent.cooking_range) { it.internalName == "range" }
        return placeMapLoc(CoordGrid(0, 50, 50, x, 33), type) to type
    }

    /**
     * Methods share one world and one player, so a menu left open by the previous test would make
     * the player access-protected and silently swallow this test's actions.
     */
    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
    }

    /** Fires the range's `Cook` op directly, skipping the walk the packet path would need. */
    private fun GameTestScope.clickCook(loc: BoundLocInfo, type: UnpackedLocType) {
        player.withProtectedAccess {
            eventBus.publish(this, LocContentEvents.Op1(loc, loc, type, type.contentGroup))
        }
    }

    /** Uses the first [food] in the inventory on [loc], the way the client's use-on-loc does. */
    private fun GameTestScope.useOnLoc(loc: BoundLocInfo, type: UnpackedLocType, food: ObjType) {
        val objType = objTypes[food]
        val slot = player.inv.objs.indexOfFirst { it != null && it.id == food.id }
        player.withProtectedAccess {
            eventBus.publish(
                this,
                LocUContentEvents.OpContent(
                    loc = loc,
                    vis = loc,
                    type = type,
                    objType = objType,
                    invSlot = slot,
                ),
            )
        }
    }
}
