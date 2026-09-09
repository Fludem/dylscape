package org.rsmod.content.custom.accountmode

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.PlayerList

/**
 * Sends a message to every player currently online.
 *
 * There was no world-broadcast primitive before this: `realms.login_broadcast` is a per-realm login
 * message, not a broadcast, and the only code doing a real one was inlined in the realm-config
 * commands.
 */
@Singleton
public class WorldBroadcast @Inject constructor(private val players: PlayerList) {
    public fun send(text: String) {
        for (player in players) {
            player.mes(text, ChatType.Broadcast)
        }
    }
}
