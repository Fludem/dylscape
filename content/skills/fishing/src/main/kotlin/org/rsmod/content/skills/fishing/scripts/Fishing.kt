package org.rsmod.content.skills.fishing.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.stat.fishingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.fishing.configs.FishingCatch
import org.rsmod.content.skills.fishing.configs.FishingContent
import org.rsmod.content.skills.fishing.configs.FishingMethod
import org.rsmod.content.skills.fishing.configs.FishingObjs
import org.rsmod.content.skills.fishing.configs.FishingSpots
import org.rsmod.events.UnboundEvent
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Fishing, built on the same skilling loop upstream uses for woodcutting and our mining module.
 *
 * Two things make fishing differ from mining in shape. Spots are **npcs**, not locs, so the loop
 * re-queues `opNpc1`/`opNpc3` rather than `opLoc1`; and a spot never depletes, so a successful
 * catch re-queues too and the player keeps fishing until the inventory fills, the bait runs out, or
 * they click away.
 *
 * Which method an op runs is decoded from the spot's own cache ops rather than configured per spot
 * — see [FishingSpots] for why the *pair* of op texts is what gets read.
 *
 * The Animal Wrangler league relic reaches in through four [Perk]s: the echo harpoon as a universal
 * tool, a second chance on a failed roll, one tick faster, and catches sent to the bank.
 *
 * TODO:
 * - Spots relocate on a timer in OSRS. Ours stand still, which makes fishing slightly better than
 *   live rather than worse, so it is a fair thing to leave until spot movement is worth building.
 * - Barbarian fishing, Tempoross, minnows and karambwan, all of which have mechanics of their own.
 */
class Fishing
@Inject
constructor(
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val perks: Perks,
    private val mapClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Spots carry their options on op1 and op3; op2 is always null across every spot in the
        // cache, so there is deliberately no op2 binding here.
        onOpNpc1(FishingContent.fishing_spot) { fish(it.npc, op = 1) }
        onOpNpc3(FishingContent.fishing_spot) { fish(it.npc, op = 3) }
    }

    private fun ProtectedAccess.fish(spot: Npc, op: Int) {
        val method = FishingSpots.methodFor(spot.visType, op) ?: return

        if (!carries(method.tool)) {
            mes(method.toolMessage)
            return
        }

        // Everything the player is high enough to land here, still ordered highest level first.
        val available = method.catches.filter { player.fishingLvl >= it.level }
        if (available.isEmpty()) {
            mes("You need a Fishing level of ${method.lowestLevel} to fish here.")
            return
        }

        if (method.bait != null && invTotal(inv, method.bait) <= 0) {
            mes(checkNotNull(method.baitMessage))
            return
        }

        val toBank = perks.has(player, Perk.FishingToBank) && !bank.isFull()
        if (inv.isFull() && !toBank) {
            mes("Your inventory is too full to hold any more fish.")
            soundSynth(synths.pillory_wrong)
            return
        }

        val delay = if (perks.has(player, Perk.FishingFaster)) FISH_DELAY - 1 else FISH_DELAY
        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + delay
            anim(method.anim)
        }

        var caught: FishingCatch? = null
        if (actionDelay < mapClock) {
            actionDelay = mapClock + delay
            spam(method.startMessage)
        } else if (actionDelay == mapClock) {
            // Each qualifying fish gets its own roll, best first, and the first success wins.
            caught = rollCatch(available)
            if (caught == null && perks.has(player, Perk.FishingSecondChance)) {
                caught =
                    if (random.randomBoolean()) rollCatch(available) ?: available.last() else null
            }
        }

        if (caught != null) {
            award(spot, method, caught, toBank)
        }

        // The spot is still there either way, so keep fishing.
        repeatOp(spot, op)
    }

    private fun ProtectedAccess.rollCatch(available: List<FishingCatch>): FishingCatch? =
        available.firstOrNull { statRandom(stats.fishing, it.rateLow, it.rateHigh, invisibleLvls) }

    private fun ProtectedAccess.award(
        spot: Npc,
        method: FishingMethod,
        caught: FishingCatch,
        toBank: Boolean,
    ) {
        if (method.bait != null) {
            invDel(inv, method.bait)
        }
        val banked = toBank && invAdd(bank, caught.fish).success
        if (!banked) {
            invAdd(inv, caught.fish)
        }
        statAdvance(stats.fishing, caught.xp * xpMods.get(player, stats.fishing))
        spam(caught.message)
        publish(CaughtFish(player, spot, caught.fish))
    }

    private fun ProtectedAccess.repeatOp(spot: Npc, op: Int) {
        when (op) {
            1 -> opNpc1(spot)
            3 -> opNpc3(spot)
        }
    }

    /**
     * The tool may be carried or wielded — a wielded dragon harpoon is the normal way to fish at a
     * harpoon spot, and the same allowance costs nothing for the rest. Under [Perk.EchoHarpoon] the
     * echo harpoon, carried or wielded, stands in for every tool.
     */
    private fun ProtectedAccess.carries(tool: ObjType): Boolean {
        if (holds(tool)) {
            return true
        }
        return perks.has(player, Perk.EchoHarpoon) && holds(FishingObjs.echo_harpoon)
    }

    private fun ProtectedAccess.holds(tool: ObjType): Boolean {
        if (player.righthand?.id == tool.id) {
            return true
        }
        return invTotal(inv, tool) > 0
    }

    data class CaughtFish(val player: Player, val spot: Npc, val fish: ObjType) : UnboundEvent

    private companion object {
        /**
         * Ticks between catch attempts, and the interval the animation is retriggered on. Five
         * ticks end to end, matching OSRS's fishing cadence: the delay is set one tick before the
         * roll lands, exactly as upstream's woodcutting loop does it.
         */
        const val FISH_DELAY = 4
    }
}
