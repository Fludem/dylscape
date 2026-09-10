package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varbit.VarBitType

typealias league_varps = LeagueVarps

typealias league_varbits = LeagueVarBits

/** Every varp here is vanilla and `Perm`, so all of it survives a relog without a migration. */
object LeagueVarps : VarpReferences() {
    /**
     * The world's flag bitset. `[proc,league_world]` returns true when bit 30 is set and bit 29 is
     * clear, which is one of the two conditions for the journal's leagues tab to draw. No
     * clientscript writes this varp -- 27 read it and none pop it -- so the server owns it
     * outright.
     */
    val map_flags = find("map_flags_cached")

    /**
     * What the relics grid compares each tier's points threshold (param 877) against. This is the
     * varp that actually unlocks tiers on the client.
     */
    val points_claimed = find("league_points_claimed")

    /** The progress bar varps; kept equal to [points_claimed]. */
    val points_currency = find("league_points_currency")
    val points_completed = find("league_points_completed")

    /** The Total Recall relic's saved tile, as a packed coordinate; 0 when nothing is saved. */
    val last_recall_coord = find("league_last_recall_source_coord")
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
     * One varbit per relic tier, holding the **1-based slot** of the relic picked in that tier (the
     * key into the tier's relic enum), or 0 for no pick. Three bits wide.
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

    /**
     * The Reloaded relic's extra pick: one varbit per tier, holding the 1-based slot of the relic
     * re-picked from that tier, or 0. At most one is ever set. They sit in `league_relics_other`,
     * which is `Perm`, and `[proc,league_relic_active]` reads them, so the grid shades a reloaded
     * relic as active without any server drawing.
     */
    val relic_selection_other =
        listOf(
            find("league_relic_selection_other_0"),
            find("league_relic_selection_other_1"),
            find("league_relic_selection_other_2"),
            find("league_relic_selection_other_3"),
            find("league_relic_selection_other_4"),
            find("league_relic_selection_other_5"),
            find("league_relic_selection_other_6"),
            find("league_relic_selection_other_7"),
        )

    operator fun get(tier: Int): VarBitType? = relic_selection.getOrNull(tier)
}
