package org.rsmod.content.skills.prayer.configs

import org.rsmod.api.type.refs.seq.SeqReferences

internal object PrayerSeqs : SeqReferences() {
    /** The crouch-and-dig animation OSRS uses for burying. */
    val bury = find("human_pickupfloor")

    /**
     * Scattering ashes has its own animation live; the cache exposes no symbol for it at this
     * revision, so the bury crouch stands in. Purely cosmetic - timings and xp are unaffected.
     */
    val scatter = find("human_pickupfloor")

    val pray = find("human_pray")
}
