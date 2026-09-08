package org.rsmod.content.other.chatchannel

import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.rsprot.protocol.game.outgoing.friendchat.MessageFriendChannel
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFull
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFullV2
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelSingleUser
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList

/**
 * A single server-wide friends chat that every online player is permanently a member of, so that
 * `/message` reaches the whole world instead of only the speaker's local view.
 *
 * Membership is not persisted: it is exactly "whoever is in [playerList]" at any moment, so a
 * restart cannot leave the channel out of sync with reality.
 */
@Singleton
class GlobalFriendChat
@Inject
constructor(private val playerList: PlayerList, private val serverConfig: ServerConfig) {
    private var messageCounter: Int = 0

    private val worldId: Int
        get() = serverConfig.world

    private val worldName: String
        get() = "World ${serverConfig.world}"

    /**
     * Sends the full member list to [player], then tells everyone already online about them.
     *
     * The incremental packet is used for the existing members on purpose: re-sending the full list
     * to everyone on each login is quadratic in bytes and makes the channel tab rebuild itself.
     */
    fun join(player: Player) {
        val entries = playerList.map(::createEntry)
        val joinUpdate =
            UpdateFriendChatChannelFullV2.JoinUpdate(
                CHANNEL_OWNER,
                CHANNEL_NAME,
                KICK_RANK,
                entries,
            )
        player.client.write(UpdateFriendChatChannelFullV2(joinUpdate))

        val added =
            UpdateFriendChatChannelSingleUser.AddedFriendChatUser(
                player.channelName,
                worldId,
                MEMBER_RANK,
                worldName,
            )
        val message = UpdateFriendChatChannelSingleUser(added)
        for (other in playerList) {
            if (other === player) {
                continue
            }
            other.client.write(message)
        }
    }

    /**
     * Tells everyone still online that [player] has left.
     *
     * Intended for [org.rsmod.game.entity.player.SessionStateEvent.Delete]: by that point [player]
     * has already been taken out of [playerList], so the loop naturally skips them.
     */
    fun leave(player: Player) {
        val removed =
            UpdateFriendChatChannelSingleUser.RemovedFriendChatUser(player.channelName, worldId)
        val message = UpdateFriendChatChannelSingleUser(removed)
        for (other in playerList) {
            other.client.write(message)
        }
    }

    /**
     * Relays [text] from [sender] to every online player, [sender] included - the client does not
     * render your own channel message locally, it only draws the copy the server sends back.
     */
    fun broadcast(sender: Player, text: String) {
        val message =
            MessageFriendChannel(
                sender.channelName,
                CHANNEL_NAME,
                worldId,
                nextMessageCounter(),
                sender.modLevel.clientCode,
                text,
            )
        for (player in playerList) {
            player.client.write(message)
        }
    }

    /** Sends [text] to every online player, other than [except], as a channel notification. */
    fun notify(text: String, except: Player? = null) {
        for (player in playerList) {
            if (player === except) {
                continue
            }
            player.mes(text, ChatType.FriendsChatNotification)
        }
    }

    private fun createEntry(player: Player): UpdateFriendChatChannelFull.FriendChatEntry =
        UpdateFriendChatChannelFull.FriendChatEntry(
            player.channelName,
            worldId,
            MEMBER_RANK,
            worldName,
        )

    private fun nextMessageCounter(): Int {
        messageCounter = (messageCounter + 1) and MAX_MESSAGE_COUNTER
        return messageCounter
    }

    private companion object {
        /** Must be base-37 encodable: at most 12 characters of `[a-zA-Z0-9_ ]`. */
        const val CHANNEL_NAME: String = "Global"

        /** A name no account can hold, so nobody is ever treated as the channel owner. */
        const val CHANNEL_OWNER: String = "Global"

        /** Everyone joins as a plain member; nobody ever reaches [KICK_RANK]. */
        const val MEMBER_RANK: Int = 0

        /** Owner rank. With every member at [MEMBER_RANK], the client greys out kick for all. */
        const val KICK_RANK: Int = 7

        /** `worldMessageCounter` is written as a 24-bit value. */
        const val MAX_MESSAGE_COUNTER: Int = 0xFFFFFF

        /** [Player.displayName] is only assigned when the realm auto-assigns display names. */
        val Player.channelName: String
            get() = displayName.ifBlank { username }
    }
}
