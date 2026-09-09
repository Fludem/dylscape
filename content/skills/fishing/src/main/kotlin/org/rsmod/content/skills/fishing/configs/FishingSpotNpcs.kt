package org.rsmod.content.skills.fishing.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType

/**
 * Every standard fishing spot in the rev 233 cache.
 *
 * Spots are npcs, named `<mapsquare>_<archetype>fish` — `freshfish` for lure/bait, `saltfish` for
 * net/bait, `rarefish` for cage/harpoon and `memberfish` for big net/harpoon — plus the four
 * unprefixed generics and Tutorial Island's `newbiefishing`. The list is the complete set the cache
 * carries and was read out of it rather than typed from a wiki, so it stays right as long as the
 * revision does.
 *
 * Only the Lumbridge ones are reachable today: npc spawns are authored, not read from the cache,
 * and `LumbridgeNpcSpawns` is currently the only spawn set in the world. It already places five
 * `0_50_49_saltfish` in the swamp river and two `0_50_50_freshfish` in the Lum, so shrimps through
 * salmon are all fishable now; the rest come alive the moment their region gets spawns.
 *
 * Deliberately excluded: `brut_fishing_spot` (barbarian fishing), the Tempoross, minnow, Camdozaal
 * and raid spots, and the quest one-offs. Those have mechanics of their own, and tagging them into
 * the group would make them behave like ordinary spots, which is worse than them not working yet.
 */
object FishingSpotNpcs : NpcReferences() {
    private val names =
        listOf(
            "0_18_57_memberfish",
            "0_18_58_memberfish",
            "0_19_48_freshfish",
            "0_19_49_memberfish",
            "0_19_53_freshfish",
            "0_19_57_freshfish",
            "0_19_58_memberfish",
            "0_20_152_freshfish",
            "0_20_46_saltfish",
            "0_20_47_saltfish",
            "0_20_52_freshfish",
            "0_21_44_rarefish",
            "0_21_46_saltfish",
            "0_21_51_freshfish",
            "0_22_52_freshfish",
            "0_23_53_memberfish",
            "0_23_53_rarefish",
            "0_23_53_saltfish",
            "0_24_46_memberfish",
            "0_24_46_rarefish",
            "0_24_49_freshfish",
            "0_24_51_memberfish",
            "0_24_51_saltfish",
            "0_24_53_saltfish",
            "0_24_55_freshfish",
            "0_25_50_freshfish",
            "0_25_55_freshfish",
            "0_26_54_memberfish",
            "0_26_54_rarefish",
            "0_26_54_saltfish",
            "0_26_56_freshfish",
            "0_26_57_freshfish",
            "0_27_46_memberfish",
            "0_27_46_saltfish",
            "0_27_59_rarefish",
            "0_27_59_saltfish",
            "0_28_56_memberfish",
            "0_28_56_rarefish",
            "0_28_56_saltfish",
            "0_33_43_saltfish",
            "0_33_51_memberfish",
            "0_33_52_memberfish",
            "0_33_52_rarefish",
            "0_34_50_freshfish",
            "0_34_53_memberfish",
            "0_34_53_rarefish",
            "0_35_44_memberfish",
            "0_35_46_rarefish",
            "0_35_50_freshfish",
            "0_37_53_freshfish",
            "0_38_45_rarefish",
            "0_38_49_freshfish",
            "0_39_44_saltfish",
            "0_39_53_freshfish",
            "0_39_55_saltfish",
            "0_40_52_freshfish",
            "0_40_53_memberfish",
            "0_40_53_rarefish",
            "0_40_60_rarefish",
            "0_41_57_memberfish",
            "0_41_57_rarefish",
            "0_41_57_saltfish",
            "0_41_73_freshfish",
            "0_42_42_memberfish",
            "0_42_55_freshfish",
            "0_43_42_memberfish",
            "0_43_51_saltfish",
            "0_44_46_freshfish",
            "0_44_52_freshfish",
            "0_44_52_saltfish",
            "0_44_53_memberfish",
            "0_44_53_rarefish",
            "0_44_53_saltfish",
            "0_45_49_rarefish",
            "0_45_49_saltfish",
            "0_46_49_saltfish",
            "0_47_57_saltfish",
            "0_48_48_newbiefishing",
            "0_48_50_saltfish",
            "0_48_53_freshfish",
            "0_49_43_rarefish",
            "0_50_49_saltfish",
            "0_50_50_freshfish",
            "0_51_49_saltfish",
            "0_52_149_freshfish",
            "0_54_49_memberfish",
            "0_55_49_memberfish",
            "52_59_rarefish",
            "freshfish",
            "memberfish",
            "rarefish",
            "saltfish",
        )

    /** Keyed by internal name so the editor and the tests can both walk the whole set. */
    val all: Map<String, NpcType> = names.associateWith { find(it) }

    val newbie: NpcType = all.getValue("0_48_48_newbiefishing")
}

/**
 * Tags every spot into [FishingContent.fishing_spot], which is the only thing the script needs from
 * the cache — it reads the spot's own op text to work out what each option does, so there is no
 * per-spot configuration to get wrong here.
 */
internal object FishingSpotEditor : NpcEditor() {
    init {
        for (spot in FishingSpotNpcs.all.values) {
            edit(spot) { contentGroup = FishingContent.fishing_spot }
        }
        // Tutorial Island's spot is the one spot a player is *sent* to, by hint arrow, on a pond
        // small enough that the cache-default five-tile wander walks it off the water entirely.
        // Pinned here rather than in a second editor because `contentGroup` above already claims
        // this type, and two editors on one type is what makes edits fight.
        edit(FishingSpotNpcs.newbie) {
            moveRestrict = nomove
            wanderRange = 0
        }
    }
}
