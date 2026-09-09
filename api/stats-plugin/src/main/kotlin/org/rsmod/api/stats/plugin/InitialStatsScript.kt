package org.rsmod.api.stats.plugin

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.baseHitpointsLvl
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onPlayerInit
import org.rsmod.game.entity.Player
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class InitialStatsScript @Inject constructor(private val statTypes: StatTypeList) :
    PluginScript() {
    private val hitpointsStartLvl by lazy { statTypes[stats.hitpoints].minLevel }

    private val Player.newAccount by boolVarBit(varbits.new_player_account)

    override fun ScriptContext.startup() {
        onPlayerInit { player.setInitialStats() }
    }

    private fun Player.setInitialStats() {
        if (newAccount && baseHitpointsLvl < hitpointsStartLvl) {
            grantStartLvl(stats.hitpoints, hitpointsStartLvl)
            appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
        }
        // Deliberately *not* gated on `newAccount`: Druidic Ritual is not implemented, so herblore
        // would otherwise be unusable - its lowest potion needs level 3 - for every character that
        // already exists. Topping up on every login is idempotent; a base level can never fall
        // back below the floor once granted.
        if (statBase(stats.herblore) < HERBLORE_START_LVL) {
            grantStartLvl(stats.herblore, HERBLORE_START_LVL)
        }
    }

    private fun Player.grantStartLvl(stat: StatType, level: Int) {
        statMap.setFineXP(stat, getFineXp(level))
        statMap.setCurrentLevel(stat, level.toByte())
        statMap.setBaseLevel(stat, level.toByte())
    }

    private fun getFineXp(level: Int): Int = PlayerSkillXPTable.getFineXPFromLevel(level)

    private companion object {
        /** Matches the herblore level Druidic Ritual's 250 xp reward leaves a new account at. */
        const val HERBLORE_START_LVL = 3
    }
}
