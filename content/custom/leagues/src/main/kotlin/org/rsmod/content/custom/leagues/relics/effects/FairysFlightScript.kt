package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onOpHeld3
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Where the Fairy's Flight mushroom can take you: the farms. Each tile is a patch's own coordinate
 * from `FarmingPatches` - cache-derived - and the teleport lands on the nearest walkable tile
 * around it. `FarmTeleportTilesTest` checks that one exists for every farm.
 */
enum class FarmTeleport(val label: String, val patch: CoordGrid) {
    Falador("Falador farm", CoordGrid(3054, 3307, 0)),
    PortSarim("Port Sarim spirit tree", CoordGrid(3059, 3257, 0)),
    Draynor("Draynor Manor", CoordGrid(3086, 3354, 0)),
    FaladorPark("Falador Park tree", CoordGrid(3003, 3372, 0)),
    Rimmington("Rimmington bush", CoordGrid(2940, 3221, 0)),
    Lumbridge("Lumbridge tree", CoordGrid(3192, 3230, 0)),
    LumbridgeHops("Lumbridge hops", CoordGrid(3229, 3315, 0)),
    ChampionsGuild("Champions' Guild bush", CoordGrid(3181, 3357, 0)),
    AlKharid("Al Kharid cactus", CoordGrid(3315, 3202, 0)),
    Canifis("Canifis mushrooms", CoordGrid(3451, 3472, 0)),
    PortPhasmatys("Port Phasmatys farm", CoordGrid(3601, 3525, 0)),
    Taverley("Taverley tree", CoordGrid(2935, 3437, 0)),
    Catherby("Catherby farm", CoordGrid(2809, 3463, 0)),
    CatherbyOrchard("Catherby orchard", CoordGrid(2860, 3433, 0)),
    Seers("Seers' Village hops", CoordGrid(2666, 3525, 0)),
    Ardougne("Ardougne farm", CoordGrid(2666, 3374, 0)),
    ArdougneBush("Ardougne bush", CoordGrid(2617, 3225, 0)),
    Yanille("Yanille hops", CoordGrid(2575, 3104, 0)),
    Brimhaven("Brimhaven orchard", CoordGrid(2764, 3212, 0)),
    GnomeStronghold("Gnome Stronghold", CoordGrid(2435, 3414, 0)),
    GnomeVillage("Tree Gnome Village", CoordGrid(2489, 3179, 0)),
    Hosidius("Hosidius farm", CoordGrid(1734, 3554, 0)),
    FarmingGuild("Farming Guild", CoordGrid(1260, 3725, 0)),
}

/**
 * The Fairy's Flight relic's Fairy mushroom: op2 Teleport opens a paged menu of farms, op3
 * Last-destination goes back to the last one. Op1 is Wield. It ignores wilderness teleport
 * restrictions. The relic's other effect, `Perk.ClueChestAnyTier`, is checked by the Edgeville clue
 * chest.
 */
class FairysFlightScript : PluginScript() {
    private val lastDestination = HashMap<Player, FarmTeleport>()

    override fun ScriptContext.startup() {
        onOpHeld2(league_objs.fairy_mushroom) { teleportMenu() }
        onOpHeld3(league_objs.fairy_mushroom) { teleportLast() }
        onEvent<SessionStateEvent.Delete> { lastDestination.remove(player) }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        if (!player.hasRelic(Relic.FairysFlight)) {
            mes(NOT_YOURS)
            return
        }
        val pick = choosePaged(FarmTeleport.entries, "Which farm?") { it.label } ?: return
        teleport(pick)
    }

    private fun ProtectedAccess.teleportLast() {
        if (!player.hasRelic(Relic.FairysFlight)) {
            mes(NOT_YOURS)
            return
        }
        val last = lastDestination[player]
        if (last == null) {
            mes("You have not flown anywhere with the mushroom yet.")
            return
        }
        teleport(last)
    }

    private fun ProtectedAccess.teleport(farm: FarmTeleport) {
        lastDestination[player] = farm
        telejump(mapFindSquareLineOfWalk(farm.patch, 0, LANDING_RADIUS) ?: farm.patch)
        mes("The mushroom puffs, and you float down beside the ${farm.label}.")
    }

    private companion object {
        const val LANDING_RADIUS = 3
        const val NOT_YOURS = "The mushroom does nothing for you."
    }
}
