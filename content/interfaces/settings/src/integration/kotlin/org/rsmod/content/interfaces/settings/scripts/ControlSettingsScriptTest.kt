package org.rsmod.content.interfaces.settings.scripts

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent

/**
 * The starting values of `option_attackpriority` and `option_attackpriority_npc`.
 *
 * Asserted as raw varps rather than through `PlayerPriority`/`NpcPriority`, which are file-private
 * to [ControlSettingsScript] and out of reach from a separate compilation unit. The raw values are
 * the more useful assertion anyway: they are what the client reads to decide whether `Attack` gets
 * a left-click, and what a save round-trips.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ControlSettingsScriptTest {
    private var Player.newAccount by boolVarBit(varbits.new_player_account)

    @Test
    fun GameTestState.`a new account starts on left-click attack`() =
        runGameTest(ControlSettingsScript::class) {
            player.newAccount = true
            player.setVarp(varps.option_attackpriority, COMBAT_LEVEL)
            player.setVarp(varps.option_attackpriority_npc, COMBAT_LEVEL)

            eventBus.publish(SessionStateEvent.Initialize(player))

            assertEquals(LEFT_CLICK, player.vars[varps.option_attackpriority]) {
                "A new account was left on 'Depends on combat levels' for players."
            }
            assertEquals(LEFT_CLICK, player.vars[varps.option_attackpriority_npc]) {
                "A new account was left on 'Depends on combat levels' for npcs."
            }
        }

    @Test
    fun GameTestState.`a returning player keeps their own choice`() =
        runGameTest(ControlSettingsScript::class) {
            player.newAccount = false
            // Zero is both "never chosen" and a deliberate pick of 'Depends on combat levels'. The
            // two are indistinguishable on the varp, so the first-login gate is the only thing
            // keeping the deliberate pick from being overwritten on every login.
            player.setVarp(varps.option_attackpriority, COMBAT_LEVEL)
            player.setVarp(varps.option_attackpriority_npc, RIGHT_CLICK_ALWAYS)

            eventBus.publish(SessionStateEvent.Initialize(player))

            assertEquals(COMBAT_LEVEL, player.vars[varps.option_attackpriority]) {
                "A returning player's player attack priority was overwritten at login."
            }
            assertEquals(RIGHT_CLICK_ALWAYS, player.vars[varps.option_attackpriority_npc]) {
                "A returning player's npc attack priority was overwritten at login."
            }
        }

    private companion object {
        /**
         * `PlayerPriority.CombatLevel`/`NpcPriority.CombatLevel`, and what an unset varp reads as.
         */
        const val COMBAT_LEVEL = 0

        /** `NpcPriority.RightClickAlways` — the value a live save was observed holding. */
        const val RIGHT_CLICK_ALWAYS = 1

        /** `PlayerPriority.LeftClick`/`NpcPriority.LeftClick`. */
        const val LEFT_CLICK = 2
    }
}
