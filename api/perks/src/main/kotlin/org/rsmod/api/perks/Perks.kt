package org.rsmod.api.perks

import jakarta.inject.Inject
import org.rsmod.game.entity.Player

/**
 * The one question skills ask about perks: does this player have [Perk] right now.
 *
 * Shaped like `XpModifiers` and `InvisibleLevels`: the set is bound empty in core, so a server with
 * no perk sources at all injects this and simply gets `false` everywhere.
 */
class Perks @Inject constructor(private val sources: Set<PerkSource>) {
    fun has(player: Player, perk: Perk): Boolean = sources.any { it.has(player, perk) }

    private fun PerkSource.has(player: Player, perk: Perk): Boolean = player.has(perk)
}
