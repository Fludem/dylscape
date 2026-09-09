package org.rsmod.content.skills.herblore.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.skills.herblore.configs.HerbloreObjs
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/**
 * Cleaning, grinding and mixing, driven through the events the client actually sends.
 *
 * Runs single-threaded on purpose: `integration-test-suite` runs the methods of one class
 * concurrently against a single shared world, and every one of these moves inventory items.
 */
@Execution(ExecutionMode.SAME_THREAD)
class HerbloreScriptTest {
    @Test
    fun GameTestState.`clean a grimy herb in its own slot`() =
        runGameTest(HerbCleaning::class) {
            fresh()
            // Slot five, not slot zero: cleaning replaces in place, and putting it anywhere but the
            // first free slot is the only way to tell that apart from a plain add.
            player.inv[5] = InvObj(HerbloreObjs.unidentified_guam)
            player.stats[stats.herblore] = 99
            val startXp = player.statMap.getXP(stats.herblore)

            opHeld1(player, slot = 5)

            assertEquals(HerbloreObjs.guam_leaf.id, player.inv[5]?.id) {
                "The clean herb did not land back in the same slot."
            }
            assertTrue(player.statMap.getXP(stats.herblore) > startXp) { "No Herblore xp." }
        }

    @Test
    fun GameTestState.`cleaning refuses below the level requirement`() =
        runGameTest(HerbCleaning::class) {
            fresh()
            player.inv[0] = InvObj(HerbloreObjs.unidentified_torstol)
            player.stats[stats.herblore] = 1

            // No `advance` at all: the handler is synchronous, so the refusal is emitted inside
            // this call and the harness clears its message buffer on the next tick.
            opHeld1(player, slot = 0)

            assertMessageSent("You need a Herblore level of 75 to clean this herb.")
            assertEquals(1, player.count(HerbloreObjs.unidentified_torstol)) {
                "The herb was consumed anyway."
            }
        }

    @Test
    fun GameTestState.`grind a unicorn horn into dust`() =
        runGameTest(HerbGrinding::class) {
            fresh()
            player.inv[0] = InvObj(HerbloreObjs.pestle_and_mortar)
            player.inv[1] = InvObj(HerbloreObjs.unicorn_horn)
            val startXp = player.statMap.getXP(stats.herblore)

            useOnHeld(HerbloreObjs.pestle_and_mortar, HerbloreObjs.unicorn_horn)
            advance(ticks = 4)

            assertEquals(1, player.count(HerbloreObjs.unicorn_horn_dust)) { "No dust." }
            assertEquals(0, player.count(HerbloreObjs.unicorn_horn)) { "The horn survived." }
            assertEquals(startXp, player.statMap.getXP(stats.herblore)) {
                "Grinding paid experience; in live it pays none."
            }
        }

    @Test
    fun GameTestState.`a herb and a vial of water make an unfinished potion, for no xp`() =
        runGameTest(PotionMixing::class) {
            fresh()
            player.inv[0] = InvObj(HerbloreObjs.vial_water)
            player.inv[1] = InvObj(HerbloreObjs.guam_leaf)
            player.stats[stats.herblore] = 99
            val startXp = player.statMap.getXP(stats.herblore)

            useOnHeld(HerbloreObjs.vial_water, HerbloreObjs.guam_leaf)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(1, player.count(HerbloreObjs.guamvial)) { "No unfinished potion." }
            assertEquals(0, player.count(HerbloreObjs.guam_leaf)) { "The herb survived." }
            assertEquals(0, player.count(HerbloreObjs.vial_water)) { "The vial survived." }
            assertEquals(startXp, player.statMap.getXP(stats.herblore)) {
                "The unfinished step paid experience; in live the whole reward is on the mix."
            }
        }

    @Test
    fun GameTestState.`an unfinished potion and a secondary make a potion`() =
        runGameTest(PotionMixing::class) {
            fresh()
            player.inv[0] = InvObj(HerbloreObjs.guamvial)
            player.inv[1] = InvObj(HerbloreObjs.eye_of_newt)
            player.stats[stats.herblore] = 99
            val startXp = player.statMap.getXP(stats.herblore)

            useOnHeld(HerbloreObjs.guamvial, HerbloreObjs.eye_of_newt)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            advance(ticks = 6)

            assertEquals(0, player.count(HerbloreObjs.guamvial)) { "The unf survived." }
            assertEquals(0, player.count(HerbloreObjs.eye_of_newt)) { "The secondary survived." }
            assertTrue(player.statMap.getXP(stats.herblore) > startXp) { "No Herblore xp." }
        }

    @Test
    fun GameTestState.`mixing honours the quantity and stops when the ingredients run out`() =
        runGameTest(PotionMixing::class) {
            fresh()
            // Unfinished potions do not stack, so three of them means three slots.
            for (slot in 0..2) player.inv[slot] = InvObj(HerbloreObjs.guamvial)
            player.inv[3] = InvObj(HerbloreObjs.eye_of_newt, count = 10)
            player.stats[stats.herblore] = 99

            useOnHeld(HerbloreObjs.guamvial, HerbloreObjs.eye_of_newt)
            // Ask for five off three unfinished potions: three is all it can be.
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 5)
            advance(ticks = 30)

            assertEquals(0, player.count(HerbloreObjs.guamvial)) { "Unfinished potions left over." }
            assertEquals(7, player.count(HerbloreObjs.eye_of_newt)) {
                "The wrong number of secondaries was spent."
            }
        }

    @Test
    fun GameTestState.`mixing refuses below the level requirement`() =
        runGameTest(PotionMixing::class) {
            fresh()
            player.inv[0] = InvObj(HerbloreObjs.torstolvial)
            player.inv[1] = InvObj(HerbloreObjs.jangerberries)
            player.stats[stats.herblore] = 1

            useOnHeld(HerbloreObjs.torstolvial, HerbloreObjs.jangerberries)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 1)
            // Exactly one tick: the press is queued, so the refusal lands on the tick that
            // processes it and the buffer is cleared at every tick after that.
            advance(ticks = 1)

            assertEquals(1, player.count(HerbloreObjs.torstolvial)) { "The unf was spent anyway." }
            assertEquals(1, player.count(HerbloreObjs.jangerberries)) {
                "The secondary was spent anyway."
            }
        }

    @Test
    fun GameTestState.`ingredients taken during the run stop the loop`() =
        runGameTest(PotionMixing::class) {
            fresh()
            for (slot in 0..4) player.inv[slot] = InvObj(HerbloreObjs.guamvial)
            player.inv[5] = InvObj(HerbloreObjs.eye_of_newt, count = 10)
            player.stats[stats.herblore] = 99

            useOnHeld(HerbloreObjs.guamvial, HerbloreObjs.eye_of_newt)
            player.resumePauseButton(SkillMultiComponents.slot_a, sub = 5)
            advance(ticks = 7)

            // Banked mid-run: the loop re-checks after every delay rather than trusting the count
            // it started with.
            val made = player.statMap.getXP(stats.herblore)
            player.clearInv()
            advance(ticks = 20)

            assertEquals(made, player.statMap.getXP(stats.herblore)) {
                "Potions were still being made after the ingredients were taken."
            }
            assertTrue(made > 0.0) { "Nothing was made before the ingredients were taken." }
        }

    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
    }

    /** Publishes the op1 the client sends for a `Clean`. */
    private fun GameTestScope.opHeld1(player: Player, slot: Int) {
        val obj = checkNotNull(player.inv[slot]) { "No obj in inv slot $slot." }
        player.withProtectedAccess {
            eventBus.publish(
                this,
                HeldObjEvents.Op1(
                    slot = slot,
                    obj = obj,
                    type = objTypes[obj],
                    inventory = player.inv,
                ),
            )
        }
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
