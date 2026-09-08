package org.rsmod.content.interfaces.channel.tab.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences

typealias channel_varbits = ChannelVarBits

object ChannelVarBits : VarBitReferences() {
    /**
     * The legacy `side_channels_tab` (930) is not referenced by a single clientscript in this
     * cache; this is the one the tab strip actually reads.
     */
    val tab_selected = find("side_channels_tab_selected")
}
