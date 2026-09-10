package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.script.onOpHeld3
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The echo tools' op3 "Toggle", which in vanilla switches their auto-smelt / auto-burn / auto-cook
 * effects. None of those are built, so the op says so rather than doing nothing.
 */
class EchoToolsScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld3(league_objs.echo_pickaxe) {
            mes("The echo pickaxe cannot smelt ores or cut gems yet.")
        }
        onOpHeld3(league_objs.echo_axe) { mes("The echo axe cannot burn or fletch logs yet.") }
        onOpHeld3(league_objs.echo_harpoon) { mes("The echo harpoon cannot cook fish yet.") }
    }
}
