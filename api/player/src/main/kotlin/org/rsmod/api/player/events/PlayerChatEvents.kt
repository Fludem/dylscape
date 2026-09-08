package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class PlayerChatEvents {
    /**
     * Fired when a player sends chat that belongs to a chat channel rather than to overhead public
     * chat, i.e. text typed with a `/` or `//` prefix.
     *
     * The client has no dedicated friends chat or clan chat prot: everything typed into the chatbox
     * arrives as `MessagePublic`, and the leading [type] byte says where it was aimed. [type] and
     * [clanType] are passed through untouched so that the consumer decides how to map them.
     */
    public data class ChannelMessage(
        public val player: Player,
        public val type: Int,
        public val clanType: Int,
        public val text: String,
    ) : UnboundEvent
}
