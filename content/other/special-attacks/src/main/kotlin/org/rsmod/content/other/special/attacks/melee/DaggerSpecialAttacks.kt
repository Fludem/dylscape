package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.PathingEntity

/**
 * The two-hit dagger specials. Both hits land on the same tick.
 * - Puncture (dragon dagger): each hit rolls on its own at 1.15x accuracy and 1.15x damage.
 * - Abyssal Puncture (abyssal dagger): one 1.25x accuracy roll decides both hits, each at 0.85x
 *   damage, so the second only lands if the first does.
 *
 * Poisoned daggers do not poison on a spec, because nothing applies weapon poison yet.
 */
class DaggerSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val puncture = Puncture(manager)
        registerMelee(special_objs.dragon_dagger, puncture)
        registerMelee(special_objs.dragon_dagger_p, puncture)
        registerMelee(special_objs.dragon_dagger_p_plus, puncture)
        registerMelee(special_objs.dragon_dagger_p_plus_plus, puncture)

        val abyssalPuncture = AbyssalPuncture(manager)
        registerMelee(special_objs.abyssal_dagger, abyssalPuncture)
        registerMelee(special_objs.abyssal_dagger_p, abyssalPuncture)
        registerMelee(special_objs.abyssal_dagger_p_plus, abyssalPuncture)
        registerMelee(special_objs.abyssal_dagger_p_plus_plus, abyssalPuncture)
    }

    private class Puncture(private val manager: SpecialAttackManager) : SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.dragon_dagger)
            spotanim(
                special_spots.dragon_dagger,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val first = manager.rollMeleeDamage(this, target, attack, 1.15, 1.15)
            val second = manager.rollMeleeDamage(this, target, attack, 1.15, 1.15)
            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            manager.continueCombat(this, target)
            return true
        }
    }

    private class AbyssalPuncture(private val manager: SpecialAttackManager) :
        SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.abyssal_dagger)
            spotanim(special_spots.abyssal_dagger, slot = constants.spotanim_slot_combat)

            val landed =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.25,
                )
            val max =
                if (landed) {
                    manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 0.85)
                } else {
                    0
                }
            val first = random.landedHit(max)
            val second = random.landedHit(max)

            manager.giveCombatXp(this, target, attack, first + second)
            manager.queueMeleeHit(this, target, first)
            manager.queueMeleeHit(this, target, second)
            manager.continueCombat(this, target)
            return true
        }
    }
}
