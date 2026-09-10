package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.varps
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.instant.InstantSpecialAttack
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.interact.InteractionNpc
import org.rsmod.game.interact.InteractionPlayer

/**
 * Quick Smash: an ordinary hit that fires the moment the bar is clicked, ignoring the attack delay.
 *
 * It is an instant special, so it strikes whatever the player is currently fighting. With no target
 * in melee reach it does nothing and costs no energy. Unlike the real thing, it cannot be armed
 * while idle to fire on the next swing.
 */
class GraniteMaulSpecialAttack
@Inject
constructor(
    private val types: AttackTypes,
    private val styles: AttackStyles,
    private val speeds: WeaponSpeeds,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val quickSmash = QuickSmash(manager, types, styles, speeds)
        registerInstant(special_objs.granite_maul, quickSmash)
        registerInstant(special_objs.granite_maul_or, quickSmash)
        registerInstant(special_objs.granite_maul_plus, quickSmash)
        registerInstant(special_objs.granite_maul_or_plus, quickSmash)
    }

    private class QuickSmash(
        private val manager: SpecialAttackManager,
        private val types: AttackTypes,
        private val styles: AttackStyles,
        private val speeds: WeaponSpeeds,
    ) : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            val target = currentTarget() ?: return false
            if (!isWithinDistance(target, 1)) {
                return false
            }

            val attack =
                CombatAttack.Melee(
                    weapon = player.righthand,
                    type = MeleeAttackType.from(types.get(player)),
                    style = MeleeAttackStyle.from(styles.get(player)),
                    stance = combatStance,
                )

            anim(special_seqs.granite_maul)
            spotanim(special_spots.granite_maul, height = 96, slot = constants.spotanim_slot_combat)

            val damage = manager.rollMeleeDamage(this, target, attack, 1.0, 1.0)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.setNextAttackDelay(this, speeds.actual(player))
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.currentTarget(): PathingEntity? {
            val target: PathingEntity =
                when (val interaction = player.interaction) {
                    is InteractionNpc -> interaction.target
                    is InteractionPlayer -> interaction.target
                    else -> return null
                }
            val valid =
                when (target) {
                    is Npc -> target.isValidTarget()
                    is Player -> target.isValidTarget()
                }
            return target.takeIf { valid }
        }
    }
}

private val ProtectedAccess.combatStance by enumVarp<CombatStance>(varps.com_mode)
