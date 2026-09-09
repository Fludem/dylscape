package org.rsmod.content.custom.barrows

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.scripts.BarrowsScript
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

/**
 * The way in.
 *
 * This is the one part of Barrows a player meets before anything else, and it went out with no test
 * at all: the dig matched the mound's centre tile exactly, so every dig on the six hills but six
 * tiles in the whole world answered "You find nothing but earth" and the minigame read as missing.
 * These tests dig from the corners of each mound, which is what a player standing on a hill is
 * actually doing.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsDigTest {
    @Test
    fun GameTestState.`digging anywhere on a mound drops into that crypt`() =
        runGameTest(BarrowsScript::class) {
            for ((brother, top) in BarrowsMap.moundTops) {
                val entrance = checkNotNull(BarrowsMap.cryptEntrances[brother])
                val corners =
                    listOf(
                        CoordGrid(top.x.first, top.z.first, 0),
                        CoordGrid(top.x.last, top.z.first, 0),
                        CoordGrid(top.x.first, top.z.last, 0),
                        CoordGrid(top.x.last, top.z.last, 0),
                        checkNotNull(BarrowsMap.mounds[brother]),
                    )
                for (from in corners) {
                    player.placeAt(from)
                    dig()
                    advance(ticks = DIG_TICKS)
                    assertEquals(entrance, player.coords, "Digging at $from missed $brother")
                }
            }
        }

    @Test
    fun GameTestState.`digging off the mounds finds nothing but earth`() =
        runGameTest(BarrowsScript::class) {
            // One tile clear of Ahrim's mound on each side, plus the middle of the graveyard.
            val ahrim = checkNotNull(BarrowsMap.moundTops[Brother.Ahrim])
            val offMound =
                listOf(
                    CoordGrid(ahrim.x.first - 1, ahrim.z.first, 0),
                    CoordGrid(ahrim.x.last + 1, ahrim.z.last, 0),
                    CoordGrid(ahrim.x.first, ahrim.z.first - 1, 0),
                    CoordGrid(ahrim.x.last, ahrim.z.last + 1, 0),
                    CoordGrid(3568, 3283, 0),
                )
            for (from in offMound) {
                player.placeAt(from)
                dig()
                assertMessageSent("You find nothing but earth.")
                advance(ticks = DIG_TICKS)
                assertEquals(from, player.coords, "Digging at $from should not have moved anyone")
                assertNull(BarrowsMap.moundAt(from), "$from reads as a mound")
            }
        }

    /** Publishes the op1 the client sends for the spade's `Dig`. */
    private fun GameTestScope.dig() {
        player.clearInv()
        player.inv[0] = InvObj(objs.spade)
        val obj = checkNotNull(player.inv[0])
        player.withProtectedAccess {
            eventBus.publish(
                this,
                HeldObjEvents.Op1(
                    slot = 0,
                    obj = obj,
                    type = objTypes[obj],
                    inventory = player.inv,
                ),
            )
        }
    }

    private companion object {
        /** Matches `BarrowsScript.DIG_TICKS`, which is private to the script. */
        const val DIG_TICKS = 2
    }
}
