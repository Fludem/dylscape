package org.rsmod.content.other.chatchannel

import jakarta.inject.Inject
import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import net.rsprot.protocol.game.outgoing.friendchat.MessageFriendChannel
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelFullV2
import net.rsprot.protocol.game.outgoing.friendchat.UpdateFriendChatChannelSingleUser
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.net.rsprot.handlers.MessagePublicHandler
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.capture.CaptureClient
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Player
import org.rsmod.game.type.mod.ModLevelTypeList

class ChatChannelTestDeps
@Inject
constructor(
    val messagePublic: MessagePublicHandler,
    val channel: GlobalFriendChat,
    val modLevels: ModLevelTypeList,
)

class ChatChannelScriptTest {
    private val Player.capture: CaptureClient
        get() = client as CaptureClient

    @Test
    fun GameTestState.`slash message reaches every online player including the sender`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")
            val beta = login(deps, "Beta")
            alpha.capture.clearOutgoing()
            beta.capture.clearOutgoing()

            deps.messagePublic.handle(alpha, chatMessage(FRIENDS_CHAT_TYPE, "hello"))

            for (player in listOf(alpha, beta)) {
                val relay = player.capture.single<MessageFriendChannel>()
                assertEquals("Alpha", relay.sender)
                assertEquals("global", relay.channelName.lowercase())
                assertEquals("hello", relay.message)
            }
            // Channel messages must never also render as an overhead balloon.
            assertNull(alpha.publicMessage)
        }

    @Test
    fun GameTestState.`plain message still renders overhead and stays out of the channel`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")
            val beta = login(deps, "Beta")
            alpha.capture.clearOutgoing()
            beta.capture.clearOutgoing()

            deps.messagePublic.handle(alpha, chatMessage(PUBLIC_CHAT_TYPE, "hello"))

            assertNotNull(alpha.publicMessage)
            assertEquals("hello", alpha.publicMessage?.text)
            assertTrue(alpha.capture.hasNone<MessageFriendChannel>())
            assertTrue(beta.capture.hasNone<MessageFriendChannel>())
        }

    @Test
    fun GameTestState.`double-slash clan message is dropped entirely`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")
            alpha.capture.clearOutgoing()

            deps.messagePublic.handle(alpha, chatMessage(CLAN_CHAT_TYPE, "hello", clanType = 0))

            assertNull(alpha.publicMessage)
            assertTrue(alpha.capture.hasNone<MessageFriendChannel>())
        }

    @Test
    fun GameTestState.`login sends the full list to the joiner and an add to everyone else`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")

            val firstJoin = alpha.capture.single<UpdateFriendChatChannelFullV2>()
            val firstUpdate = firstJoin.updateType as UpdateFriendChatChannelFullV2.JoinUpdate
            assertEquals("global", firstUpdate.channelName.lowercase())
            // The scope registers a player of its own, so the list is sized off the real thing.
            assertEquals(players.count(), firstUpdate.entries.size)
            assertTrue(firstUpdate.entries.any { it.name == "Alpha" })

            alpha.capture.clearOutgoing()
            val beta = login(deps, "Beta")

            val secondJoin = beta.capture.single<UpdateFriendChatChannelFullV2>()
            val secondUpdate = secondJoin.updateType as UpdateFriendChatChannelFullV2.JoinUpdate
            val names = secondUpdate.entries.map { it.name }
            assertTrue(names.containsAll(listOf("Alpha", "Beta")))

            // The player already online gets the cheap incremental packet, not a rebuild.
            assertTrue(alpha.capture.hasNone<UpdateFriendChatChannelFullV2>())
            val added = alpha.capture.single<UpdateFriendChatChannelSingleUser>()
            assertEquals("Beta", added.user.name)
        }

    @Test
    fun GameTestState.`leaving broadcasts a removal to everyone still online`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")
            val beta = login(deps, "Beta")
            alpha.capture.clearOutgoing()

            // `unregisterPlayer` does not publish `Delete`, so drive the channel directly - this is
            // the same call the script makes from its `Delete` subscription.
            unregisterPlayer(beta)
            deps.channel.leave(beta)

            val removed = alpha.capture.single<UpdateFriendChatChannelSingleUser>()
            assertEquals("Beta", removed.user.name)
        }

    @Test
    fun GameTestState.`each message gets its own world message counter`() =
        runInjectedGameTest(ChatChannelTestDeps::class, null, ChatChannelScript::class) { deps ->
            val alpha = login(deps, "Alpha")
            alpha.capture.clearOutgoing()

            deps.messagePublic.handle(alpha, chatMessage(FRIENDS_CHAT_TYPE, "one"))
            deps.messagePublic.handle(alpha, chatMessage(FRIENDS_CHAT_TYPE, "two"))

            val counters =
                alpha.capture.filterIsInstance<MessageFriendChannel>().map {
                    it.worldMessageCounter
                }
            assertEquals(2, counters.size)
            assertEquals(2, counters.toSet().size)
        }

    /**
     * The harness does not run the login sequence that assigns [Player.modLevel], and the chat path
     * reads it for the crown, so it has to be set by hand here.
     */
    private fun GameTestScope.login(deps: ChatChannelTestDeps, name: String): Player =
        registerPlayer(
            player =
                Player().apply {
                    displayName = name
                    modLevel = deps.modLevels[modlevels.player]
                }
        )

    private fun chatMessage(type: Int, text: String, clanType: Int = -1): MessagePublic =
        MessagePublic(type, 0, 0, text, null, clanType)

    private fun assertTrue(condition: Boolean) = assertEquals(true, condition)

    private companion object {
        const val PUBLIC_CHAT_TYPE: Int = 0
        const val CLAN_CHAT_TYPE: Int = 3

        /**
         * The value the client sends for `/` text. Only the clan type is named by rsprot's decoder;
         * the script treats every non-public, non-clan type as friends chat, so the exact number
         * here only needs to be one the client can actually produce.
         */
        const val FRIENDS_CHAT_TYPE: Int = 1
    }
}
