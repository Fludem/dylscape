package org.rsmod.content.custom.leagues.relics.effects

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
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
 * Where the Bank Heist relic's briefcase can take you. Each tile stands inside or at the door of
 * the bank; `BankTeleportTilesTest` checks every one against the real collision map.
 */
enum class BankTeleport(val label: String, val dest: CoordGrid) {
    Lumbridge("Lumbridge Castle", CoordGrid(3208, 3220, 2)),
    Draynor("Draynor Village", CoordGrid(3092, 3245, 0)),
    AlKharid("Al Kharid", CoordGrid(3269, 3167, 0)),
    VarrockWest("Varrock West", CoordGrid(3185, 3436, 0)),
    VarrockEast("Varrock East", CoordGrid(3253, 3420, 0)),
    GrandExchange("Grand Exchange", CoordGrid(3164, 3486, 0)),
    Edgeville("Edgeville", CoordGrid(3094, 3491, 0)),
    FaladorEast("Falador East", CoordGrid(3013, 3355, 0)),
    FaladorWest("Falador West", CoordGrid(2946, 3368, 0)),
    Catherby("Catherby", CoordGrid(2809, 3440, 0)),
    Seers("Seers' Village", CoordGrid(2725, 3491, 0)),
    ArdougneNorth("East Ardougne North", CoordGrid(2615, 3332, 0)),
    ArdougneSouth("East Ardougne South", CoordGrid(2655, 3283, 0)),
    Yanille("Yanille", CoordGrid(2611, 3092, 0)),
    CastleWars("Castle Wars", CoordGrid(2443, 3083, 0)),
    FishingGuild("Fishing Guild", CoordGrid(2586, 3420, 0)),
    Shilo("Shilo Village", CoordGrid(2852, 2954, 0)),
    Canifis("Canifis", CoordGrid(3512, 3480, 0)),
    PortPhasmatys("Port Phasmatys", CoordGrid(3688, 3467, 0)),
}

/**
 * The Bank Heist relic's Banker's briefcase: op1 Teleport opens a paged menu of banks, op3
 * Last-destination goes back to the last one. Op2 is Wear and keeps its default handling. It
 * ignores wilderness teleport restrictions, as the vanilla relic does.
 */
class BankHeistScript : PluginScript() {
    private val lastDestination = HashMap<Player, BankTeleport>()

    override fun ScriptContext.startup() {
        onOpHeld1(league_objs.bankers_briefcase) { teleportMenu() }
        onOpHeld3(league_objs.bankers_briefcase) { teleportLast() }
        onEvent<SessionStateEvent.Delete> { lastDestination.remove(player) }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        if (!player.hasRelic(Relic.BankHeist)) {
            mes(NOT_YOURS)
            return
        }
        val pages = BankTeleport.entries.chunked(PER_PAGE)
        var page = 0
        while (true) {
            val pick = choose(pages[page])
            if (pick != null) {
                teleport(pick)
                return
            }
            page = (page + 1) % pages.size
        }
    }

    private fun ProtectedAccess.teleportLast() {
        if (!player.hasRelic(Relic.BankHeist)) {
            mes(NOT_YOURS)
            return
        }
        val last = lastDestination[player]
        if (last == null) {
            mes("You have not teleported with the briefcase yet.")
            return
        }
        teleport(last)
    }

    private fun ProtectedAccess.teleport(bank: BankTeleport) {
        lastDestination[player] = bank
        telejump(mapFindSquareLineOfWalk(bank.dest, 0, 1) ?: bank.dest)
        mes("The briefcase snaps shut and you arrive at ${bank.label}.")
    }

    /** One page of up to four banks plus "More...", which returns `null`. */
    private suspend fun ProtectedAccess.choose(page: List<BankTeleport>): BankTeleport? {
        val more: BankTeleport? = null
        val title = "Where would you like to go?"
        return when (page.size) {
            4 ->
                choice5(
                    page[0].label,
                    page[0],
                    page[1].label,
                    page[1],
                    page[2].label,
                    page[2],
                    page[3].label,
                    page[3],
                    MORE,
                    more,
                    title,
                )
            3 ->
                choice4(
                    page[0].label,
                    page[0],
                    page[1].label,
                    page[1],
                    page[2].label,
                    page[2],
                    MORE,
                    more,
                    title,
                )
            2 -> choice3(page[0].label, page[0], page[1].label, page[1], MORE, more, title)
            1 -> choice2(page[0].label, page[0], MORE, more, title)
            else -> null
        }
    }

    private companion object {
        const val PER_PAGE = 4
        const val MORE = "More..."
        const val NOT_YOURS = "The briefcase will not open for you."
    }
}
