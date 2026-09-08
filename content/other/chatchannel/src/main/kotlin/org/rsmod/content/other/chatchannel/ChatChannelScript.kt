package org.rsmod.content.other.chatchannel

import jakarta.inject.Inject
import org.rsmod.api.player.events.PlayerChatEvents
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChatChannelScript @Inject constructor(private val channel: GlobalFriendChat) :
    PluginScript() {
    override fun ScriptContext.startup() {
        // `Login` rather than `EngineLogin`: the latter is a keyed event and `LoginScript` already
        // owns its only id. By this point the player is in the player list with their name
        // assigned.
        onPlayerLogin {
            // Joining first: a channel notification has nowhere to render until the client believes
            // it is in the channel. The joiner is skipped, they get the channel list instead.
            channel.join(player)
            channel.notify("${player.channelName} has logged in.", except = player)
        }

        // `Delete` rather than `Logout`: it is the single terminal path out of the player list, and
        // the player has already been removed from it, so the broadcasts skip them for free.
        onEvent<SessionStateEvent.Delete> {
            channel.leave(player)
            channel.notify("${player.channelName} has logged out.")
        }

        onEvent<PlayerChatEvents.ChannelMessage> {
            if (type != CLAN_CHAT_TYPE) {
                channel.broadcast(player, text)
            }
        }
    }

    private companion object {
        /**
         * The only chat type the `MessagePublic` decoder singles out. It covers every clan channel,
         * with the trailing `clanType` byte picking between main and guest. Clan chat has no server
         * side, so `//` messages are dropped rather than relayed.
         */
        const val CLAN_CHAT_TYPE: Int = 3

        val Player.channelName: String
            get() = displayName.ifBlank { username }
    }
}
