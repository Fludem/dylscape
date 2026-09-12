package org.rsmod.api.player.output

import java.util.BitSet
import net.rsprot.protocol.common.game.outgoing.inv.InventoryObject
import net.rsprot.protocol.game.outgoing.inv.UpdateInvFull
import net.rsprot.protocol.game.outgoing.inv.UpdateInvPartial
import net.rsprot.protocol.game.outgoing.inv.UpdateInvStopTransmit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

public object UpdateInventory {
    /** @see [UpdateInvFull] */
    public fun updateInvFull(player: Player, inv: Inventory) {
        val highestSlot = inv.lastOccupiedSlot()
        val provider = RspObjProvider(inv.objs)
        val message = UpdateInvFull(-(1234 + inv.type.id), inv.type.id, highestSlot, provider)
        player.client.write(message)
    }

    /**
     * Sends [inv] as the *other* player's copy of an inventory both sides share, which is how a
     * trade shows each player what the other is offering.
     *
     * The client decides this from the combined id: below [MIRROR_COMBINED_ID] it treats the update
     * as a mirror and stores it separately from the viewer's own inventory of the same id, so both
     * offers can be drawn from `tradeoffer` at once.
     *
     * Unlike [updateInvFull] this is not driven by [Player.startInvTransmit] - the inventory being
     * sent belongs to someone else - so it must be re-sent whenever that inventory changes.
     *
     * @see [UpdateInvFull]
     */
    public fun updateInvFullOther(player: Player, inv: Inventory) {
        val highestSlot = inv.lastOccupiedSlot()
        val provider = RspObjProvider(inv.objs)
        val combinedId = MIRROR_COMBINED_ID - inv.type.id
        val message = UpdateInvFull(combinedId, inv.type.id, highestSlot, provider)
        player.client.write(message)
    }

    /** @see [UpdateInvPartial] */
    public fun updateInvPartial(player: Player, inv: Inventory) {
        val changedSlots = inv.modifiedSlots.asSequence().iterator()
        val provider = RspIndexedObjProvider(inv.objs, changedSlots)
        val message = UpdateInvPartial(-1, -(1234 + inv.type.id), inv.type.id, provider)
        player.client.write(message)
    }

    /** @see [UpdateInvStopTransmit] */
    public fun updateInvStopTransmit(player: Player, inv: Inventory) {
        player.client.write(UpdateInvStopTransmit(inv.type.id))
    }

    /**
     * Mostly used for emulation when re-syncing an inventory. [slot] is usually sent as value `0`.
     */
    public fun resendSlot(inv: Inventory, slot: Int) {
        inv.modifiedSlots.set(slot)
    }

    /** Anything below this marks an inventory update as a mirror; rsprot requires < -70000. */
    private const val MIRROR_COMBINED_ID = -70001
}

private fun BitSet.asSequence(): Sequence<Int> = sequence {
    var index = nextSetBit(0)
    while (index >= 0) {
        yield(index)
        index = nextSetBit(index + 1)
    }
}

private class RspObjProvider(private val objs: Array<InvObj?>) : UpdateInvFull.ObjectProvider {
    override fun provide(slot: Int): Long {
        val obj = objs.getOrNull(slot) ?: return InventoryObject.NULL
        return InventoryObject(slot, obj.id, obj.count)
    }
}

private class RspIndexedObjProvider(private val objs: Array<InvObj?>, updateSlots: Iterator<Int>) :
    UpdateInvPartial.IndexedObjectProvider(updateSlots) {
    override fun provide(slot: Int): Long {
        val obj = objs.getOrNull(slot) ?: return InventoryObject(slot, -1, -1)
        return InventoryObject(slot, obj.id, obj.count)
    }
}
