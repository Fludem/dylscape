package org.rsmod.content.custom.knightwaves.configs

import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences

typealias knightwaves_npcs = KnightWavesNpcs

object KnightWavesNpcs : NpcReferences() {
    val squire = find("kr_squire")

    /**
     * The six opponents, in the order they are faced. These are the real Knight Waves knights and
     * they already climb in difficulty across the set, from Sir Bedivere at combat 110 to Sir
     * Gawain at 122 - so the order here is simply their cache order.
     */
    val waves =
        listOf(
            find("kr_knight1"), // Sir Bedivere
            find("kr_knight2"), // Sir Pelleas
            find("kr_knight3"), // Sir Tristram
            find("kr_knight4"), // Sir Palomedes
            find("kr_knight5"), // Sir Lucan
            find("kr_knight6"), // Sir Gawain
        )
}

internal object KnightWavesNpcEditor : NpcEditor() {
    init {
        edit(knightwaves_npcs.squire) { moveRestrict = indoors }

        for (knight in knightwaves_npcs.waves) {
            edit(knight) {
                // Two jobs. It stops the knight roaming the hall - every npc wanders five tiles
                // from its spawn unless told otherwise - and it disables the retreat branch in
                // `NpcExtensions.retaliate`, which only fires when `wanderRange > 0` and would
                // otherwise send a knight jogging home the moment the fight moved a few tiles.
                wanderRange = 0
                maxRange = 32
                defaultMode = none
                // Charges on sight, so the wave starts itself. `findNewMode` is `OpPlayer2`.
                huntMode = huntmodes.aggressive_melee
                huntRange = 10
            }
        }
    }
}
