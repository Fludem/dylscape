package org.rsmod.content.interfaces.firstlogin.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.accountmode.displayName
import org.rsmod.content.custom.accountmode.syncAccountModeVarBits
import org.rsmod.content.interfaces.firstlogin.configs.first_login_components
import org.rsmod.content.interfaces.firstlogin.configs.first_login_interfaces
import org.rsmod.content.interfaces.firstlogin.configs.first_login_queues
import org.rsmod.content.interfaces.firstlogin.configs.first_login_timers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountMode
import org.rsmod.game.entity.player.XpRateTier
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Drives `first_login_setup` (interface 1000): a two-page chooser where a new character picks an
 * account mode and an experience-rate tier, both permanent.
 *
 * The panel is opened as a **full overlay rather than a modal**, and with no close button armed. A
 * modal is always dismissable by the client - `CloseModal` arrives unprompted and
 * `PlayerMainProcess` acts on it - whereas an overlay lives in `ui.overlays` and only the server
 * can take it down. That is what makes this chooser genuinely unskippable.
 *
 * Eligibility is `xpRateTier == null`, which is backed by a database column. It deliberately does
 * *not* use `varbits.new_player_account`: that varbit sits on a temporary varp, so it is gone after
 * a relog and a player who reconnected mid-choice would never be asked again.
 */
public class FirstLoginScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    /** Backed by a database column, so it survives a relog. */
    private val Player.hasChosen: Boolean
        get() = xpRateTier != null

    override fun ScriptContext.startup() {
        onPlayerLogin { player.armPrompt() }
        onPlayerSoftTimer(first_login_timers.prompt) { player.armPrompt() }
        onPlayerQueue(first_login_queues.setup) { openSetup() }

        first_login_components.mode_buttons.forEachIndexed { index, button ->
            onIfOverlayButton(button) { player.selectMode(index) }
        }
        first_login_components.rate_buttons.forEachIndexed { index, button ->
            onIfOverlayButton(button) { player.selectRate(index) }
        }

        onCommand("chooser") {
            modLevel = modlevels.admin
            desc = "Reset account setup and reopen the chooser"
            cheat { player.resetSetup() }
        }
    }

    /**
     * Development aid: clears the choice and re-offers the panel.
     *
     * Every layout tweak costs a `packCache` and a client restart, and without this each one would
     * also need a brand new account to look at. This is the only path that un-sets a tier, and it
     * is admin-gated.
     */
    private fun Player.resetSetup() {
        xpRateTier = null
        accountMode = AccountMode.Standard
        xpRate = 1.0
        syncAccountModeVarBits()
        armPrompt()
        mes("Account setup reset. The chooser will reopen shortly.")
    }

    private fun Player.armPrompt() {
        if (hasChosen) {
            clearSoftTimer(first_login_timers.prompt)
            return
        }
        // Keeps re-offering until a tier is set, so logging out mid-choice is recoverable and
        // finishing the tutorial mid-session is picked up without the tutorial knowing about us.
        softTimer(first_login_timers.prompt, PROMPT_INTERVAL)
        queue(first_login_queues.setup, cycles = 1)
    }

    private fun ProtectedAccess.openSetup() {
        if (player.hasChosen) {
            return
        }
        ifOpenFullOverlay(first_login_interfaces.setup)
        // No close button is armed anywhere, so the panel cannot be dismissed.
        showModePage()
    }

    private fun ProtectedAccess.showModePage() {
        ifSetHide(first_login_components.page_mode, hide = false)
        ifSetHide(first_login_components.page_rate, hide = true)
    }

    private fun Player.selectMode(index: Int) {
        if (hasChosen) {
            return
        }
        val mode = MODES.getOrNull(index) ?: return
        accountMode = mode
        syncAccountModeVarBits()
        // Page turn is server-side: the panel carries no clientscripts, by design.
        ifSetHide(first_login_components.page_mode, hide = true)
        ifSetHide(first_login_components.page_rate, hide = false)
    }

    private fun Player.selectRate(index: Int) {
        if (hasChosen) {
            return
        }
        val tier = TIERS.getOrNull(index) ?: return
        commit(tier)
    }

    /**
     * The one place a choice is made permanent. Everything else refuses to act once [hasChosen] is
     * true, so a duplicate click or a replayed packet cannot re-roll an account.
     */
    private fun Player.commit(tier: XpRateTier) {
        check(xpRateTier == null) { "Account setup already completed for: $this." }
        xpRateTier = tier
        xpRate = tier.xpRate
        syncAccountModeVarBits()
        clearSoftTimer(first_login_timers.prompt)
        ifCloseSub(first_login_interfaces.setup, eventBus)
        mes(
            "You are a ${accountMode.displayName} on ${tier.id}x experience. " +
                "This cannot be changed."
        )
    }

    private companion object {
        /** Row order must match `FirstLoginBuilder.modeRows`. */
        private val MODES =
            listOf(
                AccountMode.Standard,
                AccountMode.Ironman,
                AccountMode.HardcoreIronman,
                AccountMode.UltimateIronman,
            )

        /** Row order must match `FirstLoginBuilder.rateRows`. */
        private val TIERS = listOf(XpRateTier.Rate10, XpRateTier.Rate16, XpRateTier.Rate30)

        /** Ticks between re-offers. Slow: this only ever fires for an unchosen account. */
        private const val PROMPT_INTERVAL = 25
    }
}
