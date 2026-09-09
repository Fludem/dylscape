package org.rsmod.content.custom.teleports

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.data.LastTeleportRegistry
import org.rsmod.content.custom.teleports.scripts.TeleportMenuScript
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid

class TeleportTestDeps
@Inject
constructor(val resumePause: ResumePauseButtonHandler, val registry: LastTeleportRegistry)

/**
 * Runs single-threaded on purpose: the methods share one world and each moves the same player.
 *
 * The menu suspends on a pause-button resume, so rows are picked with the real
 * [ResumePauseButtonHandler] rather than by publishing an interface event. The spellbook overlay is
 * opened first because `If3ButtonHandler` drops clicks on components whose ops are not enabled.
 *
 * Row indices are derived from [TeleportMenu] -- the same builder the script draws from -- so that
 * adding a destination cannot silently turn one of these tests into a click on a different row.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TeleportMenuScriptTest {
    @Test
    fun GameTestState.`picking a city from the category menu teleports the player`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, destinationRow(TeleportCategory.Cities, VARROCK))
            advance(1)

            assertArrivedAt(destination(VARROCK).dest)
            assertMessageSent("You teleport to Varrock.")
        }

    @Test
    fun GameTestState.`back returns to the categories and a second pick still teleports`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, backRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Skilling))
            advance(1)
            chooseRow(deps, 0)
            advance(1)

            assertTrue(player.coords != START) { "Back left the menu unable to teleport." }
        }

    @Test
    fun GameTestState.`cancel closes the menu and leaves the player where they were`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, cancelRow(previous = null))
            advance(1)

            assertEquals(START, player.coords)
        }

    @Test
    fun GameTestState.`home is the first row and reports itself as home`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, 0)
            advance(1)

            assertArrivedAt(TeleportDestinations.HOME)
            // The harness clears its message buffer every tick, so this has to be read on the tick
            // the coroutine resumed, not a few spare ones later.
            assertMessageSent("You teleport home.")
        }

    @Test
    fun GameTestState
        .`previous is absent until the first teleport, then repeats it in one click`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()
            assertNull(deps.registry[player]) { "A fresh player already has a previous teleport." }

            // With nothing remembered, the index the "Previous" row *would* occupy is "Cancel".
            val previousIndex = 1 + TeleportCategory.entries.size
            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, previousIndex)
            advance(1)
            assertEquals(START, player.coords) { "A Previous row was offered before any teleport." }

            // Teleport once, so the row appears.
            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, destinationRow(TeleportCategory.Cities, VARROCK))
            advance(1)
            assertEquals(VARROCK, deps.registry[player]?.key)

            // Now the same index repeats it.
            player.coords = START
            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, previousIndex)
            advance(1)

            assertArrivedAt(destination(VARROCK).dest)
        }

    @Test
    fun GameTestState
        .`teleporting is refused above level twenty wilderness and is not remembered`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = DEEP_WILDERNESS
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, destinationRow(TeleportCategory.Cities, VARROCK))
            // The message buffer clears every tick, so the refusal has to be read on the tick it
            // is sent, not after a few spare ones.
            advance(1)

            assertEquals(DEEP_WILDERNESS, player.coords)
            assertMessageSent("You can't teleport above level 20 Wilderness.")
            assertNull(deps.registry[player]) { "A refused teleport was remembered." }
        }

    @Test
    fun GameTestState.`each player has their own previous destination`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            val other = registerPlayer(coords = START)

            openSpellbook()
            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, categoryRow(TeleportCategory.Cities))
            advance(1)
            chooseRow(deps, destinationRow(TeleportCategory.Cities, VARROCK))
            advance(1)

            other.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
            other.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, 0, other)
            advance(1)

            assertEquals(VARROCK, deps.registry[player]?.key)
            assertEquals("home", deps.registry[other]?.key)
        }

    @Test
    fun GameTestState.`the remembered destination is released when the session is deleted`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, 0)
            advance(1)
            assertEquals("home", deps.registry[player]?.key)

            // `unregisterPlayer` does not publish `Delete` itself.
            eventBus.publish(SessionStateEvent.Delete(player))
            assertNull(deps.registry[player]) { "The registry still pins a deleted session." }
        }

    private fun GameTestScope.openSpellbook() {
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
    }

    private fun GameTestScope.chooseRow(deps: TeleportTestDeps, row: Int, target: Player = player) {
        val combined = CombinedId(components.menu_list.interfaceId, components.menu_list.component)
        val client = target.client as CaptureClient
        client.queue(deps.resumePause, ResumePauseButton(combined, row))
    }

    /** `telejump` lands on the nearest walkable tile, so the arrival can be nudged a little. */
    private fun GameTestScope.assertArrivedAt(expected: CoordGrid) {
        val distance = player.coords.chebyshevDistance(expected)
        assertTrue(distance <= ARRIVAL_SLACK) {
            "Expected to land within $ARRIVAL_SLACK of $expected, got ${player.coords}."
        }
    }

    private companion object {
        const val VARROCK = "varrock"

        /** `mapFindSquareLineOfWalk` is called with a radius of 2. */
        const val ARRIVAL_SLACK = 2

        val START = CoordGrid(0, 50, 50, 21, 18)

        /** Level 21 Wilderness: inside the x band, one level past the cut-off. */
        val DEEP_WILDERNESS = CoordGrid(3100, 3680)

        fun destination(key: String): TeleportDestination =
            requireNotNull(TeleportDestinations[key]) { "No destination keyed $key." }

        fun categoryRow(category: TeleportCategory): Int =
            TeleportMenu.topLevel(previous = null).indexOfFirst {
                it is TeleportRow.Category && it.category == category
            }

        fun cancelRow(previous: TeleportDestination?): Int =
            TeleportMenu.topLevel(previous).indexOf(TeleportRow.Cancel)

        fun backRow(category: TeleportCategory): Int =
            TeleportMenu.destinations(category).indexOf(TeleportRow.Back)

        fun destinationRow(category: TeleportCategory, key: String): Int =
            TeleportMenu.destinations(category).indexOfFirst {
                it is TeleportRow.Destination && it.destination.key == key
            }
    }
}
