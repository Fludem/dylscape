package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
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
 * Saradomin's Lightning: a 1.1x melee hit rolled against slash defence. If it lands, 1-16 magic
 * damage follows on the same tick. Only the melee part gives combat xp.
 */
class SaradominSwordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(special_objs.saradomin_sword, SaradominsLightning(manager))
    }

    private class SaradominsLightning(private val manager: SpecialAttackManager) :
        SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.saradomin_sword)
            spotanim(special_spots.saradomin_sword, slot = constants.spotanim_slot_combat)

            val melee =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.1,
                    blockType = MeleeAttackType.Slash,
                )
            manager.giveCombatXp(this, target, attack, melee)
            manager.queueMeleeHit(this, target, melee)

            if (melee > 0) {
                target.spotanim(
                    special_spots.saradomin_sword_lightning,
                    slot = constants.spotanim_slot_combat,
                )
                val lightning = random.of(1, 16)
                manager.queueMagicHit(this, target, lightning, clientDelay = 0, hitDelay = 1)
            }

            manager.continueCombat(this, target)
            return true
        }
    }
}
