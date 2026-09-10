package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.content.other.special.attacks.effects.TargetDrain
import org.rsmod.content.skills.magic.commons.FreezeManager
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.spot.SpotanimType

/**
 * The four godsword specials. They all double accuracy and roll against slash defence. They also
 * all first apply a hidden 1.1x to the max hit (floored) and then their own bonus, so Armadyl's
 * 1.25x lands at ~1.375x and Bandos's 1.1x at ~1.21x.
 *
 * Every effect needs the hit to land. The Bandos drain and the Saradomin heal are applied on the
 * attack tick rather than a tick later when the hitsplat shows.
 */
class GodswordSpecialAttacks @Inject constructor(private val freezes: FreezeManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee(
            objs.armadyl_godsword,
            Godsword(
                manager,
                special_seqs.armadyl_godsword,
                special_spots.armadyl_godsword,
                bonus = 1.25,
            ),
        )
        registerMelee(
            special_objs.armadyl_godsword_or,
            Godsword(
                manager,
                special_seqs.armadyl_godsword_or,
                special_spots.armadyl_godsword_or,
                bonus = 1.25,
            ),
        )

        val warstrike = GodswordEffect { target, damage ->
            TargetDrain.drainInOrder(target, damage, BANDOS_DRAIN_ORDER)
        }
        registerMelee(
            special_objs.bandos_godsword,
            Godsword(
                manager,
                special_seqs.bandos_godsword,
                special_spots.bandos_godsword,
                bonus = 1.1,
                effect = warstrike,
            ),
        )
        registerMelee(
            special_objs.bandos_godsword_or,
            Godsword(
                manager,
                special_seqs.bandos_godsword_or,
                special_spots.bandos_godsword_or,
                bonus = 1.1,
                effect = warstrike,
            ),
        )

        val healingBlade = GodswordEffect { _, damage ->
            statHeal(stats.hitpoints, constant = max(10, damage / 2), percent = 0)
            statHeal(stats.prayer, constant = max(5, damage / 4), percent = 0)
        }
        registerMelee(
            special_objs.saradomin_godsword,
            Godsword(
                manager,
                special_seqs.saradomin_godsword,
                special_spots.saradomin_godsword,
                effect = healingBlade,
            ),
        )
        registerMelee(
            special_objs.saradomin_godsword_or,
            Godsword(
                manager,
                special_seqs.saradomin_godsword_or,
                special_spots.saradomin_godsword_or,
                effect = healingBlade,
            ),
        )

        val iceCleave = GodswordEffect { target, _ -> freezes.freeze(target, ICE_CLEAVE_TICKS) }
        registerMelee(
            special_objs.zamorak_godsword,
            Godsword(
                manager,
                special_seqs.zamorak_godsword,
                special_spots.zamorak_godsword,
                effect = iceCleave,
            ),
        )
        registerMelee(
            special_objs.zamorak_godsword_or,
            Godsword(
                manager,
                special_seqs.zamorak_godsword_or,
                special_spots.zamorak_godsword_or,
                effect = iceCleave,
            ),
        )
    }

    /** What a godsword does beyond its damage, run only when the hit lands. */
    private fun interface GodswordEffect {
        fun ProtectedAccess.apply(target: PathingEntity, damage: Int)
    }

    private class Godsword(
        private val manager: SpecialAttackManager,
        private val seq: SeqType,
        private val spot: SpotanimType,
        private val bonus: Double = 1.0,
        private val effect: GodswordEffect? = null,
    ) : SymmetricMeleeSpecial() {
        override suspend fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            anim(seq)
            spotanim(spot, slot = constants.spotanim_slot_combat)

            val damage = rollDamage(target, attack)
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)

            if (damage > 0 && effect != null) {
                with(effect) { apply(target, damage) }
            }

            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.rollDamage(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Int {
            val landed =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 2.0,
                )
            if (!landed) {
                return 0
            }
            val godswordMax =
                manager.calculateMeleeMaxHit(this, target, attack.type, attack.style, 1.1)
            return random.landedHit((godswordMax * bonus).toInt())
        }
    }

    private companion object {
        /** Ice barrage's length: 20 seconds. */
        const val ICE_CLEAVE_TICKS = 32

        val BANDOS_DRAIN_ORDER =
            listOf(
                stats.defence,
                stats.strength,
                stats.prayer,
                stats.attack,
                stats.magic,
                stats.ranged,
            )
    }
}
