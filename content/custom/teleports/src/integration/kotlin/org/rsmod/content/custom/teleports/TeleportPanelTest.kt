package org.rsmod.content.custom.teleports

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.content.custom.teleports.configs.TeleportPanelBuilder
import org.rsmod.content.custom.teleports.configs.TeleportPanelLayout
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.configs.teleport_interfaces
import org.rsmod.content.custom.teleports.data.LastTeleportRegistry
import org.rsmod.content.custom.teleports.scripts.TeleportPanelScript
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.map.CoordGrid

class TeleportPanelDeps @Inject constructor(val registry: LastTeleportRegistry)

/**
 * Runs single-threaded on purpose: the methods share one world and each moves the same player.
 *
 * Panel clicks go through the real `If3ButtonHandler` via [GameTestScope.ifButton], which drops a
 * click unless the interface is open server-side and the component's op is enabled - so these also
 * prove the baked `events` mask is right. The spellbook overlay is opened first for the same
 * reason.
 *
 * The harness clears its capture buffers every tick, so every assertion on a message or an
 * interface update is made on the tick that produced it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TeleportPanelTest {
    @Test
    fun GameTestState.`every authored component sits at the child index its symbol names`() =
        runBasicGameTest {
            val packed =
                cacheTypes.components.values.filter { it.interfaceId == PANEL_INTERFACE_ID }
            val names = TeleportPanelBuilder.componentNames
            assertEquals(names.size, packed.size) { "The cache and the builder disagree on size." }
            for (component in packed) {
                val expected = "${TeleportPanelBuilder.INTERFACE}:${names[component.component]}"
                assertEquals(expected, component.internalName) {
                    "Child ${component.component} is not the component the builder parents to it."
                }
            }
        }

    @Test
    fun GameTestState.`hook script ids still name the scripts the layout expects`() =
        runBasicGameTest {
            val expected =
                mapOf(
                    TeleportPanelLayout.SCRIPT_STEELBORDER to "[clientscript,steelborder]",
                    TeleportPanelLayout.SCRIPT_TEXT_COLOUR to "[clientscript,text_colour_swapper]",
                    TeleportPanelLayout.SCRIPT_SETTRANS to "[clientscript,settrans]",
                )
            for ((id, name) in expected) {
                assertEquals(name, cacheTypes.clientscripts[id]?.internalName) { "Script $id." }
            }
        }

    @Test
    fun GameTestState.`home teleport opens the panel on the first tab`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()

            assertTrue(player.ui.containsModal(teleport_interfaces.panel)) { "Panel not open." }
            val first = TeleportCategory.entries.first()
            assertShowsCategory(first)
        }

    @Test
    fun GameTestState.`a tab relabels the tiles and hides the ones it does not need`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()

            player.ifButton(teleport_components.tabs[SKILLING.ordinal])
            advance(1)

            assertShowsCategory(SKILLING)
            assertTrue(player.ui.containsModal(teleport_interfaces.panel)) { "A tab closed it." }
        }

    @Test
    fun GameTestState.`picking a tile teleports the player and closes the panel`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()

            player.ifButton(teleport_components.slots[slotOf(CITIES, VARROCK)])
            advance(1)

            assertArrivedAt(destination(VARROCK).dest)
            assertMessageSent("You teleport to Varrock.")
            assertFalse(player.ui.containsModal(teleport_interfaces.panel)) { "Panel left open." }
        }

    @Test
    fun GameTestState.`a click on a tile the shown tab has no destination for does nothing`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()
            player.ifButton(teleport_components.tabs[DUNGEONS.ordinal])
            advance(1)

            // A slot the Cities tab would fill, but Dungeons leaves hidden. The server cannot see
            // client-side hiding, so this is exactly what a replayed click looks like.
            val hiddenSlot = TeleportDestinations[DUNGEONS].size
            assertTrue(hiddenSlot < TeleportDestinations[CITIES].size)
            player.ifButton(teleport_components.slots[hiddenSlot])
            advance(1)

            assertEquals(START, player.coords)
            assertNull(it.registry[player]) { "A hidden tile was remembered as a teleport." }
        }

    @Test
    fun GameTestState.`home is always offered and reports itself as home`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()

            player.ifButton(teleport_components.home)
            advance(1)

            assertArrivedAt(TeleportDestinations.HOME)
            assertMessageSent("You teleport home.")
        }

    @Test
    fun GameTestState.`previous is hidden until the first teleport, then repeats it`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) { deps ->
            player.coords = START
            openPanel()
            assertTrue(setHide(teleport_components.previous, hidden = true) in client) {
                "Previous was offered before any teleport."
            }

            player.ifButton(teleport_components.slots[slotOf(CITIES, VARROCK)])
            advance(1)
            assertEquals(VARROCK, deps.registry[player]?.key)

            player.coords = START
            openPanel()
            assertTrue(setText(teleport_components.previous_label, "Previous: Varrock") in client)
            assertTrue(setHide(teleport_components.previous, hidden = false) in client)

            player.ifButton(teleport_components.previous)
            advance(1)
            assertArrivedAt(destination(VARROCK).dest)
        }

    @Test
    fun GameTestState.`the panel reopens on the tab the player last looked at`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) {
            player.coords = START
            openPanel()
            player.ifButton(teleport_components.tabs[DUNGEONS.ordinal])
            advance(1)
            player.ifClose()

            openPanel()
            assertShowsCategory(DUNGEONS)
        }

    @Test
    fun GameTestState.`the panel refuses to open above level twenty wilderness`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) { deps ->
            player.coords = DEEP_WILDERNESS
            openPanel()

            assertFalse(player.ui.containsModal(teleport_interfaces.panel))
            assertMessageSent("You can't teleport above level 20 Wilderness.")
            assertEquals(DEEP_WILDERNESS, player.coords)
            assertNull(deps.registry[player]) { "A refused teleport was remembered." }
        }

    @Test
    fun GameTestState.`the remembered destination is released when the session is deleted`() =
        runInjectedGameTest(TeleportPanelDeps::class, null, TeleportPanelScript::class) { deps ->
            player.coords = START
            openPanel()
            player.ifButton(teleport_components.home)
            advance(1)
            assertEquals("home", deps.registry[player]?.key)

            // `unregisterPlayer` does not publish `Delete` itself.
            eventBus.publish(SessionStateEvent.Delete(player))
            assertNull(deps.registry[player]) { "The registry still pins a deleted session." }
        }

    /** Clicks Home Teleport and lets the panel open; leaves the tick's updates in [client]. */
    private fun GameTestScope.openPanel() {
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
        player.ifButton(teleport_components.home_teleport)
        advance(1)
    }

    /**
     * The tab art and every tile must match [category] - checked against the updates sent this
     * tick, since that is the only view of the client the server has.
     */
    private fun GameTestScope.assertShowsCategory(category: TeleportCategory) {
        TeleportCategory.entries.forEachIndexed { index, tab ->
            val selected = tab == category
            assertTrue(setHide(teleport_components.tabs_selected[index], !selected) in client) {
                "Tab $tab selected-art visibility is wrong while showing $category."
            }
            assertTrue(setHide(teleport_components.tabs_unselected[index], selected) in client)
        }
        val destinations = TeleportDestinations[category]
        for (slot in 0 until TeleportPanelLayout.SLOT_COUNT) {
            val destination = destinations.getOrNull(slot)
            val tile = teleport_components.slots[slot]
            assertTrue(setHide(tile, hidden = destination == null) in client) {
                "Slot $slot visibility is wrong for $category."
            }
            if (destination != null) {
                val label = teleport_components.slot_labels[slot]
                assertTrue(setText(label, destination.label) in client) {
                    "Slot $slot is not labelled ${destination.label}."
                }
            }
        }
    }

    /** `telejump` lands on the nearest walkable tile, so the arrival can be nudged a little. */
    private fun GameTestScope.assertArrivedAt(expected: CoordGrid) {
        val distance = player.coords.chebyshevDistance(expected)
        assertTrue(distance <= ARRIVAL_SLACK) {
            "Expected to land within $ARRIVAL_SLACK of $expected, got ${player.coords}."
        }
    }

    private fun setText(component: ComponentType, text: String): IfSetText =
        IfSetText(component.interfaceId, component.component, text)

    private fun setHide(component: ComponentType, hidden: Boolean): IfSetHide =
        IfSetHide(component.interfaceId, component.component, hidden)

    private companion object {
        /** `.data/symbols/.local/interface.sym`. */
        const val PANEL_INTERFACE_ID = 1001

        const val VARROCK = "varrock"

        val CITIES = TeleportCategory.Cities
        val SKILLING = TeleportCategory.Skilling
        val DUNGEONS = TeleportCategory.Dungeons

        /** `mapFindSquareLineOfWalk` is called with a radius of 2. */
        const val ARRIVAL_SLACK = 2

        val START = CoordGrid(0, 50, 50, 21, 18)

        /** Level 21 Wilderness: inside the x band, one level past the cut-off. */
        val DEEP_WILDERNESS = CoordGrid(3100, 3680)

        fun destination(key: String): TeleportDestination =
            requireNotNull(TeleportDestinations[key]) { "No destination keyed $key." }

        fun slotOf(category: TeleportCategory, key: String): Int =
            TeleportDestinations[category].indexOfFirst { it.key == key }
                .also { check(it >= 0) { "$key is not in $category." } }
    }
}
