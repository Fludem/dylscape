package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** A melee special that behaves the same against npcs and players. */
internal abstract class SymmetricMeleeSpecial : MeleeSpecialAttack {
    override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean =
        special(target, attack)

    override suspend fun ProtectedAccess.attack(
        target: Player,
        attack: CombatAttack.Melee,
    ): Boolean = special(target, attack)

    abstract suspend fun ProtectedAccess.special(
        target: PathingEntity,
        attack: CombatAttack.Melee,
    ): Boolean
}

/** Rolls a landed hit the way `PlayerAttackManager` does: `1..max`, never a zero. */
internal fun GameRandom.landedHit(max: Int): Int = if (max <= 0) 0 else of(1..max)
