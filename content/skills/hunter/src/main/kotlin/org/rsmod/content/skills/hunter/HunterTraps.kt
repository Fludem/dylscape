package org.rsmod.content.skills.hunter

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.controller.vars.intVarCon
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statRandom
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.stats.levelmod.InvisibleLevels
import org.rsmod.content.skills.hunter.configs.HunterControllers
import org.rsmod.content.skills.hunter.configs.HunterLocs
import org.rsmod.content.skills.hunter.configs.HunterNpcs
import org.rsmod.content.skills.hunter.configs.HunterParams
import org.rsmod.content.skills.hunter.configs.HunterVarCons
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Controller
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.NpcList
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcUid
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.npc.NpcType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.loc.LocLayerConstants

/**
 * The shared lifecycle behind all four trapping methods.
 *
 * One [Controller] represents one laid trap, keyed on the tile it sits on. That is the right home
 * for the state: traps *are* tile-keyed, so `conRepo.findExact(coords, type)` finds the trap under
 * any op; `onAiConTimer` gives each trap a self-rearming tick without a global scan; and
 * `Controller.duration` is a free backstop that removes the trap even if the state machine wedges.
 * There is no world-tick script hook to hang a singleton registry off in any case.
 *
 * The catch itself is deliberately a plain function taking a controller and an npc, with no
 * `ProtectedAccess` and no event receiver, so the thing that decides *when* to try a catch can be
 * swapped without touching the logic that decides *whether* one succeeds.
 *
 * Nothing is granted to the player here. XP, the product and the returned trap are all handed over
 * when the player checks a full trap, which is both what OSRS does and what keeps inventory and
 * stat changes inside protected access where they belong.
 */
@Singleton
class HunterTraps
@Inject
constructor(
    private val locRepo: LocRepository,
    private val npcRepo: NpcRepository,
    private val conRepo: ControllerRepository,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
    private val enumTypes: EnumTypeList,
    private val playerList: PlayerList,
    private val npcList: NpcList,
    private val random: GameRandom,
    private val invisibleLvls: InvisibleLevels,
    private val mapClock: MapClock,
) {
    /**
     * Places a trap on [coords] and starts its lifecycle.
     *
     * The loc is registered by the caller (which knows the right shape and angle for its method);
     * this attaches the controller and the invisible attractor that makes creatures walk in.
     */
    /**
     * Reserves [coords] for a trap and hands back the loc that was placed, or `null` if the player
     * may not lay one there.
     *
     * Split out from [register] so the caller can reserve the tile *before* spending the trap item
     * and only commit once the set-up animation has run. A blocked tile or a full allowance then
     * costs the player nothing.
     */
    fun tryPlace(owner: Player, coords: CoordGrid, kind: Int, armed: LocType): LocInfo? {
        if (countActiveTraps(owner) >= maxTraps(owner.stat(stats.hunter))) {
            return null
        }
        val loc =
            LocInfo(
                LocLayerConstants.of(LocShape.CentrepieceStraight.id),
                coords,
                LocEntity(armed.id, LocShape.CentrepieceStraight.id, 0),
            )
        if (!locRepo.add(loc, TRAP_LIFETIME + LIFETIME_SLACK)) {
            return null
        }
        return loc
    }

    /** Undoes a [tryPlace] whose caller could not go through with laying the trap. */
    fun cancelPlace(loc: LocInfo) {
        locRepo.del(loc, duration = 0)
    }

    fun register(owner: Player, coords: CoordGrid, kind: Int, loc: LocInfo) {
        val controller = Controller(HunterControllers.hunter_trap, coords)
        conRepo.add(controller, TRAP_LIFETIME + LIFETIME_SLACK)

        controller.trapOwner = owner.uid.packed
        controller.trapKind = kind
        controller.trapLocId = loc.id
        controller.enterState(TrapState.ARMED)

        val attractor = Npc(npcTypes.getValue(armedAttractor(kind).id), coords)
        npcRepo.add(attractor, TRAP_LIFETIME + LIFETIME_SLACK)
        controller.trapNpcUid = attractor.uid.packed

        controller.aiTimer(1)
    }

    /**
     * How many traps [player] currently has out.
     *
     * Derived rather than stored, so there is no counter to drift: a trap the player has walked
     * away from has already collapsed by the time they are far enough for it to matter.
     */
    fun countActiveTraps(player: Player): Int {
        val uid = player.uid.packed
        val centre = ZoneKey.from(player.coords)
        var count = 0
        // `ControllerRepository` has no radius overload, so walk the neighbouring zones by hand.
        for (dx in -TRAP_SEARCH_ZONES..TRAP_SEARCH_ZONES) {
            for (dz in -TRAP_SEARCH_ZONES..TRAP_SEARCH_ZONES) {
                for (controller in conRepo.findAll(centre.translate(dx, dz))) {
                    val mine =
                        controller.id == HunterControllers.hunter_trap.id &&
                            controller.trapOwner == uid
                    if (mine) {
                        count++
                    }
                }
            }
        }
        return count
    }

    /** OSRS's trap allowance: one more trap every twenty levels, to a maximum of five. */
    fun maxTraps(hunterLvl: Int): Int =
        when {
            hunterLvl >= 80 -> 5
            hunterLvl >= 60 -> 4
            hunterLvl >= 40 -> 3
            hunterLvl >= 20 -> 2
            else -> 1
        }

    fun findTrap(coords: CoordGrid): Controller? =
        conRepo.findExact(coords, HunterControllers.hunter_trap)

    /**
     * Rolls whether [creature] is caught by the trap it has just walked into.
     *
     * Called once, when the creature reaches the attractor. A failure is not a dead end: the hunt
     * mode keeps hunting, so the creature will wander off and try again later.
     */
    fun attemptCatch(trap: Controller, creature: Npc) {
        if (trap.trapState != TrapState.ARMED) {
            return
        }

        val type = creature.visType
        if (type.param(HunterParams.trap_kind) != trap.trapKind) {
            return
        }

        // An offline owner's traps are already collapsing; nothing should be caught in them.
        val owner = PlayerUid(trap.trapOwner).resolve(playerList) ?: return
        if (owner.stat(stats.hunter) < type.param(params.levelrequire)) {
            return
        }

        val caught =
            owner.statRandom(
                random,
                stats.hunter,
                type.param(HunterParams.rate_low),
                type.param(HunterParams.rate_high),
                invisibleLvls,
            )

        trap.trapCreature = type.id
        if (caught) {
            trap.enterState(TrapState.CATCHING, approachFrom(trap.coords, creature.coords))
            npcRepo.despawn(creature, type.param(HunterParams.creature_respawn))
        } else {
            trap.enterState(TrapState.FAILING)
        }
    }

    /**
     * One tick of one trap.
     *
     * Handles two things: advancing the transient states after their animation, and noticing that
     * the owner has gone. There is no logout hook — a trap discovers abandonment itself, which is
     * simpler than keeping a registry in step with the login lifecycle.
     */
    fun tick(trap: Controller) {
        val owner = PlayerUid(trap.trapOwner).resolve(playerList)
        val abandoned =
            owner == null ||
                owner.coords.level != trap.coords.level ||
                owner.coords.chebyshevDistance(trap.coords) > ABANDON_DISTANCE
        trap.trapAbandoned = if (abandoned) trap.trapAbandoned + 1 else 0
        if (trap.trapAbandoned >= ABANDON_TICKS) {
            collapse(trap)
            return
        }

        val elapsed = mapClock.cycle - trap.trapStageTick
        when (trap.trapState) {
            TrapState.CATCHING -> if (elapsed >= TRANSIENT_TICKS) trap.enterState(TrapState.FULL)
            TrapState.FAILING -> if (elapsed >= TRANSIENT_TICKS) trap.enterState(TrapState.FAILED)
            TrapState.ARMED -> if (elapsed >= TRAP_LIFETIME) collapse(trap)
            TrapState.FULL -> if (elapsed >= FULL_LIFETIME) collapse(trap)
            TrapState.FAILED -> if (elapsed >= FAILED_LIFETIME) collapse(trap)
        }

        if (findTrap(trap.coords) != null) {
            trap.aiTimer(1)
            trap.resetDuration()
        }
    }

    /**
     * Tears a trap down and gives the player their equipment back on the floor.
     *
     * Used both for timeouts and for abandonment, so a player who logs out mid-hunt does not lose
     * their traps, and a hunting ground does not silently fill up with other people's litter.
     */
    fun collapse(trap: Controller) {
        val placed = locTypes[trap.trapLocId]
        if (placed != null) {
            val existing = locRepo.findExact(trap.coords, placed)
            if (existing != null) {
                locRepo.del(existing, duration = 0)
            }
            // Scenery-based traps go back to the state the map ships them in rather than vanishing.
            val restore = restoreState(trap.trapKind, placed)
            if (restore != null) {
                locRepo.add(
                    LocInfo(existing?.layer ?: 0, trap.coords, entity(restore)),
                    Int.MAX_VALUE,
                )
            }
        }
        removeAttractor(trap)
        conRepo.del(trap)
    }

    /**
     * Moves a trap into a new state, swapping the loc and, on the way out of [TrapState.ARMED],
     * swapping the attractor npc for its disarmed twin so nothing else walks in.
     */
    fun Controller.enterState(state: Int, approach: Int = NORTH_APPROACH) {
        trapState = state
        trapStageTick = mapClock.cycle

        val next = stateLoc(this, state, approach)
        if (next != null) {
            swapLoc(this, next)
        }
        if (state != TrapState.ARMED) {
            disarmAttractor(this)
        }
    }

    private fun swapLoc(trap: Controller, next: LocType) {
        val current = locTypes[trap.trapLocId]?.let { locRepo.findExact(trap.coords, it) }
        val layer = current?.layer ?: 0
        val angle = current?.entity?.angle ?: 0
        val shape = current?.entity?.shape ?: 0
        if (current != null) {
            locRepo.del(current, duration = 0)
        }
        locRepo.add(
            LocInfo(layer, trap.coords, LocEntity(next.id, shape, angle)),
            TRAP_LIFETIME + LIFETIME_SLACK,
        )
        trap.trapLocId = next.id
    }

    private fun stateLoc(trap: Controller, state: Int, approach: Int): LocType? {
        val creature = npcTypes[trap.trapCreature]
        return when (state) {
            TrapState.ARMED -> null // the caller placed the armed loc already
            TrapState.CATCHING -> creature?.let { catchingLoc(it, approach) }
            TrapState.FULL -> creature?.param(HunterParams.loc_full)
            TrapState.FAILING -> failingLoc(trap.trapKind)
            TrapState.FAILED -> failedLoc(trap.trapKind)
            else -> null
        }
    }

    /**
     * Box traps show which side the creature came in from, so they carry four catching locs and
     * pick one by approach direction. Every other method has a single catching state.
     */
    private fun catchingLoc(creature: UnpackedNpcType, approach: Int): LocType {
        if (creature.param(HunterParams.trap_kind) != TrapKind.BOX) {
            return creature.param(HunterParams.loc_trapping)
        }
        val dirs = enumTypes[creature.param(HunterParams.loc_trapping_dirs)]
        return dirs[approach] ?: creature.param(HunterParams.loc_full)
    }

    private fun failingLoc(kind: Int): LocType =
        when (kind) {
            TrapKind.SNARE -> HunterLocs.snare_failing
            TrapKind.BOX -> HunterLocs.box_failing
            TrapKind.DEADFALL -> HunterLocs.deadfall_failing
            else -> HunterLocs.net_failing_swamp
        }

    private fun failedLoc(kind: Int): LocType =
        when (kind) {
            TrapKind.SNARE -> HunterLocs.snare_broken
            TrapKind.BOX -> HunterLocs.box_failed
            TrapKind.DEADFALL -> HunterLocs.deadfall_boulder
            else -> HunterLocs.net_failed_swamp
        }

    /** What the tile should look like once the trap is gone. Only scenery-based traps restore. */
    private fun restoreState(kind: Int, placed: LocType): LocType? =
        when (kind) {
            TrapKind.DEADFALL -> HunterLocs.deadfall_boulder
            TrapKind.NET -> HunterLocs.net_up_swamp.takeIf { placed.id != it.id }
            else -> null
        }

    private fun armedAttractor(kind: Int): NpcType =
        when (kind) {
            TrapKind.SNARE -> HunterNpcs.snare_trap_npc
            TrapKind.BOX -> HunterNpcs.box_trap_npc
            TrapKind.NET -> HunterNpcs.net_trap_npc
            else -> HunterNpcs.deadfall_trap_npc
        }

    private fun disarmedAttractor(kind: Int): NpcType =
        when (kind) {
            TrapKind.SNARE -> HunterNpcs.snare_trap_npc_off
            TrapKind.BOX -> HunterNpcs.box_trap_npc_off
            TrapKind.NET -> HunterNpcs.net_trap_npc_off
            else -> HunterNpcs.deadfall_trap_npc_off
        }

    private fun disarmAttractor(trap: Controller) {
        val current = resolveAttractor(trap) ?: return
        if (current.id == disarmedAttractor(trap.trapKind).id) {
            return
        }
        npcRepo.del(current, duration = 0)
        val replacement = Npc(npcTypes.getValue(disarmedAttractor(trap.trapKind).id), trap.coords)
        npcRepo.add(replacement, TRAP_LIFETIME + LIFETIME_SLACK)
        trap.trapNpcUid = replacement.uid.packed
    }

    private fun removeAttractor(trap: Controller) {
        val current = resolveAttractor(trap) ?: return
        npcRepo.del(current, duration = 0)
        trap.trapNpcUid = 0
    }

    private fun resolveAttractor(trap: Controller): Npc? =
        NpcUid(trap.trapNpcUid).takeIf { trap.trapNpcUid != 0 }?.resolve(npcList)

    /**
     * Which side of the trap the creature reached it from, snapped to the dominant axis.
     *
     * A diagonal approach has to become one of four cardinals because that is all the cache models
     * cover; picking the larger delta keeps the model matching what the player actually saw.
     */
    private fun approachFrom(trap: CoordGrid, creature: CoordGrid): Int {
        val dx = creature.x - trap.x
        val dz = creature.z - trap.z
        return if (kotlin.math.abs(dx) > kotlin.math.abs(dz)) {
            if (dx > 0) EAST_APPROACH else WEST_APPROACH
        } else {
            if (dz < 0) SOUTH_APPROACH else NORTH_APPROACH
        }
    }

    private fun entity(type: LocType): LocEntity = LocEntity(type.id, 10, 0)

    companion object {
        /** An untouched trap gives up after roughly twenty minutes. */
        const val TRAP_LIFETIME: Int = 2000

        /** A catch keeps for ten minutes before the creature escapes. */
        const val FULL_LIFETIME: Int = 1000

        /** A sprung-but-empty trap is worth less; it clears sooner. */
        const val FAILED_LIFETIME: Int = 500

        /** How long the ops-free catching and failing animations hold. */
        const val TRANSIENT_TICKS: Int = 2

        /** How far the owner may stray before the trap starts counting down to collapse. */
        const val ABANDON_DISTANCE: Int = 32

        /** Roughly a minute away, and the trap packs itself up. */
        const val ABANDON_TICKS: Int = 100

        /** Slack on every entity's own timeout so nothing outlives the controller that owns it. */
        const val LIFETIME_SLACK: Int = 100

        /** Radius, in zones, that a player's own traps are counted over. */
        const val TRAP_SEARCH_ZONES: Int = 3

        const val NORTH_APPROACH: Int = 0
        const val EAST_APPROACH: Int = 1
        const val SOUTH_APPROACH: Int = 2
        const val WEST_APPROACH: Int = 3

        var Controller.trapOwner: Int by intVarCon(HunterVarCons.trap_owner)
        var Controller.trapKind: Int by intVarCon(HunterVarCons.trap_kind)
        var Controller.trapState: Int by intVarCon(HunterVarCons.trap_state)
        var Controller.trapCreature: Int by intVarCon(HunterVarCons.trap_creature)
        var Controller.trapStageTick: Int by intVarCon(HunterVarCons.trap_stage_tick)
        var Controller.trapNpcUid: Int by intVarCon(HunterVarCons.trap_npc_uid)
        var Controller.trapLocId: Int by intVarCon(HunterVarCons.trap_loc)
        var Controller.trapAbandoned: Int by intVarCon(HunterVarCons.trap_abandoned)
    }
}
