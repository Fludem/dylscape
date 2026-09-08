package org.rsmod.content.skills.mining.configs

import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * Per-pickaxe swing animations. Upstream's [org.rsmod.api.config.refs.BaseSeqs] exposes only the
 * mining skillcape emote, so the whole ladder is declared here.
 *
 * Every tier has its own cache animation. The `_wall`, `_noreachforward`, `sang_` and `zalcano_`
 * variants that also exist are for fixed-camera or scripted mining and are deliberately not used.
 */
internal object MiningSeqs : SeqReferences() {
    val bronze = find("human_mining_bronze_pickaxe")
    val iron = find("human_mining_iron_pickaxe")
    val steel = find("human_mining_steel_pickaxe")
    val black = find("human_mining_black_pickaxe")
    val mithril = find("human_mining_mithril_pickaxe")
    val adamant = find("human_mining_adamant_pickaxe")
    val rune = find("human_mining_rune_pickaxe")
    val gilded = find("human_mining_gilded_pickaxe")
    val dragon = find("human_mining_dragon_pickaxe")
    val dragon_upgraded = find("human_mining_dragon_pickaxe_pretty")
    val third_age = find("human_mining_3a_pickaxe")
    val infernal = find("human_mining_infernal_pickaxe")
    val crystal = find("human_mining_crystal_pickaxe")
}
