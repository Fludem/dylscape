package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.script.onOpHeld1
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Overgrown relic's Leprechaun's vault: op1 Open is a bank you carry. It opens the same pair of
 * interfaces a bank booth does. The relic's Farming effects live in the farming module.
 */
class OvergrownVaultScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(league_objs.leprechauns_vault) {
            if (!player.hasRelic(Relic.Overgrown)) {
                mes("The vault stays shut for you.")
                return@onOpHeld1
            }
            ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
        }
    }
}
