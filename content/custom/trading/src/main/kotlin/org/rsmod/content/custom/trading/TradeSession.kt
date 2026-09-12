package org.rsmod.content.custom.trading

import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/**
 * One trade, shared by both players. [TradeScript] keys the same instance under both of them, so
 * either side's click finds the whole trade.
 */
internal class TradeSession(val first: Side, val second: Side) {
    var stage: Stage = Stage.Offer

    /**
     * Set while the screens are being swapped.
     *
     * Opening a modal publishes a close event for whatever it replaced, so moving to the confirm
     * screen looks exactly like the player closing the trade. Without this the trade would cancel
     * itself the moment both players accepted.
     */
    var switchingScreens: Boolean = false

    val sides: List<Side>
        get() = listOf(first, second)

    fun sideOf(player: Player): Side = if (first.player === player) first else second

    fun otherSide(player: Player): Side = if (first.player === player) second else first

    fun resetAcceptance() {
        first.accepted = false
        second.accepted = false
    }

    class Side(val player: Player, val offer: Inventory) {
        var accepted: Boolean = false
    }

    enum class Stage {
        Offer,
        Confirm,
    }
}
