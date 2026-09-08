package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.scripts.LeagueSidePanelScript

/**
 * Checks the two vars that decide whether the journal's leagues tab draws at all.
 *
 * `[proc,side_journal_switchtab]` gates the fifth tab on `[proc,league_world]` returning 1 and
 * `league_tutorial_completed` reading 3 or higher. `league_world` in turn wants bit 30 of the world
 * flags set and bit 29 clear, so the exact bit pattern is asserted rather than just "non-zero" --
 * setting bit 29 as well would silently switch the client onto the deadman branch instead.
 */
class LeagueSidePanelScriptTest {
    @Test
    fun GameTestState.`login flags the world as a league world`() =
        runGameTest(LeagueSidePanelScript::class) {
            advance(2)

            val flags = player.vars[league_varps.map_flags]
            assertEquals(1 shl 30, flags, "world flags should have bit 30 and nothing else")
            assertEquals(0, flags ushr 29 and 1, "bit 29 must stay clear for league_world")
        }

    @Test
    fun GameTestState.`login opens up the leagues tab and account`() =
        runGameTest(LeagueSidePanelScript::class) {
            advance(2)

            val completed = player.vars[league_varbits.tutorial_completed]
            assert(completed >= 3) { "league_tutorial_completed was $completed, needs to be >= 3" }
            assertEquals(1, player.vars[league_varbits.account])
            assert(player.vars[league_varbits.type] != 0) { "league_type must pick a relic set" }
        }
}
