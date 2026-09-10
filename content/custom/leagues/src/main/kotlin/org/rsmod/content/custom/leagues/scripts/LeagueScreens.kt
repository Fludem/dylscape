package org.rsmod.content.custom.leagues.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.game.type.interf.IfEvent

/**
 * Opening the full-screen leagues panels, and the two clientscripts that drive `league_relics`.
 *
 * Both were decoded from the cache (`LeagueRelicsScriptDump`); the argument lists below are the
 * whole contract, so a change in a future cache revision is a change here and nowhere else.
 */
internal object LeagueClientScripts {
    /**
     * `[clientscript,league_relics_draw_selections]`. Draws the grid from the selection varbits and
     * takes down the Loading overlay. `league_relics_init` only arms it on resize, so the first
     * draw - and every redraw after a pick - is the server's job.
     */
    const val DRAW_SELECTIONS = 733

    /**
     * `[clientscript,league_relic_expanded_view]`. The server's answer to a relic's View click;
     * everything from there to the Confirm press (Select, Back, Cancel) runs on the client.
     */
    const val EXPANDED_VIEW = 3193
}

/** Covers every relic's View child: the client numbers them 0 until [Relic.entries] size. */
private val CLICKZONE_RANGE = 0..127

/**
 * The expanded view creates its Confirm button as a dynamic child whose index we cannot pin without
 * running the client, so every plausible sub is armed.
 */
private val CONFIRM_RANGE = 0..127

/** Wide enough to cover the task rows the list can hold. */
private val TASK_LIST_RANGE = 0..511

/** `league_relics_draw_selections`'s 16th argument; proc 228 always returns 1 for the title. */
private const val TITLE_CHILD = 1

/** The draw's 17th argument, 0 when `league_relics_init` runs it. */
private const val DRAW_FLAG = 0

/** The expanded view's last argument: -1 hides the tier passive, which this server lacks. */
private const val NO_PASSIVE = -1

/**
 * `league_relics` leads with `infinity`/`universe` components, the signature of a full-screen
 * leagues panel rather than something belonging in the small mainmodal slot.
 */
internal fun ProtectedAccess.openRelicsScreen() {
    ifOpenFullOverlay(league_interfaces.relics)
    armRelicsButtons()
    drawRelicSelections()
}

/** Draws (or redraws) the grid, which also returns it from the expanded view. */
internal fun ProtectedAccess.drawRelicSelections() {
    runClientScript(LeagueClientScripts.DRAW_SELECTIONS, *drawSelectionsArgs())
}

/**
 * Shows [relic]'s expanded view. [state] decides its button: 2 draws "Select", anything else draws
 * "Locked" / "Unlocked" and makes the button print `league_relic_not_available`'s reason for that
 * same code.
 */
internal fun ProtectedAccess.showExpandedView(relic: Relic, state: Int) {
    runClientScript(LeagueClientScripts.EXPANDED_VIEW, *expandedViewArgs(relic, state))
}

internal fun ProtectedAccess.openTasksScreen() {
    ifOpenFullOverlay(league_interfaces.tasks)
    ifSetEvents(league_components.tasks_close, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.tasks_list, TASK_LIST_RANGE, IfEvent.Op1)
}

/**
 * Arms the grid's click targets. A click on a plain component arrives with `comsub == -1` and is
 * validated against the events baked into the cache, so only the dynamic children need this.
 */
private fun ProtectedAccess.armRelicsButtons() {
    ifSetEvents(league_components.relics_clickzones, CLICKZONE_RANGE, IfEvent.Op1)
    ifSetEvents(league_components.relics_confirm_button, CONFIRM_RANGE, IfEvent.Op1)
    ifSetEvents(league_components.relics_close, 0..0, IfEvent.Op1)
}

/** `league_relics_init`'s onLoad arguments 0-14, then the title child, the flag, and 15-16. */
private fun drawSelectionsArgs(): Array<Any> =
    with(league_components) {
        arrayOf(
            relics_header.packed,
            relics_available_header.packed,
            relics_backgrounds.packed,
            relics_icons.packed,
            relics_outlines.packed,
            relics_names.packed,
            relics_clickzones.packed,
            relics_view_all.packed,
            relics_view_all_scrollbar.packed,
            relics_view_one.packed,
            relics_confirm.packed,
            relics_loading.packed,
            relics_btn_menu.packed,
            relics_menu_frame.packed,
            relics_menu_overlay.packed,
            TITLE_CHILD,
            DRAW_FLAG,
            relics_progress_bar.packed,
            relics_tooltip.packed,
        )
    }

/**
 * The expanded view's 21 arguments, recovered from how the script uses each one: 0, 1 and 17 are
 * what it hides to leave the grid (and what `league_relic_back` shows again), 2 is the view it
 * shows, 3 the Loading overlay it takes down, then the view's own parts, the confirm popup that
 * `Select` raises (10; `Cancel` hides it and a redraw hides it too), the state, the relic and the
 * tier passive.
 */
private fun expandedViewArgs(relic: Relic, state: Int): Array<Any> =
    with(league_components) {
        arrayOf(
            relics_view_all.packed,
            relics_available_header.packed,
            relics_view_one.packed,
            relics_loading.packed,
            relics_icon.packed,
            relics_name.packed,
            relics_description_header.packed,
            relics_description.packed,
            relics_select_button.packed,
            relics_select_back.packed,
            relics_confirm.packed,
            relics_confirm_frame.packed,
            relics_confirm_text.packed,
            relics_confirm_button.packed,
            relics_confirm_cancel.packed,
            relics_passive_header.packed,
            relics_passive_description.packed,
            relics_view_all_scrollbar.packed,
            state,
            relic.struct.id,
            NO_PASSIVE,
        )
    }
