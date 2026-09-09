package org.rsmod.api.combat.scripts

import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.combat.NvPCombat
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.npc.NpcAttackStyle
import org.rsmod.api.combat.commons.npc.attackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.player.aggressiveNpc
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.npc.opPlayer2
import org.rsmod.api.player.isInPvnCombat
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.api.script.advanced.onDefaultAiApPlayer2
import org.rsmod.api.script.advanced.onDefaultAiOpPlayer2
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.type.category.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Public so integration suites can start it: `runGameTest` only registers the scripts it is handed,
 * and a test source set cannot see `internal`.
 */
public class NvPCombatScript
@Inject
constructor(
    private val combat: NvPCombat,
    private val areaChecker: AreaChecker,
    private val interactions: AiPlayerInteractions,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onDefaultAiOpPlayer2 { attemptCombatOp(it.target) }
        onDefaultAiApPlayer2 { attemptCombatAp(it.target) }
    }

    private fun StandardNpcAccess.attemptCombatOp(target: Player) {
        if (!canAttack(target)) {
            resetMode()
            return
        }
        val attackType = npc.resolveMeleeAttackType()
        val attack = CombatAttack.NpcMelee(attackType)
        combat.attack(this, target, attack)
    }

    /**
     * The "attack from a distance" half of npc combat.
     *
     * Three of the hunt modes -- `ranged`, `notbusy_range` and `aggressive_ranged` -- set
     * `findNewMode = ApPlayer2`, but nothing had ever registered a handler for it, so an npc using
     * any of them would walk to its attack range and then stand there indefinitely.
     */
    private fun StandardNpcAccess.attemptCombatAp(target: Player) {
        if (!canAttack(target)) {
            resetMode()
            return
        }
        val attack = npc.resolveRangedAttack()
        if (attack == null) {
            // The npc reached ap range without declaring a ranged or magic style, so it has
            // nothing to fire. Close and melee rather than `resetMode()`, which would only thrash:
            // the hunt processor re-applies `ApPlayer2` on the next cycle.
            npc.opPlayer2(target, interactions)
            return
        }
        combat.attack(this, target, attack)
    }

    private fun StandardNpcAccess.canAttack(target: Player): Boolean {
        val singleCombat = !mapMultiway(areaChecker)
        if (singleCombat) {
            if (target.isInPvpCombat()) {
                return false
            }

            if (target.isInPvnCombat()) {
                if (target.aggressiveNpc != null && target.aggressiveNpc != npc.uid) {
                    return false
                }
            }
        }
        return true
    }

    private fun Npc.resolveMeleeAttackType(): MeleeAttackType {
        val category = visType.paramOrNull(params.npc_attack_type)
        return when {
            category.isType(categories.attacktype_stab) -> MeleeAttackType.Stab
            category.isType(categories.attacktype_slash) -> MeleeAttackType.Slash
            else -> MeleeAttackType.Crush
        }
    }

    /**
     * Returns the attack this npc makes at range, or `null` if it has not declared one.
     *
     * The gate is an authored `attacktype_ranged` / `attacktype_magic` tag, never the npc's ranged
     * or magic *level*: those are vanilla cache fields that nearly every combat npc carries, so
     * inferring from them would arm thousands of npcs the moment this handler was registered.
     */
    private fun Npc.resolveRangedAttack(): CombatAttack.NpcAttack? =
        when (attackStyle()) {
            NpcAttackStyle.Melee -> null
            NpcAttackStyle.Ranged -> CombatAttack.NpcRanged(RangedAttackType.Standard)
            NpcAttackStyle.Magic -> CombatAttack.NpcMagic(maxHit = 0)
        }
}
