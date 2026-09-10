package org.rsmod.content.skills.fishing.configs

import org.rsmod.api.type.refs.obj.ObjReferences

/**
 * Objs this module needs. [org.rsmod.api.config.refs.BaseObjs] exposes only `raw_herring` out of
 * the whole skill, so tools and fish are both declared here.
 *
 * Names are cache symbols resolved by the type resolver at boot, so a typo fails the build rather
 * than silently binding the wrong item. Note the cache calls the small fishing net simply `net` and
 * the raw shrimps `raw_shrimp`, singular, despite the in-game name being plural.
 */
object FishingObjs : ObjReferences() {
    // Tools.
    val net = find("net")
    val big_net = find("big_net")
    val fishing_rod = find("fishing_rod")
    val fly_fishing_rod = find("fly_fishing_rod")
    val lobster_pot = find("lobster_pot")
    val harpoon = find("harpoon")

    /**
     * The Animal Wrangler league relic's "Echo harpoon". For a player holding `Perk.EchoHarpoon` it
     * stands in for every fishing tool.
     */
    val echo_harpoon = find("league_trailblazer_harpoon")

    // Consumed one per catch, not one per attempt.
    val fishing_bait = find("fishing_bait")
    val feather = find("feather")

    // Fish.
    val raw_shrimp = find("raw_shrimp")
    val raw_sardine = find("raw_sardine")
    val raw_herring = find("raw_herring")
    val raw_anchovies = find("raw_anchovies")
    val raw_mackerel = find("raw_mackerel")
    val raw_trout = find("raw_trout")
    val raw_cod = find("raw_cod")
    val raw_pike = find("raw_pike")
    val raw_salmon = find("raw_salmon")
    val raw_tuna = find("raw_tuna")
    val raw_lobster = find("raw_lobster")
    val raw_bass = find("raw_bass")
    val raw_swordfish = find("raw_swordfish")
    val raw_shark = find("raw_shark")

    /**
     * Tutorial Island has its own shrimps, distinct from the world's. They exist so the tutorial's
     * cooking step can hand out an item that cannot be carried out and sold, and the newbie fishing
     * spot yields these rather than [raw_shrimp].
     */
    val newbie_raw_shrimp = find("newbieraw_shrimp")
}
