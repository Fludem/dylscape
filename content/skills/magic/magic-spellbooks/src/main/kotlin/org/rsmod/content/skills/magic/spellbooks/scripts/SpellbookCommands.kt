package org.rsmod.content.skills.magic.spellbooks.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.content.skills.magic.commons.SpellbookSwitcher
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** `::spellbook 0|1|2` for testing: standard, Ancient, Lunar. Arceuus is not on this server. */
class SpellbookCommands @Inject constructor(private val switcher: SpellbookSwitcher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onCommand("spellbook") {
            modLevel = modlevels.admin
            desc = "Switch spellbook: 0 standard, 1 ancient, 2 lunar"
            invalidArgs = "Use as ::spellbook 0|1|2"
            cheat {
                val book = args.firstOrNull()?.toIntOrNull()?.let(Spellbook::get)
                if (book == null || book == Spellbook.Arceuus) {
                    player.mes("Use as ::spellbook 0|1|2")
                    return@cheat
                }
                switcher.switch(player, book)
            }
        }
    }
}
