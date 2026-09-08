package org.rsmod.api.player.output

import net.rsprot.protocol.game.outgoing.logout.Logout
import net.rsprot.protocol.game.outgoing.logout.LogoutWithReason
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow
import net.rsprot.protocol.game.outgoing.misc.client.HintArrow.TileHintArrow.HintArrowTilePosition
import net.rsprot.protocol.game.outgoing.misc.client.ServerTickEnd
import net.rsprot.protocol.game.outgoing.misc.client.UpdateRebootTimer
import net.rsprot.protocol.game.outgoing.misc.player.SetPlayerOp
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public object MiscOutput {
    /** @see [SetPlayerOp] */
    public fun setPlayerOp(player: Player, slot: Int, op: String?, priority: Boolean = false) {
        player.client.write(SetPlayerOp(slot, priority, op))
    }

    /**
     * Draws the floating hint arrow above [npc]. Only one hint arrow of any kind shows at a time; a
     * later call replaces the earlier one, and [resetHintArrow] clears it.
     *
     * @see [HintArrow.NpcHintArrow]
     */
    public fun hintArrowNpc(player: Player, npc: Npc) {
        player.client.write(HintArrow(HintArrow.NpcHintArrow(npc.slotId)))
    }

    /** Draws the hint arrow above another [target] player. @see [HintArrow.PlayerHintArrow] */
    public fun hintArrowPlayer(player: Player, target: Player) {
        player.client.write(HintArrow(HintArrow.PlayerHintArrow(target.slotId)))
    }

    /**
     * Draws the hint arrow over a tile. [height] is the vertical offset the arrow floats at, and
     * [position] which edge of the tile it points at ([HintArrowTilePosition.CENTER] by default).
     *
     * @see [HintArrow.TileHintArrow]
     */
    public fun hintArrowTile(
        player: Player,
        coords: CoordGrid,
        height: Int = 0,
        position: HintArrowTilePosition = HintArrowTilePosition.CENTER,
    ) {
        player.client.write(
            HintArrow(HintArrow.TileHintArrow(coords.x, coords.z, height, position))
        )
    }

    /** Clears any hint arrow currently shown. @see [HintArrow.ResetHintArrow] */
    public fun resetHintArrow(player: Player) {
        player.client.write(HintArrow(HintArrow.ResetHintArrow))
    }

    /** @see [ServerTickEnd] */
    public fun serverTickEnd(player: Player) {
        player.client.write(ServerTickEnd)
    }

    /** @see [Logout] */
    public fun logout(player: Player) {
        player.client.write(Logout)
    }

    /** Calls [LogoutWithReason] with an arg of `1` (reason = `Kicked`). */
    public fun logoutKicked(player: Player) {
        player.client.write(LogoutWithReason(reason = 1))
    }

    /** Calls [LogoutWithReason] with an arg of `2` (reason = `Updating`). */
    public fun logoutUpdating(player: Player) {
        player.client.write(LogoutWithReason(reason = 2))
    }

    /** @see [UpdateRebootTimer] */
    public fun updateRebootTimer(player: Player, cycles: Int) {
        require(cycles in 0..65535) { "`cycles` must be within range [0..65535]. (cycles=$cycles)" }
        player.client.write(UpdateRebootTimer(cycles))
    }

    /** Calls [UpdateRebootTimer] with an arg of `0`. */
    public fun clearUpdateRebootTimer(player: Player) {
        updateRebootTimer(player, cycles = 0)
    }
}
