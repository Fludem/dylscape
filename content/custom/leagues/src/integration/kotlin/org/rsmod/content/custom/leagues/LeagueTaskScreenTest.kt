package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.content.custom.leagues.scripts.LeagueSidePanelScript
import org.rsmod.content.custom.leagues.scripts.openTasksScreen
import org.rsmod.content.custom.leagues.tasks.LeagueTaskScript

/**
 * The tasks screen opens, swallows row presses, and closes. The list itself is drawn by the client
 * from the packed structs, so what the rows look like needs the real client.
 */
@Execution(ExecutionMode.SAME_THREAD)
class LeagueTaskScreenTest {
    @Test
    fun GameTestState.`the screen opens, a row press is harmless, and close closes it`() =
        runGameTest(LeagueSidePanelScript::class, LeagueTaskScript::class) {
            player.withProtectedAccess { openTasksScreen() }
            advance(1)
            assertTrue(player.ui.containsOverlay(league_interfaces.tasks)) { "tasks overlay open" }

            player.ifButton(league_components.tasks_list, comsub = 3)
            advance(1)
            assertTrue(player.ui.containsOverlay(league_interfaces.tasks)) { "still open" }

            player.ifButton(league_components.tasks_close)
            advance(1)
            assertFalse(player.ui.containsOverlay(league_interfaces.tasks)) { "closed" }
        }
}
