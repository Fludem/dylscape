package org.rsmod.api.net.rsprot.handlers

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import org.rsmod.api.player.events.PlayerChatEvents
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PublicMessage

class MessagePublicHandler @Inject constructor(private val eventBus: EventBus) :
    MessageHandler<MessagePublic> {
    override fun handle(player: Player, message: MessagePublic) {
        // Chatbox text aimed at a channel (`/` or `//`) arrives on this same prot, distinguished
        // only by the leading type byte. Anything that is not plain public chat is handed off as an
        // event so that it never renders as an overhead message.
        if (message.type != PUBLIC_CHAT_TYPE) {
            val event =
                PlayerChatEvents.ChannelMessage(
                    player = player,
                    type = message.type,
                    clanType = message.clanType,
                    text = message.message,
                )
            eventBus.publish(event)
            return
        }
        val publicMessage =
            PublicMessage(
                text = message.message,
                colour = message.colour,
                effect = message.effect,
                clanType = if (message.clanType == -1) null else message.clanType,
                modIcon = player.modLevel.clientCode,
                autoTyper = false,
                pattern = message.pattern?.asByteArray(),
            )
        player.publicMessage = publicMessage
    }

    private companion object {
        const val PUBLIC_CHAT_TYPE: Int = 0
    }
}
