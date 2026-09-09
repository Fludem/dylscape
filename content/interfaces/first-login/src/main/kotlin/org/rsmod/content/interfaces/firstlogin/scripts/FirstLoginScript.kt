package org.rsmod.content.interfaces.firstlogin.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifSetHide
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerQueue
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.accountmode.displayName
import org.rsmod.content.custom.accountmode.syncAccountModeVarBits
import org.rsmod.content.interfaces.firstlogin.configs.FirstLoginBuilder
import org.rsmod.content.interfaces.firstlogin.configs.first_login_components
import org.rsmod.content.interfaces.firstlogin.configs.first_login_interfaces
import org.rsmod.content.interfaces.firstlogin.configs.first_login_queues
import org.rsmod.content.interfaces.firstlogin.configs.first_login_timers
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountMode
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.entity.player.XpRateTier
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Drives `first_login_setup` (interface 1000): a three-page chooser where a new character picks an
 * account mode and an experience-rate tier, reviews both, and only then commits.
 *
 * The panel is opened as a **full overlay rather than a modal**, and with no close button armed. A
 * modal is always dismissable by the client - `CloseModal` arrives unprompted and
 * `PlayerMainProcess` acts on it - whereas an overlay lives in `ui.overlays` and only the server
 * can take it down. That is what makes this chooser genuinely unskippable.
 *
 * Neither choice is written to the player until the confirm page is accepted. Writing the mode as
 * soon as it was clicked would mean a player who picked Ironman and logged out before confirming
 * came back as an ironman - restricted, but with no tier, so still unchosen. Holding both in
 * [pending] keeps "picked" and "committed" genuinely separate.
 *
 * Eligibility is `xpRateTier == null`, which is backed by a database column. It deliberately does
 * *not* use `varbits.new_player_account`: that varbit sits on a temporary varp, so it is gone after
 * a relog and a player who reconnected mid-choice would never be asked again.
 */
public class FirstLoginScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    /**
     * In-flight selections, discarded when the session ends.
     *
     * Cleared on [SessionStateEvent.Delete] rather than on logout: logout fires before the account
     * save is queued, so tearing per-player state down there can race the save.
     */
    private val pending = HashMap<Player, PendingChoice>()

    /** Backed by a database column, so it survives a relog. */
    private val Player.hasChosen: Boolean
        get() = xpRateTier != null

    override fun ScriptContext.startup() {
        onPlayerLogin { player.armPrompt() }
        onPlayerSoftTimer(first_login_timers.prompt) { player.armPrompt() }
        onPlayerQueue(first_login_queues.setup) { openSetup() }
        onEvent<SessionStateEvent.Delete> { pending.remove(player) }

        first_login_components.mode_buttons.forEachIndexed { index, button ->
            onIfOverlayButton(button) { player.selectMode(index) }
        }
        first_login_components.rate_buttons.forEachIndexed { index, button ->
            onIfOverlayButton(button) { player.selectRate(index) }
        }
        onIfOverlayButton(first_login_components.confirm_button) { player.confirm() }
        onIfOverlayButton(first_login_components.back_button) { player.startOver() }

        onCommand("chooser") {
            modLevel = modlevels.admin
            desc = "Reset account setup and reopen the chooser"
            cheat { player.resetSetup() }
        }
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
        player.showPage(Page.Mode)
    }

    private fun Player.selectMode(index: Int) {
        if (hasChosen) {
            return
        }
        val mode = MODES.getOrNull(index) ?: return
        pending.getOrPut(this) { PendingChoice() }.mode = mode
        showPage(Page.Rate)
    }

    private fun Player.selectRate(index: Int) {
        if (hasChosen) {
            return
        }
        val choice = pending[this] ?: return
        val tier = TIERS.getOrNull(index) ?: return
        choice.tier = tier

        val mode = choice.mode ?: return
        ifSetText(first_login_components.confirm_mode, "Account type: ${mode.displayName}")
        ifSetText(first_login_components.confirm_rate, "Experience rate: ${tier.id}x")
        showPage(Page.Confirm)
    }

    /** Back out to the start. Nothing has been written yet, so this simply forgets the choices. */
    private fun Player.startOver() {
        if (hasChosen) {
            return
        }
        pending.remove(this)
        showPage(Page.Mode)
    }

    private fun Player.confirm() {
        if (hasChosen) {
            return
        }
        val choice = pending[this] ?: return
        val mode = choice.mode ?: return
        val tier = choice.tier ?: return
        commit(mode, tier)
    }

    /**
     * The one place a choice is made permanent. Every entry point above refuses to act once
     * [hasChosen] is true, so a double click or a replayed packet cannot re-roll an account.
     */
    private fun Player.commit(mode: AccountMode, tier: XpRateTier) {
        check(xpRateTier == null) { "Account setup already completed for: $this." }
        accountMode = mode
        xpRateTier = tier
        xpRate = tier.xpRate
        syncAccountModeVarBits()
        pending.remove(this)
        clearSoftTimer(first_login_timers.prompt)
        ifCloseSub(first_login_interfaces.setup, eventBus)
        mes("You are a ${mode.displayName} on ${tier.id}x experience. This cannot be changed.")
    }

    /** Pages are sibling layers; exactly one is visible at a time. */
    private fun Player.showPage(page: Page) {
        ifSetHide(first_login_components.page_mode, hide = page != Page.Mode)
        ifSetHide(first_login_components.page_rate, hide = page != Page.Rate)
        ifSetHide(first_login_components.page_confirm, hide = page != Page.Confirm)
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
        pending.remove(this)
        armPrompt()
        mes("Account setup reset. The chooser will reopen shortly.")
    }

    private enum class Page {
        Mode,
        Rate,
        Confirm,
    }

    private class PendingChoice(var mode: AccountMode? = null, var tier: XpRateTier? = null)

    private companion object {
        /** Row order must match [FirstLoginBuilder.modeRows]. */
        private val MODES =
            listOf(
                AccountMode.Standard,
                AccountMode.Ironman,
                AccountMode.HardcoreIronman,
                AccountMode.UltimateIronman,
            )

        /** Row order must match [FirstLoginBuilder.rateRows]. */
        private val TIERS = listOf(XpRateTier.Rate10, XpRateTier.Rate16, XpRateTier.Rate30)

        /** Ticks between re-offers. Slow: this only ever fires for an unchosen account. */
        private const val PROMPT_INTERVAL = 25
    }
}
