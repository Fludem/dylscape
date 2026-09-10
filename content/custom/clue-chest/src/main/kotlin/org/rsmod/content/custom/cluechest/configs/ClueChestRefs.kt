package org.rsmod.content.custom.cluechest.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.content.custom.cluechest.ClueTier

object ClueChestLocs : LocReferences() {
    /**
     * The "Loot Chest" in the Edgeville bank, placed by the map at (3094, 3488, 0) and bound by
     * nothing else. A multiloc on `wildy_loot_chest_has_loot` over `wildy_hub_loot_chest_closed`
     * and `_open`, both of which carry `Loot` on op1. Handlers bind the parent; op and item-on-loc
     * dispatch both fall back to it from whichever face the player sees.
     */
    val loot_chest = find("wildy_hub_loot_chest_multi")
}

object ClueChestObjs : ObjReferences() {
    /** Ours: `.data/symbols/.local/obj.sym`, built by `ClueKeyBuilds`. */
    val key_beginner = find("trail_key_beginner")
    val key_easy = find("trail_key_easy")
    val key_hard = find("trail_key_hard")

    /** The one Key (medium) the drop tables hand out. */
    val key_medium = find("trail_clue_medium_riddle001_key")
    val key_elite = find("trail_elite_riddle_key32")

    /** Every other vanilla Key (medium): all are "Key (medium)", so all of them open the chest. */
    val key_medium_others =
        listOf(
            find("trail_clue_medium_riddle002_key"),
            find("trail_clue_medium_riddle003_key"),
            find("trail_clue_medium_riddle004_key"),
            find("trail_clue_medium_riddle005_key"),
            find("trail_clue_medium_riddle007_key"),
            find("trail_clue_medium_riddle008_key"),
            find("trail_clue_medium_riddle011_key"),
            find("trail_clue_medium_riddle012_key"),
            find("trail_clue_medium_riddle014_key"),
            find("trail_clue_medium_riddle017_key"),
        )
}

object ClueChestInvs : InvReferences() {
    /** Inv 141, the clue reward inv. Barrows borrows it too, so it can hold another's leftovers. */
    val reward = find("trail_rewardinv")
}

object ClueChestInterfaces : InterfaceReferences() {
    val reward = find("trail_rewardscreen")
}

object ClueChestSeqs : SeqReferences() {
    val open_chest = find("human_openchest")
}

/**
 * Raw obj id -> tier. Keyed on `Int` because [ObjReferences.find] yields a `HashedObjType` and the
 * runtime hands scripts an `UnpackedObjType`, and those never compare equal.
 */
object ClueKeys {
    val tiers: Map<Int, ClueTier> by lazy {
        buildMap {
            put(ClueChestObjs.key_beginner.id, ClueTier.Beginner)
            put(ClueChestObjs.key_easy.id, ClueTier.Easy)
            put(ClueChestObjs.key_medium.id, ClueTier.Medium)
            for (key in ClueChestObjs.key_medium_others) {
                put(key.id, ClueTier.Medium)
            }
            put(ClueChestObjs.key_hard.id, ClueTier.Hard)
            put(ClueChestObjs.key_elite.id, ClueTier.Elite)
        }
    }
}
