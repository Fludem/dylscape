package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varbit.VarBitType

typealias league_varps = LeagueVarps

typealias league_varbits = LeagueVarBits

object LeagueVarps : VarpReferences() {
    /**
     * The world's flag bitset. `[proc,league_world]` returns true when bit 30 is set and bit 29 is
     * clear, which is one of the two conditions for the journal's leagues tab to draw. No
     * clientscript writes this varp -- 27 read it and none pop it -- so the server owns it
     * outright.
     */
    val map_flags = find("map_flags_cached")

    /** Candidates for the "N points earned" progress bar at the top of `league_relics`. */
    val points_currency = find("league_points_currency")
    val points_completed = find("league_points_completed")
}

object LeagueVarBits : VarBitReferences() {
    /** Marks the player as being on a league account. Bit 0 of `league_general`. */
    val account = find("league_account")

    /**
     * Which league is running, bits 1..5 of `league_general`. With this at 0 the relics grid draws
     * its chrome and no relics, so the cs2 picks its relic set from here.
     */
    val type = find("league_type")

    /**
     * One varbit per relic tier, holding the struct id of the relic chosen for that tier. The
     * client's `league_relics_draw_selections` proc reads these to shade the grid.
     */
    val relic_selection =
        listOf(
            find("league_relic_selection_0"),
            find("league_relic_selection_1"),
            find("league_relic_selection_2"),
            find("league_relic_selection_3"),
            find("league_relic_selection_4"),
            find("league_relic_selection_5"),
            find("league_relic_selection_6"),
            find("league_relic_selection_7"),
        )

    /**
     * Bits 13..18 of `league_general`. `[proc,side_journal_switchtab]` draws the fifth journal tab
     * only when this reads 3 or higher, so it gates the leagues tab alongside
     * [LeagueVarps.map_flags].
     */
    val tutorial_completed = find("league_tutorial_completed")

    /** Set by the client before opening the expanded view; tells us which relic is on screen. */
    val last_viewed = find("league_relic_last_viewed")

    operator fun get(tier: Int): VarBitType? = relic_selection.getOrNull(tier)
}
