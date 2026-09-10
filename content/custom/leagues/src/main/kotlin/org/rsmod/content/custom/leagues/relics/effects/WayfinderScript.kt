package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.registry.region.RegionRegistry
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Clue Compass relic, reworked: this server has no clue scrolls, so the compass points at
 * people instead.
 * - op2 Teleport: a paged menu of every other online player; you land beside the one you pick.
 * - op3 Last-destination: straight back to the last player you picked, wherever they are now.
 * - op1 Current-step: back to the tile you left on your last compass teleport.
 *
 * It works from any Wilderness level. A target inside an instanced region is refused: those
 * coordinates belong to someone else's copy of the map.
 */
class WayfinderScript @Inject constructor(private val players: PlayerList) : PluginScript() {
    private val lastTarget = HashMap<Player, String>()
    private val departure = HashMap<Player, CoordGrid>()

    override fun ScriptContext.startup() {
        onOpHeld2(league_objs.clue_compass) { teleportMenu() }
        onOpHeld3(league_objs.clue_compass) { teleportLast() }
        onOpHeld1(league_objs.clue_compass) { returnToDeparture() }
        onEvent<SessionStateEvent.Delete> {
            lastTarget.remove(player)
            departure.remove(player)
        }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        if (!player.hasRelic(Relic.ClueCompass)) {
            mes(NOT_YOURS)
            return
        }
        val others = players.filter { it !== player }.sortedBy { it.displayName.lowercase() }
        if (others.isEmpty()) {
            mes("The needle spins aimlessly: nobody else is online.")
            return
        }
        val pick = choosePaged(others, "Who would you like to find?") { it.displayName } ?: return
        teleportTo(pick)
    }

    private fun ProtectedAccess.teleportLast() {
        if (!player.hasRelic(Relic.ClueCompass)) {
            mes(NOT_YOURS)
            return
        }
        val name = lastTarget[player]
        if (name == null) {
            mes("You have not followed the compass to anyone yet.")
            return
        }
        val target = players.firstOrNull { it.displayName == name }
        if (target == null) {
            mes("$name is not online.")
            return
        }
        teleportTo(target)
    }

    private fun ProtectedAccess.returnToDeparture() {
        if (!player.hasRelic(Relic.ClueCompass)) {
            mes(NOT_YOURS)
            return
        }
        val back = departure.remove(player)
        if (back == null) {
            mes("The needle has nowhere to take you back to.")
            return
        }
        telejump(back)
        mes("The compass pulls you back to where you set off from.")
    }

    private fun ProtectedAccess.teleportTo(target: Player) {
        if (!target.isOnline()) {
            mes("${target.displayName} is no longer online.")
            return
        }
        if (RegionRegistry.inWorkingArea(target.coords)) {
            mes("The needle cannot find ${target.displayName}: they are somewhere out of reach.")
            return
        }
        departure[player] = player.coords
        lastTarget[player] = target.displayName
        telejump(mapFindSquareLineOfWalk(target.coords, 0, LANDING_RADIUS) ?: target.coords)
        mes("The compass needle snaps round, and you arrive beside ${target.displayName}.")
    }

    private fun Player.isOnline(): Boolean = players.any { it === this }

    private companion object {
        const val LANDING_RADIUS = 1
        const val NOT_YOURS = "The needle will not move for you."
    }
}
