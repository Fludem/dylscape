package org.rsmod.content.custom.accountmode.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onEvent
import org.rsmod.content.custom.accountmode.WorldBroadcast
import org.rsmod.content.custom.accountmode.displayName
import org.rsmod.content.custom.accountmode.markHardcoreDead
import org.rsmod.content.custom.accountmode.syncAccountModeVarBits
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountMode
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Demotes a hardcore ironman to a regular ironman when they die, and tells the world about it.
 *
 * This hangs off [PlayerDeathEvents.Death] rather than the death queue, because the queue takes
 * exactly one handler and `api/death-plugin` already owns it.
 *
 * The demotion is persisted with the character's next save, the same way `xpRate` already is, so a
 * crash in the window between death and save would leave the account hardcore. That is the generous
 * direction to fail in, and it is not worth forcing a save for.
 */
public class HardcoreDeathScript @Inject constructor(private val broadcast: WorldBroadcast) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<PlayerDeathEvents.Death> { player.demoteIfHardcore() }
    }

    private fun Player.demoteIfHardcore() {
        if (accountMode != AccountMode.HardcoreIronman) {
            return
        }
        accountMode = AccountMode.Ironman
        syncAccountModeVarBits()
        markHardcoreDead()
        mes("Your Hardcore Ironman status has been removed.", ChatType.Broadcast)
        broadcast.send("$displayName has fallen. They are no longer a Hardcore Ironman.")
    }
}
