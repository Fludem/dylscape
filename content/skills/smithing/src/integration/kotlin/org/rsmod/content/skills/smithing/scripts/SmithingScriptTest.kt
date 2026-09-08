package org.rsmod.content.skills.smithing.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.LocUContentEvents
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiInterfaces
import org.rsmod.content.skills.smithing.configs.SmeltingRecipes
import org.rsmod.content.skills.smithing.configs.SmithingComponents
import org.rsmod.content.skills.smithing.configs.SmithingContent
import org.rsmod.content.skills.smithing.configs.SmithingObjs
import org.rsmod.content.skills.smithing.configs.SmithingProductObjs
import org.rsmod.content.skills.smithing.configs.SmithingVarps
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose.
 *
 * `integration-test-suite` sets `junit.jupiter.execution.parallel.mode.default=concurrent`, so test
 * *methods* in one class run at the same time while sharing a single game world. Any test that
 * places a loc or mutates a player's inventory then races the others -- which shows up as items
 * surviving a transaction that clearly ran, or as an outright `ConcurrentModificationException`,
 * and only on some runs.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SmithingScriptTest {
    @Test
    fun GameTestState.`smelt a bronze bar from copper and tin`() =
        runGameTest(Smelting::class) {
            val type =
                findLocType(SmithingContent.smithing_furnace) { it.op.getOrNull(1) == "Smelt" }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(furnace.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(SmithingObjs.copper_ore)
            player.inv[1] = InvObj(SmithingObjs.tin_ore)
            player.stats[stats.smithing] = 1
            val startXp = player.statMap.getXP(stats.smithing)

            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    LocUContentEvents.OpContent(
                        loc = furnace,
                        vis = furnace,
                        type = type,
                        objType = cacheTypes.objs.getValue(SmithingObjs.copper_ore.id),
                        invSlot = 0,
                    ),
                )
            }
            advance(ticks = 6)

            assertEquals(1, player.count(SmithingObjs.bronze_bar)) { "No bronze bar was made." }
            assertEquals(0, player.count(SmithingObjs.copper_ore)) { "Copper was not consumed." }
            assertEquals(0, player.count(SmithingObjs.tin_ore)) { "Tin was not consumed." }
            assertTrue(player.statMap.getXP(stats.smithing) > startXp) { "No Smithing xp." }
        }

    @Test
    fun GameTestState.`refuse to smelt a bar above the player's level`() =
        runGameTest(Smelting::class) {
            val type =
                findLocType(SmithingContent.smithing_furnace) { it.op.getOrNull(1) == "Smelt" }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 36, 31), type)
            player.teleport(furnace.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(SmithingObjs.runite_ore)
            player.inv[1] = InvObj(SmithingObjs.coal, 8)
            player.stats[stats.smithing] = 1

            player.withProtectedAccess {
                eventBus.publish(
                    this,
                    LocUContentEvents.OpContent(
                        loc = furnace,
                        vis = furnace,
                        type = type,
                        objType = cacheTypes.objs.getValue(SmithingObjs.runite_ore.id),
                        invSlot = 0,
                    ),
                )
            }
            // The level check rejects before any delay, and the harness clears its message buffer
            // every tick - so this has to be asserted without advancing.
            assertMessageSent("You need a Smithing level of 85 to smelt this.")
            assertEquals(1, player.count(SmithingObjs.runite_ore)) { "Ore was consumed anyway." }
        }

    @Test
    fun GameTestState.`opening an anvil selects the carried bar`() =
        runGameTest(AnvilSmithing::class) {
            val type = findLocType(SmithingContent.smithing_anvil) { it.op.getOrNull(0) == "Smith" }
            val anvil = placeMapLoc(CoordGrid(0, 50, 50, 38, 31), type)
            player.teleport(anvil.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(SmithingObjs.bronze_bar)
            player.stats[stats.smithing] = 99

            player.opLoc1(anvil)
            advance(ticks = 1)

            // The interface draws itself from this varp alone, so getting the bar's obj id in
            // there is the entire server side of opening the menu.
            assertEquals(SmithingObjs.bronze_bar.id, player.vars[SmithingVarps.smithbars]) {
                "The smithbars varp was not set to the carried bar."
            }
            assertTrue(player.isBusy) { "Opening the anvil should leave a modal open." }
        }

    @Test
    fun GameTestState.`smith a bronze dagger`() =
        runGameTest(AnvilSmithing::class) {
            player.teleport(CoordGrid(0, 50, 50, 40, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(SmithingObjs.bronze_bar)
            player.stats[stats.smithing] = 1
            val startXp = player.statMap.getXP(stats.smithing)

            // The varp is set directly rather than by clicking the anvil, because an open modal
            // makes the player access-protected (`isBusy = isDelayed || ui.modals.isNotEmpty()`).
            // Production dispatches modal buttons through `launchLenient`, which ignores that;
            // the test harness only exposes the strict `launch`, so a click delivered while the
            // interface is genuinely open would never be handled here. Opening is covered by the
            // test above; this one covers what happens after the click.
            player.withProtectedAccess {
                vars[SmithingVarps.smithbars] = SmithingObjs.bronze_bar.id
                eventBus.publish(
                    this,
                    IfModalButton(
                        component = SmithingComponents.dagger,
                        comsub = 0,
                        obj = null,
                        op = IfButtonOp.Op1,
                    ),
                )
            }
            advance(ticks = 8)

            assertEquals(1, player.count(SmithingProductObjs.bronze_dagger)) {
                "No bronze dagger was smithed."
            }
            assertEquals(0, player.count(SmithingObjs.bronze_bar)) { "The bar was not consumed." }
            assertEquals(1, player.count(objs.hammer)) { "The hammer must not be consumed." }
            assertTrue(player.statMap.getXP(stats.smithing) > startXp) { "No Smithing xp." }
        }

    @Test
    fun GameTestState.`smith five arrowheads from one bar in a single click`() =
        runGameTest(AnvilSmithing::class) {
            player.teleport(CoordGrid(0, 50, 50, 42, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.hammer)
            player.inv[1] = InvObj(SmithingObjs.bronze_bar)
            player.stats[stats.smithing] = 99
            player.withProtectedAccess {
                vars[SmithingVarps.smithbars] = SmithingObjs.bronze_bar.id
                eventBus.publish(
                    this,
                    IfModalButton(
                        component = SmithingComponents.arrowheads,
                        comsub = 0,
                        obj = null,
                        op = IfButtonOp.Op1,
                    ),
                )
            }
            advance(ticks = 8)

            // Output quantity comes from the cache's own enum, not from anything authored here:
            // one bronze bar yields fifteen arrowheads.
            assertEquals(15, player.count(SmithingProductObjs.bronze_arrowheads)) {
                "Arrowheads did not use the cache's output quantity."
            }
        }

    @Test
    fun GameTestState.`refuse to smith without a hammer`() =
        runGameTest(AnvilSmithing::class) {
            val type = findLocType(SmithingContent.smithing_anvil) { it.op.getOrNull(0) == "Smith" }
            val anvil = placeMapLoc(CoordGrid(0, 50, 50, 44, 31), type)
            player.teleport(anvil.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(SmithingObjs.bronze_bar)
            player.stats[stats.smithing] = 99

            player.opLoc1(anvil)
            advance(ticks = 1)
            assertMessageSent("You need a hammer to work the metal with.")
        }

    @Test
    fun GameTestState.`opening a furnace shows the smelting menu`() =
        runGameTest(Smelting::class) {
            val type =
                findLocType(SmithingContent.smithing_furnace) { it.op.getOrNull(1) == "Smelt" }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 46, 31), type)
            player.teleport(furnace.coords.translateX(-1))
            player.clearInv()

            player.opLoc2(furnace)
            advance(ticks = 1)

            assertTrue(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The make-menu was not opened."
            }
            // Without this the buttons are inert: they carry no events of their own in the cache,
            // and the range has to reach every quantity because the quantity is the subcomponent.
            for (slot in SkillMultiComponents.slots) {
                assertTrue(player.ui.hasEvent(slot, 1, IfEvent.PauseButton)) {
                    "$slot: no pause button at quantity 1."
                }
                assertTrue(player.ui.hasEvent(slot, 28, IfEvent.PauseButton)) {
                    "$slot: no pause button at quantity 28."
                }
            }
        }

    @Test
    fun GameTestState.`the menu smelts as many bars as the quantity clicked`() =
        runGameTest(Smelting::class) {
            val type =
                findLocType(SmithingContent.smithing_furnace) { it.op.getOrNull(1) == "Smelt" }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 48, 31), type)
            player.teleport(furnace.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(SmithingObjs.copper_ore, 5)
            player.inv[1] = InvObj(SmithingObjs.tin_ore, 5)
            player.stats[stats.smithing] = 1

            player.opLoc2(furnace)
            advance(ticks = 1)
            // Delivered as the client would: through the button handler while the menu is open.
            // The shared make-menu script routes it back to smelting; the quantity is the
            // subcomponent.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 3)
            advance(ticks = 30)

            // Slot A is the first recipe in the table.
            assertEquals(3, player.count(SmithingObjs.bronze_bar)) {
                "The quantity in the subcomponent was not honoured."
            }
            assertEquals(2, player.count(SmithingObjs.copper_ore)) { "Too much copper was spent." }
            assertEquals(2, player.count(SmithingObjs.tin_ore)) { "Too much tin was spent." }
        }

    @Test
    fun GameTestState.`the menu refuses a bar above the player's level`() =
        runGameTest(Smelting::class) {
            val type =
                findLocType(SmithingContent.smithing_furnace) { it.op.getOrNull(1) == "Smelt" }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 50, 31), type)
            player.teleport(furnace.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(SmithingObjs.runite_ore)
            player.inv[1] = InvObj(SmithingObjs.coal, 8)
            player.stats[stats.smithing] = 1

            player.opLoc2(furnace)
            advance(ticks = 1)
            val runite =
                SmeltingRecipes.all.indexOfFirst { it.bar.id == SmithingObjs.runite_bar.id }
            player.resumePauseButton(SkillMultiComponents.slots[runite], sub = 1)
            advance(ticks = 1)

            // Every bar is listed whatever the player's level, so the refusal happens here rather
            // than by hiding the button - and it happens before any delay.
            assertMessageSent("You need a Smithing level of 85 to smelt this.")
            assertEquals(1, player.count(SmithingObjs.runite_ore)) { "Ore was consumed anyway." }
        }
}
