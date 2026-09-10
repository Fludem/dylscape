package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.projanims
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.quiver
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs
import org.rsmod.content.other.special.attacks.configs.special_spots
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * The magic bows.
 * - Snapshot (shortbow): two arrows, each at 10/7 accuracy.
 * - Powershot (longbow): one arrow that always hits.
 */
class MagicBowSpecialAttacks
@Inject
constructor(private val objTypes: ObjTypeList, private val ammunition: RangedAmmoManager) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val snapshot = Snapshot(manager, ammunition, objTypes)
        registerRanged(objs.magic_shortbow, snapshot)
        registerRanged(special_objs.magic_shortbow_i, snapshot)
        registerRanged(special_objs.magic_longbow, Powershot(manager, ammunition, objTypes))
    }

    private abstract class MagicBowSpecial(
        protected val manager: SpecialAttackManager,
        protected val ammunition: RangedAmmoManager,
        private val objTypes: ObjTypeList,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = special(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = special(target, attack)

        abstract fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean

        /** The quiver's ammo if the bow can fire [count] of it, else stops combat and `null`. */
        protected fun ProtectedAccess.readyAmmo(
            attack: CombatAttack.Ranged,
            count: Int,
        ): UnpackedObjType? {
            val weaponType = objTypes[attack.weapon]
            val quiverType = objTypes.getOrNull(player.quiver)
            val canUseAmmo = ammunition.attemptAmmoUsage(player, weaponType, quiverType)
            if (!canUseAmmo || quiverType == null) {
                manager.stopCombat(this)
                return null
            }
            val quiverCount = player.quiver?.count ?: 0
            if (quiverCount < count) {
                manager.stopCombat(this)
                mes(
                    "You need to have at least $count arrows in your quiver for this special attack."
                )
                return null
            }
            return quiverType
        }
    }

    private class Snapshot(
        manager: SpecialAttackManager,
        ammunition: RangedAmmoManager,
        objTypes: ObjTypeList,
    ) : MagicBowSpecial(manager, ammunition, objTypes) {
        override fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val quiverType = readyAmmo(attack, count = 2) ?: return false

            anim(special_seqs.magic_shortbow)
            spotanim(
                special_spots.magic_shortbow,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val travel = special_spots.glow_arrow_travel
            val proj1 = manager.spawnProjectile(this, target, travel, projanims.doublearrow_one)
            val proj2 = manager.spawnProjectile(this, target, travel, projanims.doublearrow_two)

            val first = manager.rollRangedDamage(this, target, attack, SNAPSHOT_ACCURACY)
            val second = manager.rollRangedDamage(this, target, attack, SNAPSHOT_ACCURACY)
            manager.giveCombatXp(this, target, attack, first + second)

            ammunition.useQuiverAmmo(player, quiverType, target.coords, proj1.serverCycles)
            manager.queueRangedHit(
                this,
                target,
                quiverType,
                first,
                proj1.clientCycles,
                proj1.serverCycles,
            )

            ammunition.useQuiverAmmo(player, quiverType, target.coords, proj2.serverCycles)
            manager.queueRangedDamage(this, target, quiverType, second, proj2.serverCycles)

            manager.continueCombat(this, target)
            return true
        }
    }

    private class Powershot(
        manager: SpecialAttackManager,
        ammunition: RangedAmmoManager,
        objTypes: ObjTypeList,
    ) : MagicBowSpecial(manager, ammunition, objTypes) {
        override fun ProtectedAccess.special(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            val quiverType = readyAmmo(attack, count = 1) ?: return false

            anim(seqs.human_bow)
            spotanim(
                special_spots.glow_arrow_launch,
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val proj =
                manager.spawnProjectile(
                    this,
                    target,
                    special_spots.glow_arrow_travel,
                    projanims.arrow,
                )

            // Powershot never misses, so the accuracy roll is skipped.
            val damage =
                manager.rollRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                    boltSpecDamage = 0,
                )
            manager.giveCombatXp(this, target, attack, damage)

            ammunition.useQuiverAmmo(player, quiverType, target.coords, proj.serverCycles)
            manager.queueRangedHit(
                this,
                target,
                quiverType,
                damage,
                proj.clientCycles,
                proj.serverCycles,
            )

            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val SNAPSHOT_ACCURACY = 10.0 / 7.0
    }
}
