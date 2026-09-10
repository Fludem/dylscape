package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

/**
 * Specials that are one melee hit with boosted accuracy or damage.
 *
 * The whip's PvP run energy drain is left out because run energy is pinned at max on this server.
 * The scimitar's PvP protection prayer block is also left out, since nothing can disable a prayer
 * yet.
 */
class SimpleMeleeSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val energyDrain =
            SingleHit(
                manager = manager,
                seq = special_seqs.abyssal_whip,
                spot = special_spots.abyssal_whip,
                spotOnTarget = true,
                accuracy = 1.25,
            )
        registerMelee(objs.abyssal_whip, energyDrain)
        registerMelee(special_objs.abyssal_whip_lava, energyDrain)
        registerMelee(special_objs.abyssal_whip_ice, energyDrain)
        registerMelee(special_objs.abyssal_tentacle, energyDrain)

        val shatter =
            SingleHit(
                manager = manager,
                seq = special_seqs.dragon_mace,
                spot = special_spots.dragon_mace,
                accuracy = 1.25,
                damage = 1.5,
            )
        registerMelee(special_objs.dragon_mace, shatter)

        val sever =
            SingleHit(
                manager = manager,
                seq = special_seqs.dragon_scimitar,
                spot = special_spots.dragon_scimitar,
                accuracy = 1.25,
            )
        registerMelee(special_objs.dragon_scimitar, sever)
        registerMelee(special_objs.dragon_scimitar_or, sever)
    }

    private class SingleHit(
        private val manager: SpecialAttackManager,
        private val seq: SeqType,
        private val spot: SpotanimType,
        private val spotOnTarget: Boolean = false,
        private val accuracy: Double = 1.0,
        private val damage: Double = 1.0,
    ) : SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(seq)
            if (spotOnTarget) {
                target.spotanim(spot, height = 100, slot = constants.spotanim_slot_combat)
            } else {
                spotanim(spot, height = 96, slot = constants.spotanim_slot_combat)
            }

            val hit = manager.rollMeleeDamage(this, target, attack, accuracy, damage)
            manager.giveCombatXp(this, target, attack, hit)
            manager.queueMeleeHit(this, target, hit)
            manager.continueCombat(this, target)
            return true
        }
    }
}
