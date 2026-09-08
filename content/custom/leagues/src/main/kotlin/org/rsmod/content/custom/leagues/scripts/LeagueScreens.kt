package org.rsmod.content.custom.leagues.scripts

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_interfaces
import org.rsmod.game.type.interf.IfEvent

/**
 * Opening and arming the two full-screen leagues panels.
 *
 * Both the `::leagues` commands and the journal's leagues tab raise the same screens, so the open
 * sequence lives here rather than being duplicated per entry point.
 */

/** Wide enough to cover every relic slot the grid can hold. */
private val CLICKZONE_RANGE = 0..127

/** Wide enough to cover the task rows the list can hold. */
private val TASK_LIST_RANGE = 0..511

/**
 * `league_relics` leads with `infinity`/`universe` components, the signature of a full-screen
 * leagues panel rather than something belonging in the small mainmodal slot. Opening it as a modal
 * renders it letterboxed into that slot instead.
 */
internal fun ProtectedAccess.openRelicsScreen() {
    ifOpenFullOverlay(league_interfaces.relics)
    hideRelicsLoadingOverlay()
    armRelicsButtons()
}

internal fun ProtectedAccess.openTasksScreen() {
    ifOpenFullOverlay(league_interfaces.tasks)
    ifSetEvents(league_components.tasks_close, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.tasks_list, TASK_LIST_RANGE, IfEvent.Op1)
}

/**
 * `league_relics_init` leaves a dimming "Loading..." overlay up, waiting on a server response whose
 * shape lives inside `[proc,league_relics_draw_selections]` (1064 opcodes). Hiding the two
 * components is enough to make the grid usable without decoding that.
 */
internal fun ProtectedAccess.hideRelicsLoadingOverlay() {
    ifSetHide(league_components.relics_loading, true)
    ifSetHide(league_components.relics_loading_overlay, true)
}

/**
 * Arms the grid's click targets.
 *
 * Only the list components need this. A click on a plain component arrives with `comsub == -1`, and
 * the button handler validates those against the events baked into the cache instead.
 */
internal fun ProtectedAccess.armRelicsButtons() {
    ifSetEvents(league_components.relics_clickzones, CLICKZONE_RANGE, IfEvent.Op1)
    ifSetEvents(league_components.relics_select_button, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.relics_select_back, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.relics_confirm_button, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.relics_confirm_cancel, 0..0, IfEvent.Op1)
    ifSetEvents(league_components.relics_close, 0..0, IfEvent.Op1)
}
