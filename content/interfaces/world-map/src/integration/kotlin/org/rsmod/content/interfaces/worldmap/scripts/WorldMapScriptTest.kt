package org.rsmod.content.interfaces.worldmap.scripts

import net.rsprot.protocol.game.outgoing.misc.player.RunClientScript
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.worldmap.configs.worldmap_components
import org.rsmod.content.interfaces.worldmap.configs.worldmap_interfaces
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid

class WorldMapScriptTest {
    @Test
    fun GameTestState.`open world map from orb`() =
        runGameTest(WorldMapScript::class) {
            player.ifOpenOverlay(interfaces.orbs, components.toplevel_target_orbs)

            player.ifButton(worldmap_components.orb, op = IfButtonOp.Op2)
            advance()
            assertTrue(player.ui.containsOverlay(worldmap_interfaces.worldmap))

            player.ifButton(worldmap_components.close)
            advance()
            assertFalse(player.ui.containsOverlay(worldmap_interfaces.worldmap))
        }

    @Test
    fun GameTestState.`open fullscreen world map from orb`() =
        runGameTest(WorldMapScript::class) {
            player.ifOpenOverlay(interfaces.orbs, components.toplevel_target_orbs)

            player.ifButton(worldmap_components.orb, op = IfButtonOp.Op3)
            advance()
            assertTrue(player.ui.containsOverlay(worldmap_interfaces.worldmap))

            player.ifButton(worldmap_components.esckey)
            advance()
            assertFalse(player.ui.containsOverlay(worldmap_interfaces.worldmap))
        }

    @Test
    fun GameTestState.`marker follows the player while the map is open`() =
        runGameTest(WorldMapScript::class) {
            val start = CoordGrid(3222, 3218)
            player.coords = start
            player.ifOpenOverlay(interfaces.orbs, components.toplevel_target_orbs)

            player.ifButton(worldmap_components.orb, op = IfButtonOp.Op2)
            advance()
            assertEquals(start.packed, lastMarker())

            // The map is an overlay, so the player keeps moving underneath it.
            val moved = CoordGrid(3200, 3200, level = 1)
            player.coords = moved
            advance()
            assertEquals(moved.packed, lastMarker())
        }

    @Test
    fun GameTestState.`marker is cleared and stops updating on close`() =
        runGameTest(WorldMapScript::class) {
            player.coords = CoordGrid(3222, 3218)
            player.ifOpenOverlay(interfaces.orbs, components.toplevel_target_orbs)

            player.ifButton(worldmap_components.orb, op = IfButtonOp.Op2)
            advance()

            player.ifButton(worldmap_components.close)
            advance()
            assertEquals(CoordGrid.NULL.packed, lastMarker())

            player.coords = CoordGrid(3200, 3200)
            advance()
            assertTrue(transmits().isEmpty())
        }

    /**
     * The `you are here` argument of the last `[clientscript,worldmap_transmitdata]` sent.
     *
     * [advance] clears the capture client at the start of every tick, so this -- and [transmits] --
     * only ever sees the packets from the cycle that just ran.
     */
    private fun GameTestScope.lastMarker(): Int = transmits().last().values[0] as Int

    private fun GameTestScope.transmits(): List<RunClientScript> =
        client.filterIsInstance<RunClientScript>().filter { it.id == TRANSMIT_DATA }

    private companion object {
        const val TRANSMIT_DATA = 1749
    }
}
