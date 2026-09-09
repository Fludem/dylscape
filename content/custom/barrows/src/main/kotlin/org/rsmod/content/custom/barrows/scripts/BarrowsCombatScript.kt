package org.rsmod.content.custom.barrows.scripts

import jakarta.inject.Inject
import kotlin.math.max
import kotlin.math.min
import org.rsmod.api.combat.commons.npc.attackRate
import org.rsmod.api.combat.commons.player.combatPlayDefendAnim
import org.rsmod.api.combat.commons.player.queueCombatRetaliate
import org.rsmod.api.combat.formulas.AccuracyFormulae
import org.rsmod.api.combat.formulas.MaxHitFormulae
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.isInCombat
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.stat.strengthLvl
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onAiOpPlayer2
import org.rsmod.content.custom.barrows.BarrowsStyle
import org.rsmod.content.custom.barrows.Brother
import org.rsmod.content.custom.barrows.configs.BarrowsProjAnims
import org.rsmod.content.custom.barrows.configs.barrows_npcs
import org.rsmod.content.custom.barrows.configs.barrows_seqs
import org.rsmod.content.custom.barrows.configs.barrows_spots
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The brothers' attacks, and the six mechanics that make each of them different.
 *
 * ### Why all six are here and not just the two the engine cannot do
 *
 * `NvPCombatScript` binds `onDefaultAiOpPlayer2` and unconditionally builds a
 * `CombatAttack.NpcMelee`, so out of the box *every* npc in the game attacks with melee.
 * `CombatAttack.NpcRanged` and `NpcMagic` exist as types but nothing in the repo constructs them.
 * That alone would force Ahrim and Karil to have their own handler.
 *
 * The four melee brothers could have used the engine's path - but their specials all key off the
 * damage of a swing, and the engine offers no hook between rolling that damage and applying it.
 * `onNpcHit` fires for hits the npc *receives*, not the ones it deals. So rather than split the
 * brothers across two mechanisms and leave four of them without their defining trait, all six run
 * through the shape below.
 *
 * That shape is `NvPCombat.attackMelee` with the formulae chosen per style, including the ordering
 * note that matters: **retaliation is queued before the hit**. Queued after, every hit would
 * trigger the speed-up death mechanic, because the hit queues would no longer be the last entries
 * in the queue list when they are processed.
 *
 * Every animation and graphic used here is named for its brother in the cache. Nothing is guessed:
 * where the cache names no sound for a swing, none is played rather than a plausible-sounding one
 * being invented.
 */
class BarrowsCombatScript
@Inject
constructor(
    private val accuracy: AccuracyFormulae,
    private val maxHits: MaxHitFormulae,
    private val worldRepo: WorldRepository,
    private val objTypes: ObjTypeList,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (brother in Brother.all) {
            onAiOpPlayer2(checkNotNull(barrows_npcs.brothers[brother])) {
                attack(brother, it.target)
            }
        }
    }

    private fun StandardNpcAccess.attack(brother: Brother, target: Player) {
        if (!readyToAttack(target)) {
            return
        }
        when (brother.style) {
            BarrowsStyle.Magic -> cast(target)
            BarrowsStyle.Ranged -> shoot(target)
            BarrowsStyle.Melee -> swing(brother, target)
        }
    }

    /** Ahrim: a magic attack, with a chance to sap the player's Strength. */
    private fun StandardNpcAccess.cast(target: Player) {
        anim(barrows_seqs.ahrim_special)
        val projectile =
            worldRepo.projAnim(npc, target, barrows_spots.ahrim_aura, BarrowsProjAnims.magic)
        val landed = accuracy.rollMagicAccuracy(npc, target, random)
        val damage = if (landed) random.of(maxHits.getMagicMaxHit(npc, target) + 1) else 0
        strike(target, damage, HitType.Magic, projectile.clientCycles)

        if (landed && random.of(SPECIAL_CHANCE) == 0 && target.strengthLvl > 0) {
            target.statSub(stats.strength, constant = STRENGTH_DRAIN, percent = 0)
            target.spotanim(barrows_spots.ahrim_aura, delay = projectile.clientCycles)
        }
    }

    /** Karil: a bolt from the repeating crossbow, with a chance to halve the player's Agility. */
    private fun StandardNpcAccess.shoot(target: Player) {
        anim(barrows_seqs.karil_attack)
        val projectile =
            worldRepo.projAnim(npc, target, barrows_spots.crossbow_bolt, BarrowsProjAnims.bolt)
        val landed = accuracy.rollRangedAccuracy(npc, target, random)
        val damage = if (landed) random.of(maxHits.getRangedMaxHit(npc, target) + 1) else 0
        strike(target, damage, HitType.Ranged, projectile.clientCycles)

        if (landed && random.of(SPECIAL_CHANCE) == 0 && target.agilityLvl > 0) {
            // Karil's tainted shot takes a fifth of what is left, not a flat amount.
            target.statSub(stats.agility, constant = 0, percent = AGILITY_DRAIN_PERCENT)
            target.spotanim(barrows_spots.karil_shot, delay = projectile.clientCycles)
        }
    }

    /**
     * The four melee brothers. They share one swing; what differs is [applySpecial].
     *
     * Verac is the exception to the accuracy roll: his flail ignores the player's defence, so his
     * swing is resolved as a hit that always lands.
     */
    private fun StandardNpcAccess.swing(brother: Brother, target: Player) {
        anim(npc.visType.param(params.attack_anim))

        val ignoresDefence = brother == Brother.Verac && random.of(SPECIAL_CHANCE) == 0
        val landed = ignoresDefence || accuracy.rollMeleeAccuracy(npc, target, null, random)
        val maxHit = maxHits.getMeleeMaxHit(npc, target, null)
        var damage =
            when {
                ignoresDefence -> random.of(VERAC_MINIMUM, max(VERAC_MINIMUM, maxHit))
                landed -> random.of(maxHit + 1)
                else -> 0
            }
        damage = applySpecial(brother, target, damage, maxHit, ignoresDefence)
        strike(target, damage, HitType.Melee, delay = 1)
    }

    /**
     * The melee brothers' defining traits, applied once the damage is known.
     *
     * Returns the damage to actually deal, which only Dharok changes.
     */
    private fun StandardNpcAccess.applySpecial(
        brother: Brother,
        target: Player,
        damage: Int,
        maxHit: Int,
        ignoredDefence: Boolean,
    ): Int =
        when (brother) {
            // The Wretched strength: every point of health he has lost makes him hit harder, so a
            // nearly-dead Dharok is the most dangerous thing in the crypt.
            Brother.Dharok -> {
                val missing = npc.visType.hitpoints - npc.hitpoints
                val scaled = damage + damage * missing / npc.visType.hitpoints.coerceAtLeast(1)
                min(scaled, maxHit * DHAROK_DAMAGE_CAP)
            }
            // The Infested: heals himself for what he takes out of you.
            Brother.Guthan -> {
                if (damage > 0) {
                    npc.hitpoints = min(npc.visType.hitpoints, npc.hitpoints + damage)
                    spotanim(barrows_spots.guthan_effect)
                }
                damage
            }
            // The Corrupted: saps the will to run.
            Brother.Torag -> {
                if (damage > 0) {
                    target.runEnergy = max(0, target.runEnergy - TORAG_ENERGY_DRAIN)
                    spotanim(barrows_spots.torag_effect)
                }
                damage
            }
            Brother.Verac -> {
                if (ignoredDefence) {
                    spotanim(barrows_spots.verac_desolation)
                }
                damage
            }
            else -> damage
        }

    /**
     * The guards `NvPCombat` applies before every swing, and the attack-speed bookkeeping.
     *
     * Npcs repeat their last interaction until something changes their mode, so this runs once a
     * tick while a brother is on a target and declines on most of them.
     */
    private fun StandardNpcAccess.readyToAttack(target: Player): Boolean {
        if (!target.isValidTarget()) {
            resetMode()
            return false
        }
        if (actionDelay > mapClock) {
            return false
        }
        if (!npc.isInCombat()) {
            resetMode()
            return false
        }
        actionDelay = mapClock + npc.attackRate()
        return true
    }

    /** The hit itself, delayed to land when the projectile arrives. */
    private fun StandardNpcAccess.strike(target: Player, damage: Int, type: HitType, delay: Int) {
        // Before the hit, never after - see the class comment.
        target.queueCombatRetaliate(npc)
        target.queueHit(npc, delay.coerceAtLeast(1), type, damage)
        target.combatPlayDefendAnim(objTypes)
    }

    private companion object {
        /** One swing in four carries the brother's special, which is roughly how live feels. */
        const val SPECIAL_CHANCE = 4

        const val STRENGTH_DRAIN = 2
        const val AGILITY_DRAIN_PERCENT = 20

        /** Verac's flail never taps you for nothing when it ignores defence. */
        const val VERAC_MINIMUM = 1

        /** Dharok cannot exceed double his ordinary maximum, however close to death he is. */
        const val DHAROK_DAMAGE_CAP = 2

        const val TORAG_ENERGY_DRAIN = 200
    }
}
