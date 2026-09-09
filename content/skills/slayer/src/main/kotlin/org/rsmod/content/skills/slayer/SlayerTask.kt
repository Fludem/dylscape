package org.rsmod.content.skills.slayer

/**
 * One row of `slayer_task`, reduced to the fields this module uses.
 *
 * [id] is the important one: it is both the row's own `slayer_task:id` and the value npcs carry in
 * their `slayer_task` param, so it is what links a monster to the task it counts for.
 */
data class SlayerTask(
    val id: Int,
    val name: String,
    val displayName: String,
    val minCombat: Int,
    val slayerLevel: Int,
)

/**
 * One row of `slayer_master_task`: a task on a particular master's list, with the weight it is
 * rolled at and the amount range it is assigned in.
 *
 * [unlockBit] is the `slayer_unlock:bit` the row is gated behind, or null when it is always
 * available.
 */
data class SlayerAssignment(
    val task: SlayerTask,
    val weight: Int,
    val minAmount: Int,
    val maxAmount: Int,
    val unlockBit: Int?,
)
