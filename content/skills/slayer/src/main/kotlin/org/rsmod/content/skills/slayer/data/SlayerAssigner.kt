package org.rsmod.content.skills.slayer.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.slayerLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.content.skills.slayer.SlayerAssignment
import org.rsmod.content.skills.slayer.SlayerProgress.hasSlayerUnlock
import org.rsmod.game.entity.Player

/**
 * Picks a task off a master's list, using the weights the cache itself carries.
 *
 * Every filter here answers "could this player be handed this task at all", and each has a reason
 * to exist: the combat and slayer requirements are the task's own, the unlock filter stops rows
 * like Duradel's "Boss" being rolled before it is bought, and [SlayerTaskRepository.hasNpcs]
 * excludes the handful of tasks in the table with nothing in this rev that counts for them - a
 * player handed one of those could never finish it.
 */
@Singleton
class SlayerAssigner
@Inject
constructor(private val repo: SlayerTaskRepository, private val random: GameRandom) {
    /** Everything [masterId] could hand [player] right now. */
    fun candidates(player: Player, masterId: Int): List<SlayerAssignment> =
        repo.assignments(masterId).filter { it.isAvailableTo(player) }

    /**
     * Rolls one task, or null when the master has nothing to give - which a low-level player at a
     * high-level master really can hit, and is why the caller has to handle it.
     */
    fun roll(player: Player, masterId: Int): SlayerAssignment? {
        val candidates = candidates(player, masterId)
        if (candidates.isEmpty()) {
            return null
        }
        val total = candidates.sumOf(SlayerAssignment::weight)
        var roll = random.of(maxExclusive = total)
        for (candidate in candidates) {
            roll -= candidate.weight
            if (roll < 0) {
                return candidate
            }
        }
        // Unreachable while the weights are positive, which the repository guarantees by dropping
        // any row weighted zero or less.
        return candidates.last()
    }

    /** How many kills [assignment] is handed out as. */
    fun amount(assignment: SlayerAssignment): Int {
        val min = assignment.minAmount
        val max = assignment.maxAmount
        return if (max <= min) min.coerceAtLeast(1) else random.of(min, max)
    }

    private fun SlayerAssignment.isAvailableTo(player: Player): Boolean {
        if (player.slayerLvl < task.slayerLevel) {
            return false
        }
        if (player.combatLevel < task.minCombat) {
            return false
        }
        if (unlockBit != null && !player.hasSlayerUnlock(unlockBit)) {
            return false
        }
        return repo.hasNpcs(task.id)
    }
}
