package org.rsmod.content.interfaces.journal.tab

import org.rsmod.api.utils.vars.VarEnumDelegate

enum class SideJournalTab(override val varValue: Int) : VarEnumDelegate {
    Summary(varValue = 0),
    Quests(varValue = 1),
    Tasks(varValue = 2),
    /**
     * `side_journal` reserves five tab slots and `[proc,side_journal_switchtab]` switches on
     * `side_journal_tab` in slot order, so the fourth value is adventure paths and the fifth is
     * leagues. The fourth is not implemented, which is why this jumps from 2 to 4.
     */
    Leagues(varValue = 4),
}
