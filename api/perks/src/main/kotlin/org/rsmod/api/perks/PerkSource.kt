package org.rsmod.api.perks

import org.rsmod.game.entity.Player

/**
 * Something that can grant [Perk]s, bound into a Guice set with `addSetBinding<PerkSource>` from a
 * plugin module. [Perks] asks every bound source.
 */
fun interface PerkSource {
    fun Player.has(perk: Perk): Boolean
}
