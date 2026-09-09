package org.rsmod.api.player.ui

import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.stats
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.stat.StatType

/**
 * The `levelup_display` interface keeps one hidden layer per skill, each holding that skill's icon.
 *
 * _Note: Hunter has no layer in the interface, so it resolves to `null` and its dialogue is shown
 * without an icon._
 */
internal object LevelUpComponents {
    private val layers: Map<Int, ComponentType> by lazy {
        mapOf(
            stats.agility.id to components.levelup_agility,
            stats.attack.id to components.levelup_attack,
            stats.construction.id to components.levelup_construction,
            stats.cooking.id to components.levelup_cooking,
            stats.crafting.id to components.levelup_crafting,
            stats.defence.id to components.levelup_defence,
            stats.farming.id to components.levelup_farming,
            stats.firemaking.id to components.levelup_firemaking,
            stats.fishing.id to components.levelup_fishing,
            stats.fletching.id to components.levelup_fletching,
            stats.herblore.id to components.levelup_herblore,
            stats.hitpoints.id to components.levelup_hitpoints,
            stats.magic.id to components.levelup_magic,
            stats.mining.id to components.levelup_mining,
            stats.prayer.id to components.levelup_prayer,
            stats.ranged.id to components.levelup_ranging,
            stats.runecrafting.id to components.levelup_runecrafting,
            stats.slayer.id to components.levelup_slayer,
            stats.smithing.id to components.levelup_smithing,
            stats.strength.id to components.levelup_strength,
            stats.thieving.id to components.levelup_thieving,
            stats.woodcutting.id to components.levelup_woodcutting,
        )
    }

    operator fun get(stat: StatType): ComponentType? = layers[stat.id]
}
