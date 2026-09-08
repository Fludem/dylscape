package org.rsmod.content.skills.fishing.configs

import org.rsmod.api.type.refs.seq.SeqReferences

/**
 * The five fishing animations, one per method. Upstream's [org.rsmod.api.config.refs.BaseSeqs]
 * exposes only the fishing skillcape emote, so all of them are declared here.
 *
 * Both rod methods share [rod]: the cache has a separate `human_fish_onspot` used for scripted
 * cutscene fishing, which is deliberately not used.
 */
object FishingSeqs : SeqReferences() {
    val small_net = find("human_smallnet")
    val big_net = find("human_largenet")
    val rod = find("human_fishing_casting")
    val cage = find("human_lobster")
    val harpoon = find("human_harpoon")
}
