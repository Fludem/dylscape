package org.rsmod.content.skills.farming.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.farmingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.farming.configs.FarmingObjs
import org.rsmod.content.skills.farming.configs.FarmingSeqs
import org.rsmod.content.skills.farming.configs.FarmingTimers
import org.rsmod.content.skills.farming.data.FarmingRates
import org.rsmod.content.skills.farming.data.FarmingSaplings
import org.rsmod.content.skills.farming.data.SaplingChain
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Growing a tree seed into the sapling a patch will actually accept.
 *
 * Eight of the twenty patch kinds -- every tree, fruit tree, hardwood, calquat, celastrus, redwood,
 * crystal and spirit tree -- refuse a seed and want a sapling, so without this half the farming
 * skill has nothing plantable. The chain is the cache's own (`plantpot_compost` ->
 * `plantpot_oak_seed` -> `plantpot_oak_seed_watered` -> `plantpot_oak_sapling`); [FarmingSaplings]
 * lists it per tree.
 *
 * One simplification against the real game: an empty plant pot is filled by using it on any farming
 * patch with a trowel in hand, rather than by digging a specific soil source.
 *
 * The watered pot ripens on a timer rather than from a timestamp, unlike patches, because it is
 * carried rather than stood in: an item in an inventory is only interesting while its owner is
 * online, so there is nothing to catch up on at login.
 */
class SaplingScript
@Inject
constructor(private val objTypes: ObjTypeList, private val xpMods: XpModifiers) : PluginScript() {
    private val filledPots by lazy {
        setOf(
            objTypes[FarmingObjs.plantpot_compost].id,
            objTypes[FarmingObjs.plantpot_supercompost].id,
        )
    }

    private val wateringCans by lazy {
        FarmingObjs.watering_cans.mapTo(HashSet()) { objTypes[it].id }
    }

    override fun ScriptContext.startup() {
        for (chain in FarmingSaplings.all) {
            onOpHeldU(chain.seed) { saplingUse(it.first, it.second) }
            onOpHeldU(chain.potted) { saplingUse(it.first, it.second) }
        }
        onOpHeldU(FarmingObjs.plantpot_compost) { saplingUse(it.first, it.second) }
        onOpHeldU(FarmingObjs.plantpot_supercompost) { saplingUse(it.first, it.second) }
        for (can in FarmingObjs.watering_cans) {
            onOpHeldU(can) { saplingUse(it.first, it.second) }
        }
        onPlayerSoftTimer(FarmingTimers.sapling) { player.ripenSaplings() }
    }

    /**
     * Works out what the player meant regardless of which item they dragged onto which: the client
     * decides that, and both orders mean the same thing here.
     */
    private suspend fun ProtectedAccess.saplingUse(
        first: UnpackedObjType,
        second: UnpackedObjType,
    ) {
        val potSeed = FarmingSaplings.bySeed[first.id] ?: FarmingSaplings.bySeed[second.id]
        val filledPot = listOf(first, second).firstOrNull { it.id in filledPots }
        if (potSeed != null && filledPot != null) {
            potSeed(potSeed, filledPot)
            return
        }
        val potted = FarmingSaplings.byPotted[first.id] ?: FarmingSaplings.byPotted[second.id]
        val can = listOf(first, second).firstOrNull { it.id in wateringCans }
        if (potted != null && can != null) {
            waterPot(potted, can)
            return
        }
        mes("Nothing interesting happens.")
    }

    private suspend fun ProtectedAccess.potSeed(chain: SaplingChain, pot: UnpackedObjType) {
        if (player.farmingLvl < chain.level) {
            mes("You need a Farming level of ${chain.level} to plant that seed.")
            return
        }
        if (!invDel(inv, chain.seed, count = 1).success) {
            return
        }
        if (!invDel(inv, pot, count = 1).success) {
            return
        }
        anim(FarmingSeqs.trowel_dig)
        invAdd(inv, chain.potted)
        statAdvance(stats.farming, chain.potXp * xpMods.get(player, stats.farming))
        delay(1)
        mes("You plant the seed in the plant pot. It needs watering now.")
    }

    private suspend fun ProtectedAccess.waterPot(chain: SaplingChain, can: UnpackedObjType) {
        if (!invDel(inv, chain.potted, count = 1).success) {
            return
        }
        invAdd(inv, chain.watered)
        emptyCan(can)
        anim(FarmingSeqs.watering)
        player.softTimer(FarmingTimers.sapling, RIPEN_CYCLES)
        delay(1)
        mes("You water the seedling. It will sprout into a sapling shortly.")
    }

    /** A watering can drops one charge per use; the last one leaves an empty can behind. */
    private fun ProtectedAccess.emptyCan(can: UnpackedObjType) {
        val index = FarmingObjs.watering_cans.indexOfFirst { objTypes[it].id == can.id }
        if (index == -1) {
            return
        }
        invDel(inv, can, count = 1)
        val next =
            if (index == 0) FarmingObjs.watering_can_0 else FarmingObjs.watering_cans[index - 1]
        invAdd(inv, next)
    }

    /**
     * Turns every watered pot the player is carrying into a sapling at once, then stops the timer.
     * One timer for the whole inventory means potting a second seed while the first is ripening
     * simply resets the wait, which is close enough to the real thing and far simpler than a clock
     * per pot.
     */
    private fun Player.ripenSaplings() {
        var sprouted = 0
        for (chain in FarmingSaplings.all) {
            val held = inv.count(objTypes[chain.watered])
            if (held <= 0) {
                continue
            }
            invDel(inv, chain.watered, count = held)
            invAdd(inv, chain.sapling, count = held)
            sprouted += held
        }
        clearSoftTimer(FarmingTimers.sapling)
        if (sprouted > 0) {
            mes("Your seedling${if (sprouted == 1) " has" else "s have"} grown into a sapling.")
        }
    }

    private companion object {
        /** The vanilla five minutes a seedling takes, cut by [FarmingRates.GROWTH_SPEEDUP]. */
        val RIPEN_CYCLES: Int = (FarmingRates.stageMillis(5) / 600L).toInt().coerceAtLeast(1)
    }
}
