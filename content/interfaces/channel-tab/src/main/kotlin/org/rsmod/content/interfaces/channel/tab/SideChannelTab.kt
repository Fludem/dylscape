package org.rsmod.content.interfaces.channel.tab

import org.rsmod.api.utils.vars.VarEnumDelegate

/**
 * The four tabs of the `side_channels` side panel, in the order `[proc,side_channels_settab]`
 * expects. The index doubles as the key into `enum_3839` (tab op text) and `enum_3840` (tab icon),
 * which the client uses to label and draw the strip.
 *
 * Values 4 to 6 are the group ironman variants. Interface 707 hard-codes its fifth slot to `-1` and
 * hides it - those tabs only exist on `side_channels_large` - so they are not modelled here.
 */
enum class SideChannelTab(override val varValue: Int) : VarEnumDelegate {
    ChatChannel(varValue = 0),
    YourClan(varValue = 1),
    GuestClan(varValue = 2),
    Grouping(varValue = 3),
}
