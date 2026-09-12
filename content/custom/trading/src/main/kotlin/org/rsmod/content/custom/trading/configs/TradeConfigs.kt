package org.rsmod.content.custom.trading.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias trade_interfaces = TradeInterfaces

typealias trade_components = TradeComponents

/**
 * The vanilla trade screens. `trademain` (335) and `tradeside` (336) are the first screen -- the
 * offer grids and the player's inventory -- and `tradeconfirm` (334) is the second.
 *
 * Both screens draw themselves: their setup scripts read the `tradeoffer` inventory and re-register
 * on its updates, so the server sends the two inventories and fills in the text. That, and which
 * component is which, was read off the cache in `TradeInterfaceDump` rather than guessed.
 */
object TradeInterfaces : InterfaceReferences() {
    val main = find("trademain")
    val side = find("tradeside")
    val confirm = find("tradeconfirm")
}

object TradeComponents : ComponentReferences() {
    /** Your own offer. The client puts "Remove", "Remove-5/10/All/X" and "Examine" on its slots. */
    val your_offer = find("trademain:your_offer")

    /** The partner's offer, drawn from the mirrored copy of `tradeoffer`. */
    val other_offer = find("trademain:other_offer")

    /** "Trading with: <name>". */
    val title = find("trademain:title")

    /** Blank, "Waiting for other player..." or "Other player has accepted." */
    val status = find("trademain:status")

    /** "<name> has N free inventory slots." */
    val free_space_text = find("trademain:free_space_text")

    val accept = find("trademain:accept")
    val decline = find("trademain:decline")

    /** The whole side interface is one empty layer; the inventory is built into it. */
    val side_layer = find("tradeside:side_layer")

    val confirm_title = find("tradeconfirm:title")
    val confirm_opponent = find("tradeconfirm:tradeopponent")
    val confirm_giving = find("tradeconfirm:you_will_give")
    val confirm_receiving = find("tradeconfirm:you_will_receive")
    val confirm_accept = find("tradeconfirm:trade2accept")
    val confirm_decline = find("tradeconfirm:trade2decline")
}
