package org.rsmod.content.custom.zulrah

import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.player.hit.processor.InstantPlayerHitProcessor
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.hit.takeInstantHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.toxins.Toxins
import org.rsmod.content.custom.zulrah.configs.ZulrahShrine
import org.rsmod.content.custom.zulrah.configs.zulrah_locs
import org.rsmod.content.custom.zulrah.configs.zulrah_npcs
import org.rsmod.content.custom.zulrah.configs.zulrah_projanims
import org.rsmod.content.custom.zulrah.configs.zulrah_seqs
import org.rsmod.content.custom.zulrah.configs.zulrah_spots
import org.rsmod.content.custom.zulrah.configs.zulrah_timers
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

/**
 * The fight itself: one Zulrah per player, in that player's own copy of the shrine, working through
 * [ZulrahRotation] one action at a time.
 *
 * ### Why a tick-counting state machine and not a coroutine
 *
 * The obvious shape is a suspending loop - attack, `delay(3)`, attack, dive - and it does not work.
 * A suspended npc is *busy*, and `NpcHitScript` only processes the hits an npc receives while it is
 * not busy, so every hit the player landed would be held back until the loop ended; and the next
 * thing that launched a coroutine on the npc would cancel the rotation outright. So nothing here
 * suspends. `zulrah_fight` fires every tick, [tick] reads where the fight is up to, does at most
 * one thing and returns.
 *
 * ### Why the state is keyed on the npc object
 *
 * Each dive calls `changeType`, which reassigns the npc's uid. The [Npc] instance survives it, and
 * so do its hitpoints and hero points, so that is the key. For the same reason the timer is bound
 * on all three forms: npc timers dispatch on `visType`, the form Zulrah currently wears.
 *
 * ### What it deliberately does not do
 *
 * No random rotations, no Jad phase, no magma stun, and no cap on the damage Zulrah takes.
 */
@Singleton
class ZulrahFight
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val npcTypes: NpcTypeList,
    private val objTypes: ObjTypeList,
    private val collision: CollisionFlagMap,
    private val accuracy: AccuracyFormulae,
    private val toxins: Toxins,
    private val hitProcessor: InstantPlayerHitProcessor,
) {
    private val fights = IdentityHashMap<Npc, ZulrahFightState>()

    /** Every fight still running, for the tests and for the logout and death hooks. */
    val active: Collection<ZulrahFightState>
        get() = fights.values

    fun fightOf(npc: Npc): ZulrahFightState? = fights[npc]

    fun fightOf(player: Player): ZulrahFightState? =
        fights.values.firstOrNull { it.owner === player }

    /**
     * Allocates a private shrine, lands the player on the platform and raises Zulrah in the middle.
     * Returns `null`, having moved nothing, when the server is out of region slots.
     */
    fun enter(access: ProtectedAccess): ZulrahFightState? {
        val region = regionRepo.add(ZulrahShrine.template) ?: return null
        access.telejump(region.normal[ZulrahShrine.arrival])

        val first = ZulrahRotation.phases.first()
        val spawn = region.normal[checkNotNull(ZulrahShrine.spots[first.spot])]
        val npc = Npc(npcTypes[checkNotNull(zulrah_npcs.forms[first.form])], spawn)
        npcRepo.add(npc, ZULRAH_LIFETIME)
        npc.anim(zulrah_seqs.rise)
        npc.hideAllOps()

        val state = ZulrahFightState(npc, access.player, region, phaseIndex = 0)
        state.stage = ZulrahStage.Emerging
        state.countdown = RISE_TICKS
        fights[npc] = state
        npc.timer(zulrah_timers.fight, 1)
        return state
    }

    /** Called every tick by `zulrah_fight` on whichever form Zulrah is wearing. */
    fun tick(access: StandardNpcAccess) {
        val npc = access.npc
        // No fight means Zulrah has just been killed and its death sequence is running. That
        // sequence owns the despawn; deleting the npc here as well makes it fail with
        // `UnexpectedSlot`.
        val state = fights[npc] ?: return
        if (npc.hitpoints <= 0) {
            return
        }
        if (!ownerPresent(state)) {
            end(state)
            return
        }
        val mapClock = access.mapClock
        landPending(state, mapClock)
        hurtInClouds(state, access.random, mapClock)
        resolveWhip(access, state, mapClock)

        if (--state.countdown > 0) {
            return
        }
        when (state.stage) {
            ZulrahStage.Emerging -> {
                npc.showAllOps()
                state.stage = ZulrahStage.Acting
                state.countdown = 1
            }
            ZulrahStage.Acting -> act(access, state, mapClock)
            ZulrahStage.Diving -> surface(access, state)
        }
    }

    /** Zulrah is dead. The drop table has already been claimed by `DropTableScript`. */
    fun killed(npc: Npc) {
        val state = fights.remove(npc) ?: return
        clearSnakelings(state)
    }

    /** The owner left, died or logged out: take everything of this fight out of the world. */
    fun end(state: ZulrahFightState) {
        fights.remove(state.npc)
        clearSnakelings(state)
        if (state.npc.isSlotAssigned) {
            npcRepo.del(state.npc, Int.MAX_VALUE)
        }
    }

    fun ownerPresent(state: ZulrahFightState): Boolean {
        val owner = state.owner
        return owner.isValidTarget() && state.region.contains(owner.coords)
    }

    private fun act(access: StandardNpcAccess, state: ZulrahFightState, mapClock: Int) {
        val phase = ZulrahRotation.phases[state.phaseIndex]
        if (state.actionIndex >= phase.actions.size) {
            dive(access, state)
            return
        }
        val action = phase.actions[state.actionIndex++]
        state.countdown = ATTACK_TICKS
        access.npc.faceSquare(state.owner.coords)
        when (action) {
            ZulrahAction.Attack ->
                when (phase.form) {
                    ZulrahForm.Serpentine -> spit(access, state, magic = false)
                    ZulrahForm.Tanzanite ->
                        spit(access, state, magic = access.random.of(TANZANITE_RANGED_ONE_IN) != 0)
                    ZulrahForm.Magma -> stare(access, state, mapClock)
                }
            ZulrahAction.Cloud -> barrage(access, state, mapClock)
            ZulrahAction.Snakeling -> egg(access, state, mapClock)
        }
    }

    /** A ranged or magic attack at the owner, landing when the projectile does. */
    private fun spit(access: StandardNpcAccess, state: ZulrahFightState, magic: Boolean) {
        val npc = access.npc
        val target = state.owner
        access.anim(zulrah_seqs.attack)
        val spot = if (magic) zulrah_spots.magic else zulrah_spots.ranged
        val projectile = worldRepo.projAnim(npc, target, spot, zulrah_projanims.spit)
        val (serverDelay, _) = projectile.durations
        val landed =
            if (magic) {
                accuracy.rollMagicAccuracy(npc, target, access.random)
            } else {
                accuracy.rollRangedAccuracy(npc, target, access.random)
            }
        val damage = if (landed) access.random.of(MAX_HIT + 1) else 0
        // Before the hit, never after: queued after it, every hit trips the speed-up death.
        target.queueCombatRetaliate(npc, serverDelay)
        val hit =
            target.queueHit(
                npc,
                serverDelay.coerceAtLeast(1),
                if (magic) HitType.Magic else HitType.Ranged,
                damage,
            )
        target.combatPlayDefendAnim(objTypes)
        if (hit.damage > 0 && access.random.of(VENOM_ONE_IN) == 0) {
            toxins.envenom(target)
        }
    }

    /**
     * The magma form's attack, first half: it fixes on the tile the owner is standing on and raises
     * its tail. [resolveWhip] brings it down [WHIP_DELAY] ticks later.
     */
    private fun stare(access: StandardNpcAccess, state: ZulrahFightState, mapClock: Int) {
        val target = state.owner.coords
        val npc = access.npc
        val centreX = npc.coords.x + ZulrahShrine.ZULRAH_SIZE / 2
        access.anim(if (target.x < centreX) zulrah_seqs.tail_left else zulrah_seqs.tail_right)
        state.whip = ZulrahWhip(target, mapClock + WHIP_DELAY)
        state.countdown = MAGMA_ATTACK_TICKS
    }

    /**
     * The tail lands. Typeless, so no prayer stops it; the only defence is not being within a tile
     * of where the owner stood when Zulrah stared.
     */
    private fun resolveWhip(access: StandardNpcAccess, state: ZulrahFightState, mapClock: Int) {
        val whip = state.whip ?: return
        if (mapClock < whip.landsAt) {
            return
        }
        state.whip = null
        val owner = state.owner
        if (!owner.coords.withinOneTileOf(whip.target)) {
            return
        }
        val damage = access.random.of(WHIP_MIN_HIT, WHIP_MAX_HIT)
        owner.queueCombatRetaliate(access.npc, 1)
        owner.queueHit(access.npc, 1, HitType.Typeless, damage)
    }

    /**
     * A venom barrage: a projectile to a platform tile near the owner, and a cloud where it lands.
     */
    private fun barrage(access: StandardNpcAccess, state: ZulrahFightState, mapClock: Int) {
        val tile = platformTileNear(state, access.random)
        access.anim(zulrah_seqs.attack)
        val projectile =
            worldRepo.projAnim(access.npc, tile, zulrah_spots.cloud, zulrah_projanims.spit)
        state.pending +=
            ZulrahLanding(tile, mapClock + projectile.durations.serverDelay, cloud = true)
    }

    /** An egg to a platform tile near the owner, which hatches into a snakeling on landing. */
    private fun egg(access: StandardNpcAccess, state: ZulrahFightState, mapClock: Int) {
        val tile = platformTileNear(state, access.random)
        access.anim(zulrah_seqs.attack)
        val projectile =
            worldRepo.projAnim(access.npc, tile, zulrah_spots.egg, zulrah_projanims.spit)
        state.pending +=
            ZulrahLanding(tile, mapClock + projectile.durations.serverDelay, cloud = false)
    }

    private fun landPending(state: ZulrahFightState, mapClock: Int) {
        val iterator = state.pending.iterator()
        while (iterator.hasNext()) {
            val landing = iterator.next()
            if (mapClock < landing.landsAt) {
                continue
            }
            iterator.remove()
            if (landing.cloud) {
                // A 3x3 loc is placed by its south-west corner, so this centres the cloud on the
                // tile the barrage was aimed at.
                val corner = landing.tile.translate(-1, -1)
                locRepo.add(
                    corner,
                    zulrah_locs.poison_cloud,
                    CLOUD_TICKS,
                    LocAngle.West,
                    LocShape.CentrepieceStraight,
                )
                state.clouds += ZulrahCloud(corner, mapClock + CLOUD_TICKS)
            } else {
                val type =
                    if (state.eggsHatched++ % 2 == 0) {
                        zulrah_npcs.snakeling_melee
                    } else {
                        zulrah_npcs.snakeling_magic
                    }
                val snakeling = Npc(npcTypes[type], landing.tile)
                npcRepo.add(snakeling, SNAKELING_LIFETIME)
                snakeling.anim(zulrah_seqs.snakeling_spawn)
                state.snakelings += snakeling
            }
        }
    }

    private fun hurtInClouds(state: ZulrahFightState, random: GameRandom, mapClock: Int) {
        state.clouds.removeAll { it.expiresAt <= mapClock }
        val owner = state.owner
        if (state.clouds.none { it.covers(owner.coords) }) {
            return
        }
        val damage = random.of(CLOUD_MIN_HIT, CLOUD_MAX_HIT)
        owner.takeInstantHit(HitType.Typeless, damage.coerceAtMost(owner.hitpoints), hitProcessor)
        toxins.envenom(owner)
    }

    private fun dive(access: StandardNpcAccess, state: ZulrahFightState) {
        access.anim(zulrah_seqs.dive)
        access.hideAllOps()
        state.stage = ZulrahStage.Diving
        state.countdown = DIVE_TICKS
    }

    /**
     * Under water: change form, move to the next spot, and come back up. The player's target was
     * already lost when the uid changed, which is live behaviour too - you re-click every phase.
     */
    private fun surface(access: StandardNpcAccess, state: ZulrahFightState) {
        state.phaseIndex = ZulrahRotation.next(state.phaseIndex)
        state.actionIndex = 0
        val phase = ZulrahRotation.phases[state.phaseIndex]
        access.changeType(checkNotNull(zulrah_npcs.forms[phase.form]), Int.MAX_VALUE, npcTypes)
        val spot = state.region.normal[checkNotNull(ZulrahShrine.spots[phase.spot])]
        access.telejump(spot, collision)
        access.anim(zulrah_seqs.emerge)
        state.stage = ZulrahStage.Emerging
        state.countdown = EMERGE_TICKS
    }

    private fun platformTileNear(state: ZulrahFightState, random: GameRandom): CoordGrid {
        val owner = state.owner.coords
        val near =
            ZulrahShrine.platform
                .map { state.region.normal[it] }
                .filter { max(abs(it.x - owner.x), abs(it.z - owner.z)) <= LANDING_RADIUS }
        return if (near.isEmpty()) owner else near[random.of(near.size)]
    }

    private fun clearSnakelings(state: ZulrahFightState) {
        for (snakeling in state.snakelings) {
            if (snakeling.isSlotAssigned) {
                npcRepo.del(snakeling, Int.MAX_VALUE)
            }
        }
        state.snakelings.clear()
    }

    private fun Region.contains(coords: CoordGrid): Boolean =
        coords.x in southWest.x..northEast.x && coords.z in southWest.z..northEast.z

    private fun CoordGrid.withinOneTileOf(other: CoordGrid): Boolean =
        level == other.level && abs(x - other.x) <= 1 && abs(z - other.z) <= 1

    companion object {
        /** Every attack Zulrah makes, of every style, tops out here. */
        const val MAX_HIT: Int = 41

        /** Zulrah's attack rate, which the cache already carries as `attackrate = 3`. */
        const val ATTACK_TICKS: Int = 3

        /**
         * The magma form stares for this long before its tail comes down: time enough to take two
         * steps, which is the counterplay.
         */
        const val WHIP_DELAY: Int = 3

        /** Stare, whip and recover. Two of these make up a magma phase. */
        const val MAGMA_ATTACK_TICKS: Int = 6

        /** The wiki's 20-30 for a tail that connects. */
        const val WHIP_MIN_HIT: Int = 20
        const val WHIP_MAX_HIT: Int = 30

        const val RISE_TICKS: Int = 4
        const val DIVE_TICKS: Int = 3
        const val EMERGE_TICKS: Int = 3

        /** Tuning, not a wiki figure: how long a toxic cloud lingers. */
        const val CLOUD_TICKS: Int = 30

        /** Tuning, not a wiki figure: the per-tick damage of standing in a cloud. */
        const val CLOUD_MIN_HIT: Int = 1
        const val CLOUD_MAX_HIT: Int = 4

        /** Tuning, not a wiki figure: one landed ranged or magic hit in four envenoms. */
        const val VENOM_ONE_IN: Int = 4

        /** The blue form mostly casts; one attack in four is a ranged spit instead. */
        const val TANZANITE_RANGED_ONE_IN: Int = 4

        /** The wiki's forty seconds, after which an ignored snakeling dies off. */
        const val SNAKELING_LIFETIME: Int = 67

        /** How far from the owner clouds and eggs land. */
        const val LANDING_RADIUS: Int = 2

        /**
         * Long enough that no fight outlasts it; the fight deletes Zulrah itself on every normal
         * exit, and the region takes anything left behind.
         */
        const val ZULRAH_LIFETIME: Int = 6000
    }
}

enum class ZulrahStage {
    Emerging,
    Acting,
    Diving,
}

data class ZulrahWhip(val target: CoordGrid, val landsAt: Int)

data class ZulrahLanding(val tile: CoordGrid, val landsAt: Int, val cloud: Boolean)

data class ZulrahCloud(val southWest: CoordGrid, val expiresAt: Int) {
    fun covers(coords: CoordGrid): Boolean =
        coords.level == southWest.level &&
            coords.x in southWest.x until southWest.x + CLOUD_SIZE &&
            coords.z in southWest.z until southWest.z + CLOUD_SIZE

    private companion object {
        const val CLOUD_SIZE = 3
    }
}

class ZulrahFightState(val npc: Npc, val owner: Player, val region: Region, var phaseIndex: Int) {
    var actionIndex: Int = 0
    var stage: ZulrahStage = ZulrahStage.Emerging
    var countdown: Int = 0
    var whip: ZulrahWhip? = null
    var eggsHatched: Int = 0
    val pending: MutableList<ZulrahLanding> = mutableListOf()
    val clouds: MutableList<ZulrahCloud> = mutableListOf()
    val snakelings: MutableList<Npc> = mutableListOf()
}
