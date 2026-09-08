package org.rsmod.content.custom.leagues.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias league_interfaces = LeagueInterfaces

/**
 * Leagues interfaces ship in the vanilla rev 233 cache; none of these are authored by us. See
 * [LeagueComponents] for the `league_relics` layout this spike drives.
 */
object LeagueInterfaces : InterfaceReferences() {
    val relics = find("league_relics")

    /**
     * Vanilla never opens a leagues screen on its own; the side panel is its companion, and
     * `league_relics_init` appears to expect it to be present.
     */
    val side_panel = find("league_side_panel")

    val tasks = find("league_tasks")
}
