package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias league_components = LeagueComponents

object LeagueComponents : ComponentReferences() {
    val relics_close = find("league_relics:close_button")

    /** Layer holding one click target per relic in the grid. */
    val relics_clickzones = find("league_relics:clickzones")

    /** Expanded single-relic view, shown after a grid click. */
    val relics_select_button = find("league_relics:select_button")
    val relics_select_back = find("league_relics:select_back")

    /** "Are you sure?" overlay raised by [relics_select_button]. */
    val relics_confirm_button = find("league_relics:confirm_button")
    val relics_confirm_cancel = find("league_relics:confirm_cancel")

    /**
     * The dimming "Loading..." overlay `league_relics_init` leaves up while it waits for a server
     * response we do not know how to send. Hiding it is enough to make the grid usable.
     */
    val relics_loading = find("league_relics:loading")
    val relics_loading_overlay = find("league_relics:loading_overlay")

    /**
     * Buttons down the `league_side_panel` footer. All four carry Op1 in the cache, so clicks reach
     * the server without the script arming them.
     */
    val side_mastery_button = find("league_side_panel:mastery_button")
    val side_tasks_button = find("league_side_panel:tasks_button")
    val side_areas_button = find("league_side_panel:areas_button")
    val side_relics_button = find("league_side_panel:relics_button")

    val tasks_close = find("league_tasks:close_button")
    val tasks_list = find("league_tasks:tasks")
}
