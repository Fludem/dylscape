package org.rsmod.api.net.rsprot.provider

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.jvm.java
import net.rsprot.protocol.game.incoming.buttons.If1Button
import net.rsprot.protocol.game.incoming.buttons.If3Button
import net.rsprot.protocol.game.incoming.buttons.IfButtonD
import net.rsprot.protocol.game.incoming.buttons.IfButtonT
import net.rsprot.protocol.game.incoming.buttons.IfRunScript
import net.rsprot.protocol.game.incoming.buttons.IfSubOp
import net.rsprot.protocol.game.incoming.clan.AffinedClanSettingsAddBannedFromChannel
import net.rsprot.protocol.game.incoming.clan.AffinedClanSettingsSetMutedFromChannel
import net.rsprot.protocol.game.incoming.clan.ClanChannelFullRequest
import net.rsprot.protocol.game.incoming.clan.ClanChannelKickUser
import net.rsprot.protocol.game.incoming.clan.ClanSettingsFullRequest
import net.rsprot.protocol.game.incoming.events.EventAppletFocus
import net.rsprot.protocol.game.incoming.events.EventCameraPosition
import net.rsprot.protocol.game.incoming.events.EventKeyboard
import net.rsprot.protocol.game.incoming.events.EventMouseClickV1
import net.rsprot.protocol.game.incoming.events.EventMouseClickV2
import net.rsprot.protocol.game.incoming.events.EventMouseMove
import net.rsprot.protocol.game.incoming.events.EventMouseScroll
import net.rsprot.protocol.game.incoming.events.EventNativeMouseMove
import net.rsprot.protocol.game.incoming.friendchat.FriendChatJoinLeave
import net.rsprot.protocol.game.incoming.friendchat.FriendChatKick
import net.rsprot.protocol.game.incoming.friendchat.FriendChatSetRank
import net.rsprot.protocol.game.incoming.locs.OpLoc
import net.rsprot.protocol.game.incoming.locs.OpLoc6
import net.rsprot.protocol.game.incoming.locs.OpLocT
import net.rsprot.protocol.game.incoming.messaging.MessagePrivate
import net.rsprot.protocol.game.incoming.messaging.MessagePublic
import net.rsprot.protocol.game.incoming.misc.client.ConnectionTelemetry
import net.rsprot.protocol.game.incoming.misc.client.DetectModifiedClient
import net.rsprot.protocol.game.incoming.misc.client.Idle
import net.rsprot.protocol.game.incoming.misc.client.MapBuildComplete
import net.rsprot.protocol.game.incoming.misc.client.MembershipPromotionEligibility
import net.rsprot.protocol.game.incoming.misc.client.NoTimeout
import net.rsprot.protocol.game.incoming.misc.client.RSevenStatus
import net.rsprot.protocol.game.incoming.misc.client.ReflectionCheckReply
import net.rsprot.protocol.game.incoming.misc.client.SendPingReply
import net.rsprot.protocol.game.incoming.misc.client.SoundJingleEnd
import net.rsprot.protocol.game.incoming.misc.client.WindowStatus
import net.rsprot.protocol.game.incoming.misc.user.BugReport
import net.rsprot.protocol.game.incoming.misc.user.ClickWorldMap
import net.rsprot.protocol.game.incoming.misc.user.ClientCheat
import net.rsprot.protocol.game.incoming.misc.user.CloseModal
import net.rsprot.protocol.game.incoming.misc.user.HiscoreRequest
import net.rsprot.protocol.game.incoming.misc.user.IfCrmViewClick
import net.rsprot.protocol.game.incoming.misc.user.MoveGameClick
import net.rsprot.protocol.game.incoming.misc.user.MoveMinimapClick
import net.rsprot.protocol.game.incoming.misc.user.OculusLeave
import net.rsprot.protocol.game.incoming.misc.user.SendSnapshot
import net.rsprot.protocol.game.incoming.misc.user.SetChatFilterSettings
import net.rsprot.protocol.game.incoming.misc.user.SetHeading
import net.rsprot.protocol.game.incoming.misc.user.Teleport
import net.rsprot.protocol.game.incoming.npcs.OpNpc
import net.rsprot.protocol.game.incoming.npcs.OpNpc6
import net.rsprot.protocol.game.incoming.npcs.OpNpcT
import net.rsprot.protocol.game.incoming.objs.OpObj
import net.rsprot.protocol.game.incoming.objs.OpObj6
import net.rsprot.protocol.game.incoming.objs.OpObjT
import net.rsprot.protocol.game.incoming.players.OpPlayer
import net.rsprot.protocol.game.incoming.players.OpPlayerT
import net.rsprot.protocol.game.incoming.resumed.ResumePCountDialog
import net.rsprot.protocol.game.incoming.resumed.ResumePNameDialog
import net.rsprot.protocol.game.incoming.resumed.ResumePObjDialog
import net.rsprot.protocol.game.incoming.resumed.ResumePStringDialog
import net.rsprot.protocol.game.incoming.resumed.ResumePauseButton
import net.rsprot.protocol.game.incoming.social.FriendListAdd
import net.rsprot.protocol.game.incoming.social.FriendListDel
import net.rsprot.protocol.game.incoming.social.IgnoreListAdd
import net.rsprot.protocol.game.incoming.social.IgnoreListDel
import net.rsprot.protocol.game.incoming.worldentities.OpWorldEntity
import net.rsprot.protocol.game.incoming.worldentities.OpWorldEntity6
import net.rsprot.protocol.game.incoming.worldentities.OpWorldEntityT
import net.rsprot.protocol.message.codec.incoming.GameMessageConsumerRepositoryBuilder
import net.rsprot.protocol.message.codec.incoming.provider.DefaultGameMessageConsumerRepositoryProvider
import org.rsmod.api.net.rsprot.handlers.ClientCheatHandler
import org.rsmod.api.net.rsprot.handlers.CloseModalHandler
import org.rsmod.api.net.rsprot.handlers.If3ButtonHandler
import org.rsmod.api.net.rsprot.handlers.IfButtonDHandler
import org.rsmod.api.net.rsprot.handlers.IfButtonTHandler
import org.rsmod.api.net.rsprot.handlers.MapBuildCompleteHandler
import org.rsmod.api.net.rsprot.handlers.MessagePublicHandler
import org.rsmod.api.net.rsprot.handlers.MoveGameClickHandler
import org.rsmod.api.net.rsprot.handlers.MoveMinimapClickHandler
import org.rsmod.api.net.rsprot.handlers.NoopMessageHandler
import org.rsmod.api.net.rsprot.handlers.OpLoc6Handler
import org.rsmod.api.net.rsprot.handlers.OpLocHandler
import org.rsmod.api.net.rsprot.handlers.OpLocTHandler
import org.rsmod.api.net.rsprot.handlers.OpNpc6Handler
import org.rsmod.api.net.rsprot.handlers.OpNpcHandler
import org.rsmod.api.net.rsprot.handlers.OpNpcTHandler
import org.rsmod.api.net.rsprot.handlers.OpObj6Handler
import org.rsmod.api.net.rsprot.handlers.OpObjHandler
import org.rsmod.api.net.rsprot.handlers.OpPlayerHandler
import org.rsmod.api.net.rsprot.handlers.OpPlayerTHandler
import org.rsmod.api.net.rsprot.handlers.ResumePCountDialogHandler
import org.rsmod.api.net.rsprot.handlers.ResumePNameDialogHandler
import org.rsmod.api.net.rsprot.handlers.ResumePObjDialogHandler
import org.rsmod.api.net.rsprot.handlers.ResumePStringDialogHandler
import org.rsmod.api.net.rsprot.handlers.ResumePauseButtonHandler
import org.rsmod.api.net.rsprot.handlers.WindowStatusHandler
import org.rsmod.game.entity.Player

@Singleton
class MessageConsumerProvider
@Inject
constructor(
    private val windowStatus: WindowStatusHandler,
    private val moveGameClick: MoveGameClickHandler,
    private val moveMinimapClick: MoveMinimapClickHandler,
    private val opLoc: OpLocHandler,
    private val opLocT: OpLocTHandler,
    private val opLoc6: OpLoc6Handler,
    private val clientCheat: ClientCheatHandler,
    private val opNpc: OpNpcHandler,
    private val opNpcT: OpNpcTHandler,
    private val opNpc6: OpNpc6Handler,
    private val opPlayer: OpPlayerHandler,
    private val opPlayerT: OpPlayerTHandler,
    private val messagePublic: MessagePublicHandler,
    private val if3Button: If3ButtonHandler,
    private val closeModal: CloseModalHandler,
    private val resumePauseButton: ResumePauseButtonHandler,
    private val opObj: OpObjHandler,
    private val opObj6: OpObj6Handler,
    private val resumePCountDialog: ResumePCountDialogHandler,
    private val resumePNameDialog: ResumePNameDialogHandler,
    private val resumePStringDialog: ResumePStringDialogHandler,
    private val resumePObjDialog: ResumePObjDialogHandler,
    private val ifButtonD: IfButtonDHandler,
    private val ifButtonT: IfButtonTHandler,
    private val mapBuildComplete: MapBuildCompleteHandler,
) {
    fun get(): DefaultGameMessageConsumerRepositoryProvider<Player> {
        val builder = GameMessageConsumerRepositoryBuilder<Player>()
        builder.addListener(WindowStatus::class.java, windowStatus)
        builder.addListener(MoveGameClick::class.java, moveGameClick)
        builder.addListener(MoveMinimapClick::class.java, moveMinimapClick)
        builder.addListener(OpLoc::class.java, opLoc)
        builder.addListener(OpLocT::class.java, opLocT)
        builder.addListener(OpLoc6::class.java, opLoc6)
        builder.addListener(ClientCheat::class.java, clientCheat)
        builder.addListener(OpNpc::class.java, opNpc)
        builder.addListener(OpNpcT::class.java, opNpcT)
        builder.addListener(OpNpc6::class.java, opNpc6)
        builder.addListener(OpPlayer::class.java, opPlayer)
        builder.addListener(OpPlayerT::class.java, opPlayerT)
        builder.addListener(MessagePublic::class.java, messagePublic)
        builder.addListener(If3Button::class.java, if3Button)
        builder.addListener(CloseModal::class.java, closeModal)
        builder.addListener(ResumePauseButton::class.java, resumePauseButton)
        builder.addListener(OpObj::class.java, opObj)
        builder.addListener(OpObj6::class.java, opObj6)
        builder.addListener(ResumePCountDialog::class.java, resumePCountDialog)
        builder.addListener(ResumePNameDialog::class.java, resumePNameDialog)
        builder.addListener(ResumePStringDialog::class.java, resumePStringDialog)
        builder.addListener(ResumePObjDialog::class.java, resumePObjDialog)
        builder.addListener(IfButtonD::class.java, ifButtonD)
        builder.addListener(IfButtonT::class.java, ifButtonT)
        builder.addListener(MapBuildComplete::class.java, mapBuildComplete)
        registerUnhandled(builder)
        return DefaultGameMessageConsumerRepositoryProvider(builder.build())
    }

    /**
     * Registers [NoopMessageHandler] for every prot the client can send that we do not implement.
     *
     * A decoded message with no registered consumer disconnects the player (see
     * [NoopMessageHandler]), so this list must stay in sync with the decoders registered by
     * rsprot's `DesktopGameMessageDecoderRepository`.
     */
    private fun registerUnhandled(builder: GameMessageConsumerRepositoryBuilder<Player>) {
        val noop = NoopMessageHandler()

        // Friends chat, clan chat and social lists. The global friends channel is driven entirely
        // server-side, so every client-initiated membership change is ignored.
        builder.addListener(FriendChatJoinLeave::class.java, noop)
        builder.addListener(FriendChatKick::class.java, noop)
        builder.addListener(FriendChatSetRank::class.java, noop)
        builder.addListener(MessagePrivate::class.java, noop)
        builder.addListener(FriendListAdd::class.java, noop)
        builder.addListener(FriendListDel::class.java, noop)
        builder.addListener(IgnoreListAdd::class.java, noop)
        builder.addListener(IgnoreListDel::class.java, noop)
        builder.addListener(SetChatFilterSettings::class.java, noop)
        builder.addListener(ClanChannelFullRequest::class.java, noop)
        builder.addListener(ClanSettingsFullRequest::class.java, noop)
        builder.addListener(ClanChannelKickUser::class.java, noop)
        builder.addListener(AffinedClanSettingsAddBannedFromChannel::class.java, noop)
        builder.addListener(AffinedClanSettingsSetMutedFromChannel::class.java, noop)

        // Telemetry and other unsolicited client chatter.
        builder.addListener(Idle::class.java, noop)
        builder.addListener(NoTimeout::class.java, noop)
        builder.addListener(SoundJingleEnd::class.java, noop)
        builder.addListener(ConnectionTelemetry::class.java, noop)
        builder.addListener(DetectModifiedClient::class.java, noop)
        builder.addListener(MembershipPromotionEligibility::class.java, noop)
        builder.addListener(ReflectionCheckReply::class.java, noop)
        builder.addListener(SendPingReply::class.java, noop)
        builder.addListener(RSevenStatus::class.java, noop)
        builder.addListener(EventAppletFocus::class.java, noop)
        builder.addListener(EventCameraPosition::class.java, noop)
        builder.addListener(EventKeyboard::class.java, noop)
        builder.addListener(EventMouseClickV1::class.java, noop)
        builder.addListener(EventMouseClickV2::class.java, noop)
        builder.addListener(EventMouseMove::class.java, noop)
        builder.addListener(EventMouseScroll::class.java, noop)
        builder.addListener(EventNativeMouseMove::class.java, noop)

        // Miscellaneous user actions with no server-side implementation yet.
        builder.addListener(BugReport::class.java, noop)
        builder.addListener(HiscoreRequest::class.java, noop)
        builder.addListener(ClickWorldMap::class.java, noop)
        builder.addListener(OculusLeave::class.java, noop)
        builder.addListener(SendSnapshot::class.java, noop)
        builder.addListener(Teleport::class.java, noop)
        builder.addListener(SetHeading::class.java, noop)
        builder.addListener(IfCrmViewClick::class.java, noop)
        builder.addListener(If1Button::class.java, noop)
        builder.addListener(IfRunScript::class.java, noop)
        builder.addListener(IfSubOp::class.java, noop)
        builder.addListener(OpObjT::class.java, noop)
        builder.addListener(OpWorldEntity::class.java, noop)
        builder.addListener(OpWorldEntity6::class.java, noop)
        builder.addListener(OpWorldEntityT::class.java, noop)
    }
}
