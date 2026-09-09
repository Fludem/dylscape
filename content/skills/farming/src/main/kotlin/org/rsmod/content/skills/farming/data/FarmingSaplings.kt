package org.rsmod.content.skills.farming.data

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

/**
 * Every obj in the plant-pot chain, declared as plain `val`s.
 *
 * They have to be declared here rather than built on the fly inside [FarmingSaplings], because the
 * boot-time resolver instantiates each `*References` object and reads the handles it created during
 * its own initialisation. A handle minted later -- the first time some other object is touched --
 * is never resolved, and shows up as a null id the moment a script reads it.
 */
public object FarmingSaplingObjs : ObjReferences() {
    public val acorn: ObjType = find("acorn")
    public val plantpot_acorn: ObjType = find("plantpot_acorn")
    public val plantpot_acorn_watered: ObjType = find("plantpot_acorn_watered")
    public val plantpot_oak_sapling: ObjType = find("plantpot_oak_sapling")
    public val willow_seed: ObjType = find("willow_seed")
    public val plantpot_willow_seed: ObjType = find("plantpot_willow_seed")
    public val plantpot_willow_seed_watered: ObjType = find("plantpot_willow_seed_watered")
    public val plantpot_willow_sapling: ObjType = find("plantpot_willow_sapling")
    public val maple_seed: ObjType = find("maple_seed")
    public val plantpot_maple_seed: ObjType = find("plantpot_maple_seed")
    public val plantpot_maple_seed_watered: ObjType = find("plantpot_maple_seed_watered")
    public val plantpot_maple_sapling: ObjType = find("plantpot_maple_sapling")
    public val yew_seed: ObjType = find("yew_seed")
    public val plantpot_yew_seed: ObjType = find("plantpot_yew_seed")
    public val plantpot_yew_seed_watered: ObjType = find("plantpot_yew_seed_watered")
    public val plantpot_yew_sapling: ObjType = find("plantpot_yew_sapling")
    public val magic_tree_seed: ObjType = find("magic_tree_seed")
    public val plantpot_magic_tree_seed: ObjType = find("plantpot_magic_tree_seed")
    public val plantpot_magic_tree_seed_watered: ObjType = find("plantpot_magic_tree_seed_watered")
    public val plantpot_magic_tree_sapling: ObjType = find("plantpot_magic_tree_sapling")
    public val spirit_tree_seed: ObjType = find("spirit_tree_seed")
    public val plantpot_spirit_tree_seed: ObjType = find("plantpot_spirit_tree_seed")
    public val plantpot_spirit_tree_seed_watered: ObjType =
        find("plantpot_spirit_tree_seed_watered")
    public val plantpot_spirit_tree_sapling: ObjType = find("plantpot_spirit_tree_sapling")
    public val apple_tree_seed: ObjType = find("apple_tree_seed")
    public val plantpot_apple_seed: ObjType = find("plantpot_apple_seed")
    public val plantpot_apple_seed_watered: ObjType = find("plantpot_apple_seed_watered")
    public val plantpot_apple_sapling: ObjType = find("plantpot_apple_sapling")
    public val banana_tree_seed: ObjType = find("banana_tree_seed")
    public val plantpot_banana_seed: ObjType = find("plantpot_banana_seed")
    public val plantpot_banana_seed_watered: ObjType = find("plantpot_banana_seed_watered")
    public val plantpot_banana_sapling: ObjType = find("plantpot_banana_sapling")
    public val orange_tree_seed: ObjType = find("orange_tree_seed")
    public val plantpot_orange_seed: ObjType = find("plantpot_orange_seed")
    public val plantpot_orange_seed_watered: ObjType = find("plantpot_orange_seed_watered")
    public val plantpot_orange_sapling: ObjType = find("plantpot_orange_sapling")
    public val curry_tree_seed: ObjType = find("curry_tree_seed")
    public val plantpot_curry_seed: ObjType = find("plantpot_curry_seed")
    public val plantpot_curry_seed_watered: ObjType = find("plantpot_curry_seed_watered")
    public val plantpot_curry_sapling: ObjType = find("plantpot_curry_sapling")
    public val pineapple_tree_seed: ObjType = find("pineapple_tree_seed")
    public val plantpot_pineapple_seed: ObjType = find("plantpot_pineapple_seed")
    public val plantpot_pineapple_seed_watered: ObjType = find("plantpot_pineapple_seed_watered")
    public val plantpot_pineapple_sapling: ObjType = find("plantpot_pineapple_sapling")
    public val papaya_tree_seed: ObjType = find("papaya_tree_seed")
    public val plantpot_papaya_seed: ObjType = find("plantpot_papaya_seed")
    public val plantpot_papaya_seed_watered: ObjType = find("plantpot_papaya_seed_watered")
    public val plantpot_papaya_sapling: ObjType = find("plantpot_papaya_sapling")
    public val palm_tree_seed: ObjType = find("palm_tree_seed")
    public val plantpot_palm_seed: ObjType = find("plantpot_palm_seed")
    public val plantpot_palm_seed_watered: ObjType = find("plantpot_palm_seed_watered")
    public val plantpot_palm_sapling: ObjType = find("plantpot_palm_sapling")
    public val dragonfruit_tree_seed: ObjType = find("dragonfruit_tree_seed")
    public val plantpot_dragonfruit_seed: ObjType = find("plantpot_dragonfruit_seed")
    public val plantpot_dragonfruit_seed_watered: ObjType =
        find("plantpot_dragonfruit_seed_watered")
    public val plantpot_dragonfruit_sapling: ObjType = find("plantpot_dragonfruit_sapling")
    public val calquat_tree_seed: ObjType = find("calquat_tree_seed")
    public val plantpot_calquat_seed: ObjType = find("plantpot_calquat_seed")
    public val plantpot_calquat_seed_watered: ObjType = find("plantpot_calquat_seed_watered")
    public val plantpot_calquat_sapling: ObjType = find("plantpot_calquat_sapling")
    public val teak_seed: ObjType = find("teak_seed")
    public val plantpot_teak_seed: ObjType = find("plantpot_teak_seed")
    public val plantpot_teak_seed_watered: ObjType = find("plantpot_teak_seed_watered")
    public val plantpot_teak_sapling: ObjType = find("plantpot_teak_sapling")
    public val mahogany_seed: ObjType = find("mahogany_seed")
    public val plantpot_mahogany_seed: ObjType = find("plantpot_mahogany_seed")
    public val plantpot_mahogany_seed_watered: ObjType = find("plantpot_mahogany_seed_watered")
    public val plantpot_mahogany_sapling: ObjType = find("plantpot_mahogany_sapling")
    public val celastrus_tree_seed: ObjType = find("celastrus_tree_seed")
    public val plantpot_celastrus_tree_seed: ObjType = find("plantpot_celastrus_tree_seed")
    public val plantpot_celastrus_tree_seed_watered: ObjType =
        find("plantpot_celastrus_tree_seed_watered")
    public val plantpot_celastrus_tree_sapling: ObjType = find("plantpot_celastrus_tree_sapling")
    public val redwood_tree_seed: ObjType = find("redwood_tree_seed")
    public val plantpot_redwood_tree_seed: ObjType = find("plantpot_redwood_tree_seed")
    public val plantpot_redwood_tree_seed_watered: ObjType =
        find("plantpot_redwood_tree_seed_watered")
    public val plantpot_redwood_tree_sapling: ObjType = find("plantpot_redwood_tree_sapling")
    public val crystal_tree_seed: ObjType = find("crystal_tree_seed")
    public val plantpot_crystal_tree_seed: ObjType = find("plantpot_crystal_tree_seed")
    public val plantpot_crystal_tree_seed_watered: ObjType =
        find("plantpot_crystal_tree_seed_watered")
    public val plantpot_crystal_tree_sapling: ObjType = find("plantpot_crystal_tree_sapling")
}

/**
 * The plant-pot chain that turns a tree seed into a sapling.
 *
 * Nothing that goes in a tree, fruit tree, hardwood, calquat, celastrus, redwood, crystal or spirit
 * tree patch is a seed: they all take a *sapling*, and the only way to get one is to grow it in a
 * pot of soil first. Without this, half the patches in [FarmingPatches] would have nothing that
 * could be planted in them.
 *
 * The chain is the cache's own, and its names spell the steps out: `plantpot_compost` ->
 * `plantpot_oak_seed` -> `plantpot_oak_seed_watered` -> `plantpot_oak_sapling`. Only the last of
 * those is what a patch accepts.
 */
public class SaplingChain(
    public val seed: ObjType,
    public val potted: ObjType,
    public val watered: ObjType,
    public val sapling: ObjType,
    public val level: Int,
    /** Vanilla gives a little Farming xp for potting the seed. */
    public val potXp: Double,
)

public object FarmingSaplings {
    public val all: List<SaplingChain> =
        listOf(
            // oak
            SaplingChain(
                seed = FarmingSaplingObjs.acorn,
                potted = FarmingSaplingObjs.plantpot_acorn,
                watered = FarmingSaplingObjs.plantpot_acorn_watered,
                sapling = FarmingSaplingObjs.plantpot_oak_sapling,
                level = 15,
                potXp = 14.0,
            ),
            // willow
            SaplingChain(
                seed = FarmingSaplingObjs.willow_seed,
                potted = FarmingSaplingObjs.plantpot_willow_seed,
                watered = FarmingSaplingObjs.plantpot_willow_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_willow_sapling,
                level = 30,
                potXp = 25.0,
            ),
            // maple
            SaplingChain(
                seed = FarmingSaplingObjs.maple_seed,
                potted = FarmingSaplingObjs.plantpot_maple_seed,
                watered = FarmingSaplingObjs.plantpot_maple_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_maple_sapling,
                level = 45,
                potXp = 45.0,
            ),
            // yew
            SaplingChain(
                seed = FarmingSaplingObjs.yew_seed,
                potted = FarmingSaplingObjs.plantpot_yew_seed,
                watered = FarmingSaplingObjs.plantpot_yew_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_yew_sapling,
                level = 60,
                potXp = 81.0,
            ),
            // magic
            SaplingChain(
                seed = FarmingSaplingObjs.magic_tree_seed,
                potted = FarmingSaplingObjs.plantpot_magic_tree_seed,
                watered = FarmingSaplingObjs.plantpot_magic_tree_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_magic_tree_sapling,
                level = 75,
                potXp = 145.5,
            ),
            // spirit tree
            SaplingChain(
                seed = FarmingSaplingObjs.spirit_tree_seed,
                potted = FarmingSaplingObjs.plantpot_spirit_tree_seed,
                watered = FarmingSaplingObjs.plantpot_spirit_tree_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_spirit_tree_sapling,
                level = 83,
                potXp = 199.5,
            ),
            // apple
            SaplingChain(
                seed = FarmingSaplingObjs.apple_tree_seed,
                potted = FarmingSaplingObjs.plantpot_apple_seed,
                watered = FarmingSaplingObjs.plantpot_apple_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_apple_sapling,
                level = 27,
                potXp = 22.0,
            ),
            // banana
            SaplingChain(
                seed = FarmingSaplingObjs.banana_tree_seed,
                potted = FarmingSaplingObjs.plantpot_banana_seed,
                watered = FarmingSaplingObjs.plantpot_banana_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_banana_sapling,
                level = 33,
                potXp = 28.0,
            ),
            // orange
            SaplingChain(
                seed = FarmingSaplingObjs.orange_tree_seed,
                potted = FarmingSaplingObjs.plantpot_orange_seed,
                watered = FarmingSaplingObjs.plantpot_orange_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_orange_sapling,
                level = 39,
                potXp = 35.5,
            ),
            // curry
            SaplingChain(
                seed = FarmingSaplingObjs.curry_tree_seed,
                potted = FarmingSaplingObjs.plantpot_curry_seed,
                watered = FarmingSaplingObjs.plantpot_curry_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_curry_sapling,
                level = 42,
                potXp = 40.0,
            ),
            // pineapple
            SaplingChain(
                seed = FarmingSaplingObjs.pineapple_tree_seed,
                potted = FarmingSaplingObjs.plantpot_pineapple_seed,
                watered = FarmingSaplingObjs.plantpot_pineapple_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_pineapple_sapling,
                level = 51,
                potXp = 57.0,
            ),
            // papaya
            SaplingChain(
                seed = FarmingSaplingObjs.papaya_tree_seed,
                potted = FarmingSaplingObjs.plantpot_papaya_seed,
                watered = FarmingSaplingObjs.plantpot_papaya_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_papaya_sapling,
                level = 57,
                potXp = 72.0,
            ),
            // palm
            SaplingChain(
                seed = FarmingSaplingObjs.palm_tree_seed,
                potted = FarmingSaplingObjs.plantpot_palm_seed,
                watered = FarmingSaplingObjs.plantpot_palm_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_palm_sapling,
                level = 68,
                potXp = 110.5,
            ),
            // dragonfruit
            SaplingChain(
                seed = FarmingSaplingObjs.dragonfruit_tree_seed,
                potted = FarmingSaplingObjs.plantpot_dragonfruit_seed,
                watered = FarmingSaplingObjs.plantpot_dragonfruit_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_dragonfruit_sapling,
                level = 81,
                potXp = 141.5,
            ),
            // calquat
            SaplingChain(
                seed = FarmingSaplingObjs.calquat_tree_seed,
                potted = FarmingSaplingObjs.plantpot_calquat_seed,
                watered = FarmingSaplingObjs.plantpot_calquat_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_calquat_sapling,
                level = 72,
                potXp = 129.5,
            ),
            // teak
            SaplingChain(
                seed = FarmingSaplingObjs.teak_seed,
                potted = FarmingSaplingObjs.plantpot_teak_seed,
                watered = FarmingSaplingObjs.plantpot_teak_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_teak_sapling,
                level = 35,
                potXp = 35.0,
            ),
            // mahogany
            SaplingChain(
                seed = FarmingSaplingObjs.mahogany_seed,
                potted = FarmingSaplingObjs.plantpot_mahogany_seed,
                watered = FarmingSaplingObjs.plantpot_mahogany_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_mahogany_sapling,
                level = 55,
                potXp = 63.0,
            ),
            // celastrus
            SaplingChain(
                seed = FarmingSaplingObjs.celastrus_tree_seed,
                potted = FarmingSaplingObjs.plantpot_celastrus_tree_seed,
                watered = FarmingSaplingObjs.plantpot_celastrus_tree_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_celastrus_tree_sapling,
                level = 85,
                potXp = 204.0,
            ),
            // redwood
            SaplingChain(
                seed = FarmingSaplingObjs.redwood_tree_seed,
                potted = FarmingSaplingObjs.plantpot_redwood_tree_seed,
                watered = FarmingSaplingObjs.plantpot_redwood_tree_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_redwood_tree_sapling,
                level = 90,
                potXp = 230.0,
            ),
            // crystal
            SaplingChain(
                seed = FarmingSaplingObjs.crystal_tree_seed,
                potted = FarmingSaplingObjs.plantpot_crystal_tree_seed,
                watered = FarmingSaplingObjs.plantpot_crystal_tree_seed_watered,
                sapling = FarmingSaplingObjs.plantpot_crystal_tree_sapling,
                level = 74,
                potXp = 126.0,
            ),
        )

    /**
     * Keyed on raw obj ids: a `HashedObjType` never compares equal to an `UnpackedObjType`. Lazy,
     * because a reference has no id until the server binds it at boot.
     */
    public val bySeed: Map<Int, SaplingChain> by lazy { all.associateBy { it.seed.id } }
    public val byPotted: Map<Int, SaplingChain> by lazy { all.associateBy { it.potted.id } }
    public val byWatered: Map<Int, SaplingChain> by lazy { all.associateBy { it.watered.id } }
}
