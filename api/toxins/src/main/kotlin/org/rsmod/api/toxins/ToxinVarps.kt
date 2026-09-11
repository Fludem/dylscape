package org.rsmod.api.toxins

import org.rsmod.api.type.refs.varp.VarpReferences
import org.rsmod.game.type.varp.VarpType

internal object ToxinVarps : VarpReferences() {
    /**
     * Varp 102, the whole poison state in one int. `orbs_update_health` (clientscript 446) reads it
     * straight: `>= 1_000_000` draws the venom orb, `> 0` the poison orb, anything else the normal
     * one. It is already `Perm` scope and `OnSetAlways` transmit in the cache, so the state
     * survives a logout and the orb follows every write with no editor of ours. See [Toxins] for
     * the rest of the encoding.
     */
    val poison: VarpType = find("poison")
}
