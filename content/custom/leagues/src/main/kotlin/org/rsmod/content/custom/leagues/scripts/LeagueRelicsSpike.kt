package org.rsmod.content.custom.leagues.scripts

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.script.onCommand
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Spike for driving the vanilla `league_relics` interface (id 655) from the server.
 *
 * Nothing here is authored content: the interface, its 63 components and the `league_relics_init`
 * clientscript all ship in the rev 233 cache. The point of this script is to establish how much of
 * the Relics screen the vanilla cs2 draws for us, and where the server has to take over.
 *
 * ### What the cache decode already settled
 *
 * The 68 relic definitions are structs (param 879 name, 880 description, 885 tier, the rest sprite
 * ids), but **the server cannot choose which relics the grid offers**. Each
 * `league_relic_selection_<tier>` varbit is only *three bits wide* — max value 7 — so it holds a
 * slot index within a tier, not a struct id. The index-to-relic mapping lives inside the cs2, and
 * no enum in the cache exposes it. The offered set is therefore fixed to whatever league this cache
 * revision shipped; changing it means authoring cs2, which this repo cannot compile.
 *
 * What the server *can* drive: the points bar, which slot is selected per tier, and the repick
 * flags. That is what this spike exercises.
 * - `::leagues [modal] [leagueType] [points]` opens the screen and arms the buttons.
 * - `::leaguespick <tier> <slot>` writes a selection varbit directly, to see the grid shade in.
 * - `::leaguestasks` opens `league_tasks`, the task list.
 *
 * Every button the cs2 routes back is echoed with its component sub-id and op, since that mapping
 * is the other thing we need before writing real selection logic.
 */
class LeagueRelicsSpike
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher, private val eventBus: EventBus) :
    PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onCommand("leagues") {
            modLevel = modlevels.admin
            desc = "Open the leagues relics interface (::leagues [modal] [leagueType] [points])"
            cheat(::openRelics)
        }
        onCommand("leaguestasks") {
            modLevel = modlevels.admin
            desc = "Open the leagues task list (::leaguestasks [leagueType])"
            cheat(::openTasks)
        }
        onCommand("leaguespick") {
            modLevel = modlevels.admin
            desc = "Set a relic selection varbit (::leaguespick tier slot)"
            cheat(::pickRelic)
        }

        // The button handler routes by how the interface was opened: an overlay publishes
        // IfOverlayButton, a modal publishes IfModalButton. The opener is switchable below, so both
        // routings are registered.
        // `ifClose` only closes modals, so it can never dismiss the full-overlay opener;
        // `ifCloseSub` closes both.
        onIfModalButton(league_components.relics_close) {
            logButton(player, "close[modal]", it.comsub, it.op)
            ifCloseSub(league_interfaces.relics)
        }
        onIfOverlayButton(league_components.relics_close) {
            logButton(player, "close[overlay]", comsub, op)
            player.ifCloseSub(league_interfaces.relics, eventBus)
            player.ifCloseSub(league_interfaces.side_panel, eventBus)
        }

        onIfOverlayButton(league_components.relics_clickzones) {
            logButton(player, "clickzones[overlay]", comsub, op)
            protectedAccess.launch(player) { selectRelicAt(comsub) }
        }
        onIfModalButton(league_components.relics_clickzones) {
            logButton(player, "clickzones[modal]", it.comsub, it.op)
            selectRelicAt(it.comsub)
        }
        onIfOverlayButton(league_components.tasks_close) {
            logButton(player, "tasks_close[overlay]", comsub, op)
            player.ifCloseSub(league_interfaces.tasks, eventBus)
        }
        onIfOverlayButton(league_components.tasks_list) {
            logButton(player, "tasks_list[overlay]", comsub, op)
        }

        registerGridButton("select_button", league_components.relics_select_button)
        registerGridButton("select_back", league_components.relics_select_back)
        registerGridButton("confirm_button", league_components.relics_confirm_button)
        registerGridButton("confirm_cancel", league_components.relics_confirm_cancel)
    }

    private fun ScriptContext.registerGridButton(label: String, button: ComponentType) {
        onIfModalButton(button) { logButton(player, "$label[modal]", it.comsub, it.op) }
        onIfOverlayButton(button) { logButton(player, "$label[overlay]", comsub, op) }
    }

    private fun openRelics(cheat: Cheat) =
        with(cheat) {
            // `league_relics` leads with `infinity`/`universe` components, the signature of a
            // full-screen leagues panel rather than something that belongs in the small mainmodal
            // slot. Which opener the cs2 actually wants is still unconfirmed, so both are
            // reachable.
            val fullScreen = args.none { it.equals("modal", ignoreCase = true) }
            val numbers = args.mapNotNull(String::toIntOrNull)
            val leagueType = numbers.getOrNull(0) ?: DEFAULT_LEAGUE_TYPE
            val points = numbers.getOrNull(1) ?: DEFAULT_POINTS
            val launched =
                protectedAccess.launch(player) {
                    // Without these the grid renders empty: the cs2 has no league to pick a relic
                    // set from.
                    vars[league_varbits.account] = 1
                    vars[league_varbits.type] = leagueType
                    // Which of these two the "N points earned" bar reads is still unconfirmed, so
                    // both are set. Isolate with `::varp league_points_completed <n>`.
                    vars[league_varps.points_currency] = points
                    vars[league_varps.points_completed] = points
                    if (fullScreen) {
                        openRelicsScreen()
                    } else {
                        ifOpenMainSidePair(league_interfaces.relics, league_interfaces.side_panel)
                        hideRelicsLoadingOverlay()
                        armRelicsButtons()
                    }
                    val opener = if (fullScreen) "full overlay" else "main+side modal"
                    mes("Opened league_relics as $opener.")
                    mes("league_type=$leagueType points=$points")
                }
            if (!launched) {
                player.mes("You are busy right now.")
            }
        }

    private fun openTasks(cheat: Cheat) =
        with(cheat) {
            val leagueType = args.firstNotNullOfOrNull(String::toIntOrNull) ?: DEFAULT_LEAGUE_TYPE
            val launched =
                protectedAccess.launch(player) {
                    vars[league_varbits.account] = 1
                    vars[league_varbits.type] = leagueType
                    openTasksScreen()
                    mes("Opened league_tasks (league_type=$leagueType).")
                }
            if (!launched) {
                player.mes("You are busy right now.")
            }
        }

    private fun pickRelic(cheat: Cheat) =
        with(cheat) {
            val tier = args.getOrNull(0)?.toIntOrNull()
            val slot = args.getOrNull(1)?.toIntOrNull()
            if (tier == null || slot == null) {
                player.mes("Use as ::leaguespick tier slot (ex: 0 3)")
                return
            }
            val varbit = league_varbits[tier]
            if (varbit == null) {
                player.mes("No selection varbit for tier $tier (valid tiers are 0-7).")
                return
            }
            if (slot !in SELECTION_SLOTS) {
                player.mes("Slot must be ${SELECTION_SLOTS.first}-${SELECTION_SLOTS.last}.")
                player.mes("The varbit is 3 bits wide, so it indexes a slot, not a relic id.")
                return
            }
            val launched =
                protectedAccess.launch(player) {
                    vars[varbit] = slot
                    mes("Set ${varbit.internalName} to slot $slot.")
                }
            if (!launched) {
                player.mes("You are busy right now.")
            }
        }

    /**
     * Selects the relic the player clicked in the grid.
     *
     * The vanilla flow raises an expanded single-relic view with its own Select and confirm steps,
     * driven by `[clientscript,league_relic_expanded_view]` (21 int arguments, 746 opcodes).
     * Neither that argument list nor the response the client waits for is recoverable without
     * decompiling the proc, so this instead commits the choice straight to the selection varbit and
     * redraws.
     *
     * The grid is laid out as one column per tier, so the clicked sub-id maps to a tier and a slot
     * within it. [RELICS_PER_TIER] is read off the rendered grid rather than the cache; if a click
     * reports the wrong tier, this is the constant to change.
     */
    private suspend fun ProtectedAccess.selectRelicAt(comsub: Int) {
        if (comsub < 0) {
            return
        }
        val tier = comsub / RELICS_PER_TIER
        val slot = (comsub % RELICS_PER_TIER) + 1
        val varbit = league_varbits[tier]
        if (varbit == null) {
            mes("Clicked sub-id $comsub -> tier $tier, which has no selection varbit.")
            return
        }
        vars[league_varbits.last_viewed] = comsub.coerceAtMost(MAX_LAST_VIEWED)
        vars[varbit] = slot
        mes("Selected tier $tier slot $slot (sub-id $comsub).")

        // The grid only re-reads the selection varbits on init, so reopen to redraw.
        redrawRelics()
    }

    private fun ProtectedAccess.redrawRelics() = openRelicsScreen()

    /**
     * `league_relic_last_viewed` is 5 bits, so it too is an index rather than a struct id. Echoing
     * it tells us which slot the cs2 put on screen when the confirm button was pressed.
     */
    private fun ProtectedAccess.reportLastViewed() {
        val viewed = vars[league_varbits.last_viewed]
        mes("league_relic_last_viewed=$viewed")
        logger.debug { "league_relics confirm: last_viewed=$viewed" }
    }

    private fun logButton(player: Player, component: String, comsub: Int, op: IfButtonOp) {
        player.mes("$component comsub=$comsub op=$op")
        logger.debug { "league_relics button: component=$component, comsub=$comsub, op=$op" }
    }

    private companion object {
        private const val DEFAULT_POINTS = 20

        /**
         * `league_type` is 5 bits, so 1-31 are all valid. Which value maps to which league is
         * unknown; sweep it with `::leagues <n>` until the grid populates.
         */
        private const val DEFAULT_LEAGUE_TYPE = 5

        /** Rows in the rendered relics grid: one column per tier, three relics down each. */
        private const val RELICS_PER_TIER = 3

        /** `league_relic_last_viewed` is 5 bits. */
        private const val MAX_LAST_VIEWED = 31

        /** Selection varbits are 3 bits wide; 0 reads as "nothing chosen for this tier". */
        private val SELECTION_SLOTS = 1..7
    }
}
