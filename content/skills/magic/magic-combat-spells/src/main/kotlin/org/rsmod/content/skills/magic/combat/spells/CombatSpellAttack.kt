package org.rsmod.content.skills.magic.combat.spells

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.areas
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.spells.attack.SpellAttack
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.SpellImpact.Companion.queueSpellImpact
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.synth.SynthType
import org.rsmod.map.zone.ZoneKey

/** The animation, graphics and sounds of one spell. Anything `null` is simply not played. */
public data class SpellFx(
    val staffSeq: SeqType,
    val unarmedSeq: SeqType = staffSeq,
    val launch: SpotanimType? = null,
    val travel: SpotanimType? = null,
    val impact: SpotanimType? = null,
    val castSound: SynthType? = null,
    val hitSound: SynthType? = null,
)

/**
 * One combat spell that is not one of the twenty elementals, built from a few lambdas.
 *
 * The cast is `ElementalSpells.ElementalSpellAttack.cast` step for step: pay and animate, fire the
 * projectile, roll splash, roll damage, play the hit, queue the hit, carry on autocasting. Two
 * things are added. [maxHit] may return `null`, for curses and binds that deal no damage but still
 * need the accuracy roll, the hit fx and the retaliation a hit brings. And [onHit] runs on the tick
 * the hit lands, through the caster's `magic_spell_impact` queue, so a freeze or a drain arrives
 * with its hitsplat rather than a projectile flight before it.
 *
 * Spells without a travel graphic (the god spells, the blood and shadow bursts and barrages) have
 * no projectile to time against, so they land on the next tick. That is a chosen constant; check it
 * against a real client before trusting it.
 *
 * [multi] spells (bursts and barrages) also strike every attackable npc within one tile of the
 * primary target when that target stands in a multi-combat area, each with its own accuracy and
 * damage roll. Secondary *player* targets are not struck: this server has no PvP rules to say who
 * may be hit.
 */
public class CombatSpellAttack(
    private val manager: SpellAttackManager,
    private val casting: SpellCasting,
    private val objTypes: ObjTypeList,
    private val npcRepo: NpcRepository,
    private val fx: SpellFx,
    private val multi: Boolean = false,
    private val maxHit: (caster: ProtectedAccess, target: PathingEntity) -> Int?,
    private val canTarget: (caster: ProtectedAccess, target: PathingEntity) -> String? = { _, _ ->
        null
    },
    private val onHit: (caster: Player, target: PathingEntity, damage: Int) -> Unit = { _, _, _ -> },
) : SpellAttack {
    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Spell) {
        cast(target, attack)
    }

    override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Spell) {
        cast(target, attack)
    }

    private fun ProtectedAccess.cast(target: PathingEntity, attack: CombatAttack.Spell) {
        val refusal = canTarget(this, target)
        if (refusal != null) {
            mes(refusal)
            manager.stopCombat(this)
            return
        }
        val castResult = casting.attemptCombat(this, attack)
        if (castResult.isFailure()) {
            return
        }
        anim(objTypes.getOrNull(attack.weapon).castAnim())
        fx.launch?.let { spotanim(it, height = 92) }

        strike(target, attack, castResult, fx.castSound)
        if (multi && target is Npc && inArea(areas.multiway, target.coords)) {
            for (extra in extraTargets(target)) {
                strike(extra, attack, castResult, castSound = null)
            }
        }
        manager.continueCombatIfAutocast(this, target)
    }

    private fun ProtectedAccess.strike(
        target: PathingEntity,
        attack: CombatAttack.Spell,
        castResult: MagicRuneManager.CastResult,
        castSound: SynthType?,
    ) {
        val (serverDelay, clientDelay) =
            if (fx.travel != null) {
                manager.spawnProjectile(this, target, fx.travel, projanims.magic_spell).durations
            } else {
                ProjAnim.Duration(NO_PROJECTILE_HIT_DELAY, 0)
            }
        val spell = attack.spell.obj

        val splash = manager.rollSplash(this, target, attack, castResult)
        if (splash) {
            manager.playSplashFx(this, target, clientDelay, castSound, soundRadius = 8)
            manager.queueSplashHit(this, target, spell, clientDelay, serverDelay)
            return
        }

        val baseMaxHit = maxHit(this, target)
        val damage =
            if (baseMaxHit != null) {
                manager.rollMaxHit(this, target, attack, castResult, baseMaxHit)
            } else {
                0
            }
        manager.playHitFx(
            source = this,
            target = target,
            clientDelay = clientDelay,
            castSound = castSound,
            soundRadius = 8,
            hitSpot = fx.impact,
            hitSpotHeight = 124,
            hitSound = fx.hitSound,
        )
        if (baseMaxHit != null) {
            manager.giveCombatXp(this, target, attack, damage)
        }
        manager.queueMagicHit(this, target, spell, damage, clientDelay, serverDelay)
        val caster = player
        queueSpellImpact(target, serverDelay) { landed -> onHit(caster, landed, damage) }
    }

    private fun extraTargets(primary: Npc): List<Npc> =
        npcRepo
            .findAll(ZoneKey.from(primary.coords), zoneRadius = 1)
            .filter { it !== primary && it.isValidTarget() && it.isAttackable() }
            .filter { it.coords.level == primary.coords.level }
            .filter { it.coords.chebyshevDistance(primary.coords) <= 1 }
            .take(MAX_EXTRA_TARGETS)
            .toList()

    private fun Npc.isAttackable(): Boolean = visType.op.getOrNull(ATTACK_OP_INDEX) != null

    private fun UnpackedObjType?.castAnim(): SeqType =
        if (this != null && isCategoryType(categories.staff)) fx.staffSeq else fx.unarmedSeq

    private companion object {
        /** A burst or barrage hits a 3x3 square: the target plus up to eight neighbours. */
        const val MAX_EXTRA_TARGETS = 8
        const val NO_PROJECTILE_HIT_DELAY = 1
        /** `Attack` is the second right-click option on every attackable npc. */
        const val ATTACK_OP_INDEX = 1
    }
}
