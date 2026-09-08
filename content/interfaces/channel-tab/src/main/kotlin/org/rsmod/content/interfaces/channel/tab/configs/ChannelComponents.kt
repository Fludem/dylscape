package org.rsmod.content.interfaces.channel.tab.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias channel_components = ChannelComponents

object ChannelComponents : ComponentReferences() {
    val contents = find("side_channels:contents")
    val tab_0 = find("side_channels:tab_0")
    val tab_1 = find("side_channels:tab_1")
    val tab_2 = find("side_channels:tab_2")
    val tab_3 = find("side_channels:tab_3")
}
