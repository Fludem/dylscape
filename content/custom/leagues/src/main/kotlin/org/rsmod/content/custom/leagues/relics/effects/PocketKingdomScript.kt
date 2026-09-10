package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.config.refs.invs
import org.rsmod.api.config.refs.objs
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.player.stat.miningLvl
import org.rsmod.api.player.stat.woodcuttingLvl
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_timers
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.RelicUnlocked
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.content.skills.herblore.configs.HerbloreRecipes
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.inv.InvTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Pocket Kingdom relic, reworked: this server has no Miscellania, so the kingdom simply pays
 * tribute. Every [DELIVERY_TICKS] (10 minutes) logged in, a delivery lands in the bank, sized to
 * the player's levels: [RESOURCE_COUNT] of the best logs, ore and raw fish they could gather, the
 * best grimy herb they could clean, and [COINS] coins.
 *
 * A delivery goes into the bank whole or not at all. If the bank cannot take it, it waits - one
 * delivery at most - for the next cycle, or for op2 Collect, which hands it over noted into the
 * inventory instead. op1 Check says when the next one is due.
 */
class PocketKingdomScript
@Inject
constructor(
    private val clock: MapClock,
    private val invTypes: InvTypeList,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    private val nextDelivery = HashMap<Player, Int>()
    private val waiting = HashMap<Player, List<Tribute>>()

    override fun ScriptContext.startup() {
        onPlayerLogin { start(player) }
        onEvent<RelicUnlocked> { if (relic == Relic.PocketKingdom) start(player) }
        onPlayerSoftTimer(league_timers.pocket_kingdom) { deliver(player) }
        onOpHeld1(league_objs.pocket_kingdom) { check() }
        onOpHeld2(league_objs.pocket_kingdom) { collect() }
        onEvent<SessionStateEvent.Delete> {
            nextDelivery.remove(player)
            waiting.remove(player)
        }
    }

    private fun start(player: Player) {
        if (!player.hasRelic(Relic.PocketKingdom)) {
            return
        }
        player.softTimer(league_timers.pocket_kingdom, DELIVERY_TICKS)
        nextDelivery[player] = clock.cycle + DELIVERY_TICKS
    }

    private fun deliver(player: Player) {
        if (!player.hasRelic(Relic.PocketKingdom)) {
            player.clearSoftTimer(league_timers.pocket_kingdom)
            nextDelivery.remove(player)
            return
        }
        nextDelivery[player] = clock.cycle + DELIVERY_TICKS
        val delivery = waiting.remove(player) ?: tribute(player)
        val bank = player.invMap.getOrPut(invTypes[invs.bank])
        if (!fits(bank, delivery)) {
            waiting[player] = delivery
            player.mes(
                "Your kingdom's delivery could not fit in your bank. Collect it from your " +
                    "Pocket kingdom, or make room before the next one."
            )
            return
        }
        for (item in delivery) {
            player.invAdd(bank, item.obj, item.count)
        }
        player.mes("Your kingdom sends tribute to your bank: ${describe(delivery)}.")
    }

    private fun ProtectedAccess.check() {
        if (!player.hasRelic(Relic.PocketKingdom)) {
            mes(NOT_YOURS)
            return
        }
        val due = nextDelivery[player]
        if (due == null) {
            mes("Your kingdom has not started sending tribute yet. Try again after relogging.")
            return
        }
        val minutes = ((due - mapClock).coerceAtLeast(0) * TICK_MILLIS / 60_000L) + 1
        mes("Your next delivery arrives within $minutes minute${if (minutes == 1L) "" else "s"}.")
        mes("It will hold ${describe(tribute(player))}.")
        if (player in waiting) {
            mes("A delivery is waiting for you: use Collect to take it.")
        }
    }

    private fun ProtectedAccess.collect() {
        if (!player.hasRelic(Relic.PocketKingdom)) {
            mes(NOT_YOURS)
            return
        }
        val delivery = waiting[player]
        if (delivery == null) {
            mes("There is no delivery waiting. Deliveries go straight to your bank.")
            return
        }
        val slotsNeeded =
            delivery.count { invTotal(inv, it.obj) == 0 || !objTypes[it.obj].stackable }
        if (inv.freeSpace() < slotsNeeded) {
            mes("You need $slotsNeeded free inventory slots to collect the delivery.")
            return
        }
        waiting.remove(player)
        for (item in delivery) {
            invAdd(inv, item.obj, item.count, cert = true)
        }
        mes("You collect your kingdom's delivery: ${describe(delivery)}.")
    }

    private fun tribute(player: Player): List<Tribute> {
        val herb =
            HerbloreRecipes.cleaning.lastOrNull { it.levelReq <= player.herbloreLvl }?.grimy
                ?: HerbloreRecipes.cleaning.first().grimy
        return listOf(
            Tribute(best(KingdomYield.logs, player.woodcuttingLvl), RESOURCE_COUNT),
            Tribute(best(KingdomYield.ores, player.miningLvl), RESOURCE_COUNT),
            Tribute(best(KingdomYield.fish, player.fishingLvl), RESOURCE_COUNT),
            Tribute(herb, HERB_COUNT),
            Tribute(objs.coins, COINS),
        )
    }

    private fun best(ladder: List<Pair<Int, ObjType>>, level: Int): ObjType =
        ladder.last { it.first <= level }.second

    /**
     * Whether every item of [delivery] finds room: a bank slot it already stacks in, or a new one.
     */
    private fun fits(bank: Inventory, delivery: List<Tribute>): Boolean {
        val newSlots = delivery.count { item -> bank.none { it?.id == item.obj.id } }
        return bank.freeSpace() >= newSlots
    }

    private fun describe(delivery: List<Tribute>): String =
        delivery.joinToString(", ") { "%,d x %s".format(it.count, objTypes[it.obj].name) }

    private data class Tribute(val obj: ObjType, val count: Int)

    private companion object {
        /** 10 minutes of 600ms ticks. */
        const val DELIVERY_TICKS = 1000
        const val TICK_MILLIS = 600L
        const val RESOURCE_COUNT = 50
        const val HERB_COUNT = 10
        const val COINS = 25_000
        const val NOT_YOURS = "The tiny kingdom ignores you."
    }
}

/** What the kingdom's workers can bring back, by the level the player needs to gather it. */
internal object KingdomYield : ObjReferences() {
    val logs =
        listOf(
            1 to find("logs"),
            15 to find("oak_logs"),
            30 to find("willow_logs"),
            45 to find("maple_logs"),
            60 to find("yew_logs"),
            75 to find("magic_logs"),
        )

    val ores =
        listOf(
            1 to find("copper_ore"),
            15 to find("iron_ore"),
            30 to find("coal"),
            55 to find("mithril_ore"),
            70 to find("adamantite_ore"),
            85 to find("runite_ore"),
        )

    val fish =
        listOf(
            1 to find("raw_trout"),
            30 to find("raw_salmon"),
            40 to find("raw_lobster"),
            50 to find("raw_swordfish"),
            76 to find("raw_shark"),
            82 to find("raw_anglerfish"),
        )
}
