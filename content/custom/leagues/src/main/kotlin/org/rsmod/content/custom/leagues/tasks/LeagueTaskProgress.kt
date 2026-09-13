package org.rsmod.content.custom.leagues.tasks

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.leagues.configs.league_task_varps
import org.rsmod.events.EventBus
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType

/** Published once per task, on the tick it completes. */
data class LeagueTaskCompleted(val player: Player, val task: LeagueTask) : UnboundEvent

/**
 * What each player has done towards each task.
 *
 * Completion is one bit in the vanilla `league_task_completed_<n>` varps - the same bit the
 * client's `[proc,league_task_is_completed]` reads - which are `Perm`, so a finished task needs
 * nothing saved beyond the varp. Counters for the unfinished counted tasks (kill 25, mine 100) live
 * here and go through `CharacterLeagueTaskPipeline`.
 *
 * The counter maps are read from the account saver's thread while the game thread updates them, so
 * they are concurrent maps: a save may catch a count one behind, never a torn one.
 */
@Singleton
class LeagueTaskProgress @Inject constructor(private val eventBus: EventBus) {
    private val counters = HashMap<Player, ConcurrentHashMap<Int, Int>>()

    fun isDone(player: Player, task: LeagueTask): Boolean =
        player.vars[varpFor(task)] and (1 shl task.completionBit) != 0

    fun count(player: Player, task: LeagueTask): Int =
        if (isDone(player, task)) task.target else counters[player]?.get(task.id) ?: 0

    /** Adds [by] to [task]'s counter, completing it at its target. Returns `true` on completion. */
    fun advance(player: Player, task: LeagueTask, by: Int = 1): Boolean {
        if (by <= 0 || isDone(player, task)) {
            return false
        }
        val map = counters.getOrPut(player) { ConcurrentHashMap() }
        val now = (map[task.id] ?: 0).toLong() + by
        if (now >= task.target) {
            return complete(player, task)
        }
        map[task.id] = now.toInt()
        return false
    }

    /** Marks [task] done, tells the player, and publishes [LeagueTaskCompleted]. */
    fun complete(player: Player, task: LeagueTask): Boolean {
        if (isDone(player, task)) {
            return false
        }
        val varp = varpFor(task)
        VarPlayerIntMapSetter.set(player, varp, player.vars[varp] or (1 shl task.completionBit))
        counters.getOrPut(player) { ConcurrentHashMap() }.remove(task.id)
        player.mes(
            "<col=00c8ff>League task completed:</col> ${task.name} " +
                "<col=00c8ff>(+${task.points} points)</col>"
        )
        eventBus.publish(LeagueTaskCompleted(player, task))
        return true
    }

    fun points(player: Player): Int =
        LeagueTasks.all.sumOf { if (isDone(player, it)) it.points else 0 }

    fun completedCount(player: Player): Int = LeagueTasks.all.count { isDone(player, it) }

    /** Clears every completion bit and counter. */
    fun reset(player: Player) {
        for (varp in league_task_varps.completed) {
            if (player.vars[varp] != 0) {
                VarPlayerIntMapSetter.set(player, varp, 0)
            }
        }
        counters[player]?.clear()
    }

    /** Whether this player has counter state worth writing. */
    fun tracked(player: Player): Boolean = player in counters

    fun snapshot(player: Player): Map<Int, Int> = counters[player]?.toMap() ?: emptyMap()

    /** Installs saved counters; unknown task ids (retired tasks) are dropped. */
    fun restore(player: Player, saved: Map<Int, Int>) {
        val map = ConcurrentHashMap<Int, Int>()
        for ((id, count) in saved) {
            if (id in LeagueTasks.byId && count > 0) {
                map[id] = count
            }
        }
        counters[player] = map
    }

    fun forget(player: Player) {
        counters.remove(player)
    }

    private fun varpFor(task: LeagueTask): VarpType =
        league_task_varps.completed[task.completionVarp]
}
