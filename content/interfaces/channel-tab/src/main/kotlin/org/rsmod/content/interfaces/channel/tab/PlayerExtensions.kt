package org.rsmod.content.interfaces.channel.tab

import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.player.ui.ifCloseSub
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.content.interfaces.channel.tab.configs.channel_components
import org.rsmod.content.interfaces.channel.tab.configs.channel_interfaces
import org.rsmod.content.interfaces.channel.tab.configs.channel_varbits
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.InterfaceType

internal var Player.sideChannelTab by enumVarBit<SideChannelTab>(channel_varbits.tab_selected)

internal fun tabInterface(tab: SideChannelTab): InterfaceType =
    when (tab) {
        SideChannelTab.ChatChannel -> interfaces.chatchannel_current
        SideChannelTab.YourClan -> channel_interfaces.clans_sidepanel
        SideChannelTab.GuestClan -> channel_interfaces.clans_guest_sidepanel
        SideChannelTab.Grouping -> channel_interfaces.grouping
    }

/**
 * Opens the active tab's interface into `side_channels:contents`.
 *
 * The tab strip itself needs nothing from us: `side_channels:universe` carries a baked onload hook
 * that runs `[clientscript,side_channels_init]` with the four tab components, which is why the tabs
 * already draw. But no clientscript in the cache references the contents component, and cs2 has no
 * interface-opening opcode at all, so the panel body can only ever be filled by the server.
 *
 * The chat-channel tab additionally reads `chatchannel_blocked`, and draws a "blocked" notice
 * rather than the member list when it is set. It lives on the `toplevel_temp` varp, which nothing
 * writes today, so it is left at its default of zero.
 */
internal fun Player.openChannelTab(tab: SideChannelTab, eventBus: EventBus) {
    ifOpenOverlay(tabInterface(tab), channel_components.contents, eventBus)
}

internal fun Player.switchChannelTab(open: SideChannelTab, eventBus: EventBus) {
    val previous = sideChannelTab
    if (previous == open) {
        return
    }
    // Writing the varbit is what repaints the strip: it lives on varp `chat_filter_clan`, which
    // `side_channels:universe` listens to with `if_setonvartransmit`.
    sideChannelTab = open
    ifCloseSub(tabInterface(previous), eventBus)
    openChannelTab(open, eventBus)
}
