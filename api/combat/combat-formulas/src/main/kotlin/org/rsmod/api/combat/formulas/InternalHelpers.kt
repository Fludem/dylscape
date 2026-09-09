package org.rsmod.api.combat.formulas

import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.varps
import org.rsmod.game.entity.Player
import org.rsmod.game.type.npc.UnpackedNpcType

/**
 * Hit chance formulas internally use decimals (e.g., `1%` = `0.01`, `100%` = `1.0`). To maintain
 * consistency with other combat formulas that use whole integers, we scale them using this
 * constant.
 */
internal const val HIT_CHANCE_SCALE: Int = 10_000

internal fun scale(base: Int, multiplier: Int, divisor: Int): Int = (base * multiplier) / divisor

/**
 * Whether [player] is currently assigned this npc, which is what every black mask, slayer helm and
 * slayer staff(e) bonus in the melee, ranged and magic formulas is gated on.
 *
 * Both halves come out of the cache, so this needs nothing from the slayer content module: npcs
 * carry their task in the `slayer_task` param and the player's task is in `slayer_target`, and the
 * two share an id space. Zero is the param's own default, so a monster that is nobody's task is
 * never on task even if the player somehow has task 0 assigned.
 */
internal fun UnpackedNpcType.isSlayerTask(player: Player): Boolean {
    val task = param(params.slayer_task)
    return task != 0 && task == player.vars[varps.slayer_target]
}
