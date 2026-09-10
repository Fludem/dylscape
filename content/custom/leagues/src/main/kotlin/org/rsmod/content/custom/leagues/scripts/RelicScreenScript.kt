package org.rsmod.content.custom.leagues.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.content.custom.leagues.relics.LeaguePoints
import org.rsmod.content.custom.leagues.relics.LeaguePointsSync
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.relics.clearRelic
import org.rsmod.content.custom.leagues.relics.relicIn
import org.rsmod.content.custom.leagues.relics.setRelic
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Picking relics on the vanilla `league_relics` screen.
 *
 * The client does most of the work. A relic's View click shows Loading and reaches the server,
 * which answers with the expanded view and a state code; from there Select, Back and Cancel all run
 * client-side, and the only press that comes back is **Confirm**. So the server's job is:
 * - on View, decide the relic's state and show it ([view]);
 * - on Confirm, re-check everything and commit ([confirm]).
 *
 * A tier's first pick is free. Swapping it for another relic in the same tier costs
 * [Relic.REPICK_COSTS], taken from the inventory first and then the bank.
 */
class RelicScreenScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val screen: RelicScreen,
    private val points: LeaguePointsSync,
) : PluginScript() {
    /** The relic each player has open in the expanded view; Confirm acts on this. */
    private val viewing = HashMap<Player, Relic>()

    private val Int.formatted: String
        get() = "%,d".format(this)

    /** The codes `league_relic_expanded_view` and `league_relic_not_available` share. */
    private enum class ViewState(val code: Int) {
        NeedPoints(0),
        Unlocked(1),
        Selectable(2),
        PreviousTier(3),
    }

    override fun ScriptContext.startup() {
        onIfOverlayButton(league_components.relics_clickzones) {
            val relic = Relic.forComsub(comsub) ?: return@onIfOverlayButton
            launchAccess(player) { view(relic) }
        }
        onIfOverlayButton(league_components.relics_confirm_button) {
            launchAccess(player) { confirm() }
        }
        onIfOverlayButton(league_components.relics_close) {
            viewing.remove(player)
            player.ifCloseSub(league_interfaces.relics, eventBus)
        }
        onEvent<SessionStateEvent.Delete> {
            viewing.remove(player)
            points.forget(player)
        }

        onCommand("relics") {
            modLevel = modlevels.admin
            desc = "Open the league relics screen"
            cheat(::openScreen)
        }
        onCommand("relicreset") {
            modLevel = modlevels.admin
            desc = "Clear relic picks (::relicreset [tier 1-8|all])"
            cheat(::resetRelics)
        }
        onCommand("leaguepoints") {
            modLevel = modlevels.admin
            desc = "Override League Points until relog (::leaguepoints <n>|clear)"
            cheat(::overridePoints)
        }
    }

    private fun openScreen(cheat: Cheat) = launchAccess(cheat.player) { screen.open(this) }

    private fun launchAccess(player: Player, action: suspend ProtectedAccess.() -> Unit) {
        if (!protectedAccess.launch(player) { action() }) {
            player.mes("You are busy right now.")
        }
    }

    private fun ProtectedAccess.view(relic: Relic) {
        points.sync(player)
        val state = viewState(player, relic)
        viewing[player] = relic
        showExpandedView(relic, state.code)

        // The client draws "Select" for any selectable relic. An unbuilt one says "Coming soon" in
        // its description and gets no button at all; the server refuses it on Confirm regardless.
        val selectable = state == ViewState.Selectable
        ifSetHide(league_components.relics_select_button, selectable && !relic.implemented)
        if (selectable && relic.implemented) {
            ifSetText(league_components.relics_confirm_text, confirmText(relic))
        }
    }

    private fun ProtectedAccess.confirm() {
        val relic = viewing[player] ?: return
        points.sync(player)
        val refusal =
            when (viewState(player, relic)) {
                ViewState.Unlocked -> "You have already unlocked this relic."
                ViewState.PreviousTier -> "You should unlock a relic from the previous tier first."
                ViewState.NeedPoints -> needPointsMessage(relic)
                ViewState.Selectable ->
                    if (relic.implemented) null else "That relic is not on this server yet."
            }
        if (refusal != null) {
            refuse(refusal)
            return
        }

        val previous = player.relicIn(relic.tier)
        if (previous != null) {
            val cost = Relic.REPICK_COSTS[relic.tier]
            if (!payCoins(cost)) {
                refuse("You need ${cost.formatted} coins in your inventory or bank to swap relics.")
                return
            }
        }

        player.setRelic(relic)
        viewing.remove(player)
        screen.grantItem(this, relic)
        drawRelicSelections()
        publish(RelicUnlocked(player, relic, previous))

        if (previous == null) {
            mes("You unlock the ${relic.displayName} relic.")
        } else {
            mes("You swap ${previous.displayName} for ${relic.displayName}.")
        }
    }

    /** Explains a refused Confirm, in the popup (still on screen) and in chat. */
    private fun ProtectedAccess.refuse(message: String) {
        ifSetText(league_components.relics_confirm_text, message)
        mes(message)
    }

    private fun viewState(player: Player, relic: Relic): ViewState =
        when {
            player.relicIn(relic.tier) == relic -> ViewState.Unlocked
            points.points(player) < Relic.TIER_POINTS[relic.tier] -> ViewState.NeedPoints
            relic.tier > 0 && player.relicIn(relic.tier - 1) == null -> ViewState.PreviousTier
            else -> ViewState.Selectable
        }

    private fun ProtectedAccess.confirmText(relic: Relic): String {
        val name = "<col=ffffff>${relic.displayName}</col>"
        val cost = Relic.REPICK_COSTS[relic.tier].formatted
        val current = player.relicIn(relic.tier)
        if (current != null) {
            return "Swap <col=ffffff>${current.displayName}</col> for the $name relic?" +
                "<br><br>This costs <col=ffffff>$cost</col> coins, taken from your inventory " +
                "first and then your bank."
        }
        return "Are you sure that you wish to unlock the $name relic?<br><br>You can only have " +
            "one relic from each tier. Swapping it later costs <col=ffffff>$cost</col> coins."
    }

    private fun needPointsMessage(relic: Relic): String {
        val total = LeaguePoints.totalLevelFor(relic.tier)
        return "You need a total level of $total to unlock a relic from tier ${relic.tier + 1}."
    }

    /**
     * Takes [cost] coins, inventory first and then the bank; nothing is taken unless all can be.
     */
    private fun ProtectedAccess.payCoins(cost: Int): Boolean {
        val carried = invTotal(inv, objs.coins)
        val banked = invTotal(bank, objs.coins)
        if (carried.toLong() + banked < cost) {
            return false
        }
        val fromInv = minOf(carried, cost)
        if (fromInv > 0) {
            invDel(inv, objs.coins, count = fromInv)
        }
        val fromBank = cost - fromInv
        if (fromBank > 0) {
            invDel(bank, objs.coins, count = fromBank)
        }
        return true
    }

    private fun resetRelics(cheat: Cheat) =
        with(cheat) {
            val arg = args.firstOrNull()
            val tiers =
                when {
                    arg == null || arg.equals("all", ignoreCase = true) -> 0 until Relic.TIER_COUNT
                    else -> {
                        val tier = arg.toIntOrNull()?.minus(1)
                        if (tier == null || tier !in 0 until Relic.TIER_COUNT) {
                            player.mes("Use as ::relicreset [tier 1-${Relic.TIER_COUNT}|all]")
                            return
                        }
                        tier..tier
                    }
                }
            for (tier in tiers) {
                player.clearRelic(tier)
            }
            player.mes("Cleared relic picks for tiers ${tiers.first + 1}-${tiers.last + 1}.")
        }

    private fun overridePoints(cheat: Cheat) =
        with(cheat) {
            val arg = args.firstOrNull()
            if (arg.equals("clear", ignoreCase = true)) {
                points.forget(player)
                points.sync(player)
                player.mes("League Points follow your total level again: ${points.points(player)}.")
                return
            }
            val value = arg?.toIntOrNull()
            if (value == null || value < 0) {
                player.mes("Use as ::leaguepoints <n> or ::leaguepoints clear")
                return
            }
            points.override(player, value)
            player.mes("League Points set to $value until you log out.")
        }
}
