package org.rsmod.content.custom.skillingtasks

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Player

/**
 * Picks a task the player can do, weighted to the top of what they can do.
 *
 * For each kind, the rows the player has the level for are cut down to the three highest levels
 * among them, so a 99 woodcutter is offered maples, yews and magics and never regular logs. The
 * kind is then picked uniformly among those with anything left, and a row uniformly within it. The
 * previous task is left out so nobody is handed the same job twice running.
 */
@Singleton
class SkillingTaskAssigner @Inject constructor(private val random: GameRandom) {
    /** Everything [player] could be handed right now, per kind, before the random pick. */
    fun candidates(
        player: Player,
        kind: TaskKind? = null,
        exclude: String? = null,
    ): Map<TaskKind, List<SkillingTask>> {
        val result = LinkedHashMap<TaskKind, List<SkillingTask>>()
        for (k in TaskKind.entries) {
            if (kind != null && k != kind) {
                continue
            }
            val level = player.statBase(k.stat)
            val reachable = SkillingTasks.all.filter { it.kind == k && it.level <= level }
            val topLevels = reachable.map { it.level }.distinct().sortedDescending().take(TOP_TIERS)
            val rows = reachable.filter { it.level in topLevels && it.key != exclude }
            if (rows.isNotEmpty()) {
                result[k] = rows
            }
        }
        return result
    }

    /** One task, or null when nothing in [kind] (or anything at all) is available to the player. */
    fun roll(player: Player, kind: TaskKind? = null, exclude: String? = null): SkillingTask? {
        val candidates = candidates(player, kind, exclude)
        if (candidates.isEmpty()) {
            return null
        }
        val picked = random.pick(candidates.keys.toList())
        return random.pick(candidates.getValue(picked))
    }

    /** 100 to 200, in steps of five. */
    fun amount(): Int = random.of(MIN_STEPS, MAX_STEPS) * STEP

    companion object {
        const val TOP_TIERS = 3
        const val STEP = 5
        const val MIN_STEPS = 20
        const val MAX_STEPS = 40
        const val MIN_AMOUNT = MIN_STEPS * STEP
        const val MAX_AMOUNT = MAX_STEPS * STEP
    }
}
