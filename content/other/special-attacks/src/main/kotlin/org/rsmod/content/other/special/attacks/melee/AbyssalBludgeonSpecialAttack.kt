package org.rsmod.content.other.special.attacks.melee

import kotlin.math.max
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
import org.rsmod.game.entity.PathingEntity

/** Penance: 0.5% more damage for every prayer point the wielder is missing. */
class AbyssalBludgeonSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(special_objs.abyssal_bludgeon, Penance(manager))
    }

    private class Penance(private val manager: SpecialAttackManager) : SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.abyssal_bludgeon)
            target.spotanim(special_spots.abyssal_bludgeon, slot = constants.spotanim_slot_combat)

            val missingPrayer = max(0, statBase(stats.prayer) - stat(stats.prayer))
            val multiplier = 1.0 + missingPrayer * 0.005

            val damage = manager.rollMeleeDamage(this, target, attack, 1.0, multiplier)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }
    }
}
