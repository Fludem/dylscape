package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.refs.seq.SeqReferences

internal object HunterSeqs : SeqReferences() {
    /** Crouching to place a bird snare, box trap or deadfall. */
    val setting_trap = find("hunting_setting_trap_small")

    /** Reaching up to tie a rope or hang a net on a young tree. */
    val setting_sapling_trap = find("hunting_setting_sapling_trap")

    /** Pulling a net back down off a young tree. */
    val dismantle_net = find("human_hunting_dismantle_net")
}
