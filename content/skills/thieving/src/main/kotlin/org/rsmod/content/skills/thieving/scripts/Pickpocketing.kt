package org.rsmod.content.skills.thieving.scripts

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stat.thievingLvl
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.thieving.configs.PickpocketTarget
import org.rsmod.content.skills.thieving.configs.ThievingRates
import org.rsmod.content.skills.thieving.configs.ThievingSeqs
import org.rsmod.content.skills.thieving.configs.ThievingSpotanims
import org.rsmod.content.skills.thieving.configs.ThievingTargets
import org.rsmod.content.skills.thieving.scripts.CoinPouches.Companion.autoOpenPouchesIfFull
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.ProtectedAccessLostException
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Pickpocketing, built on the same skilling loop as fishing and mining but tuned to be AFK.
 *
 * Three things make it differ from the other skills in shape.
 *
 * **It never fills the inventory.** A success pays a `pickpocket_coin_pouch_*`, which is stackable,
 * so one click keeps stealing indefinitely rather than stopping 28 items later. That is the whole
 * point of the pouch layer, and it is vanilla behaviour rather than something invented here — the
 * cache even ships the destroy note "You may simply open the pouch instead to continue
 * pickpocketing".
 *
 * **A failure does not end the action.** In OSRS being caught stuns you and you have to click
 * again. Here the stun still lands — animation, graphic, damage and a few ticks frozen — but the
 * loop re-queues itself afterwards, so an unattended player keeps going.
 *
 * **Targets are registered per npc type, not by content group.** See [ThievingTargets] for why:
 * `man` and `woman` already belong to upstream's `content.person`, and content group is a single
 * field.
 *
 * `Pickpocket` is op3 on every target, read out of the cache by `ThievingOpsDump` rather than
 * assumed — it is op3 even on npcs like `farmer1` whose op1 is empty.
 */
class Pickpocketing
@Inject
constructor(private val xpMods: XpModifiers, private val invisibleLvls: InvisibleLevels) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for ((npc, target) in ThievingTargets.all) {
            onOpNpc3(npc) { pickpocket(it.npc, target) }
        }
    }

    /**
     * Suspends only on the failure branch.
     *
     * That matters: while a player is access-protected `PlayerInteractionProcessor` skips them
     * entirely, so a handler that suspended on every tick would stall the very interaction it
     * re-queues. The success path stays synchronous and drives its cadence off the map clock, which
     * is the idiom fishing and mining use.
     */
    private suspend fun ProtectedAccess.pickpocket(npc: Npc, target: PickpocketTarget) {
        if (player.thievingLvl < target.level) {
            mes("You need a Thieving level of ${target.level} to pickpocket the ${npc.name}.")
            return
        }

        // Failures still cost hitpoints and this loop is designed to run unattended, so there are
        // two layers keeping an AFK session safe. This is the outer one: park the player somewhere
        // they can come back to rather than at one hitpoint. The inner one is the damage clamp in
        // `stun`, which makes a stun physically incapable of landing a killing blow.
        if (player.hitpoints <= player.stopHitpoints()) {
            mes("You are too badly hurt to carry on thieving.")
            soundSynth(synths.pillory_wrong)
            return
        }

        if (!npc.isValidTarget()) {
            return
        }

        // Pouches stack, so this only trips when the bag is genuinely full of something else.
        if (inv.isFull() && invTotal(inv, target.pouch) <= 0) {
            mes("Your inventory is too full to hold any more coin pouches.")
            soundSynth(synths.pillory_wrong)
            return
        }

        if (skillAnimDelay <= mapClock) {
            skillAnimDelay = mapClock + PICKPOCKET_DELAY
            anim(ThievingSeqs.pickpocket)
        }

        if (actionDelay < mapClock) {
            actionDelay = mapClock + PICKPOCKET_DELAY
            spam("You attempt to pick the ${npc.name}'s pocket.")
        } else if (actionDelay == mapClock) {
            val success = statRandom(stats.thieving, target.rateLow, target.rateHigh, invisibleLvls)
            if (success) {
                award(npc, target)
            } else {
                stun(npc, target)
            }
        }

        // The whole AFK mechanism. An op fires once and clears its own interaction, so without this
        // the player would stop after a single attempt.
        opNpc3(npc)
    }

    private fun ProtectedAccess.award(npc: Npc, target: PickpocketTarget) {
        invAdd(inv, target.pouch)
        statAdvance(
            stats.thieving,
            target.baseXp * ThievingRates.XP_RATE * xpMods.get(player, stats.thieving),
        )
        spam("You pick the ${npc.name}'s pocket.")
        publish(Pickpocketed(player, npc, target.pouch))

        // Keeps the session from ever stalling on a full stack; see `CoinPouches.AUTO_OPEN_AT`.
        autoOpenPouchesIfFull(target.pouch)
    }

    /**
     * Catches the player and freezes them, then lets the caller re-queue the op.
     *
     * `delay` is what actually locks the player down: while `isDelayed` every op and movement
     * packet handler early-returns and clears the map flag, so they cannot walk or click out of it.
     * The `walktriggers.stunned` trigger looks like the tool for this but is not — it only
     * publishes an event when the player is *not* busy and then clears itself, so it locks nothing.
     *
     * When the delay resumes, control returns to [pickpocket], which falls straight through to
     * `opNpc3` and the loop continues without a click. If the player was cancelled during the stun
     * the delay throws instead, and the re-queue is never reached — which is the correct way out.
     */
    private suspend fun ProtectedAccess.stun(npc: Npc, target: PickpocketTarget) {
        spam("You fail to pick the ${npc.name}'s pocket.")
        npc.say("What do you think you're doing?")
        anim(ThievingSeqs.stunned)
        spotanim(ThievingSpotanims.stunned)
        // Instant, not queued. `queueHit` uses a *strong* queue, and
        // `PlayerQueueProcessor.canLaunchQueue` refuses to launch anything but a soft queue while
        // access is protected -- which we are about to be, for the whole stun. A queued hit would
        // therefore sit dormant until the delay ended and then fire on the resume tick, out of sync
        // with the animation, inside a second protected-access coroutine racing the re-queue below.
        // `takeInstantHit` applies the damage synchronously with no queue and no combat
        // attribution, which also keeps an AFK skiller from being flagged as in combat.
        //
        // The clamp is the hard half of the death guard: a thieving stun can never be the killing
        // blow, whatever the player's hitpoints are when it lands.
        val damage = min(random.of(target.stunDamage), player.hitpoints - 1)
        if (damage > 0) {
            takeInstantHit(HitType.Typeless, damage)
        }

        try {
            delay(target.stunTicks)
        } catch (_: ProtectedAccessLostException) {
            // Something took our access while we were frozen -- a server-driven modal, or a logout.
            // Both are fair reasons to drop the loop, and catching it here matters: left to
            // propagate, `PlayerMainProcess.tryOrDisconnect` would turn it into a disconnect.
            return
        }
        resetAnim()
    }

    data class Pickpocketed(val player: Player, val npc: Npc, val pouch: ObjType) : UnboundEvent

    private companion object {
        /**
         * Ticks between attempts. OSRS pickpockets on a three-tick cycle; keeping that here means
         * the AFK-ness comes from never being interrupted rather than from the skill being faster
         * per action than it should be.
         */
        const val PICKPOCKET_DELAY = 3

        /**
         * The share of the player's own hitpoints bar an unattended session parks at. The damage
         * clamp already makes a stun unable to kill, but on its own that would leave a returning
         * player sitting at one hitpoint; stopping here leaves them somewhere safe.
         *
         * It has to be a fraction rather than a flat number. A flat ten is a fifth of a maxed bar
         * but the *whole* bar of a fresh level-3 account, so it refused every attempt that account
         * ever made before a single roll was taken.
         */
        const val STOP_HITPOINTS_PERCENT = 20

        /**
         * Floor for [STOP_HITPOINTS_PERCENT], so the smallest bars still stop above one hitpoint.
         */
        const val MIN_STOP_HITPOINTS = 2

        /**
         * Read off [baseHitpointsLvl] rather than the current level: a boost should not move the
         * floor, and neither should the damage already taken this session.
         */
        fun Player.stopHitpoints(): Int =
            max(MIN_STOP_HITPOINTS, baseHitpointsLvl * STOP_HITPOINTS_PERCENT / 100)
    }
}
