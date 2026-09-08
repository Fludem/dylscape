package org.rsmod.content.interfaces.channel.tab.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias channel_interfaces = ChannelInterfaces

object ChannelInterfaces : InterfaceReferences() {
    val clans_sidepanel = find("clans_sidepanel")
    val clans_guest_sidepanel = find("clans_guest_sidepanel")
    val grouping = find("grouping")
}
