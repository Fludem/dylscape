package org.rsmod.content.interfaces.journal.tab.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias journal_interfaces = JournalInterfaces

object JournalInterfaces : InterfaceReferences() {
    val league_side_panel = find("league_side_panel")
}
