package org.rsmod.api.net.rsprot.handlers

import com.github.michaelbull.logging.InlineLogger
import net.rsprot.protocol.message.IncomingGameMessage
import org.rsmod.game.entity.Player

/**
 * Silently discards an incoming message.
 *
 * Every prot the client is able to send must have a consumer registered for it:
 * `Session.processIncomingPackets` throws an `IllegalStateException` when a decoded message has no
 * entry in the consumer repository, and `PlayerInputProcess.tryOrDisconnect` turns that throw into
 * a forced disconnect. Registering this handler for a prot we do not implement makes it a no-op
 * instead of a dropped connection.
 *
 * The discarded message is logged at debug level. Without it, a player action that lands on an
 * unimplemented prot is indistinguishable from one the server received and chose to ignore, which
 * makes "I clicked it and nothing happened" impossible to diagnose.
 *
 * The type parameter of [MessageHandler] is contravariant, so a single instance can be registered
 * for any message type.
 */
class NoopMessageHandler : MessageHandler<IncomingGameMessage> {
    private val logger = InlineLogger()

    override fun handle(player: Player, message: IncomingGameMessage) {
        logger.debug { "[Noop] Discarded unhandled message for $player: $message" }
    }
}
