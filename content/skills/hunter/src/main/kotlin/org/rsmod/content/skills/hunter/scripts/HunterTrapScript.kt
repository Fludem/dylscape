package org.rsmod.content.skills.hunter.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.npc.events.interact.AiNpcContentEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hunterLvl
import org.rsmod.api.repo.controller.ControllerRepository
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onAiConTimer
import org.rsmod.api.script.onNpcAccessEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.hunter.HunterTraps
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapCreature
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapKind
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapOwner
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapState
import org.rsmod.content.skills.hunter.TrapKind
import org.rsmod.content.skills.hunter.TrapState
import org.rsmod.content.skills.hunter.configs.HunterContent
import org.rsmod.content.skills.hunter.configs.HunterControllers
import org.rsmod.content.skills.hunter.configs.HunterLocs
import org.rsmod.content.skills.hunter.configs.HunterObjs
import org.rsmod.content.skills.hunter.configs.HunterParams
import org.rsmod.content.skills.hunter.configs.HunterSeqs
import org.rsmod.game.entity.Controller
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Every player-facing and AI-facing binding for trapping. The lifecycle itself lives in
 * [HunterTraps]; this file only decides when to enter it.
 *
 * The ops are the cache's own, decoded rather than assumed: both trap items already carry **"Lay"
 * on iop1**, an armed trap carries **"Dismantle"** on op1, a sprung one **"Check"**, and young
 * trees and deadfall boulders carry **"Set-trap"**. Nothing here needed an op edit.
 *
 * The catch is driven by the vanilla mechanism rather than a poll. A laid trap spawns an invisible
 * attractor npc; the creature's hunt mode targets it, the hunt processor routes the creature onto
 * the trap tile, and arriving publishes [AiNpcContentEvents.Op1] keyed on the attractor's content
 * group — with the *creature* as the access receiver and the attractor as `target`. That is what
 * makes a player able to stand and watch a chinchompa walk into their box.
 */
class HunterTrapScript
@Inject
constructor(
    private val traps: HunterTraps,
    private val locRepo: LocRepository,
    private val conRepo: ControllerRepository,
    private val locTypes: LocTypeList,
    private val npcTypes: NpcTypeList,
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Laying: bird snares and box traps go down on the player's own tile.
        onOpHeld1(HunterContent.hunter_trap_obj) { layHeldTrap(it.type, it.slot) }

        // Net traps are built on a young tree: rope first, then the net.
        onOpLocU(HunterContent.hunter_young_tree, HunterObjs.rope) { tieRope(it.loc, it.type) }
        onOpLocU(HunterContent.hunter_roped_tree, HunterObjs.small_fishing_net) {
            hangNet(it.loc, it.type)
        }

        // Deadfalls are built by wedging logs under a boulder.
        onOpLocU(HunterContent.hunter_deadfall_boulder, HunterObjs.logs) {
            setDeadfall(it.loc, it.type)
        }

        // Checking and dismantling. Grouped by what the player can do, so four methods and sixteen
        // creatures need two bindings rather than one per loc state.
        onOpLoc1(HunterContent.hunter_trap_armed) { dismantle(it.loc) }
        onOpLoc1(HunterContent.hunter_trap_sprung) { checkTrap(it.loc) }

        // The per-trap tick, and the creature walking in.
        onAiConTimer(HunterControllers.hunter_trap) { traps.tick(controller) }
        onNpcAccessEvent<AiNpcContentEvents.Op1>(HunterContent.hunter_trap_npc.id) {
            val trap = traps.findTrap(it.target.coords)
            if (trap != null) {
                traps.attemptCatch(trap, npc)
            }
        }
    }

    private suspend fun ProtectedAccess.layHeldTrap(trap: UnpackedObjType, slot: Int) {
        val kind = trap.param(HunterParams.trap_kind)
        val armed = if (kind == TrapKind.SNARE) HunterLocs.snare_armed else HunterLocs.box_armed

        val max = traps.maxTraps(player.hunterLvl)
        if (traps.countActiveTraps(player) >= max) {
            mes("You can only lay $max trap${if (max == 1) "" else "s"} at a time.")
            return
        }

        // Reserve the tile before consuming anything, so a blocked tile never eats the trap.
        val tile = player.coords
        val loc = traps.tryPlace(player, tile, kind, armed)
        if (loc == null) {
            mes("You can't set a trap here.")
            return
        }

        anim(HunterSeqs.setting_trap)
        delay(SET_TICKS)

        val deleted = invDel(inv, trap, count = 1, slot = slot)
        if (!deleted.success) {
            traps.cancelPlace(loc)
            resetAnim()
            return
        }

        resetAnim()
        traps.register(player, tile, kind, loc)
        // Step off the trap, the way OSRS backs you west off a freshly laid one.
        walk(tile.translateX(-1))
    }

    private suspend fun ProtectedAccess.tieRope(tree: BoundLocInfo, type: UnpackedLocType) {
        if (!canLayAnotherTrap()) {
            return
        }
        if (invTotal(inv, HunterObjs.small_fishing_net) < 1) {
            mes("You need a small fishing net as well as a rope to set a net trap.")
            return
        }

        val roped = HunterLocs.ropedTrees[HunterLocs.youngTrees.indexOfFirst { it.id == type.id }]
        anim(HunterSeqs.setting_sapling_trap)
        delay(SET_TICKS)

        val deleted = invDel(inv, HunterObjs.rope, count = 1)
        if (!deleted.success) {
            resetAnim()
            return
        }
        resetAnim()
        locRepo.change(tree, roped, HunterTraps.TRAP_LIFETIME + HunterTraps.LIFETIME_SLACK)
        mes("You tie the rope to the tree.")
    }

    private suspend fun ProtectedAccess.hangNet(tree: BoundLocInfo, type: UnpackedLocType) {
        val index = HunterLocs.ropedTrees.indexOfFirst { it.id == type.id }
        val armed = HunterLocs.netArmed[index]

        anim(HunterSeqs.setting_sapling_trap)
        delay(SET_TICKS)

        val deleted = invDel(inv, HunterObjs.small_fishing_net, count = 1)
        if (!deleted.success) {
            resetAnim()
            return
        }
        resetAnim()
        locRepo.change(tree, armed, HunterTraps.TRAP_LIFETIME + HunterTraps.LIFETIME_SLACK)

        val placed = locRepo.findExact(tree.coords, armed)
        if (placed != null) {
            traps.register(player, tree.coords, TrapKind.NET, placed)
        }
        mes("You set up the net trap.")
    }

    private suspend fun ProtectedAccess.setDeadfall(boulder: BoundLocInfo, type: UnpackedLocType) {
        if (!canLayAnotherTrap()) {
            return
        }
        if (invTotal(inv, HunterObjs.knife) < 1) {
            mes("You need a knife to carve the logs into a deadfall support.")
            return
        }

        anim(HunterSeqs.setting_trap)
        locRepo.change(boulder, HunterLocs.deadfall_setting, SET_TICKS)
        delay(SET_TICKS)

        val deleted = invDel(inv, HunterObjs.logs, count = 1)
        if (!deleted.success) {
            resetAnim()
            return
        }
        resetAnim()

        val setting = locRepo.findExact(boulder.coords, HunterLocs.deadfall_setting)
        if (setting != null) {
            locRepo.del(setting, duration = 0)
        }
        val armed =
            LocInfo(
                boulder.layer,
                boulder.coords,
                LocEntity(HunterLocs.deadfall_armed.id, boulder.entity.shape, boulder.entity.angle),
            )
        locRepo.add(armed, HunterTraps.TRAP_LIFETIME + HunterTraps.LIFETIME_SLACK)
        traps.register(player, boulder.coords, TrapKind.DEADFALL, armed)
        mes("You wedge the logs under the boulder.")
    }

    /**
     * Takes an untouched or failed trap back down.
     *
     * Ownership is checked before anything else: someone else's trap is not yours to take apart,
     * and OSRS says so rather than silently doing nothing.
     */
    private suspend fun ProtectedAccess.dismantle(loc: BoundLocInfo) {
        val trap = traps.findTrap(loc.coords) ?: return
        if (!ownsTrap(trap)) {
            return
        }
        anim(HunterSeqs.dismantle_net)
        delay(SET_TICKS)
        resetAnim()
        returnTrapKit(trap)
        traps.collapse(trap)
    }

    /**
     * Collects a catch: the product, the trap back, and the XP.
     *
     * This is the only place a player gains anything from trapping. The catch roll itself happens
     * out in the world with no player attached, so everything owed is settled here instead.
     */
    private suspend fun ProtectedAccess.checkTrap(loc: BoundLocInfo) {
        val trap = traps.findTrap(loc.coords) ?: return
        if (!ownsTrap(trap)) {
            return
        }

        if (trap.trapState != TrapState.FULL) {
            // A failed trap has nothing in it; taking it back down is the only thing to do.
            dismantle(loc)
            return
        }

        if (inv.isFull()) {
            mes("You don't have enough space in your inventory.")
            soundSynth(synths.pillory_wrong)
            return
        }

        val creature = npcTypes[trap.trapCreature]
        if (creature == null) {
            traps.collapse(trap)
            return
        }

        anim(HunterSeqs.setting_trap)
        delay(SET_TICKS)
        resetAnim()

        val product = creature.paramOrNull(params.skill_productitem)
        if (product != null) {
            invAdd(inv, product)
            spam("You catch a ${objTypes[product].name.lowercase()}.")
        } else {
            spam("You catch a ${creature.name.lowercase()}, and it wriggles free.")
        }

        val secondary = creature.paramOrNull(HunterParams.secondary_product)
        val secondaryCount = creature.param(HunterParams.secondary_count)
        if (secondary != null && secondaryCount > 0) {
            invAdd(inv, secondary, secondaryCount)
        }

        val xp = creature.param(params.skill_xp) / XP_FINE_SCALE
        statAdvance(stats.hunter, xp * xpMods.get(player, stats.hunter))

        returnTrapKit(trap)
        traps.collapse(trap)
    }

    private fun ProtectedAccess.returnTrapKit(trap: Controller) {
        val obj =
            when (trap.trapKind) {
                TrapKind.SNARE -> HunterObjs.bird_snare
                TrapKind.BOX -> HunterObjs.box_trap
                TrapKind.NET -> HunterObjs.rope
                else -> HunterObjs.logs
            }
        invAdd(inv, obj)
        if (trap.trapKind == TrapKind.NET) {
            invAdd(inv, HunterObjs.small_fishing_net)
        }
    }

    private fun ProtectedAccess.ownsTrap(trap: Controller): Boolean {
        if (trap.trapOwner == player.uid.packed) {
            return true
        }
        mes("This isn't your trap.")
        return false
    }

    private fun ProtectedAccess.canLayAnotherTrap(): Boolean {
        val max = traps.maxTraps(player.hunterLvl)
        if (traps.countActiveTraps(player) < max) {
            return true
        }
        mes("You can only lay $max trap${if (max == 1) "" else "s"} at a time.")
        return false
    }

    private fun entity(type: LocType): LocEntity =
        LocEntity(type.id, LocShape.CentrepieceStraight.id, 0)

    companion object {
        /** How long every set-up and take-down animation runs for. */
        const val SET_TICKS: Int = 3

        /** `params.skill_xp` is stored in tenths; `statAdvance` wants whole xp. */
        const val XP_FINE_SCALE: Double = 10.0
    }
}
