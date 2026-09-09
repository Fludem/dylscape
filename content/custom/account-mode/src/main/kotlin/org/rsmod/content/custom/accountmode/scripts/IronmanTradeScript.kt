package org.rsmod.content.custom.accountmode.scripts

import org.rsmod.api.player.output.mes
import org.rsmod.api.script.advanced.onOpPlayer4
import org.rsmod.game.entity.player.AccountModeRules
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Refuses player-to-player trading for ironmen.
 *
 * Trading is not implemented in this codebase at all - there is no trade module, and op 4 had no
 * handler - so today this script only ever prints a refusal. It is registered now on purpose.
 * `EventBus` errors on a duplicate handler id, so whoever implements trading will collide with this
 * registration and has to confront the ironman rule rather than discovering it later. A comment
 * would not have that property.
 *
 * The login script separately hides the "Trade with" menu entry from ironmen, but that is cosmetic:
 * `OpPlayerHandler` never verifies that an op was actually sent, so this is the real guard.
 */
public class IronmanTradeScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpPlayer4 {
            if (!AccountModeRules.canTradePlayers(player)) {
                player.mes("As an ironman, you cannot trade other players.")
                return@onOpPlayer4
            }
            player.mes("Trading is not yet implemented.")
        }
    }
}
