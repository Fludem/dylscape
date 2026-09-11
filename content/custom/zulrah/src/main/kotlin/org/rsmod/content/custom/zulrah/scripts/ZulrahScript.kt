package org.rsmod.content.custom.zulrah.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiApPlayer2
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.api.script.onNpcTimer
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onPlayerLogout
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.api.toxins.Toxins
import org.rsmod.content.custom.zulrah.ZulrahFight
import org.rsmod.content.custom.zulrah.configs.ZulrahShrine
import org.rsmod.content.custom.zulrah.configs.ZulrahVarps
import org.rsmod.content.custom.zulrah.configs.zulrah_locs
import org.rsmod.content.custom.zulrah.configs.zulrah_npcs
import org.rsmod.content.custom.zulrah.configs.zulrah_objs
import org.rsmod.content.custom.zulrah.configs.zulrah_projanims
import org.rsmod.content.custom.zulrah.configs.zulrah_seqs
import org.rsmod.content.custom.zulrah.configs.zulrah_spots
import org.rsmod.content.custom.zulrah.configs.zulrah_timers
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Everything that binds: the boat in, the fight timer, the snakelings' attacks, and every way out.
 *
 * The fight's rules live in [ZulrahFight]; this is only wiring, plus the snakelings, whose attacks
 * are small enough not to need a class of their own.
 *
 * There is deliberately no `queues.death` handler for Zulrah. `DropTableScript` already claims that
 * queue for all three forms, and a second binding for the same type is a boot failure (or, since it
 * catches that, a silent loss of the drop table depending on scan order). Kill-reactive work hangs
 * off [NpcDeathEvents.Killed] instead.
 */
class ZulrahScript
@Inject
constructor(
    private val fight: ZulrahFight,
    private val accuracy: AccuracyFormulae,
    private val worldRepo: WorldRepository,
    private val objTypes: ObjTypeList,
    private val toxins: Toxins,
) : PluginScript() {
    /**
     * Players in the minute between a kill and the trip home. Timers are not saved, so this is what
     * tells the logout hook they are still standing in a shrine that will not survive them.
     */
    private val returning = HashSet<Player>()

    override fun ScriptContext.startup() {
        // The placed boat is a multiloc on `snakeboss_info`; every state of it resolves to the
        // one-op boat, and its out-of-range default is the two-op one. Both board.
        onOpLoc1(zulrah_locs.boat_board) { board() }
        onOpLoc1(zulrah_locs.boat_board_quick) { board() }

        for (form in zulrah_npcs.forms.values) {
            onNpcTimer(form, zulrah_timers.fight) { fight.tick(this) }
            // Zulrah never answers an attack on its own: without this, `NpcRetaliateScript` would
            // set it on the player and `NvPCombatScript` would have it try to punch from the water
            // alongside the rotation. A type binding wins over the default one.
            onNpcQueue(form, queues.com_retaliate_player) {}
        }

        onAiOpPlayer2(zulrah_npcs.snakeling_melee) { snakelingAttack(it.target, magic = false) }
        onAiApPlayer2(zulrah_npcs.snakeling_melee) { snakelingAttack(it.target, magic = false) }
        onAiOpPlayer2(zulrah_npcs.snakeling_magic) { snakelingAttack(it.target, magic = true) }
        onAiApPlayer2(zulrah_npcs.snakeling_magic) { snakelingAttack(it.target, magic = true) }

        onEvent<NpcDeathEvents.Killed> { zulrahKilled(npc, killer) }
        onPlayerTimer(zulrah_timers.return_home) { returnHome() }
        onEvent<PlayerDeathEvents.Death> { fight.fightOf(player)?.let(fight::end) }
        onPlayerLogout { evict(player) }
        onEvent<SessionStateEvent.Delete> { returning.remove(player) }

        onOpHeld1(zulrah_objs.zul_andra_teleport) { readScroll() }
    }

    private fun ProtectedAccess.board() {
        if (fight.fightOf(player) != null) {
            return
        }
        mes("You board the sacrificial boat and row out to Zulrah's shrine.")
        if (fight.enter(this) == null) {
            mes("The shrine is too crowded right now. Try again in a moment.")
        }
    }

    private fun zulrahKilled(npc: Npc, killer: Player?) {
        val state = fight.fightOf(npc) ?: return
        fight.killed(npc)
        val owner = killer ?: state.owner
        val kills = owner.vars[ZulrahVarps.kills] + 1
        VarPlayerIntMapSetter.set(owner, ZulrahVarps.kills, kills)
        owner.mes("Your Zulrah kill count is: $kills.")
        owner.mes("The shrine will return you to Zul-Andra in a minute.")
        owner.timer(zulrah_timers.return_home, RETURN_TICKS)
        returning += owner
    }

    private fun ProtectedAccess.returnHome() {
        player.clearTimer(zulrah_timers.return_home)
        if (!returning.remove(player) || fight.fightOf(player) != null) {
            // Back in another fight already; a stale timer must not pull them out of it.
            return
        }
        telejump(ZulrahShrine.zulAndra)
    }

    /**
     * A player who logs out inside the shrine would come back to a region that no longer exists, so
     * their coordinates are moved to Zul-Andra before the save runs - the construction module's
     * `evictFromHouse` does the same. The post-kill grace period is covered too: it is spent inside
     * the shrine with no fight left to find.
     */
    private fun evict(player: Player) {
        val state = fight.fightOf(player)
        if (state != null) {
            fight.end(state)
            player.coords = ZulrahShrine.zulAndra
            return
        }
        if (returning.remove(player)) {
            player.clearTimer(zulrah_timers.return_home)
            player.coords = ZulrahShrine.zulAndra
        }
    }

    private fun ProtectedAccess.readScroll() {
        val removed = invDel(inv, zulrah_objs.zul_andra_teleport, count = 1)
        if (!removed.success) {
            return
        }
        fight.fightOf(player)?.let(fight::end)
        player.clearTimer(zulrah_timers.return_home)
        returning.remove(player)
        telejump(ZulrahShrine.zulAndra)
        mes("You read the scroll and are whisked away to Zul-Andra.")
    }

    /**
     * A snakeling's bite or spell. They are accurate and hit up to the wiki's 15, and either can
     * envenom. Rolled here rather than through `NvPCombat`, whose magic driver would read no max
     * hit for the magic snakeling.
     */
    private fun StandardNpcAccess.snakelingAttack(target: Player, magic: Boolean) {
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
        anim(zulrah_seqs.snakeling_attack)

        val delay: Int
        val landed: Boolean
        if (magic) {
            val projectile =
                worldRepo.projAnim(npc, target, zulrah_spots.snakeling_magic, zulrah_projanims.spit)
            delay = projectile.durations.serverDelay.coerceAtLeast(1)
            landed = accuracy.rollMagicAccuracy(npc, target, random)
        } else {
            delay = 1
            landed = accuracy.rollMeleeAccuracy(npc, target, null, random)
        }
        val damage = if (landed) random.of(SNAKELING_MAX_HIT + 1) else 0
        target.queueCombatRetaliate(npc, delay)
        val hit = target.queueHit(npc, delay, if (magic) HitType.Magic else HitType.Melee, damage)
        target.combatPlayDefendAnim(objTypes)
        if (hit.damage > 0 && random.of(ZulrahFight.VENOM_ONE_IN) == 0) {
            toxins.envenom(target)
        }
    }

    private companion object {
        /** A minute to pick up the loot. */
        const val RETURN_TICKS = 100

        const val SNAKELING_MAX_HIT = 15
    }
}
