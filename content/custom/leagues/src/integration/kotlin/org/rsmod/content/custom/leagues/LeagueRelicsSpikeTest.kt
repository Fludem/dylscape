package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.scripts.LeagueRelicsSpike
import org.rsmod.game.type.interf.IfEvent

/**
 * Server-side reproduction of what `::leagues` sends, so the outgoing packets can be read without a
 * client attached.
 *
 * The capture buffer is cleared every tick, so it must be read *before* the next [advance].
 */
class LeagueRelicsSpikeTest {
    @Test
    fun GameTestState.`full overlay opener sends the expected packets`() =
        runGameTest(LeagueRelicsSpike::class) {
            advance(2)
            client.clearOutgoing()

            player.withProtectedAccess {
                vars[league_varps.points_currency] = 20
                ifOpenFullOverlay(league_interfaces.relics)
                ifSetEvents(league_components.relics_clickzones, 0..127, IfEvent.Op1)
            }

            println("=== FULL OVERLAY")
            for (message in client.outgoingMessages) {
                println("OUT ${message::class.simpleName} $message")
            }
        }

    @Test
    fun GameTestState.`main side pair opener sends the expected packets`() =
        runGameTest(LeagueRelicsSpike::class) {
            advance(2)
            client.clearOutgoing()

            player.withProtectedAccess {
                vars[league_varps.points_currency] = 20
                ifOpenMainSidePair(league_interfaces.relics, league_interfaces.side_panel)
                ifSetEvents(league_components.relics_clickzones, 0..127, IfEvent.Op1)
            }

            println("=== MAIN SIDE PAIR")
            for (message in client.outgoingMessages) {
                println("OUT ${message::class.simpleName} $message")
            }
        }
}
