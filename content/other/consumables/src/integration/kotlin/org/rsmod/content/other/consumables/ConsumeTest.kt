package org.rsmod.content.other.consumables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldContentEvents
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.other.consumables.scripts.Consume
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType

/**
 * The mechanics of eating.
 *
 * `SAME_THREAD` because every case moves inventory, stats and the shared map clock; run in parallel
 * the methods would advance the clock under each other and the cooldown assertions would be noise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ConsumeTest {
    /**
     * Objs by internal name.
     *
     * The module declares no per-obj `val`s - the families call `find` from a loop - so the tests
     * reach the cache directly rather than duplicating several hundred references.
     */
    private fun GameTestState.obj(name: String): ObjType =
        checkNotNull(cacheTypes.objs.values.firstOrNull { it.internalName == name }) {
            "No obj named '$name' in the cache."
        }

    @Test
    fun GameTestState.`eating heals and consumes the food`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(5)
            player.giveOne(shrimp)

            eatSlot(player, 0)

            assertEquals(8, player.stat(stats.hitpoints)) { "Shrimps should heal 3." }
            assertEquals(0, player.count(shrimp)) { "The shrimps were not consumed." }
            assertMessageSent("You eat the shrimps.")
        }

    @Test
    fun GameTestState.`eating at full health never overheals`() =
        runGameTest(Consume::class) {
            val shark = obj("shark")
            player.healthyAt()
            val base = player.statBase(stats.hitpoints)
            player.giveOne(shark)

            eatSlot(player, 0)

            assertEquals(base, player.stat(stats.hitpoints)) { "A shark healed past the base." }
            assertEquals(0, player.count(shark)) { "The shark should still be eaten." }
        }

    /**
     * The refusal is synchronous, so it is asserted without advancing: the harness clears its
     * message buffer every tick, and the second click never produces one anyway.
     */
    @Test
    fun GameTestState.`a second bite inside the cooldown is swallowed`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(1)
            player.giveOne(shrimp, slot = 0)
            player.giveOne(shrimp, slot = 1)

            eatSlot(player, 0)
            eatSlot(player, 1)

            assertEquals(4, player.stat(stats.hitpoints)) { "The second shrimps healed too." }
            assertEquals(1, player.count(shrimp)) { "The second shrimps was consumed." }
        }

    @Test
    fun GameTestState.`the cooldown lifts after three ticks`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(1)
            player.giveOne(shrimp, slot = 0)
            player.giveOne(shrimp, slot = 1)

            eatSlot(player, 0)
            advance(ticks = 3)
            eatSlot(player, 1)

            assertEquals(7, player.stat(stats.hitpoints)) { "The second shrimps did not heal." }
            assertEquals(0, player.count(shrimp))
        }

    @Test
    fun GameTestState.`eating pushes the next attack back`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(5)
            player.giveOne(shrimp)
            player.actionDelay = player.currentMapClock + 1

            eatSlot(player, 0)

            assertEquals(player.currentMapClock + 3, player.actionDelay) {
                "Eating should push the attack delay out to three ticks."
            }
        }

    @Test
    fun GameTestState.`eating never shortens a longer attack delay`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(5)
            player.giveOne(shrimp)
            val longDelay = player.currentMapClock + 6
            player.actionDelay = longDelay

            eatSlot(player, 0)

            assertEquals(longDelay, player.actionDelay) {
                "Eating handed out a free attack-speed reset."
            }
        }

    /**
     * The whole reason `foodDelay` is a separate field. A player mid-fight with a slow weapon has
     * an attack delay pending; that must not stop them eating.
     */
    @Test
    fun GameTestState.`a pending attack delay does not block eating`() =
        runGameTest(Consume::class) {
            val shrimp = obj("shrimp")
            player.hurtTo(5)
            player.giveOne(shrimp)
            player.actionDelay = player.currentMapClock + 6

            eatSlot(player, 0)

            assertEquals(8, player.stat(stats.hitpoints)) { "The weapon cooldown blocked a heal." }
            assertEquals(0, player.count(shrimp))
        }

    @Test
    fun GameTestState.`a karambwan combos with an ordinary food`() =
        runGameTest(Consume::class) {
            val karambwan = obj("tbwt_cooked_karambwan")
            val shark = obj("shark")
            player.hurtTo(1)
            player.giveOne(shark, slot = 0)
            player.giveOne(karambwan, slot = 1)

            eatSlot(player, 0)
            eatSlot(player, 1)

            assertEquals(0, player.count(shark)) { "The shark was not eaten." }
            assertEquals(0, player.count(karambwan)) { "The karambwan did not combo." }
            assertEquals(1 + 20 + 18, player.stat(stats.hitpoints)) { "Both heals did not land." }
        }

    @Test
    fun GameTestState.`a second karambwan in the same tick is refused`() =
        runGameTest(Consume::class) {
            val cake = obj("cake")
            val karambwan = obj("tbwt_cooked_karambwan")
            player.hurtTo(1)
            player.giveOne(karambwan, slot = 0)
            player.giveOne(karambwan, slot = 1)

            eatSlot(player, 0)
            eatSlot(player, 1)

            assertEquals(1, player.count(karambwan)) { "Karambwans stacked with themselves." }
        }

    /**
     * The single most likely regression in the whole module: `invReplace` would drop the half-eaten
     * cake into the first free slot and shuffle the inventory under the player.
     */
    @Test
    fun GameTestState.`a cake is eaten in stages, in its own slot`() =
        runGameTest(Consume::class) {
            val cake = obj("cake")
            val cakeSlice = obj("cake_slice")
            val partialCake = obj("partial_cake")
            val shrimp = obj("shrimp")
            player.hurtTo(1)
            player.giveOne(shrimp, slot = 0)
            player.giveOne(cake, slot = 5)

            eatSlot(player, 5)

            assertEquals(partialCake.id, player.inv[5]?.id) { "The 2/3 cake left slot 5." }
            assertEquals(5, player.stat(stats.hitpoints)) { "A bite of cake should heal 4." }

            advance(ticks = 3)
            eatSlot(player, 5)
            assertEquals(cakeSlice.id, player.inv[5]?.id) { "The slice left slot 5." }

            advance(ticks = 3)
            eatSlot(player, 5)
            assertNull(player.inv[5]) { "The last bite should leave the slot empty." }
            assertEquals(13, player.stat(stats.hitpoints)) { "Three bites should heal 12." }
        }

    @Test
    fun GameTestState.`an anglerfish heals above the base level`() =
        runGameTest(Consume::class) {
            val anglerfish = obj("anglerfish")
            player.healthyAt()
            val base = player.statBase(stats.hitpoints)
            player.giveOne(anglerfish)

            eatSlot(player, 0)

            assertTrue(player.stat(stats.hitpoints) > base) {
                "Anglerfish did not overheal: ${player.stat(stats.hitpoints)} vs base $base."
            }
        }

    @Test
    fun GameTestState.`a jug of wine drains attack and leaves the jug`() =
        runGameTest(Consume::class) {
            val emptyJug = obj("jug_empty")
            val jugOfWine = obj("jug_wine")
            player.hurtTo(1)
            // Attack needs a real base too: `statDrain` floors at zero, so a level-1 player cannot
            // show a two-level drain at all.
            player.statMap.setBaseLevel(stats.attack, MAX_LEVEL.toByte())
            player.statMap.setCurrentLevel(stats.attack, MAX_LEVEL.toByte())
            val attack = player.stat(stats.attack)
            player.giveOne(jugOfWine, slot = 3)

            eatSlot(player, 3)

            assertEquals(12, player.stat(stats.hitpoints)) { "Wine should heal 11." }
            assertEquals(attack - 2, player.stat(stats.attack)) { "Wine should drain 2 Attack." }
            assertEquals(emptyJug.id, player.inv[3]?.id) { "The jug was not handed back." }
        }

    @Test
    fun GameTestState.`a refused food is not consumed`() =
        runGameTest(Consume::class) {
            val rottenApple = obj("rottenapples")
            player.hurtTo(5)
            player.giveOne(rottenApple)

            eatSlot(player, 0)

            assertEquals(1, player.count(rottenApple)) { "A refusal ate the item." }
            assertEquals(5, player.stat(stats.hitpoints)) { "A refusal healed." }
            assertMessageSent("You don't want to eat that.")
        }

    /**
     * Purple sweets are the stackable case. Eating one has to decrement the stack rather than clear
     * the slot, and they restore run energy rather than being worth their healing.
     */
    @Test
    fun GameTestState.`a stackable sweet decrements its stack`() =
        runGameTest(Consume::class) {
            val purpleSweets = obj("trail_sweets")
            player.hurtTo(5)
            player.runEnergy = 0
            player.inv[0] = InvObj(purpleSweets, count = 10)

            eatSlot(player, 0)

            assertEquals(9, player.count(purpleSweets)) { "The stack did not decrement by one." }
            assertNotNull(player.inv[0]) { "The whole stack was consumed." }
            assertTrue(player.runEnergy > 0) { "Purple sweets should restore run energy." }
        }

    @Test
    fun GameTestState.`a rock cake costs hitpoints`() =
        runGameTest(Consume::class) {
            val cake = obj("cake")
            val rockCake = obj("rockcake")
            player.hurtTo(10)
            player.giveOne(rockCake)

            eatSlot(player, 0)
            advance(ticks = 1)

            assertTrue(player.stat(stats.hitpoints) < 10) { "The rock cake did not bite." }
        }

    /**
     * Gives the player a real Hitpoints level and hurts them to [hitpoints].
     *
     * The base level matters as much as the current one: a fresh test player has every stat at 1,
     * and `statHeal` clamps into `current..base`, so hurting to 5 without raising the base first
     * asks it to coerce into an empty range.
     */
    private fun Player.hurtTo(hitpoints: Int, base: Int = MAX_LEVEL) {
        statMap.setBaseLevel(stats.hitpoints, base.toByte())
        statMap.setCurrentLevel(stats.hitpoints, hitpoints.toByte())
    }

    private fun Player.healthyAt(base: Int = MAX_LEVEL) = hurtTo(base, base)

    private fun Player.giveOne(obj: ObjType, slot: Int = 0) {
        inv[slot] = InvObj(obj)
    }

    /**
     * Publishes the op the client would send.
     *
     * There is no `opHeld` helper on the test scope, so the event goes out directly - the same
     * approach `PrayerScriptTest` takes. Every consumable under test carries its verb on op1, which
     * is the group binding; the op4 outliers are covered by the coverage suite instead.
     */
    private fun GameTestScope.eatSlot(player: Player, slot: Int) {
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
        private const val MAX_LEVEL = 99
    }
}
