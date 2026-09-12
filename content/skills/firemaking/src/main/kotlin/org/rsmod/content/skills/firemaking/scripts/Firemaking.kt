package org.rsmod.content.skills.firemaking.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.objParam
import org.rsmod.api.config.objXpParam
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.firemakingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.firemaking.configs.FiremakingContent
import org.rsmod.content.skills.firemaking.configs.FiremakingLocs
import org.rsmod.content.skills.firemaking.configs.FiremakingObjs
import org.rsmod.content.skills.firemaking.configs.FiremakingParams
import org.rsmod.content.skills.firemaking.configs.FiremakingQueues
import org.rsmod.content.skills.firemaking.configs.FiremakingSeqs
import org.rsmod.game.MapClock
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.obj.Obj
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.loc.LocLayerConstants

/**
 * Firemaking: use a tinderbox on logs to light a fire on the tile you are standing on.
 *
 * The OSRS sequence is reproduced fairly closely — you keep attempting until the logs catch, the
 * fire replaces you on your tile and you step back one square west, and the fire eventually
 * collapses into a pile of ashes. Two deliberate simplifications:
 * - The logs are consumed on success rather than dropped to the floor first. In OSRS a failed light
 *   leaves the logs on the ground; here a player who walks away mid-attempt simply keeps them,
 *   which is strictly friendlier and avoids an item-loss bug in exchange for a cosmetic difference.
 * - Fires are lit on the player's own tile only. Lighting on a *different* tile (by using the
 *   tinderbox on floor logs) is not wired up yet.
 */
class Firemaking
@Inject
constructor(
    private val locRepo: LocRepository,
    private val objRepo: ObjRepository,
    private val xpMods: XpModifiers,
    private val invisibleLvls: InvisibleLevels,
    private val perks: Perks,
    // Named `gameClock`, not `mapClock`: `ProtectedAccess.mapClock` is an Int and would
    // shadow this inside every extension function on it.
    private val gameClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeldU(FiremakingContent.firemaking_logs, objs.tinderbox) {
            lightFire(it.first, it.firstSlot)
        }
        onPlayerQueueWithArgs<LightAttempt>(FiremakingQueues.light) { attemptLight(it.args) }
    }

    private fun ProtectedAccess.lightFire(logs: UnpackedObjType, slot: Int) {
        if (player.firemakingLvl < logs.firemakingLevelReq) {
            mes("You need a Firemaking level of ${logs.firemakingLevelReq} to light these logs.")
            return
        }

        val tile = player.coords
        val fire =
            LocInfo(LocLayerConstants.of(LocShape.CentrepieceStraight.id), tile, fireEntity())

        // Reserve the tile before consuming anything: if something already occupies it the player
        // keeps their logs.
        if (!locRepo.add(fire, logs.fireBurnTicks)) {
            mes("You can't light a fire here.")
            return
        }
        // Registering was only a reachability probe; the real fire is placed once it catches.
        locRepo.del(fire, duration = 0)

        mes("You attempt to light the logs.")
        anim(FiremakingSeqs.light_fire)
        weakQueue(
            FiremakingQueues.light,
            ATTEMPT_TICKS,
            LightAttempt(logs, slot, tile, attempt = 1),
        )
    }

    /**
     * A single attempt, re-queued weakly rather than looped behind `delay`.
     *
     * A delayed player cannot walk, so the old loop could freeze them for
     * [MAX_ATTEMPTS] * [ATTEMPT_TICKS] ticks with no way out -- which is exactly what happened to a
     * player on the live server. A weak queue is dropped the moment they walk or act, which is also
     * how OSRS lets you give up on a fire.
     *
     * Everything is re-checked, because a tick has passed since the last attempt: the logs may be
     * gone and the tile may have been built on.
     */
    private fun ProtectedAccess.attemptLight(attempt: LightAttempt) {
        val logs = attempt.logs
        val held = inv[attempt.slot]
        if (player.coords != attempt.tile || held == null || held.id != logs.id) {
            resetAnim()
            return
        }

        val neverFail = perks.has(player, Perk.NeverFailFire)
        val lit =
            neverFail ||
                statRandom(stats.firemaking, logs.fireRateLow, logs.fireRateHigh, invisibleLvls)
        if (!lit) {
            if (attempt.attempt >= MAX_ATTEMPTS) {
                resetAnim()
                mes("You fail to light the logs.")
                return
            }
            anim(FiremakingSeqs.light_fire)
            weakQueue(FiremakingQueues.light, ATTEMPT_TICKS, attempt.next())
            return
        }

        val tile = attempt.tile
        val fire =
            LocInfo(LocLayerConstants.of(LocShape.CentrepieceStraight.id), tile, fireEntity())

        // Everything below must happen together: the logs are only spent once the fire is real.
        val deleted = invDel(inv, logs, count = 1, slot = attempt.slot)
        if (!deleted.success) {
            resetAnim()
            return
        }

        if (!locRepo.add(fire, logs.fireBurnTicks)) {
            // The tile was taken while we were swinging; refund rather than eat the logs.
            invAdd(inv, logs, count = 1)
            resetAnim()
            mes("You can't light a fire here.")
            return
        }

        resetAnim()
        mes("The fire catches and the logs begin to burn.")
        statAdvance(stats.firemaking, logs.fireXp * xpMods.get(player, stats.firemaking))

        // Ashes appear exactly as the fire dies, so the tile is never both burning and ashen.
        objRepo.addDelayed(
            Obj.fromServer(gameClock, tile, FiremakingObjs.ashes, count = 1),
            spawnDelay = logs.fireBurnTicks,
            duration = ASHES_DURATION,
        )

        // Step out of the fire, the way OSRS backs you off west.
        walk(tile.translateX(-1))
    }

    private fun fireEntity(): LocEntity =
        LocEntity(
            id = FiremakingLocs.fire.id,
            shape = LocShape.CentrepieceStraight.id,
            angle = LocAngle.West.id,
        )

    /** One pending attempt, carried on the weak queue. */
    private data class LightAttempt(
        val logs: UnpackedObjType,
        val slot: Int,
        val tile: CoordGrid,
        val attempt: Int,
    ) {
        fun next(): LightAttempt = copy(attempt = attempt + 1)
    }

    companion object {
        private const val ATTEMPT_TICKS = 4
        /** Generous: OSRS never permanently fails, this only stops a runaway loop. */
        private const val MAX_ATTEMPTS = 40
        private const val ASHES_DURATION = 200

        val UnpackedObjType.firemakingLevelReq: Int by objParam(params.levelrequire)
        val UnpackedObjType.fireXp: Double by objXpParam(params.skill_xp)
        val UnpackedObjType.fireRateLow: Int by objParam(FiremakingParams.rate_low)
        val UnpackedObjType.fireRateHigh: Int by objParam(FiremakingParams.rate_high)
        val UnpackedObjType.fireBurnTicks: Int by objParam(FiremakingParams.burn_ticks)
    }
}
