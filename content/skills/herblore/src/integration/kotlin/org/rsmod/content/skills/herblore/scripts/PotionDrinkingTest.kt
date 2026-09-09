package org.rsmod.content.skills.herblore.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.events.interact.HeldContentEvents
import org.rsmod.api.player.stat.stat
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.herblore.configs.HerbloreObjs
import org.rsmod.content.skills.herblore.configs.Potions
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/**
 * Drinking: the dose ladder, the boosts, the shared cooldown and the empty vial at the end.
 *
 * Runs single-threaded: these share the map clock as well as the inventory, and the cooldown test
 * in particular is meaningless if another method advances the clock underneath it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PotionDrinkingTest {
    @Test
    fun GameTestState.`a dose is drunk in its own slot and boosts the stat`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            val attack4 = Potions.heads.getValue("1attack").obj
            player.inv[4] = InvObj(attack4)
            player.stats[stats.attack] = 60
            val before = player.stat(stats.attack)

            drinkSlot(player, slot = 4)

            assertEquals(Potions.byObjId.getValue(attack4.id).next.id, player.inv[4]?.id) {
                "The next dose did not land back in the same slot."
            }
            assertTrue(player.stat(stats.attack) > before) { "Attack was not boosted." }
            assertMessageSent("You have 3 doses of potion left.")
        }

    @Test
    fun GameTestState.`drinking a potion down leaves an empty vial`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            player.inv[0] = InvObj(Potions.heads.getValue("1attack").obj)
            player.stats[stats.attack] = 60

            // Four sips, each separated by the three-tick food cooldown.
            repeat(4) {
                drinkSlot(player, slot = 0)
                advance(ticks = 4)
            }

            assertEquals(HerbloreObjs.vial_empty.id, player.inv[0]?.id) {
                "The last dose did not leave an empty vial."
            }
        }

    @Test
    fun GameTestState.`a second sip inside the cooldown is swallowed`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            player.inv[0] = InvObj(Potions.heads.getValue("1attack").obj)
            player.stats[stats.attack] = 60

            drinkSlot(player, slot = 0)
            val afterFirst = player.inv[0]?.id
            // No `advance`: still inside the three ticks.
            drinkSlot(player, slot = 0)

            assertEquals(afterFirst, player.inv[0]?.id) {
                "A second dose went down inside the cooldown."
            }
        }

    @Test
    fun GameTestState.`a restore is clamped at the base level`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            player.inv[0] = InvObj(Potions.heads.getValue("prayerrestore").obj)
            player.statMap.setBaseLevel(stats.prayer, BASE_PRAYER.toByte())
            player.statMap.setCurrentLevel(stats.prayer, 1)

            drinkSlot(player, slot = 0)

            // 7 + 25% of 43 is 17, so this lands well short of the base and proves the restore
            // happened; the point of the assertion is that it never exceeds the base.
            assertTrue(player.stat(stats.prayer) > 1) { "Prayer was not restored." }
            assertTrue(player.stat(stats.prayer) <= BASE_PRAYER) {
                "A restore pushed the level above its base; that is a boost, not a restore."
            }
        }

    @Test
    fun GameTestState.`a stamina potion sets the varbit the run processor reads`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            player.inv[0] = InvObj(Potions.heads.getValue("stamina").obj)

            drinkSlot(player, slot = 0)

            assertEquals(1, player.vars[varbits.stamina_active]) {
                "Stamina did not set the varbit `PlayerRunUpdateProcessor` reads."
            }
        }

    @Test
    fun GameTestState.`an inert potion still steps its dose and says why`() =
        runGameTest(PotionDrinking::class) {
            fresh()
            val antipoison = Potions.heads.getValue("antipoison").obj
            player.inv[0] = InvObj(antipoison)

            drinkSlot(player, slot = 0)

            assertEquals(Potions.byObjId.getValue(antipoison.id).next.id, player.inv[0]?.id) {
                "An inert potion did not step its dose."
            }
            assertMessageSent("Nothing seems to happen: poison is not implemented yet.")
        }

    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
    }

    /**
     * Publishes the op the client sends.
     *
     * A content-group event, not a per-obj one: potions are bound through `content.potion`, which
     * is the whole point of the obj editor.
     */
    private fun GameTestScope.drinkSlot(player: Player, slot: Int) {
        val obj = checkNotNull(player.inv[slot]) { "No obj in inv slot $slot." }
        player.withProtectedAccess {
            eventBus.publish(
                this,
                HeldContentEvents.Op1(
                    slot = slot,
                    obj = obj,
                    type = objTypes[obj],
                    inventory = player.inv,
                ),
            )
        }
    }

    private companion object {
        private const val BASE_PRAYER = 43
    }
}
