package org.rsmod.content.custom.trading.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.invtx.invMoveAll
import org.rsmod.api.invtx.invTransfer
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.ClientScripts.interfaceInvInit
import org.rsmod.api.player.output.GameMessage
import org.rsmod.api.player.output.UpdateInventory.updateInvFullOther
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.ifCloseModal
import org.rsmod.api.player.ui.ifOpenMain
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.ui.ifSetText
import org.rsmod.api.script.advanced.onOpPlayer4
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.content.custom.trading.TradeSession
import org.rsmod.content.custom.trading.configs.trade_components
import org.rsmod.content.custom.trading.configs.trade_interfaces
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountModeRules
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Player-to-player trading on the vanilla screens.
 *
 * Both screens draw themselves from the `tradeoffer` inventory, so this only has to move objs, send
 * the two inventories and fill in the text. The partner's offer is the *same* inventory id sent as
 * a mirror - see [updateInvFullOther] - which is why it must be re-sent by hand whenever it
 * changes.
 *
 * The offer inventory is `Perm` scope, so anything left in it is saved. Every path out of a trade
 * therefore empties it back into the player's inventory, including logout, which fires before the
 * account save. [reclaimStrandedOffer] is the belt-and-braces version for a server that stopped
 * mid-trade.
 *
 * This owns the "Trade with" op, and with it the ironman rule that used to live in
 * `IronmanTradeScript`: op 4 is the only real guard, since the client is free to send an op that
 * was never offered in the menu.
 */
class TradeScript
@Inject
constructor(private val eventBus: EventBus, private val invTypes: InvTypeList) : PluginScript() {
    /** Keyed under both players, so either side's click finds the trade. */
    private val sessions = HashMap<Player, TradeSession>()

    /** Who each player has asked to trade. Cleared once the other side asks back. */
    private val requests = HashMap<Player, Player>()

    override fun ScriptContext.startup() {
        onOpPlayer4 { requestTrade(it.target) }

        onIfModalButton(trade_components.side_layer) { offerObj(it.comsub, it.op) }
        onIfModalButton(trade_components.your_offer) { withdrawObj(it.comsub, it.op) }
        onIfModalButton(trade_components.accept) { acceptOffer(player) }
        onIfModalButton(trade_components.decline) { decline(player) }
        onIfModalButton(trade_components.confirm_accept) { acceptConfirm(player) }
        onIfModalButton(trade_components.confirm_decline) { decline(player) }

        onIfClose(trade_interfaces.main) { screenClosed(player) }
        onIfClose(trade_interfaces.confirm) { screenClosed(player) }

        onPlayerLogin { player.reclaimStrandedOffer() }
        onPlayerLogout {
            decline(player)
            requests.remove(player)
            requests.values.removeIf { it === player }
        }
    }

    private fun ProtectedAccess.requestTrade(target: Player) {
        if (!AccountModeRules.canTradePlayers(player)) {
            mes("As an ironman, you cannot trade other players.")
            return
        }
        if (!AccountModeRules.canTradePlayers(target)) {
            mes("That player is an ironman and cannot trade.")
            return
        }
        if (target === player || player in sessions) {
            return
        }
        if (target in sessions) {
            mes("Other player is busy at the moment.")
            return
        }

        // They asked first, so this click is the acceptance rather than a new request.
        if (requests[target] === player) {
            requests.remove(target)
            requests.remove(player)
            openTrade(player, target)
            return
        }

        requests[player] = target
        mes("Sending trade offer...")
        GameMessage.requestMes(
            player = target,
            text = "wishes to trade with you.",
            name = player.displayName,
            type = ChatType.TradeReq,
        )
    }

    private fun openTrade(first: Player, second: Player) {
        val session =
            TradeSession(
                first = TradeSession.Side(first, first.offerInv()),
                second = TradeSession.Side(second, second.offerInv()),
            )
        sessions[first] = session
        sessions[second] = session
        for (side in session.sides) {
            side.player.openOfferScreen(session)
        }
        session.refreshAll()
    }

    private fun Player.openOfferScreen(session: TradeSession) {
        val partner = session.otherSide(this).player
        startInvTransmit(session.sideOf(this).offer)
        startInvTransmit(inv)
        ifOpenMainSidePair(
            main = trade_interfaces.main,
            side = trade_interfaces.side,
            colour = -1,
            transparency = -1,
            eventBus = eventBus,
        )
        interfaceInvInit(
            player = this,
            inv = inv,
            target = trade_components.side_layer,
            objRowCount = 4,
            objColCount = 7,
            op1 = "Offer<col=ff9040>",
            op2 = "Offer-5<col=ff9040>",
            op3 = "Offer-10<col=ff9040>",
            op4 = "Offer-All<col=ff9040>",
            op5 = "Offer-X<col=ff9040>",
        )
        ifSetEvents(trade_components.side_layer, 0 until inv.size, *OBJ_EVENTS)
        ifSetEvents(
            trade_components.your_offer,
            0 until session.sideOf(this).offer.size,
            *OBJ_EVENTS,
        )
        ifSetEvents(trade_components.accept, -1..-1, IfEvent.Op1)
        ifSetEvents(trade_components.decline, -1..-1, IfEvent.Op1)
        ifSetText(trade_components.title, "Trading with: ${partner.displayName}")
    }

    private fun Player.openConfirmScreen(session: TradeSession) {
        val partner = session.otherSide(this).player
        ifCloseModal(trade_interfaces.side, eventBus)
        ifOpenMain(trade_interfaces.confirm, eventBus)
        ifSetEvents(trade_components.confirm_accept, -1..-1, IfEvent.Op1)
        ifSetEvents(trade_components.confirm_decline, -1..-1, IfEvent.Op1)
        ifSetText(trade_components.confirm_opponent, "Trading with:<br>${partner.displayName}")
        ifSetText(trade_components.confirm_title, "Are you sure you want to make this trade?")
        ifSetText(trade_components.confirm_giving, "You are about to give:")
        ifSetText(trade_components.confirm_receiving, "In return you will receive:")
        updateInvFullOther(this, session.otherSide(this).offer)
    }

    private fun TradeSession.refreshAll() {
        for (side in sides) {
            side.player.refresh(this)
        }
    }

    /** The partner's offer is not a transmitted inventory, so it is pushed on every change. */
    private fun Player.refresh(session: TradeSession) {
        val partner = session.otherSide(this)
        updateInvFullOther(this, partner.offer)
        if (session.stage == TradeSession.Stage.Offer) {
            val free = partner.player.inv.freeSpace()
            val slots = if (free == 1) "slot" else "slots"
            ifSetText(
                trade_components.free_space_text,
                "${partner.player.displayName} has $free free inventory $slots.",
            )
            ifSetText(trade_components.status, statusText(session))
        }
    }

    private fun Player.statusText(session: TradeSession): String =
        when {
            session.otherSide(this).accepted -> "Other player has accepted."
            session.sideOf(this).accepted -> "Waiting for other player..."
            else -> ""
        }

    private suspend fun ProtectedAccess.offerObj(slot: Int, op: IfButtonOp) {
        val session = sessions[player] ?: return
        if (session.stage != TradeSession.Stage.Offer) {
            return
        }
        val obj = player.inv[slot] ?: return
        val count = requestedCount(op) ?: countDialog("How many would you like to offer?")
        moveObjs(player.inv, session.sideOf(player).offer, slot, obj.id, count, session)
    }

    private suspend fun ProtectedAccess.withdrawObj(slot: Int, op: IfButtonOp) {
        val session = sessions[player] ?: return
        if (session.stage != TradeSession.Stage.Offer) {
            return
        }
        val offer = session.sideOf(player).offer
        val obj = offer[slot] ?: return
        val count = requestedCount(op) ?: countDialog("How many would you like to remove?")
        moveObjs(offer, player.inv, slot, obj.id, count, session)
    }

    /** `null` means the op asks the player for a count. */
    private fun requestedCount(op: IfButtonOp): Int? =
        when (op) {
            IfButtonOp.Op1 -> 1
            IfButtonOp.Op2 -> 5
            IfButtonOp.Op3 -> 10
            IfButtonOp.Op4 -> Int.MAX_VALUE
            else -> null
        }

    /**
     * Moves up to [count] of [objId], starting at [slot].
     *
     * Non-stackables hold one per slot, so "Offer-All" has to walk the rest of the inventory rather
     * than lean on a single transfer's count.
     */
    private fun ProtectedAccess.moveObjs(
        from: Inventory,
        into: Inventory,
        slot: Int,
        objId: Int,
        count: Int,
        session: TradeSession,
    ) {
        if (count <= 0) {
            return
        }
        var remaining = count
        var moved = 0
        val slots = listOf(slot) + (0 until from.size).filter { it != slot }
        for (index in slots) {
            if (remaining <= 0) {
                break
            }
            val obj = from[index] ?: continue
            if (obj.id != objId) {
                continue
            }
            val transfer =
                player.invTransfer(
                    from = from,
                    fromSlot = index,
                    count = minOf(obj.count, remaining),
                    into = into,
                )
            val completed = transfer.completed()
            moved += completed
            remaining -= completed
            if (transfer.failure || completed == 0) {
                break
            }
        }
        if (moved == 0) {
            return
        }
        // Any change invalidates both acceptances, so nobody can accept a different trade than the
        // one they agreed to.
        session.resetAcceptance()
        session.refreshAll()
    }

    private fun acceptOffer(player: Player) {
        val session = sessions[player] ?: return
        if (session.stage != TradeSession.Stage.Offer) {
            return
        }
        session.sideOf(player).accepted = true
        if (session.sides.all { it.accepted }) {
            session.openConfirmScreens()
        } else {
            session.refreshAll()
        }
    }

    private fun TradeSession.openConfirmScreens() {
        stage = TradeSession.Stage.Confirm
        resetAcceptance()
        // Opening a modal closes the one it replaces, which would otherwise read as a decline.
        switchingScreens = true
        for (side in sides) {
            side.player.openConfirmScreen(this)
        }
        switchingScreens = false
    }

    private fun acceptConfirm(player: Player) {
        val session = sessions[player] ?: return
        if (session.stage != TradeSession.Stage.Confirm) {
            return
        }
        session.sideOf(player).accepted = true
        if (session.sides.all { it.accepted }) {
            session.complete()
            return
        }
        player.ifSetText(trade_components.confirm_title, "Waiting for other player...")
        session
            .otherSide(player)
            .player
            .ifSetText(trade_components.confirm_title, "Other player has accepted.")
    }

    private fun TradeSession.complete() {
        val giving = first.offer.occupiedSpace()
        val receiving = second.offer.occupiedSpace()
        if (second.player.inv.freeSpace() < giving || first.player.inv.freeSpace() < receiving) {
            for (side in sides) {
                side.player.mes("Not enough inventory space to complete this trade.")
            }
            cancel()
            return
        }

        second.player.invMoveAll(from = first.offer, into = second.player.inv)
        first.player.invMoveAll(from = second.offer, into = first.player.inv)

        switchingScreens = true
        for (side in sides) {
            side.player.endTrade(side)
            side.player.mes("Accepted trade.")
        }
        forget()
    }

    private fun decline(player: Player) {
        val session = sessions[player] ?: return
        session.cancel(declinedBy = player)
    }

    private fun screenClosed(player: Player) {
        val session = sessions[player] ?: return
        if (session.switchingScreens) {
            return
        }
        session.cancel(declinedBy = player)
    }

    private fun TradeSession.cancel(declinedBy: Player? = null) {
        switchingScreens = true
        for (side in sides) {
            side.player.endTrade(side)
            if (side.player === declinedBy) {
                side.player.mes("Trade cancelled.")
            } else {
                side.player.mes("Other player declined trade.")
            }
        }
        forget()
    }

    /** Returns the offer, closes the screens, and stops transmitting the offer inventory. */
    private fun Player.endTrade(side: TradeSession.Side) {
        invMoveAll(from = side.offer, into = inv)
        ifCloseModal(trade_interfaces.confirm, eventBus)
        ifCloseModal(trade_interfaces.main, eventBus)
        ifCloseModal(trade_interfaces.side, eventBus)
        stopInvTransmit(side.offer)
    }

    private fun TradeSession.forget() {
        for (side in sides) {
            sessions.remove(side.player)
        }
    }

    /** The offer inventory is saved, so a server that stopped mid-trade leaves objs in it. */
    private fun Player.reclaimStrandedOffer() {
        val offer = offerInv()
        if (offer.isEmpty()) {
            return
        }
        val moved = invMoveAll(from = offer, into = inv)
        if (moved.anyCompleted()) {
            mes("Items from an unfinished trade have been returned to you.")
        }
    }

    private fun Player.offerInv(): Inventory = invMap.getOrPut(invTypes[invs.tradeoffer])

    private companion object {
        /** Offer/remove ops 1-5 plus Examine, matching the ops the screens' own scripts draw. */
        val OBJ_EVENTS =
            arrayOf(IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op10)
    }
}
