package org.rsmod.content.other.special.attacks.effects

import kotlin.math.min
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statSub
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatType

/**
 * Lowers a special attack target's stats.
 *
 * Players drain through `statSub`. Npcs have no drain api: their current levels are plain fields on
 * [Npc], which `NpcRegenProcessor` walks back towards base, so they are lowered directly. An npc
 * has no prayer, so a drain aimed at one is simply skipped.
 */
internal object TargetDrain {
    fun level(target: PathingEntity, stat: StatType): Int =
        when (target) {
            is Player -> target.stat(stat)
            is Npc -> target.level(stat) ?: 0
        }

    /** Lowers [stat] by up to [amount], stopping at 0, and returns how much was taken. */
    fun drain(target: PathingEntity, stat: StatType, amount: Int): Int {
        if (amount <= 0) {
            return 0
        }
        return when (target) {
            is Player -> {
                val before = target.stat(stat)
                target.statSub(stat, amount, 0)
                before - target.stat(stat)
            }
            is Npc -> target.drain(stat, amount)
        }
    }

    /** Spends [amount] down [order], moving to the next stat only once the current one is 0. */
    fun drainInOrder(target: PathingEntity, amount: Int, order: List<StatType>) {
        var remaining = amount
        for (stat in order) {
            if (remaining <= 0) {
                return
            }
            remaining -= drain(target, stat, remaining)
        }
    }

    private fun Npc.drain(stat: StatType, amount: Int): Int {
        val current = level(stat) ?: return 0
        val drained = min(current, amount)
        val lowered = current - drained
        when {
            stat.isType(stats.attack) -> attackLvl = lowered
            stat.isType(stats.strength) -> strengthLvl = lowered
            stat.isType(stats.defence) -> defenceLvl = lowered
            stat.isType(stats.ranged) -> rangedLvl = lowered
            stat.isType(stats.magic) -> magicLvl = lowered
        }
        return drained
    }

    private fun Npc.level(stat: StatType): Int? =
        when {
            stat.isType(stats.attack) -> attackLvl
            stat.isType(stats.strength) -> strengthLvl
            stat.isType(stats.defence) -> defenceLvl
            stat.isType(stats.ranged) -> rangedLvl
            stat.isType(stats.magic) -> magicLvl
            else -> null
        }
}
