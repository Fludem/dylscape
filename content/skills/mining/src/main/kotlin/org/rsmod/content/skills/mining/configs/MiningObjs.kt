package org.rsmod.content.skills.mining.configs

import org.rsmod.api.type.refs.obj.ObjReferences

/**
 * Objs this module needs that [org.rsmod.api.config.refs.BaseObjs] does not expose.
 *
 * Upstream publishes an oddly-shaped slice of the pickaxe ladder — bronze, dragon and everything
 * cosmetic above it, but none of the ordinary mid-tiers — so the gaps are filled here rather than
 * by editing a shared upstream file. Names are cache symbols, resolved by the type resolver at
 * boot, so a typo fails the build instead of silently binding the wrong item.
 */
internal object MiningObjs : ObjReferences() {
    val iron_pickaxe = find("iron_pickaxe")
    val steel_pickaxe = find("steel_pickaxe")
    val black_pickaxe = find("black_pickaxe")
    val mithril_pickaxe = find("mithril_pickaxe")
    val adamant_pickaxe = find("adamant_pickaxe")
    val rune_pickaxe = find("rune_pickaxe")
    val gilded_pickaxe = find("trail_gilded_pickaxe")

    /**
     * The Power Miner league relic's "Echo pickaxe". It only works as a pickaxe for a player
     * holding `Perk.EchoPickaxe`, and then as a crystal pickaxe with no level requirement.
     */
    val echo_pickaxe = find("league_trailblazer_pickaxe")

    // Ores. `content.ore` exists upstream but nothing is tagged into it, so these are also what
    // `MiningOres` uses to populate that group.
    val clay = find("clay")
    val copper_ore = find("copper_ore")
    val tin_ore = find("tin_ore")
    val iron_ore = find("iron_ore")
    val silver_ore = find("silver_ore")
    val coal = find("coal")
    val gold_ore = find("gold_ore")
    val mithril_ore = find("mithril_ore")
    val adamantite_ore = find("adamantite_ore")
    val runite_ore = find("runite_ore")
    val amethyst = find("amethyst")
}
