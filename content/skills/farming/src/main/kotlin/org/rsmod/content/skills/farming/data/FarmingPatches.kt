package org.rsmod.content.skills.farming.data

import org.rsmod.content.skills.farming.configs.FarmingLocs
import org.rsmod.content.skills.farming.configs.FarmingVarBits
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.varbit.VarBitType
import org.rsmod.map.CoordGrid

/**
 * Every farming patch in the game, with the loc(s) it is placed as and the varbit that draws it.
 *
 * A patch is keyed by its **primary loc id**, which is what the saved state is stored against. All
 * but one patch is a single loc; the Farming Guild redwood is nineteen -- nine on the ground, nine
 * on the level above and one above that -- sharing `farming_transmit_i`, so it is one logical patch
 * that happens to be clickable in nineteen places.
 *
 * [varbit] repeats down the list on purpose. Only fourteen transmit varbits exist for eighty-odd
 * patches, and Jagex reuses them between farms that can never be on screen at once; [coords] is
 * what lets `FarmingSyncScript` pick the right patch for each one.
 *
 * Both the loc ids and the coords were read out of the game cache -- the multiloc tables in the loc
 * archive and the map's own loc placements -- rather than transcribed from a wiki.
 */
public class FarmingPatch(
    public val kind: PatchKind,
    public val locs: List<LocType>,
    public val varbit: VarBitType,
    /** Roughly the middle of the patch, used to decide which patch a transmit varbit belongs to. */
    public val coords: CoordGrid,
) {
    public val primary: LocType
        get() = locs.first()

    public val key: Int
        get() = primary.id
}

public object FarmingPatches {
    public val all: List<FarmingPatch> =
        listOf(
            // allotment
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3052, 3309, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_2),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3057, 3305, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_3),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2809, 3467, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_4),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(2809, 3460, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_5),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2666, 3378, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_6),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(2666, 3371, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_7),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3599, 3527, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_8),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3604, 3523, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_9),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3794, 2835, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_10),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1736, 3556, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_11),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(1732, 3552, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_13),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(1269, 3725, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_12),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(1269, 3734, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_15),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3290, 6096, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_14),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3290, 6102, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_17),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(1587, 3096, 0),
            ),
            FarmingPatch(
                PatchKind.Allotment,
                listOf(FarmingLocs.farming_veg_patch_16),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1583, 3100, 0),
            ),
            // flower
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_1),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(3054, 3307, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_2),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(2809, 3463, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_3),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(2666, 3374, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_4),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(3601, 3525, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_5),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(1734, 3554, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_6),
                FarmingVarBits.farming_transmit_h,
                CoordGrid(1260, 3725, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_7),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(3292, 6099, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_8),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(1585, 3098, 0),
            ),
            FarmingPatch(
                PatchKind.Flower,
                listOf(FarmingLocs.farming_flower_patch_9),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(1352, 3022, 0),
            ),
            // herb
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_1),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(3058, 3311, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_2),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(2813, 3463, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_3),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(2670, 3374, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_4),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(3605, 3529, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_5),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3789, 2837, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_6),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(1738, 3550, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_7),
                FarmingVarBits.farming_transmit_e,
                CoordGrid(1238, 3726, 0),
            ),
            FarmingPatch(
                PatchKind.Herb,
                listOf(FarmingLocs.farming_herb_patch_8),
                FarmingVarBits.farming_transmit_d,
                CoordGrid(1581, 3094, 0),
            ),
            // hops
            FarmingPatch(
                PatchKind.Hops,
                listOf(FarmingLocs.farming_hops_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2575, 3104, 0),
            ),
            FarmingPatch(
                PatchKind.Hops,
                listOf(FarmingLocs.farming_hops_patch_2),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2810, 3336, 0),
            ),
            FarmingPatch(
                PatchKind.Hops,
                listOf(FarmingLocs.farming_hops_patch_3),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3229, 3315, 0),
            ),
            FarmingPatch(
                PatchKind.Hops,
                listOf(FarmingLocs.farming_hops_patch_4),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2666, 3525, 0),
            ),
            FarmingPatch(
                PatchKind.Hops,
                listOf(FarmingLocs.farming_hops_patch_5),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1364, 2938, 0),
            ),
            // bush
            FarmingPatch(
                PatchKind.Bush,
                listOf(FarmingLocs.farming_bush_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3181, 3357, 0),
            ),
            FarmingPatch(
                PatchKind.Bush,
                listOf(FarmingLocs.farming_bush_patch_2),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2940, 3221, 0),
            ),
            FarmingPatch(
                PatchKind.Bush,
                listOf(FarmingLocs.farming_bush_patch_3),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2591, 3863, 0),
            ),
            FarmingPatch(
                PatchKind.Bush,
                listOf(FarmingLocs.farming_bush_patch_4),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2617, 3225, 0),
            ),
            FarmingPatch(
                PatchKind.Bush,
                listOf(FarmingLocs.farming_bush_patch_5),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(1260, 3733, 0),
            ),
            // fruit tree
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_1),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(2475, 3445, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_2),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2489, 3179, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_3),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2764, 3212, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_4),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2860, 3433, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_5),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2570, 4633, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_6),
                FarmingVarBits.farming_transmit_k,
                CoordGrid(1242, 3758, 0),
            ),
            FarmingPatch(
                PatchKind.FruitTree,
                listOf(FarmingLocs.farming_fruit_tree_patch_7),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(1349, 3056, 0),
            ),
            // tree
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2935, 3437, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_2),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3003, 3372, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_3),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3580, 4194, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_4),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3192, 3230, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_5),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2435, 3414, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_6),
                FarmingVarBits.farming_transmit_g,
                CoordGrid(1231, 3735, 0),
            ),
            FarmingPatch(
                PatchKind.Tree,
                listOf(FarmingLocs.farming_tree_patch_7),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1365, 3320, 0),
            ),
            // hardwood
            FarmingPatch(
                PatchKind.Hardwood,
                listOf(FarmingLocs.farming_hardwood_tree_patch_2),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3707, 3832, 0),
            ),
            FarmingPatch(
                PatchKind.Hardwood,
                listOf(FarmingLocs.farming_hardwood_tree_patch_3),
                FarmingVarBits.farming_transmit_c,
                CoordGrid(3701, 3836, 0),
            ),
            FarmingPatch(
                PatchKind.Hardwood,
                listOf(FarmingLocs.farming_hardwood_tree_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3714, 3834, 0),
            ),
            FarmingPatch(
                PatchKind.Hardwood,
                listOf(FarmingLocs.farming_hardwood_tree_patch_4),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1686, 2971, 0),
            ),
            // cactus
            FarmingPatch(
                PatchKind.Cactus,
                listOf(FarmingLocs.farming_cactus_patch),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3315, 3202, 0),
            ),
            FarmingPatch(
                PatchKind.Cactus,
                listOf(FarmingLocs.farming_cactus_patch_2),
                FarmingVarBits.farming_transmit_f,
                CoordGrid(1264, 3747, 0),
            ),
            // calquat
            FarmingPatch(
                PatchKind.Calquat,
                listOf(FarmingLocs.farming_calquat_tree_patch),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(2379, 4220, 0),
            ),
            FarmingPatch(
                PatchKind.Calquat,
                listOf(FarmingLocs.farming_calquat_tree_patch_2),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1366, 3031, 0),
            ),
            // mushroom
            FarmingPatch(
                PatchKind.Mushroom,
                listOf(FarmingLocs.farming_mushroom_patch),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3451, 3472, 0),
            ),
            // belladonna
            FarmingPatch(
                PatchKind.Belladonna,
                listOf(FarmingLocs.farming_belladonna_patch),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3086, 3354, 0),
            ),
            FarmingPatch(
                PatchKind.Belladonna,
                listOf(FarmingLocs.farming_belladonna_patch_2),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(1450, 3354, 0),
            ),
            // seaweed
            FarmingPatch(
                PatchKind.Seaweed,
                listOf(FarmingLocs.farming_seaweed_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3733, 10273, 1),
            ),
            FarmingPatch(
                PatchKind.Seaweed,
                listOf(FarmingLocs.farming_seaweed_patch_2),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(3733, 10267, 1),
            ),
            // spirit tree
            FarmingPatch(
                PatchKind.SpiritTree,
                listOf(FarmingLocs.farming_spirit_tree_patch_1),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(3059, 3257, 0),
            ),
            FarmingPatch(
                PatchKind.SpiritTree,
                listOf(FarmingLocs.farming_spirit_tree_patch_2),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(2612, 3857, 0),
            ),
            FarmingPatch(
                PatchKind.SpiritTree,
                listOf(FarmingLocs.farming_spirit_tree_patch_3),
                FarmingVarBits.farming_transmit_b,
                CoordGrid(2801, 3202, 0),
            ),
            FarmingPatch(
                PatchKind.SpiritTree,
                listOf(FarmingLocs.farming_spirit_tree_patch_4),
                FarmingVarBits.farming_transmit_f,
                CoordGrid(1692, 3541, 0),
            ),
            FarmingPatch(
                PatchKind.SpiritTree,
                listOf(FarmingLocs.farming_spirit_tree_patch_5),
                FarmingVarBits.farming_transmit_a,
                CoordGrid(1252, 3749, 0),
            ),
            // celastrus
            FarmingPatch(
                PatchKind.Celastrus,
                listOf(FarmingLocs.farming_celastrus_patch_1),
                FarmingVarBits.farming_transmit_l,
                CoordGrid(1243, 3749, 0),
            ),
            // redwood
            FarmingPatch(
                PatchKind.Redwood,
                listOf(
                    FarmingLocs.farming_redwood_tree_patch_0_1,
                    FarmingLocs.farming_redwood_tree_patch_0_2,
                    FarmingLocs.farming_redwood_tree_patch_0_3,
                    FarmingLocs.farming_redwood_tree_patch_0_4,
                    FarmingLocs.farming_redwood_tree_patch_0_5,
                    FarmingLocs.farming_redwood_tree_patch_0_6,
                    FarmingLocs.farming_redwood_tree_patch_0_7,
                    FarmingLocs.farming_redwood_tree_patch_0_8,
                    FarmingLocs.farming_redwood_tree_patch_0_9,
                    FarmingLocs.farming_redwood_tree_patch_1_1,
                    FarmingLocs.farming_redwood_tree_patch_1_2,
                    FarmingLocs.farming_redwood_tree_patch_1_3,
                    FarmingLocs.farming_redwood_tree_patch_1_4,
                    FarmingLocs.farming_redwood_tree_patch_1_5,
                    FarmingLocs.farming_redwood_tree_patch_1_6,
                    FarmingLocs.farming_redwood_tree_patch_1_7,
                    FarmingLocs.farming_redwood_tree_patch_1_8,
                    FarmingLocs.farming_redwood_tree_patch_1_9,
                    FarmingLocs.farming_redwood_tree_patch_2_1,
                ),
                FarmingVarBits.farming_transmit_i,
                CoordGrid(1225, 3756, 0),
            ),
            // anima
            FarmingPatch(
                PatchKind.Anima,
                listOf(FarmingLocs.farming_anima_patch_1),
                FarmingVarBits.farming_transmit_m,
                CoordGrid(1231, 3722, 0),
            ),
            // crystal
            FarmingPatch(
                PatchKind.Crystal,
                listOf(FarmingLocs.farming_crystal_tree_patch_1),
                FarmingVarBits.farming_transmit_e,
                CoordGrid(3291, 6118, 0),
            ),
            // hespori
            FarmingPatch(
                PatchKind.Hespori,
                listOf(FarmingLocs.hespori_patch),
                FarmingVarBits.farming_transmit_j,
                CoordGrid(1246, 10086, 0),
            ),
            // grape
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_01),
                FarmingVarBits.farming_transmit_a1,
                CoordGrid(1808, 3562, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_02),
                FarmingVarBits.farming_transmit_a2,
                CoordGrid(1808, 3560, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_03),
                FarmingVarBits.farming_transmit_b1,
                CoordGrid(1808, 3558, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_04),
                FarmingVarBits.farming_transmit_b2,
                CoordGrid(1808, 3553, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_05),
                FarmingVarBits.farming_transmit_c1,
                CoordGrid(1808, 3551, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_06),
                FarmingVarBits.farming_transmit_c2,
                CoordGrid(1808, 3549, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_07),
                FarmingVarBits.farming_transmit_d1,
                CoordGrid(1807, 3562, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_08),
                FarmingVarBits.farming_transmit_d2,
                CoordGrid(1807, 3560, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_09),
                FarmingVarBits.farming_transmit_e1,
                CoordGrid(1807, 3558, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_10),
                FarmingVarBits.farming_transmit_e2,
                CoordGrid(1807, 3553, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_11),
                FarmingVarBits.farming_transmit_f1,
                CoordGrid(1807, 3551, 0),
            ),
            FarmingPatch(
                PatchKind.Grape,
                listOf(FarmingLocs.grapevine_patch_clickzone_12),
                FarmingVarBits.farming_transmit_f2,
                CoordGrid(1807, 3549, 0),
            ),
        )

    /**
     * Every loc id that carries the patch op hooks, mapped to the patch it belongs to. Lazy: a
     * reference has no id until the server binds it at boot.
     */
    public val byLoc: Map<Int, FarmingPatch> by lazy {
        buildMap {
            for (patch in all) {
                for (loc in patch.locs) {
                    put(loc.id, patch)
                }
            }
        }
    }

    /** The patches sharing each transmit varbit, which is what the sync pass walks. */
    public val byVarBit: Map<Int, List<FarmingPatch>> by lazy { all.groupBy { it.varbit.id } }

    public fun of(kind: PatchKind): List<FarmingPatch> = all.filter { it.kind == kind }
}
