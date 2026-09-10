package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias league_components = LeagueComponents

/**
 * `league_relics` (655) and its companions, all vanilla. Which component feeds which clientscript
 * argument is set out in `RelicScreen`, next to the calls themselves.
 */
object LeagueComponents : ComponentReferences() {
    // The grid, in `league_relics_init`'s onLoad argument order.
    val relics_header = find("league_relics:header")
    val relics_available_header = find("league_relics:relic_available_header")
    val relics_backgrounds = find("league_relics:backgrounds")
    val relics_icons = find("league_relics:icons")
    val relics_outlines = find("league_relics:outlines")
    val relics_names = find("league_relics:names")

    /** One "View" child per relic, numbered tier by tier from 0 - see `Relic.forComsub`. */
    val relics_clickzones = find("league_relics:clickzones")
    val relics_view_all = find("league_relics:view_all")
    val relics_view_all_scrollbar = find("league_relics:view_all_scrollbar")
    val relics_view_one = find("league_relics:view_one")
    val relics_confirm = find("league_relics:confirm")

    /** Raised by the client on a View click; the expanded view or a redraw takes it down. */
    val relics_loading = find("league_relics:loading")
    val relics_loading_overlay = find("league_relics:loading_overlay")
    val relics_btn_menu = find("league_relics:league_btn_menu")
    val relics_menu_frame = find("league_relics:league_menu_frame")
    val relics_menu_overlay = find("league_relics:league_menu_overlay")
    val relics_progress_bar = find("league_relics:progress_bar")
    val relics_tooltip = find("league_relics:tooltip")
    val relics_close = find("league_relics:close_button")

    // The expanded single-relic view.
    val relics_icon = find("league_relics:icon")
    val relics_name = find("league_relics:name")
    val relics_description_header = find("league_relics:description_header")
    val relics_description = find("league_relics:description")
    val relics_select_button = find("league_relics:select_button")
    val relics_select_back = find("league_relics:select_back")
    val relics_passive_header = find("league_relics:passive_header")
    val relics_passive_description = find("league_relics:passive_description")

    // The "Are you sure?" popup the Select button raises, client-side.
    val relics_confirm_overlay = find("league_relics:confirm_overlay")
    val relics_confirm_frame = find("league_relics:confirm_frame")
    val relics_confirm_text = find("league_relics:confirm_text")

    /** The only button on the whole screen whose press reaches the server during a pick. */
    val relics_confirm_button = find("league_relics:confirm_button")
    val relics_confirm_cancel = find("league_relics:confirm_cancel")

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
