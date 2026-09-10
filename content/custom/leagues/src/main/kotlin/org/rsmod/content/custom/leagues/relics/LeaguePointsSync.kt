package org.rsmod.content.custom.leagues.relics

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.game.entity.Player
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.game.type.varp.VarpType

/**
 * Keeps a player's League Points varps in step with their total level (see [LeaguePoints]).
 *
 * `league_points_claimed` is what the client checks a tier's threshold against, so it is the one
 * that matters; the two progress-bar varps are kept equal to it so the bar tells the same story.
 */
@Singleton
class LeaguePointsSync @Inject constructor(private val statTypes: StatTypeList) {
    /** `::leaguepoints` overrides, for testing tiers. Held until the player logs out. */
    private val overrides = HashMap<Player, Int>()

    fun totalLevel(player: Player): Int =
        statTypes.values.filterNot { it.unreleased }.sumOf { player.statBase(it) }

    fun points(player: Player): Int =
        overrides[player] ?: LeaguePoints.forTotalLevel(totalLevel(player))

    fun sync(player: Player) {
        val points = points(player)
        player.write(league_varps.points_claimed, points)
        player.write(league_varps.points_currency, points)
        player.write(league_varps.points_completed, points)
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
