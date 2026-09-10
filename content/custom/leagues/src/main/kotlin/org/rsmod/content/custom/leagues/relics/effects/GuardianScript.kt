package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.events.PlayerDeathEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseAttackLvl
import org.rsmod.api.player.stat.baseMagicLvl
import org.rsmod.api.player.stat.baseRangedLvl
import org.rsmod.api.player.stat.baseStrengthLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld2
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.configs.league_timers
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.hit.HitType
import org.rsmod.game.interact.InteractionNpcOp
import org.rsmod.game.interact.InteractionNpcT
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Guardian relic's horn: op1 Summon calls a Guardian that follows you for [LIFETIME_TICKS] (30
 * minutes); op2 Dismiss sends it away.
 *
 * Every [ATTACK_TICKS] the Guardian strikes whatever npc you are attacking, for up to `10 + your
 * highest combat stat / 3`, and it never misses. The hit is queued with **you** as its source, so
 * kill credit, drops and slayer all work exactly as if you had landed it - and nothing ever has to
 * fight the Guardian back, since it has no ops.
 *
 * There is no npc-follower system, so the Guardian is an ordinary npc in `PlayerFollow` mode, which
 * also teleports it along when you get more than 15 tiles away. It leaves on dismissal, death,
 * logout, a second summon, or when its time runs out.
 */
class GuardianScript
@Inject
constructor(
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
    private val hitModifier: NpcHitModifier,
    private val random: GameRandom,
    private val clock: MapClock,
) : PluginScript() {
    private val guardians = HashMap<Player, Summoned>()

    override fun ScriptContext.startup() {
        onOpHeld1(league_objs.guardian_horn) { summon() }
        onOpHeld2(league_objs.guardian_horn) {
            dismiss(player, "Your Guardian returns to the horn.")
        }
        onPlayerSoftTimer(league_timers.guardian) { tick(player) }
        onEvent<PlayerDeathEvents.Death> { dismiss(player, "Your Guardian fades away.") }
        onEvent<SessionStateEvent.Delete> { dismiss(player, message = null) }
    }

    private fun ProtectedAccess.summon() {
        if (!player.hasRelic(Relic.Guardian)) {
            mes("The horn makes no sound for you.")
            return
        }
        dismiss(player, message = null)
        val tile = mapFindSquareLineOfWalk(player.coords, 1, 2) ?: player.coords
        val npc = Npc(npcTypes[guardian_npcs.guardian], tile)
        npcRepo.add(npc, duration = LIFETIME_TICKS)
        npc.facePlayer(player)
        npc.mode = NpcMode.PlayerFollow
        guardians[player] = Summoned(npc, expiresAt = clock.cycle + LIFETIME_TICKS)
        player.softTimer(league_timers.guardian, ATTACK_TICKS)
        spotanim(guardian_spots.summon)
        mes("You sound the horn, and your Guardian answers. It will stay for 30 minutes.")
    }

    private fun tick(player: Player) {
        val summoned = guardians[player]
        if (summoned == null) {
            player.clearSoftTimer(league_timers.guardian)
            return
        }
        if (clock.cycle >= summoned.expiresAt) {
            // The repository's lifecycle removes the npc itself at this cycle.
            guardians.remove(player)
            player.clearSoftTimer(league_timers.guardian)
            player.mes("Your Guardian's time is up, and it fades away.")
            return
        }
        if (!player.hasRelic(Relic.Guardian)) {
            dismiss(player, "Without the Guardian relic, your Guardian leaves you.")
            return
        }
        val target = combatTarget(player) ?: return
        if (!target.isValidTarget() || !target.coords.isWithin(summoned.npc, ATTACK_RANGE)) {
            return
        }
        summoned.npc.spotanim(guardian_spots.attack)
        target.spotanim(guardian_spots.impact, delay = HIT_DELAY * 30)
        target.queueHit(
            source = player,
            delay = HIT_DELAY,
            type = HitType.Magic,
            damage = random.of(0, maxHit(player)),
            modifier = hitModifier,
        )
    }

    private fun dismiss(player: Player, message: String?) {
        val summoned = guardians.remove(player) ?: return
        player.clearSoftTimer(league_timers.guardian)
        if (clock.cycle < summoned.expiresAt) {
            npcRepo.del(summoned.npc, Int.MAX_VALUE)
        }
        if (message != null) {
            player.mes(message)
        }
    }

    /**
     * The npc [player] is fighting. Player combat keeps itself going by re-issuing `opnpc2` (or
     * `opnpct` for a spell) after every swing, so the live interaction names the target. Talking,
     * pickpocketing or using an item on an npc are interactions too, and are not fights.
     */
    private fun combatTarget(player: Player): Npc? =
        when (val interaction = player.interaction) {
            is InteractionNpcOp -> interaction.target.takeIf { interaction.op == InteractionOp.Op2 }
            is InteractionNpcT -> interaction.target.takeIf { interaction.objType == null }
            else -> null
        }

    private fun maxHit(player: Player): Int {
        val best =
            max(
                max(player.baseAttackLvl, player.baseStrengthLvl),
                max(player.baseRangedLvl, player.baseMagicLvl),
            )
        return BASE_MAX_HIT + best / 3
    }

    private fun CoordGrid.isWithin(npc: Npc, range: Int): Boolean =
        level == npc.coords.level &&
            abs(x - npc.coords.x) <= range &&
            abs(z - npc.coords.z) <= range

    private data class Summoned(val npc: Npc, val expiresAt: Int)

    private companion object {
        /** 30 minutes of 600ms ticks. */
        const val LIFETIME_TICKS = 3000
        const val ATTACK_TICKS = 4
        const val ATTACK_RANGE = 10
        const val HIT_DELAY = 2
        const val BASE_MAX_HIT = 10
    }
}

typealias guardian_npcs = GuardianNpcs

typealias guardian_spots = GuardianSpots

object GuardianNpcs : NpcReferences() {
    val guardian = find("league_guardian")
}

object GuardianSpots : SpotanimReferences() {
    val summon = find("league5_summon_guardian_player_01_spotanim_01")
    val attack = find("league05_guardian_attack_magic_spotanim_01")
    val impact = find("league05_guardian_attack_magic_impactanim_01")
}
