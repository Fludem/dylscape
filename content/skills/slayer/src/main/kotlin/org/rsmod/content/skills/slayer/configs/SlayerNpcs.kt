@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.slayer.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences

internal typealias slayer_npcs = SlayerNpcs

/**
 * The six masters, keyed to the `master_id` their rows use in `slayer_master_task`.
 *
 * All six carry an identical op layout, which was decoded from the cache rather than assumed:
 * `Talk-to` on op1, nothing on op2, `Assignment` on op3, `Trade` on op4 and `Rewards` on op5. That
 * uniformity is why this module binds ops directly instead of routing everything through a dialogue
 * menu.
 *
 * Note the spellings. Turael is `tureal` in the cache, and the numbered `slayer_master_N` types
 * without a suffix (13651, 13652, 13654) are nameless op-less shells - the named `_tureal`,
 * `_mazchna` and `_duradel` variants are the ones that work.
 */
object SlayerNpcs : NpcReferences() {
    val turael = find("slayer_master_1_tureal")
    val mazchna = find("slayer_master_2_mazchna")
    val vannaka = find("slayer_master_3")
    val chaeldar = find("slayer_master_4")
    val duradel = find("slayer_master_5_duradel")
    val nieve = find("slayer_master_nieve")

    /** Master id in `slayer_master_task` -> the npc that assigns from that list. */
    val byMasterId: Map<Int, org.rsmod.game.type.npc.NpcType> =
        mapOf(1 to turael, 2 to mazchna, 3 to vannaka, 4 to chaeldar, 5 to duradel, 6 to nieve)

    val all: List<org.rsmod.game.type.npc.NpcType> = byMasterId.values.toList()
}

/**
 * Pins the masters to their posts.
 *
 * This is not optional decoration. The cache never writes RSMod's `wanderRange` opcode, so every
 * npc falls back to a five-tile wander box anchored on its spawn tile - more than enough to walk a
 * master out of the room a player was sent to.
 *
 * Only the range is pinned; `moveRestrict = indoors` is deliberately not set. Half of these six
 * stand outdoors - Mazchna in the Canifis square, Nieve out by the Stronghold tree - and confining
 * them to a building they are not in would be the wrong kind of stuck. With the range at zero there
 * is nothing left to restrict.
 */
internal object SlayerNpcEditor : NpcEditor() {
    init {
        for (master in slayer_npcs.all) {
            edit(master) { wanderRange = 0 }
        }
    }
}
