package org.rsmod.content.other.special.attacks.melee

import kotlin.math.max
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

/**
 * Slice and Dice: four hits. Accuracy is rolled hit by hit until one lands, and that hit sets the
 * pattern. With `m` as the normal max hit and every fraction floored:
 * - 1st lands: `m/2..m-1`, then half of that, half again, and that plus one.
 * - 2nd lands: 0, then `3m/8..7m/8`, half of that, and that plus one.
 * - 3rd lands: 0, 0, then `m/4..3m/4`, and that plus one.
 * - 4th lands: 0, 0, 0, then `m/4..5m/4`.
 * - None land: 0-0-1-1 two times in three, otherwise 0-0-0-0.
 *
 * The first two hits land on the attack tick and the last two a tick later.
 */
class DragonClawsSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val sliceAndDice = SliceAndDice(manager)
        registerMelee(objs.dragon_claws, sliceAndDice)
        registerMelee(special_objs.dragon_claws_or, sliceAndDice)
    }

    private class SliceAndDice(private val manager: SpecialAttackManager) :
        SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(special_seqs.dragon_claws)
            spotanim(special_spots.dragon_claws, slot = constants.spotanim_slot_combat)

            val hits = rollHits(target, attack)
            manager.giveCombatXp(this, target, attack, hits.sum())
            manager.queueMeleeHit(this, target, hits[0], delay = 1)
            manager.queueMeleeHit(this, target, hits[1], delay = 1)
            manager.queueMeleeHit(this, target, hits[2], delay = 2)
            manager.queueMeleeHit(this, target, hits[3], delay = 2)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rollHits(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): IntArray {
            val m = manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.0)
            fun lands(): Boolean =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            fun between(low: Int, high: Int): Int = random.of(low, max(low, high))

            return when {
                lands() -> {
                    val first = between(m / 2, m - 1)
                    val second = first / 2
                    val third = second / 2
                    intArrayOf(first, second, third, third + 1)
                }
                lands() -> {
                    val second = between(m * 3 / 8, m * 7 / 8)
                    val third = second / 2
                    intArrayOf(0, second, third, third + 1)
                }
                lands() -> {
                    val third = between(m / 4, m * 3 / 4)
                    intArrayOf(0, 0, third, third + 1)
                }
                lands() -> intArrayOf(0, 0, 0, between(m / 4, m * 5 / 4))
                random.of(0, 2) != 0 -> intArrayOf(0, 0, 1, 1)
                else -> intArrayOf(0, 0, 0, 0)
            }
        }
    }
}
