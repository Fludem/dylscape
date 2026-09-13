package org.rsmod.content.custom.leagues.relics

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.content.custom.leagues.tasks.LeagueTaskProgress
import org.rsmod.game.entity.Player
import org.rsmod.game.type.varp.VarpType

/**
 * Keeps a player's League Points varps in step with the tasks they have completed.
 *
 * Points are never stored: they are the sum of every completed task's points, read off the
 * completion bits each time. `league_points_claimed` is what the client checks a tier's threshold
 * against, so it is the one that matters; the two progress-bar varps are kept equal to it so the
 * bar tells the same story, and writing `league_points_completed` is also what makes an open task
 * list redraw itself (the screen arms `league_tasks_transmit` on that varp).
 */
@Singleton
class LeaguePointsSync @Inject constructor(private val progress: LeagueTaskProgress) {
    /** `::leaguepoints` overrides, for testing tiers. Held until the player logs out. */
    private val overrides = HashMap<Player, Int>()

    fun points(player: Player): Int = overrides[player] ?: progress.points(player)

    fun sync(player: Player) {
        val points = points(player)
        player.write(league_varps.points_claimed, points)
        player.write(league_varps.points_currency, points)
        player.write(league_varps.points_completed, points)

        val completed = progress.completedCount(player)
        if (player.vars[league_varbits.total_tasks_completed] != completed) {
            VarPlayerIntMapSetter.set(player, league_varbits.total_tasks_completed, completed)
        }
    }

    fun override(player: Player, points: Int) {
        overrides[player] = points
        sync(player)
    }

    fun forget(player: Player) {
        overrides.remove(player)
    }

    private fun Player.write(varp: VarpType, value: Int) {
        if (vars[varp] != value) {
            VarPlayerIntMapSetter.set(this, varp, value)
        }
    }
}
