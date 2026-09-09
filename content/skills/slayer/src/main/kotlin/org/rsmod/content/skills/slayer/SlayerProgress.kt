package org.rsmod.content.skills.slayer

import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.content.skills.slayer.configs.slayer_varbits
import org.rsmod.content.skills.slayer.configs.slayer_varps
import org.rsmod.game.entity.Player

/**
 * A player's slayer state, as properties over the vanilla variables.
 *
 * Nothing here is stored anywhere else. That is what lets the stock client's own task helper and
 * reward interface read the same task the server thinks the player is on, and it means the state
 * saves with the character without a migration or a registry.
 */
object SlayerProgress {
    /** The `slayer_task:id` currently assigned, or [NO_TASK] when the player has none. */
    var Player.slayerTask: Int by intVarp(slayer_varps.task)

    /** Kills left. Reaching zero is what completes the task, so it is never left negative. */
    var Player.slayerRemaining: Int by intVarp(slayer_varps.remaining)

    /** The amount the current task was handed out at. */
    var Player.slayerAssigned: Int by intVarp(slayer_varps.assigned)

    /** The `master_id` that assigned the current task. */
    var Player.slayerMaster: Int by intVarBit(slayer_varbits.master)

    /** Unspent reward points. */
    var Player.slayerPoints: Int by intVarBit(slayer_varbits.points)

    /** Tasks completed in a row, which is what the point payout tiers off. */
    var Player.slayerStreak: Int by intVarBit(slayer_varbits.tasksCompleted)

    /**
     * Whether the player has bought the unlock occupying [bit] of `slayer_unlock`.
     *
     * The bitset spans two varps because there are more unlocks than a varp has bits; which half a
     * bit lives in is the only thing callers are spared knowing.
     */
    fun Player.hasSlayerUnlock(bit: Int): Boolean {
        val varp = if (bit < VARP_BITS) slayer_varps.unlocks else slayer_varps.unlocksHigh
        return vars[varp] and (1 shl (bit % VARP_BITS)) != 0
    }

    /** True while the player owes kills on a task. */
    val Player.hasSlayerTask: Boolean
        get() = slayerTask != NO_TASK && slayerRemaining > 0

    /** The `slayer_task` param's own cache default, so "no task" needs no sentinel of our own. */
    const val NO_TASK: Int = 0

    private const val VARP_BITS = 32
}
