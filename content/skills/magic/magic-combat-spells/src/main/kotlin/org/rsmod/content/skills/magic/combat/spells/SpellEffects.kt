package org.rsmod.content.skills.magic.combat.spells

import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statDrain
import org.rsmod.api.player.stat.statHeal
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/** The stats the curses and the shadow spells can lower. */
public enum class DrainStat {
    Attack,
    Strength,
    Defence,
}

public object SpellEffects {
    /**
     * Lowers [stat] on [target] by [percent] of its base, the curse rule: only a target whose stat
     * is still at (or above) its base can be drained, so the spells never stack. Returns whether
     * anything changed.
     */
    public fun drain(target: PathingEntity, stat: DrainStat, percent: Int): Boolean =
        when (target) {
            is Npc -> drainNpc(target, stat, percent)
            is Player -> {
                target.statDrain(stat.playerStat(), constant = 0, percent = percent)
                true
            }
        }

    private fun drainNpc(npc: Npc, stat: DrainStat, percent: Int): Boolean {
        val (current, base) =
            when (stat) {
                DrainStat.Attack -> npc.attackLvl to npc.baseAttackLvl
                DrainStat.Strength -> npc.strengthLvl to npc.baseStrengthLvl
                DrainStat.Defence -> npc.defenceLvl to npc.baseDefenceLvl
            }
        if (current < base) {
            return false
        }
        val drained = maxOf(0, current - base * percent / 100)
        when (stat) {
            DrainStat.Attack -> npc.attackLvl = drained
            DrainStat.Strength -> npc.strengthLvl = drained
            DrainStat.Defence -> npc.defenceLvl = drained
        }
        return true
    }

    /** Blood spells: the caster recovers a share of the damage they dealt. */
    public fun healCaster(caster: Player, damage: Int, percent: Int) {
        val heal = damage * percent / 100
        if (heal > 0) {
            caster.statHeal(stats.hitpoints, constant = heal, percent = 0)
        }
    }

    /**
     * Whether Crumble Undead may target [npc]. RSMod's `params.undead` is honoured first, but no
     * npc in this cache carries it, so the fallback is the creature's name: the families the spell
     * works on in the official game are all named for what they are.
     */
    public fun isUndead(npc: Npc): Boolean {
        val tagged = npc.visType.paramMap?.getOrNull(params.undead)
        if (tagged != null) {
            return tagged != 0
        }
        val name = npc.visType.name.lowercase()
        return UNDEAD_WORDS.any { it in name }
    }

    private fun DrainStat.playerStat() =
        when (this) {
            DrainStat.Attack -> stats.attack
            DrainStat.Strength -> stats.strength
            DrainStat.Defence -> stats.defence
        }

    private val UNDEAD_WORDS =
        listOf(
            "skeleton",
            "skeletal",
            "zombie",
            "ghost",
            "ghast",
            "shade",
            "mummy",
            "banshee",
            "ankou",
            "revenant",
            "zogre",
            "undead",
            "lich",
            "wight",
            "vampyre",
            "crawling hand",
            "spectre",
        )
}
