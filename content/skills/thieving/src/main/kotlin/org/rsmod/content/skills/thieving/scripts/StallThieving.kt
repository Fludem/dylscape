package org.rsmod.content.skills.thieving.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.thieving.configs.Stall
import org.rsmod.content.skills.thieving.configs.ThievingRates
import org.rsmod.content.skills.thieving.configs.ThievingSeqs
import org.rsmod.content.skills.thieving.configs.ThievingStalls
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Market stalls: the mining loop with the depletion taken out.
 *
 * Three things follow from "stalls never deplete", and together they make this the simplest script
 * in the module.
 *
 * **Re-queueing is trivial.** Nothing here calls `LocRepository.change`, so the loc the interaction
 * points at stays valid forever, `InteractionLoc.isValid()` keeps returning true, and the tail is a
 * bare `opLoc2`. Contrast mining, which must change the rock and therefore must *not* re-queue.
 *
 * **There is no success roll.** Stealing from a stall always succeeds in OSRS — what paces it there
 * is the respawn timer, which we have removed. Rolling for failure instead would just be dead time
 * with no penalty attached, which is the opposite of what an AFK skill wants. [Stall.ticks] is the
 * throttle now, and the level requirement is a gate rather than a curve. Because nothing suspends,
 * this handler is a plain function rather than a `suspend` one.
 *
 * **Loot is paid bank-noted.** This is what stops the 5x multiplier working against the player.
 * Cake, silk, fur, silver ore and gems do not stack, so five loose items a theft would fill an
 * inventory in six actions and make a stall session *shorter* than a faithful one. Noted items
 * stack, so the haul is a pure gain and the session runs until the player walks away. `invAdd`
 * already takes a `cert` flag, so this costs nothing beyond passing it — and every product in the
 * table has a cert link, which `ThievingConfigTest` pins.
 */
class StallThieving @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((loc, stall) in ThievingStalls.all) {
            onOpLoc2(loc) { steal(it.loc, stall) }
        }
    }

    private fun ProtectedAccess.steal(loc: BoundLocInfo, stall: Stall) {
        if (player.thievingLvl < stall.level) {
            mes("You need a Thieving level of ${stall.level} to steal from the ${stall.what}.")
            return
        }

        // Noted loot stacks, so this realistically only trips on a bag full of something else.
        if (inv.isFull()) {
            mes("Your inventory is too full to hold any more loot.")
            soundSynth(synths.pillory_wrong)
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + stall.ticks
            faceLoc(loc)
            anim(ThievingSeqs.pickpocket)
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + stall.ticks
            spam("You attempt to steal from the ${stall.what}.")
        } else if (actionDelay == mapClock) {
            award(loc, stall)
        }

        // The stall is still standing either way, so keep stealing.
        opLoc2(loc)
    }

    private fun ProtectedAccess.award(loc: BoundLocInfo, stall: Stall) {
        val product = random.pick(stall.loot)
        val count = stall.baseCount * ThievingRates.LOOT_RATE
        invAdd(inv, product, count = count, cert = true)
        statAdvance(
            stats.thieving,
            stall.baseXp * ThievingRates.XP_RATE * xpMods.get(player, stats.thieving),
        )
        spam("You steal from the ${stall.what}.")
        publish(StoleFromStall(player, loc, product, count))
    }

    data class StoleFromStall(
        val player: Player,
        val stall: BoundLocInfo,
        val product: ObjType,
        val count: Int,
    ) : UnboundEvent
}
