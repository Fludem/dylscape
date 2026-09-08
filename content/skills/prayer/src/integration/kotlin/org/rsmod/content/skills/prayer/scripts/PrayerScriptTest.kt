package org.rsmod.content.skills.prayer.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.events.interact.HeldContentEvents
import org.rsmod.api.player.events.interact.LocUContentEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.prayer.configs.PrayerAltarLocs
import org.rsmod.content.skills.prayer.scripts.BuryBones.Companion.prayerXp
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.map.CoordGrid

/**
 * Run single-threaded. The suite defaults to running a class's methods concurrently, and the bury
 * cooldown is expressed against the shared map clock, so two of these tests racing would move the
 * clock under each other.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PrayerScriptTest {
    @Test
    fun GameTestState.`bury a bone for xp`() =
        runGameTest(BuryBones::class) {
            player.teleport(CoordGrid(0, 50, 50, 34, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.bones)
            player.actionDelay = -1
            val startXp = player.statMap.getXP(stats.prayer)

            buryInvSlot(0)

            // Burying resolves synchronously, and the capture buffer is cleared on the next tick,
            // so everything is asserted before advancing.
            assertEquals(0, player.count(objs.bones)) { "The bone was not consumed." }
            assertTrue(player.statMap.getXP(stats.prayer) > startXp) { "No prayer xp was granted." }
            assertMessagesSent("You dig a hole in the ground.", "You bury the bones.")
        }

    @Test
    fun GameTestState.`only bury one bone per cooldown`() =
        runGameTest(BuryBones::class) {
            player.teleport(CoordGrid(0, 50, 50, 34, 31))
            player.clearInv()
            player.inv[0] = InvObj(objs.bones)
            player.inv[1] = InvObj(objs.bones)
            player.actionDelay = -1

            // Two clicks in the same tick: the second lands inside the two-tick cooldown and must
            // be dropped silently rather than burying a second bone for free.
            buryInvSlot(0)
            buryInvSlot(1)
            advance(ticks = 1)
            assertEquals(1, player.count(objs.bones)) { "A second bone was buried on cooldown." }

            advance(ticks = 2)
            buryInvSlot(1)
            advance(ticks = 1)
            assertEquals(0, player.count(objs.bones)) { "The cooldown never expired." }
        }

    @Test
    fun GameTestState.`recharge prayer points at an altar`() =
        runGameTest(PrayAtAltar::class) {
            val type = locTypes[PrayerAltarLocs.altar]
            val altar = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(altar.coords.translateX(-1))
            player.setBaseLevel(stats.prayer, 43)
            player.setCurrentLevel(stats.prayer, 5)

            // One tick to run the op, then the three-tick prayer, so the closing message lands on
            // tick four and would be cleared from the capture buffer by tick five.
            player.opLoc1(altar)
            advance(ticks = 4)

            assertEquals(43, player.stats[stats.prayer]) { "Prayer points were not recharged." }
            assertMessageSent("...and recharge your prayer.")
        }

    @Test
    fun GameTestState.`refuse to recharge full prayer points`() =
        runGameTest(PrayAtAltar::class) {
            val type = locTypes[PrayerAltarLocs.altar]
            val altar = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), type)
            player.teleport(altar.coords.translateX(-1))
            player.setBaseLevel(stats.prayer, 43)
            player.setCurrentLevel(stats.prayer, 43)

            player.opLoc1(altar)
            advance(ticks = 1)
            assertMessageSent("You already have full prayer points.")
            assertEquals(43, player.stats[stats.prayer])
        }

    @Test
    fun GameTestState.`offer a bone at the chaos altar for bonus xp`() =
        runGameTest(PrayAtAltar::class) {
            val altarType = locTypes[PrayerAltarLocs.chaos_altar]
            val altar = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), altarType)
            val boneType = objTypes[objs.bones]
            player.teleport(altar.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(objs.bones)
            val startXp = player.statMap.getFineXP(stats.prayer)

            random.next = 99 // Above the 50% save threshold, so the bone is consumed.
            offerInvSlot(altar, altarType, 0)
            advance(ticks = 3)

            assertEquals(0, player.count(objs.bones)) { "The offered bone was not consumed." }
            // 3.5x, asserted loosely as "clearly more than three buries" so whatever xp rate the
            // realm runs at cannot make this flap.
            val gained = player.statMap.getFineXP(stats.prayer) - startXp
            val threeBuries = PlayerStatMap.toFineXP(boneType.prayerXp * 3).toInt()
            assertTrue(gained >= threeBuries) {
                "Offering granted $gained fine xp, no better than burying the same bone."
            }
        }

    @Test
    fun GameTestState.`keep the bone when the chaos altar's save roll lands`() =
        runGameTest(PrayAtAltar::class) {
            val altarType = locTypes[PrayerAltarLocs.chaos_altar]
            val altar = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), altarType)
            player.teleport(altar.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(objs.bones)
            val startXp = player.statMap.getFineXP(stats.prayer)

            random.next = 0 // Inside the 50% save threshold.
            offerInvSlot(altar, altarType, 0)
            advance(ticks = 3)

            assertEquals(1, player.count(objs.bones)) { "The saved bone was consumed anyway." }
            assertTrue(player.statMap.getFineXP(stats.prayer) > startXp) {
                "A saved bone must still pay out its offering xp."
            }
        }

    @Test
    fun GameTestState.`refuse offerings at an ordinary altar`() =
        runGameTest(PrayAtAltar::class) {
            val altarType = locTypes[PrayerAltarLocs.altar]
            val altar = placeMapLoc(CoordGrid(0, 50, 50, 34, 31), altarType)
            player.teleport(altar.coords.translateX(-1))
            player.clearInv()
            player.inv[0] = InvObj(objs.bones)

            // The refusal happens before the script's first delay, so it is asserted without
            // advancing, for the same reason burying is.
            offerInvSlot(altar, altarType, 0)

            assertMessageSent("This altar has no use for your bones.")
            assertEquals(1, player.count(objs.bones)) { "The bone was consumed for nothing." }
        }

    /**
     * There is no `opHeld` helper on the test scope yet, so the op events are published directly -
     * the same approach the firemaking tests take for `use-on` ops.
     */
    private fun GameTestScope.buryInvSlot(slot: Int) {
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

    private fun GameTestScope.offerInvSlot(
        altar: BoundLocInfo,
        altarType: UnpackedLocType,
        slot: Int,
    ) {
        val obj = checkNotNull(player.inv[slot]) { "No obj in inv slot $slot." }
        player.withProtectedAccess {
            eventBus.publish(
                this,
                LocUContentEvents.OpContent(
                    loc = altar,
                    vis = altar,
                    type = altarType,
                    objType = objTypes[obj],
                    invSlot = slot,
                ),
            )
        }
    }
}
