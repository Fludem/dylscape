package org.rsmod.content.skills.slayer.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal typealias slayer_varps = SlayerVarps

internal typealias slayer_varbits = SlayerVarBits

/**
 * Slayer progress lives in the vanilla variables rather than a database table of our own.
 *
 * That is deliberate: these are the variables the stock client's own task helper and reward
 * interface already read, so putting the task where vanilla puts it means the client half works
 * without reimplementing it. It also saves with the character for free - the same reasoning
 * `KnightWavesProgress` documents for its varbit.
 */
object SlayerVarps : VarpReferences() {
    /** The current task, as a `slayer_task:id` - the same id space as npc param `slayer_task`. */
    val task = find("slayer_target")

    /** Kills left on the current task. Zero means no task. */
    val remaining = find("slayer_count")

    /** What the task was assigned at, kept so "how many did I start with" can be answered. */
    val assigned = find("slayer_count_original")

    /**
     * Bitset of purchased unlocks, indexed by `slayer_unlock:bit`.
     *
     * There are 58 unlock rows and a varp holds 32 bits, so vanilla splits them across two: bits
     * 0..31 here and 32.. in [unlocksHigh]. `SlayerProgress.hasUnlock` is what knows that.
     */
    val unlocks = find("slayer_rewards_unlocks")

    /** Unlock bits 32 and up. See [unlocks]. */
    val unlocksHigh = find("slayer_rewards_unlocks1")

    /** Bitset of blocked tasks. */
    val blocked = find("slayer_rewards_blocked")
}

object SlayerVarBits : VarBitReferences() {
    /** Which master assigned the current task, as a `slayer_master_task:master_id`. 4 bits. */
    val master = find("slayer_master")

    /** Reward points. 17 bits, so it saturates at 131071 rather than anywhere reachable. */
    val points = find("slayer_points")

    /** The completion streak, which is what the point payout tiers off. 14 bits. */
    val tasksCompleted = find("slayer_tasks_completed")
}
