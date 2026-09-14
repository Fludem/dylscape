package org.rsmod.content.custom.skillingtasks

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** Published on the tick a task reaches its target, after the payout has landed. */
data class SkillingTaskCompleted(val player: Player, val task: SkillingTask, val target: Int) :
    UnboundEvent

/**
 * One player's standing with the Taskmaster.
 *
 * Fields are volatile because the account saver snapshots them from its own thread while the game
 * thread advances them: a save may catch a count one behind, never a torn one.
 */
class PlayerTasks {
    @Volatile var taskKey: String? = null
    @Volatile var target: Int = 0
    @Volatile var progress: Int = 0
    @Volatile var streak: Int = 0
    @Volatile var points: Int = 0
    @Volatile var completed: Int = 0
    @Volatile var lastTaskKey: String? = null

    val task: SkillingTask?
        get() = taskKey?.let(SkillingTasks::find)

    val hasTask: Boolean
        get() = task != null && target > 0
}

/**
 * Every player's task, progress, streak and points, and the rules for paying a finished task.
 *
 * A finished task pays at once, where the player stands, like slayer: bonus experience in the
 * task's skill sized off what the task itself was worth, and points sized off its level. Nothing is
 * owed until the player walks back, so the Taskmaster only ever has to hand out the next one.
 */
@Singleton
class SkillingTaskState
@Inject
constructor(private val eventBus: EventBus, private val xpMods: XpModifiers) {
    private val players = ConcurrentHashMap<Player, PlayerTasks>()

    operator fun get(player: Player): PlayerTasks = players.getOrPut(player) { PlayerTasks() }

    fun peek(player: Player): PlayerTasks? = players[player]

    fun assign(player: Player, task: SkillingTask, amount: Int) {
        val state = this[player]
        state.taskKey = task.key
        state.target = amount
        state.progress = 0
    }

    /** Clears the task and the streak. */
    fun cancel(player: Player) {
        val state = this[player]
        state.lastTaskKey = state.taskKey
        state.taskKey = null
        state.target = 0
        state.progress = 0
        state.streak = 0
    }

    /**
     * Adds [by] to the current task if [kind] and [product] are what it asked for. Returns `true`
     * when that finished the task.
     */
    fun advance(player: Player, kind: TaskKind, product: ObjType, by: Int = 1): Boolean {
        if (by <= 0) {
            return false
        }
        val state = players[player] ?: return false
        val task = state.task ?: return false
        if (state.target <= 0 || !task.counts(kind, product)) {
            return false
        }
        val now = min(state.progress.toLong() + by, state.target.toLong()).toInt()
        state.progress = now
        if (now < state.target) {
            return false
        }
        complete(player, state, task)
        return true
    }

    /** Pays out the current task as if it were finished. Returns `false` when there is none. */
    fun forceComplete(player: Player): Boolean {
        val state = players[player] ?: return false
        val task = state.task ?: return false
        if (state.target <= 0) {
            return false
        }
        state.progress = state.target
        complete(player, state, task)
        return true
    }

    fun addPoints(player: Player, amount: Int) {
        val state = this[player]
        state.points = (state.points + amount).coerceAtLeast(0)
    }

    /** Takes [amount] points if the player has them. */
    fun spendPoints(player: Player, amount: Int): Boolean {
        val state = players[player] ?: return false
        if (amount < 0 || state.points < amount) {
            return false
        }
        state.points -= amount
        return true
    }

    fun points(player: Player): Int = players[player]?.points ?: 0

    fun forget(player: Player) {
        players.remove(player)
    }

    fun tracked(player: Player): Boolean = players.containsKey(player)

    fun restore(player: Player, saved: PlayerTasks) {
        players[player] = saved
    }

    private fun complete(player: Player, state: PlayerTasks, task: SkillingTask) {
        val target = state.target
        val xp = bonusXp(task, target, state.streak)
        val points = pointsFor(task, state.streak + 1)

        state.streak += 1
        state.completed += 1
        state.points += points
        state.lastTaskKey = task.key
        state.taskKey = null
        state.target = 0
        state.progress = 0

        val paid = player.statAdvance(task.kind.stat, xp * xpMods.get(player, task.kind.stat))
        player.mes(
            "<col=00c8ff>Skilling task complete:</col> ${task.describe(target)}. " +
                "You earn ${paid.format()} ${task.kind.skillName} XP and $points skilling points."
        )
        player.mes(
            "Streak: ${state.streak}. You have ${state.points} skilling points. " +
                "See the Taskmaster in Edgeville for another task."
        )
        eventBus.publish(SkillingTaskCompleted(player, task, target))
    }

    private fun Int.format(): String = "%,d".format(this)

    companion object {
        /** The bonus is a fifth of what the task itself paid, growing five percent a streak. */
        fun bonusXp(task: SkillingTask, target: Int, streak: Int): Double =
            target * task.xpEach * BONUS_SHARE * (1.0 + STREAK_BONUS * min(streak, STREAK_CAP))

        /** One point plus one per ten levels, with slayer-style milestones on the streak. */
        fun pointsFor(task: SkillingTask, streak: Int): Int {
            val base = 1 + task.level / 10
            val milestone =
                when {
                    streak % 10 == 0 -> 10
                    streak % 5 == 0 -> 5
                    else -> 0
                }
            return base + milestone
        }

        private const val BONUS_SHARE = 0.2
        private const val STREAK_BONUS = 0.05
        private const val STREAK_CAP = 10
    }
}
