package org.rsmod.content.skills.crafting.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.LocUContentEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiInterfaces
import org.rsmod.content.skills.crafting.configs.CraftingContent
import org.rsmod.content.skills.crafting.configs.CraftingGoldComponents
import org.rsmod.content.skills.crafting.configs.CraftingInterfaces
import org.rsmod.content.skills.crafting.configs.CraftingObjs
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: `integration-test-suite` runs the methods of one class
 * concurrently against a single shared world, and these all move inventory items.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CraftingScriptTest {
    @Test
    fun GameTestState.`cut a sapphire`() =
        runGameTest(GemCutting::class) {
            fresh()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(CraftingObjs.uncut_sapphire)
            player.stats[stats.crafting] = 99
            val startXp = player.statMap.getXP(stats.crafting)

            useOnHeld(objs.chisel, CraftingObjs.uncut_sapphire)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(CraftingObjs.sapphire)) { "The gem was not cut." }
            assertEquals(0, player.count(CraftingObjs.uncut_sapphire)) { "The uncut gem survived." }
            assertTrue(player.statMap.getXP(stats.crafting) > startXp) { "No Crafting xp." }
        }

    @Test
    fun GameTestState.`a sapphire never shatters`() =
        runGameTest(GemCutting::class) {
            fresh()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(CraftingObjs.uncut_sapphire)
            player.stats[stats.crafting] = 20

            // The roll is out of 100; the top of the range would shatter anything that can.
            random.next = 99
            useOnHeld(objs.chisel, CraftingObjs.uncut_sapphire)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(CraftingObjs.sapphire)) { "A precious gem shattered." }
            assertEquals(0, player.count(CraftingObjs.crushed_gemstone))
        }

    @Test
    fun GameTestState.`a low-level opal can shatter`() =
        runGameTest(GemCutting::class) {
            fresh()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(CraftingObjs.uncut_opal)
            player.stats[stats.crafting] = 1
            val startXp = player.statMap.getXP(stats.crafting)

            // A roll of 0 is below any non-zero shatter chance, so this is the failing branch.
            random.next = 0
            useOnHeld(objs.chisel, CraftingObjs.uncut_opal)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(CraftingObjs.crushed_gemstone)) {
                "The opal did not shatter."
            }
            assertEquals(0, player.count(CraftingObjs.opal)) { "It shattered and cut at once." }
            assertEquals(startXp, player.statMap.getXP(stats.crafting)) {
                "A shattered gem paid xp."
            }
        }

    @Test
    fun GameTestState.`gem cutting refuses below the level requirement`() =
        runGameTest(GemCutting::class) {
            fresh()
            player.inv[0] = InvObj(objs.chisel)
            player.inv[1] = InvObj(CraftingObjs.uncut_diamond)
            player.stats[stats.crafting] = 1

            useOnHeld(objs.chisel, CraftingObjs.uncut_diamond)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            // Exactly one tick: the click is queued, so the refusal is emitted on the tick that
            // processes it, and the harness clears its message buffer at every tick after that.
            advance(ticks = 1)

            assertMessageSent("You need a Crafting level of 43 to cut a diamond.")
            assertEquals(1, player.count(CraftingObjs.uncut_diamond)) {
                "The gem was spent anyway."
            }
        }

    @Test
    fun GameTestState.`stitch leather gloves`() =
        runGameTest(LeatherCrafting::class) {
            fresh()
            player.inv[0] = InvObj(CraftingObjs.needle)
            player.inv[1] = InvObj(CraftingObjs.thread)
            player.inv[2] = InvObj(CraftingObjs.leather)
            player.stats[stats.crafting] = 99
            val startXp = player.statMap.getXP(stats.crafting)

            useOnHeld(CraftingObjs.needle, CraftingObjs.leather)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(CraftingObjs.leather_gloves)) {
                "The gloves were not made."
            }
            assertEquals(0, player.count(CraftingObjs.leather)) { "The leather survived." }
            assertTrue(player.statMap.getXP(stats.crafting) > startXp) { "No Crafting xp." }
            assertFalse(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The menu stayed open after a pick."
            }
        }

    @Test
    fun GameTestState.`leather crafting needs thread`() =
        runGameTest(LeatherCrafting::class) {
            fresh()
            player.inv[0] = InvObj(CraftingObjs.needle)
            player.inv[1] = InvObj(CraftingObjs.leather)
            player.stats[stats.crafting] = 99

            // The refusal happens inside the item-on-item event, which runs synchronously, so this
            // is asserted without advancing.
            useOnHeld(CraftingObjs.needle, CraftingObjs.leather)

            assertMessageSent("You need some thread to make anything out of leather.")
            assertEquals(1, player.count(CraftingObjs.leather)) { "The leather was consumed." }
        }

    @Test
    fun GameTestState.`one reel of thread stretches to five items`() =
        runGameTest(LeatherCrafting::class) {
            fresh()
            player.inv[0] = InvObj(CraftingObjs.needle)
            player.inv[1] = InvObj(CraftingObjs.thread, count = 2)
            for (slot in 2..7) player.inv[slot] = InvObj(CraftingObjs.leather)
            player.stats[stats.crafting] = 99

            useOnHeld(CraftingObjs.needle, CraftingObjs.leather)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 6)
            advance(ticks = 30)

            assertEquals(6, player.count(CraftingObjs.leather_gloves)) { "Wrong number of gloves." }
            assertEquals(1, player.count(CraftingObjs.thread)) {
                "The wrong amount of thread was spent."
            }
        }

    @Test
    fun GameTestState.`d'hide bodies cost three hides each`() =
        runGameTest(LeatherCrafting::class) {
            fresh()
            player.inv[0] = InvObj(CraftingObjs.needle)
            player.inv[1] = InvObj(CraftingObjs.thread)
            for (slot in 2..8) player.inv[slot] = InvObj(CraftingObjs.dragon_leather)
            player.stats[stats.crafting] = 99

            useOnHeld(CraftingObjs.needle, CraftingObjs.dragon_leather)
            // Slot c on the green d'hide menu is the body.
            player.resumePauseButton(SkillMultiComponents.slot_c, sub = 5)
            advance(ticks = 30)

            assertEquals(2, player.count(CraftingObjs.green_dhide_body)) { "Wrong body count." }
            assertEquals(1, player.count(CraftingObjs.dragon_leather)) {
                "The odd hide was consumed."
            }
        }

    @Test
    fun GameTestState.`string an unstrung amulet with a ball of wool`() =
        runGameTest(Spinning::class) {
            fresh()
            player.inv[0] = InvObj(CraftingObjs.ball_of_wool)
            player.inv[1] = InvObj(CraftingObjs.unstrung_sapphire_amulet)

            useOnHeld(CraftingObjs.ball_of_wool, CraftingObjs.unstrung_sapphire_amulet)

            assertEquals(1, player.count(CraftingObjs.sapphire_amulet)) {
                "The amulet was not strung."
            }
            assertEquals(0, player.count(CraftingObjs.unstrung_sapphire_amulet))
            assertEquals(0, player.count(CraftingObjs.ball_of_wool)) { "The wool survived." }
        }

    @Test
    fun GameTestState.`a tanner tans the hides the make-menu asked for`() =
        runGameTest(Tanning::class) {
            fresh()
            val tanner = spawnTanner()
            for (slot in 0 until 3) {
                player.inv[slot] = InvObj(CraftingObjs.cowhide)
            }
            player.inv[3] = InvObj(objs.coins, 100)

            player.opNpc3(tanner)
            advance(ticks = 2)
            assertTrue(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The tanner did not open the make-menu."
            }

            // Slot a is soft leather, the first row of the tanning table. Two of three hides.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 2)
            advance(ticks = 2)

            assertEquals(2, player.count(CraftingObjs.leather)) { "Wrong amount tanned." }
            assertEquals(1, player.count(CraftingObjs.cowhide)) { "Too many hides consumed." }
            assertEquals(98, player.count(objs.coins)) { "Soft leather costs one coin a hide." }
        }

    @Test
    fun GameTestState.`tanning without the coins for it is refused`() =
        runGameTest(Tanning::class) {
            fresh()
            val tanner = spawnTanner()
            player.inv[0] = InvObj(CraftingObjs.dragonhide_green)

            player.opNpc3(tanner)
            advance(ticks = 2)
            // Slot d is green d'hide, at 20 coins a hide, and the player has none.
            player.resumePauseButton(SkillMultiComponents.slot_d, sub = 1)
            advance(ticks = 1)

            assertMessageSent("You can't afford to tan that. It costs 20 coins a hide.")
            assertEquals(1, player.count(CraftingObjs.dragonhide_green)) { "The hide was taken." }
        }

    @Test
    fun GameTestState.`the gold panel casts as many as the quantity button chose`() =
        runGameTest(Jewellery::class) {
            val type = findLocType(CraftingContent.smithing_furnace) { it.op.getOrNull(1) != null }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 24, 20), type)
            player.teleport(furnace.coords.translateX(-1))
            fresh()
            // Bars do not stack, so five of them means five slots.
            for (slot in 0 until 5) player.inv[slot] = InvObj(CraftingObjs.gold_bar)
            player.inv[5] = InvObj(CraftingObjs.ring_mould)
            player.stats[stats.crafting] = 99

            useOnLoc(furnace, type, CraftingObjs.gold_bar)
            advance(ticks = 1)
            assertTrue(player.ui.containsModal(CraftingInterfaces.crafting_gold)) {
                "A gold bar on a furnace did not open the gold panel."
            }

            // Both buttons carry their op in the cache, so both arrive with no subcomponent: the
            // quantity is only known because the server mirrors whichever `make_*` was pressed.
            player.ifButton(CraftingGoldComponents.make_5)
            player.ifButton(CraftingGoldComponents.gold_ring)
            advance(ticks = 30)

            assertEquals(5, player.count(CraftingObjs.gold_ring)) { "Wrong number of rings." }
            assertEquals(0, player.count(CraftingObjs.gold_bar)) { "Bars were left over." }
        }

    @Test
    fun GameTestState.`the gold panel makes one when no quantity was chosen`() =
        runGameTest(Jewellery::class) {
            val type = findLocType(CraftingContent.smithing_furnace) { it.op.getOrNull(1) != null }
            val furnace = placeMapLoc(CoordGrid(0, 50, 50, 26, 20), type)
            player.teleport(furnace.coords.translateX(-1))
            fresh()
            for (slot in 0 until 5) player.inv[slot] = InvObj(CraftingObjs.gold_bar)
            player.inv[5] = InvObj(CraftingObjs.ring_mould)
            player.stats[stats.crafting] = 99

            useOnLoc(furnace, type, CraftingObjs.gold_bar)
            advance(ticks = 1)
            player.ifButton(CraftingGoldComponents.gold_ring)
            advance(ticks = 30)

            assertEquals(1, player.count(CraftingObjs.gold_ring)) { "The panel opens on one." }
            assertEquals(4, player.count(CraftingObjs.gold_bar)) { "Too many bars were spent." }
        }

    /** Publishes the obj-on-loc event the way the client's "use A on that" arrives. */
    private fun GameTestScope.useOnLoc(loc: BoundLocInfo, type: UnpackedLocType, obj: ObjType) {
        val slot = player.inv.objs.indexOfFirst { it != null && it.id == obj.id }
        player.withProtectedAccess {
            eventBus.publish(
                this,
                LocUContentEvents.OpType(
                    loc = loc,
                    vis = loc,
                    type = type,
                    objType = objTypes[obj],
                    invSlot = slot,
                ),
            )
        }
    }

    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
    }

    private fun GameTestScope.spawnTanner(): Npc {
        val type = npcTypes.values.single { it.internalName == "ellis_tanner" }
        val npc = spawnNpc(TANNER_COORDS, type)
        player.teleport(TANNER_COORDS.translateX(-1))
        return npc
    }

    /** Publishes the obj-on-obj event the way the client's "use A on B" arrives. */
    private fun GameTestScope.useOnHeld(first: ObjType, second: ObjType) {
        player.withProtectedAccess {
            eventBus.publish(
                this,
                HeldUEvents.Type(
                    first = objTypes[first],
                    firstSlot = player.inv.indexOfFirst { it?.id == first.id },
                    second = objTypes[second],
                    secondSlot = player.inv.indexOfFirst { it?.id == second.id },
                ),
            )
        }
    }

    private companion object {
        /** Empty ground, well clear of anything the other methods in this class touch. */
        val TANNER_COORDS = CoordGrid(0, 50, 50, 20, 20)
    }
}
