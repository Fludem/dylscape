package org.rsmod.content.interfaces.channel.tab.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.interfaces.channel.tab.SideChannelTab
import org.rsmod.content.interfaces.channel.tab.configs.channel_components
import org.rsmod.content.interfaces.channel.tab.openChannelTab
import org.rsmod.content.interfaces.channel.tab.sideChannelTab
import org.rsmod.content.interfaces.channel.tab.switchChannelTab
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ChannelTabScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen(interfaces.side_channels) { player.openActiveChannelTab() }

        // Each tab carries `events=0x2` in the cache, so op 1 reaches us as an overlay button. The
        // `if_setonop` the client installs on them is cosmetic and does not suppress the packet.
        onIfOverlayButton(channel_components.tab_0) {
            player.switchChannelTab(SideChannelTab.ChatChannel)
        }

        onIfOverlayButton(channel_components.tab_1) {
            player.switchChannelTab(SideChannelTab.YourClan)
        }

        onIfOverlayButton(channel_components.tab_2) {
            player.switchChannelTab(SideChannelTab.GuestClan)
        }

        onIfOverlayButton(channel_components.tab_3) {
            player.switchChannelTab(SideChannelTab.Grouping)
        }
    }

    private fun Player.openActiveChannelTab() = openChannelTab(sideChannelTab, eventBus)

    private fun Player.switchChannelTab(open: SideChannelTab) = switchChannelTab(open, eventBus)
}
