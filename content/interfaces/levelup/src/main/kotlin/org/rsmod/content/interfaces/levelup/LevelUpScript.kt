package org.rsmod.content.interfaces.levelup

import jakarta.inject.Inject
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.advanced.onAdvanceStat
import org.rsmod.game.type.spot.SpotanimType
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.stat.StatTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LevelUpScript @Inject constructor(private val statTypes: StatTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        onAdvanceStat { advanceStat(it.args) }
    }

    private fun ProtectedAccess.advanceStat(stat: StatType) {
        spotanim(fireworks(stat))
        levelUpBox(stat)
    }

    private fun ProtectedAccess.fireworks(stat: StatType): SpotanimType {
        val maxLevel = statTypes[stat].maxLevel
        if (statBase(stat) < maxLevel) {
            return spotanims.levelup_anim
        }
        return if (isMaxed()) spotanims.levelup_max else spotanims.levelup_99_anim
    }

    /** Returns `true` when every released stat sits at its max level. */
    private fun ProtectedAccess.isMaxed(): Boolean =
        statTypes.values.none { !it.unreleased && statBase(it) < it.maxLevel }
}
