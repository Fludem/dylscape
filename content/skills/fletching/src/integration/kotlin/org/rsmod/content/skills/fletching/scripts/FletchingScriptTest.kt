package org.rsmod.content.skills.fletching.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiInterfaces
import org.rsmod.content.skills.fletching.configs.FletchingObjs
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/**
 * Runs single-threaded on purpose: `integration-test-suite` runs the methods of one class
 * concurrently against a single shared world, and these all move inventory items.
 */
@Execution(ExecutionMode.SAME_THREAD)
class FletchingScriptTest {
    @Test
    fun GameTestState.`cut logs into arrow shafts`() =
        runGameTest(LogFletching::class) {
            fresh()
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(FletchingObjs.logs)
            player.stats[stats.fletching] = 99
            val startXp = player.statMap.getXP(stats.fletching)

            useOnHeld(objs.knife, FletchingObjs.logs)
            // Slot a is the first product the log offers, which is the shaft.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(15, player.count(FletchingObjs.arrow_shaft)) {
                "Not a full set of shafts."
            }
            assertEquals(0, player.count(FletchingObjs.logs)) { "The logs survived." }
            assertTrue(player.statMap.getXP(stats.fletching) > startXp) { "No Fletching xp." }
        }

    @Test
    fun GameTestState.`cutting honours the quantity and stops when the logs run out`() =
        runGameTest(LogFletching::class) {
            fresh()
            player.inv[0] = InvObj(objs.knife)
            for (slot in 1..3) player.inv[slot] = InvObj(FletchingObjs.logs)
            player.stats[stats.fletching] = 99

            useOnHeld(objs.knife, FletchingObjs.logs)
            // Ask for five bows off three logs: three is all it can be.
            player.resumePauseButton(SkillMultiComponents.slot_b, sub = 5)
            advance(ticks = 30)

            assertEquals(3, player.count(FletchingObjs.unstrung_shortbow)) { "Wrong bow count." }
            assertEquals(0, player.count(FletchingObjs.logs)) { "Logs were left over." }
        }

    @Test
    fun GameTestState.`a shield eats two logs per shield`() =
        runGameTest(LogFletching::class) {
            fresh()
            player.inv[0] = InvObj(objs.knife)
            for (slot in 1..5) player.inv[slot] = InvObj(FletchingObjs.oak_logs)
            player.stats[stats.fletching] = 99

            useOnHeld(objs.knife, FletchingObjs.oak_logs)
            // Slot c on the oak menu is the shield.
            player.resumePauseButton(SkillMultiComponents.slot_c, sub = 5)
            advance(ticks = 30)

            assertEquals(2, player.count(FletchingObjs.oak_shield)) { "Wrong shield count." }
            assertEquals(1, player.count(FletchingObjs.oak_logs)) { "The odd log was consumed." }
        }

    @Test
    fun GameTestState.`cutting refuses below the level requirement`() =
        runGameTest(LogFletching::class) {
            fresh()
            player.inv[0] = InvObj(objs.knife)
            player.inv[1] = InvObj(FletchingObjs.yew_logs)
            player.stats[stats.fletching] = 1

            useOnHeld(objs.knife, FletchingObjs.yew_logs)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            // Exactly one tick: the click is queued, so the refusal is emitted on the tick that
            // processes it, and the harness clears its message buffer at every tick after that.
            advance(ticks = 1)

            assertMessageSent("You need a Fletching level of 65 to make a yew shortbow (u).")
            assertEquals(1, player.count(FletchingObjs.yew_logs)) { "The logs were spent anyway." }
        }

    @Test
    fun GameTestState.`string a shortbow`() =
        runGameTest(BowStringing::class) {
            fresh()
            player.inv[0] = InvObj(FletchingObjs.unstrung_shortbow)
            player.inv[1] = InvObj(FletchingObjs.bow_string)
            player.stats[stats.fletching] = 99
            val startXp = player.statMap.getXP(stats.fletching)

            useOnHeld(FletchingObjs.unstrung_shortbow, FletchingObjs.bow_string)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(FletchingObjs.shortbow)) { "The bow was not strung." }
            assertEquals(0, player.count(FletchingObjs.unstrung_shortbow)) {
                "An unstrung bow survived."
            }
            assertEquals(0, player.count(FletchingObjs.bow_string)) { "The string survived." }
            assertTrue(player.statMap.getXP(stats.fletching) > startXp) { "No Fletching xp." }
            assertFalse(player.ui.containsModal(SkillMultiInterfaces.skillmulti)) {
                "The menu stayed open after a pick."
            }
        }

    @Test
    fun GameTestState.`feather shafts into headless arrows a batch at a time`() =
        runGameTest(ArrowFletching::class) {
            fresh()
            player.inv[0] = InvObj(FletchingObjs.arrow_shaft, count = 30)
            player.inv[1] = InvObj(FletchingObjs.feather, count = 30)
            player.stats[stats.fletching] = 99

            useOnHeld(FletchingObjs.arrow_shaft, FletchingObjs.feather)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(15, player.count(FletchingObjs.headless_arrow)) { "Not one batch." }
            assertEquals(15, player.count(FletchingObjs.arrow_shaft)) { "Wrong shafts left." }
            assertEquals(15, player.count(FletchingObjs.feather)) { "Wrong feathers left." }
        }

    @Test
    fun GameTestState.`a short final batch is still made and paid for`() =
        runGameTest(ArrowFletching::class) {
            fresh()
            player.inv[0] = InvObj(FletchingObjs.headless_arrow, count = 4)
            player.inv[1] = InvObj(FletchingObjs.bronze_arrowheads, count = 4)
            player.stats[stats.fletching] = 99
            val startXp = player.statMap.getXP(stats.fletching)

            useOnHeld(FletchingObjs.headless_arrow, FletchingObjs.bronze_arrowheads)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(4, player.count(FletchingObjs.bronze_arrow)) {
                "The short batch was dropped."
            }
            assertEquals(0, player.count(FletchingObjs.headless_arrow))
            assertTrue(player.statMap.getXP(stats.fletching) > startXp) {
                "The short batch paid nothing."
            }
        }

    @Test
    fun GameTestState.`darts come ten to a batch`() =
        runGameTest(ArrowFletching::class) {
            fresh()
            player.inv[0] = InvObj(FletchingObjs.bronze_dart_tip, count = 25)
            player.inv[1] = InvObj(FletchingObjs.feather, count = 25)
            player.stats[stats.fletching] = 99

            useOnHeld(FletchingObjs.bronze_dart_tip, FletchingObjs.feather)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(10, player.count(FletchingObjs.bronze_dart)) {
                "Darts are not batched by ten."
            }
        }

    @Test
    fun GameTestState.`fit limbs to a stock`() =
        runGameTest(CrossbowFletching::class) {
            fresh()
            player.inv[0] = InvObj(FletchingObjs.limbs_bronze)
            player.inv[1] = InvObj(FletchingObjs.stock_wood)
            player.stats[stats.fletching] = 99

            useOnHeld(FletchingObjs.limbs_bronze, FletchingObjs.stock_wood)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(FletchingObjs.unstrung_bronze_crossbow)) {
                "The crossbow was not assembled."
            }
            assertEquals(0, player.count(FletchingObjs.limbs_bronze))
            assertEquals(0, player.count(FletchingObjs.stock_wood))
        }

    @Test
    fun GameTestState.`materials taken during the delay stop the run`() =
        runGameTest(LogFletching::class) {
            fresh()
            player.inv[0] = InvObj(objs.knife)
            for (slot in 1..5) player.inv[slot] = InvObj(FletchingObjs.logs)
            player.stats[stats.fletching] = 99

            useOnHeld(objs.knife, FletchingObjs.logs)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 5)
            advance(ticks = 7)

            // Banked mid-run: the loop re-checks after every delay rather than trusting the count
            // it started with.
            val made = player.count(FletchingObjs.arrow_shaft)
            player.clearInv()
            player.inv[0] = InvObj(objs.knife)
            advance(ticks = 20)

            assertEquals(0, player.count(FletchingObjs.arrow_shaft)) {
                "Shafts appeared after the logs were gone."
            }
            assertTrue(made > 0) { "Nothing was made before the logs were taken." }
        }

    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
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
}
