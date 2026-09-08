package org.rsmod.content.other.commands

import kotlin.math.max
import org.rsmod.annotations.InternalApi
import org.rsmod.api.cheat.CheatHandlerBuilder
import org.rsmod.api.config.refs.modlevels
import org.rsmod.api.player.stat.PlayerSkillXP
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.script.onCommand
import org.rsmod.game.cheat.Cheat
import org.rsmod.game.entity.Player
import org.rsmod.game.stat.PlayerSkillXPTable
import org.rsmod.game.type.stat.StatType
import org.rsmod.game.type.stat.UnpackedStatType
import org.rsmod.plugin.scripts.ScriptContext
import org.simmetrics.metrics.StringMetrics

private val levenshteinMetric = StringMetrics.levenshtein()

internal fun ScriptContext.onCommand(
    command: String,
    desc: String,
    cheat: Cheat.() -> Unit,
    init: CheatHandlerBuilder.() -> Unit = {},
) {
    onCommand(command) {
        this.modLevel = modlevels.admin
        this.desc = desc
        this.cheat(cheat)
        init()
    }
}

/**
 * Sets the base level of [stat] to [level], along with the xp required for that level. Handles both
 * increases and decreases.
 *
 * Xp rates are explicitly ignored so that the resulting level always matches [level] exactly.
 */
@OptIn(InternalApi::class)
internal fun Player.setStatLevel(stat: UnpackedStatType, level: Int) {
    val xp = PlayerSkillXPTable.getXPFromLevel(level)
    val baseLevel = statMap.getBaseLevel(stat)
    val targetLevel = max(stat.minLevel, level)
    if (baseLevel > targetLevel) {
        statRevert(stat, targetLevel, xp)
        return
    }
    val xpDelta = xp - statMap.getXP(stat)
    statMap.setCurrentLevel(stat, targetLevel.toByte())
    statAdvance(stat, xpDelta.toDouble(), rate = 1.0, globalRate = 1.0)
}

// There is, by design, no helper function to decrease stat xp, as xp reduction is not a
// standard operation in normal gameplay.
@OptIn(InternalApi::class)
private fun Player.statRevert(stat: StatType, targetLevel: Int, targetXp: Int) {
    statMap.setCurrentLevel(stat, statMap.getBaseLevel(stat))
    val levelDelta = stat(stat) - targetLevel
    require(levelDelta > 0) { "This function can only be used to reduce stat levels." }
    statMap.setXP(stat, targetXp)
    statMap.setBaseLevel(stat, targetLevel.toByte())
    statSub(stat, constant = levelDelta, percent = 0)
    appearance.combatLevel = PlayerSkillXP.calculateCombatLevel(this)
    PlayerInterfaceUpdates.updateCombatLevel(this)
}

/**
 * Resolves [arg] into a type id from [names], accepting either a literal id or a debug name.
 *
 * @return `null` if [arg] is not an id and has no matching entry in [names].
 */
internal fun resolveArgTypeId(arg: String, names: Map<String, Int>): Int? {
    val argAsInt = arg.toIntOrNull()
    if (argAsInt != null) {
        return argAsInt
    }
    val sanitized = arg.replace("-", "_")
    return names[sanitized]
}

/** Corrects [name] into the closest matching entry in [names], if one is close enough. */
internal fun resolveTypeName(name: String, names: Map<String, Int>): String =
    when {
        name in names -> name
        name.toIntOrNull() != null -> name
        else -> findClosestNameMatch(name, names.keys) ?: name
    }

internal fun List<String>.asTypeNameAndNumber(defaultNumber: Number): Pair<String, String> =
    if (size > 1 && last().toLongOrNull() != null) {
        dropLast(1).joinToString("_") to last()
    } else {
        joinToString("_") to defaultNumber.toString()
    }

internal fun List<String>.asTypeName(): String = joinToString("_")

internal fun findClosestNameMatch(input: String, names: Iterable<String>): String? {
    val normalizedInput = input.replace("_", " ")

    var bestMatchScore = 0.0f
    var bestMatchName: String? = null
    for (name in names) {
        val score = levenshteinMetric.compare(normalizedInput, name.replace("_", " "))
        if (score > bestMatchScore) {
            bestMatchScore = score
            bestMatchName = name
        }
    }

    return if (bestMatchScore >= 0.5) bestMatchName else null
}
