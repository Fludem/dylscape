package org.rsmod.api.combat.commons.npc

import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.game.entity.Npc
import org.rsmod.game.type.category.isType

/**
 * The combat style an npc attacks a target with.
 *
 * This is read off `params.npc_attack_type`, the same param the melee path has always used to pick
 * between stab, slash and crush. OSRS's own npc `attacktype` field carries all five values;
 * upstream authored only the melee three because [org.rsmod.api.combat.CombatAttack.NpcMelee] was
 * the only attack the dispatcher could build, so nothing could have read the other two.
 */
public enum class NpcAttackStyle {
    Melee,
    Ranged,
    Magic;

    /**
     * Whether this style attacks from a distance, and so engages through `ApPlayer2` rather than
     * `OpPlayer2`. Both the hunt path and the retaliate path branch on this: a ranged npc that
     * retaliates into op mode closes to melee range and throws punches with its attack animation.
     */
    public val attacksAtRange: Boolean
        get() = this != Melee
}

/**
 * Resolves [npc]'s declared attack style.
 *
 * Anything that is not explicitly tagged `attacktype_ranged` or `attacktype_magic` is [Melee] --
 * the same fallback the melee path has always applied. That default is load-bearing: combat levels
 * *are* vanilla cache fields, so nearly every combat npc carries a non-zero ranged and magic level,
 * and inferring style from stats rather than from an authored tag would arm thousands of npcs at
 * once.
 */
public fun Npc.attackStyle(): NpcAttackStyle {
    val category = visType.paramOrNull(params.npc_attack_type)
    return when {
        category.isType(categories.attacktype_ranged) -> NpcAttackStyle.Ranged
        category.isType(categories.attacktype_magic) -> NpcAttackStyle.Magic
        else -> NpcAttackStyle.Melee
    }
}
