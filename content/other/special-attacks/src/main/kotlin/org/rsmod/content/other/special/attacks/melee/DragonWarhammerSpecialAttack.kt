package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.content.other.special.attacks.effects.TargetDrain
import org.rsmod.game.entity.PathingEntity

/** Smash: 50% more damage, and a landed hit takes 30% of the target's current Defence. */
class DragonWarhammerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val smash = Smash(manager)
        registerMelee(special_objs.dragon_warhammer, smash)
        registerMelee(special_objs.dragon_warhammer_or, smash)
    }

    private class Smash(private val manager: SpecialAttackManager) : SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.dragon_warhammer)
            spotanim(special_spots.dragon_warhammer, slot = constants.spotanim_slot_combat)

            val damage = manager.rollMeleeDamage(this, target, attack, 1.0, 1.5)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)

            if (damage > 0) {
                val defence = TargetDrain.level(target, stats.defence)
                TargetDrain.drain(target, stats.defence, defence * 30 / 100)
            }

            manager.continueCombat(this, target)
            return true
        }
    }
}
