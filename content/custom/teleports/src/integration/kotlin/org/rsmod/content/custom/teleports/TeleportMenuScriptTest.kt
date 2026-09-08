package org.rsmod.content.custom.teleports

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.util.CombinedId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.scripts.TeleportMenuScript
import org.rsmod.map.CoordGrid

class TeleportTestDeps @Inject constructor(val resumePause: ResumePauseButtonHandler)

/**
 * Runs single-threaded on purpose: the methods share one world and each moves the same player.
 *
 * The menu suspends on a pause-button resume, so rows are picked with the real
 * [ResumePauseButtonHandler] rather than by publishing an interface event. The spellbook overlay is
 * opened first because `If3ButtonHandler` drops clicks on components whose ops are not enabled.
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
            chooseRow(deps, CITIES)
            advance(1)
            chooseRow(deps, VARROCK_ROW)
            advance(1)

            assertArrivedAt(VARROCK)
        }

    @Test
    fun GameTestState.`back returns to the categories and a second pick still teleports`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = START
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, CITIES)
            advance(1)
            // One past the five cities is the trailing "Back" row.
            chooseRow(deps, CITY_COUNT)
            advance(1)
            chooseRow(deps, SKILLING)
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
            // One past the categories is the trailing "Cancel" row.
            chooseRow(deps, TeleportCategoryCount)
            advance(1)

            assertEquals(START, player.coords)
        }

    @Test
    fun GameTestState.`teleporting is refused above level twenty wilderness`() =
        runInjectedGameTest(TeleportTestDeps::class, null, TeleportMenuScript::class) { deps ->
            player.coords = DEEP_WILDERNESS
            openSpellbook()

            player.ifButton(teleport_components.home_teleport)
            advance(1)
            chooseRow(deps, CITIES)
            advance(1)
            chooseRow(deps, VARROCK_ROW)
            // The message buffer clears every tick, so the refusal has to be read on the tick it
            // is sent, not after a few spare ones.
            advance(1)

            assertEquals(DEEP_WILDERNESS, player.coords)
        }

    private fun GameTestScope.openSpellbook() {
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
    }

    private fun GameTestScope.chooseRow(deps: TeleportTestDeps, row: Int) {
        val combined = CombinedId(components.menu_list.interfaceId, components.menu_list.component)
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
        /** Row indices into the menus the script builds. */
        const val CITIES = 0
        const val SKILLING = 1
        const val TeleportCategoryCount = 2
        const val CITY_COUNT = 5
        const val VARROCK_ROW = 1

        /** `mapFindSquareLineOfWalk` is called with a radius of 2. */
        const val ARRIVAL_SLACK = 2

        val START = CoordGrid(0, 50, 50, 21, 18)
        val VARROCK = CoordGrid(0, 50, 53, 13, 32)

        /** Level 21 Wilderness: inside the x band, one level past the cut-off. */
        val DEEP_WILDERNESS = CoordGrid(3100, 3680)
    }
}
