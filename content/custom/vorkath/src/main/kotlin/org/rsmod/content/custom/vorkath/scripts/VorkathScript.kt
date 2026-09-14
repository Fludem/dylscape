package org.rsmod.content.custom.vorkath.scripts

import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.config.refs.mesanims
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.custom.vorkath.configs.VorkathArena
import org.rsmod.content.custom.vorkath.configs.vorkath_locs
import org.rsmod.content.custom.vorkath.configs.vorkath_npcs
import org.rsmod.content.custom.vorkath.configs.vorkath_projanims
import org.rsmod.content.custom.vorkath.configs.vorkath_seqs
import org.rsmod.content.custom.vorkath.configs.vorkath_spots
import org.rsmod.content.custom.vorkath.configs.vorkath_timers
import org.rsmod.content.custom.vorkath.configs.vorkath_varps
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * A basic Vorkath: poke it, fight it, loot it.
 *
 * This is the minimal tier on purpose. The dragon asleep on its platform is the placed npc. A poke
 * takes it off the map and puts a real `vorkath` npc in its place, which shoots and casts at
 * whoever woke it through the ordinary hunt/ap path, and dies through the ordinary death sequence -
 * so the generated drop table in `content/custom/drop-tables` claims the loot. A kill brings the
 * sleeper back after a pause, and an awake Vorkath left alone brings it back at once.
 *
 * Two npcs rather than one `changeType`, because every npc event - the AI attack, the timer, and
 * above all the death queue the drop table binds - is keyed on the npc's *base* type id. A
 * transmogged sleeper would die as `vorkath_sleeping`, which the drop-table loader deliberately
 * skips (it has no `Attack` op), and the loot would be lost.
 *
 * The one thing rolled here rather than through `NvPCombat` is the attack itself, because Vorkath
 * alternates two styles with two projectiles and the shared driver fires the single one an npc
 * declares. The shape is `ZulrahScript.snakelingAttack`, and it keeps the same two invariants: do
 * nothing until `actionDelay` has passed, and queue the retaliation *before* the hit.
 *
 * There is deliberately no `queues.death` handler. `DropTableScript` claims that queue for
 * `vorkath`, and a second binding is a boot failure. Kill-reactive work hangs off
 * [NpcDeathEvents.Killed] instead.
 *
 * Not built, and listed so nobody reads it as an oversight: the instance, the acid pool and the
 * walk, the zombified spawn, the fireball, dragonfire itself (the server has no antifire mechanic
 * at all), melee when adjacent, the damage cap, the Dragon Slayer II gate, and Torfinn's collection
 * service. The north lip of the crater has no climb-over either: in this cache the placed multiloc
 * only ever resolves to its op-less child, so the way out is the way in.
 */
class VorkathScript
@Inject
constructor(
    private val aiInteractions: AiPlayerInteractions,
    private val npcTypes: NpcTypeList,
    private val npcRepo: NpcRepository,
    private val worldRepo: WorldRepository,
    private val objTypes: ObjTypeList,
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
) : PluginScript() {
    /** Each awake dragon and the sleeper it replaced, so the sleeper can be put back. */
    private val sleepers = IdentityHashMap<Npc, Npc>()

    override fun ScriptContext.startup() {
        onOpNpc1(vorkath_npcs.sleeping) { poke(it.npc) }

        onAiApPlayer2(vorkath_npcs.awake) { attack(it.target) }
        onAiOpPlayer2(vorkath_npcs.awake) { attack(it.target) }
        onNpcTimer(vorkath_npcs.awake, vorkath_timers.sleep) { sleepIfIdle() }
        onEvent<NpcDeathEvents.Killed> { vorkathKilled(npc, killer) }

        onOpNpc1(vorkath_npcs.torfinn_rellekka) { offerCrossing(it.npc, toUngael = true) }
        onOpNpc3(vorkath_npcs.torfinn_rellekka) { sail(toUngael = true) }
        onOpNpc1(vorkath_npcs.torfinn_ungael) { offerCrossing(it.npc, toUngael = false) }
        onOpNpc3(vorkath_npcs.torfinn_ungael) { sail(toUngael = false) }
        onOpLoc1(vorkath_locs.ungael_boat) { sail(toUngael = false) }

        onOpLoc1(vorkath_locs.crater_entrance) {
            climbOver(
                it.loc.coords,
                it.type,
                south = VorkathArena.craterOutsideSouth,
                north = VorkathArena.craterInsideSouth,
            )
        }
    }

    /**
     * Wakes the dragon and points it at whoever poked it.
     *
     * The sleeper is deleted for good (it is put back by hand) and the fighting form is added with
     * a finite duration, so `respawns` stays false and its death deletes it rather than respawning
     * a second awake dragon. The first attack is held back long enough for the wake-up animation.
     */
    private fun ProtectedAccess.poke(npc: Npc) {
        if (!npc.isSlotAssigned || npc.id != vorkath_npcs.sleeping.id) {
            mes("Vorkath is already awake.")
            return
        }
        mes("You poke the sleeping dragon... and it stirs.")

        val awake = Npc(npcTypes[vorkath_npcs.awake], npc.coords)
        npcRepo.del(npc, Int.MAX_VALUE)
        npcRepo.add(awake, AWAKE_LIFETIME)
        sleepers[awake] = npc

        awake.anim(vorkath_seqs.wake)
        awake.actionDelay = mapClock + WAKE_TICKS
        awake.apPlayer2(player, aiInteractions)
        awake.timer(vorkath_timers.sleep, SLEEP_CHECK_TICKS)
    }

    /**
     * One attack: a coin flip between the ranged spit and the magic blast, each with its own
     * projectile and impact, landing when the projectile does. Ranged max hit is derived from the
     * cache's ranged level (32, which is live); magic is authored at the live 30, because the
     * formula's read of a level-150 caster gives 16.
     */
    private fun StandardNpcAccess.attack(target: Player) {
        if (!target.isValidTarget()) {
            resetMode()
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        if (!npc.isInCombat()) {
            resetMode()
            return
        }
        actionDelay = mapClock + npc.attackRate()
        anim(npc.visType.param(params.attack_anim))

        val magic = random.of(2) == 0
        val travel = if (magic) vorkath_spots.magic_travel else vorkath_spots.ranged_travel
        val impact = if (magic) vorkath_spots.magic_impact else vorkath_spots.ranged_impact
        val projectile = worldRepo.projAnim(npc, target, travel, vorkath_projanims.spit)
        val (serverDelay, clientDelay) = projectile.durations

        val landed: Boolean
        val maxHit: Int
        if (magic) {
            landed = accuracy.rollMagicAccuracy(npc, target, random)
            maxHit = MAGIC_MAX_HIT
        } else {
            landed = accuracy.rollRangedAccuracy(npc, target, random)
            maxHit = maxHits.getRangedMaxHit(npc, target)
        }
        val damage = if (landed) random.of(0..maxHit) else 0

        // Before the hit, never after: queued after it, every hit trips the speed-up death.
        target.queueCombatRetaliate(npc, serverDelay)
        target.queueHit(
            npc,
            serverDelay.coerceAtLeast(1),
            if (magic) HitType.Magic else HitType.Ranged,
            damage,
        )
        target.spotanim(impact, delay = clientDelay, height = IMPACT_HEIGHT, slot = 0)
        target.combatPlayDefendAnim(objTypes, clientDelay)
    }

    /**
     * Runs on the awake form every [SLEEP_CHECK_TICKS]. Still fighting: check again later. Nobody
     * about: take the fighting form off the map and put the sleeper straight back.
     */
    private fun StandardNpcAccess.sleepIfIdle() {
        if (npc.hitpoints <= 0) {
            // The death sequence owns this npc now; `vorkathKilled` restores the sleeper.
            return
        }
        if (npc.isInCombat()) {
            timer(vorkath_timers.sleep, SLEEP_CHECK_TICKS)
            return
        }
        val sleeper = sleepers.remove(npc) ?: return
        npcRepo.del(npc, Int.MAX_VALUE)
        npcRepo.add(sleeper, Int.MAX_VALUE)
    }

    /**
     * The death sequence deletes the fighting form on its own (it was added with a finite
     * duration); the sleeper comes back after [RESPAWN_TICKS], which is the pause a respawning boss
     * would have had.
     */
    private fun vorkathKilled(npc: Npc, killer: Player?) {
        val sleeper = sleepers.remove(npc) ?: return
        npcRepo.addDelayed(sleeper, RESPAWN_TICKS, Int.MAX_VALUE)
        if (killer == null) {
            return
        }
        val kills = killer.vars[vorkath_varps.kills] + 1
        VarPlayerIntMapSetter.set(killer, vorkath_varps.kills, kills)
        killer.mes("Your Vorkath kill count is: $kills.")
    }

    private suspend fun ProtectedAccess.offerCrossing(npc: Npc, toUngael: Boolean) {
        val destination = if (toUngael) "Ungael" else "Rellekka"
        chatNpc(npc, mesanims.quiz, "Looking to sail to $destination?")
        val sail = choice2("Yes, take me to $destination.", true, "Not right now.", false)
        if (sail) {
            sail(toUngael)
        }
    }

    private suspend fun ProtectedAccess.sail(toUngael: Boolean) {
        val dest = if (toUngael) VorkathArena.ungaelArrival else VorkathArena.rellekkaArrival
        val name = if (toUngael) "Ungael" else "Rellekka"
        mes("You board the boat and sail to $name.")
        delay(1)
        telejump(dest)
    }

    /** Over the lip of the crater, landing on whichever side the player did not start on. */
    private suspend fun ProtectedAccess.climbOver(
        loc: CoordGrid,
        type: UnpackedLocType,
        south: CoordGrid,
        north: CoordGrid,
    ) {
        val dest = if (player.coords.z < loc.z) north else south
        arriveDelay()
        // `climb_anim` carries a default of `human_reachforladder`, so this is safe on a loc that
        // does not declare one of its own.
        anim(type.param(params.climb_anim))
        delay(1)
        telejump(dest)
    }

    private companion object {
        /** Live: 30. */
        const val MAGIC_MAX_HIT = 30

        /** Holds the first attack until the wake-up animation has played. */
        const val WAKE_TICKS = 4

        /** How often an awake Vorkath asks whether anyone is still fighting it. */
        const val SLEEP_CHECK_TICKS = 50

        /** 30 seconds between a kill and the sleeper's return. */
        const val RESPAWN_TICKS = 50

        /**
         * A week. Finite so the fighting form never respawns on its own, and far longer than the
         * idle check leaves it standing; if it ever elapsed the platform would stay empty until the
         * next boot.
         */
        const val AWAKE_LIFETIME = 1_000_000

        const val IMPACT_HEIGHT = 124
    }
}
