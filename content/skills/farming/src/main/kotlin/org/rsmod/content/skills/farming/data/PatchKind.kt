package org.rsmod.content.skills.farming.data

/**
 * The kinds of patch in the game, and the varbit states every patch of that kind shares.
 *
 * The first four states are always the same shape: `0` is a patch buried in weeds, `1` and `2` are
 * partly raked, and `3` is bare soil ready for a seed. Grape vines are the exception -- they have
 * no weeds at all, only an empty trellis at `0` -- which is why the weed states are a property here
 * rather than a constant.
 */
public enum class PatchKind(
    /** Weed states, worst first. Raking walks backwards along this list towards [cleared]. */
    public val weeds: IntArray,
    /** Bare soil: what a raked, harvested or dug-up patch shows. */
    public val cleared: Int,
    /** What the patch is called in messages. */
    public val label: String,
) {
    Allotment(intArrayOf(0, 1, 2), 3, "allotment"),
    Flower(intArrayOf(0, 1, 2), 3, "flower patch"),
    Herb(intArrayOf(0, 1, 2), 3, "herb patch"),
    Hops(intArrayOf(0, 1, 2), 3, "hops patch"),
    Bush(intArrayOf(0, 1, 2), 3, "bush patch"),
    FruitTree(intArrayOf(0, 1, 2), 3, "fruit tree patch"),
    Tree(intArrayOf(0, 1, 2), 3, "tree patch"),
    Hardwood(intArrayOf(0, 1, 2), 3, "hardwood tree patch"),
    Cactus(intArrayOf(0, 1, 2), 3, "cactus patch"),
    Calquat(intArrayOf(0, 1, 2), 3, "calquat patch"),
    Mushroom(intArrayOf(0, 1, 2), 3, "mushroom patch"),
    Belladonna(intArrayOf(0, 1, 2), 3, "belladonna patch"),
    Seaweed(intArrayOf(0, 1, 2), 3, "seaweed patch"),
    SpiritTree(intArrayOf(0, 1, 2), 3, "spirit tree patch"),
    Celastrus(intArrayOf(0, 1, 2), 3, "celastrus patch"),
    Redwood(intArrayOf(0, 1, 2), 3, "redwood patch"),
    Anima(intArrayOf(0, 1, 2), 3, "anima patch"),
    Crystal(intArrayOf(0, 1, 2), 3, "crystal tree patch"),
    Hespori(intArrayOf(0, 1, 2), 3, "hespori patch"),
    Grape(intArrayOf(), 0, "vine");

    /** How many rakes a fully overgrown patch takes. */
    public val rakes: Int
        get() = weeds.size
}

/** How a grown crop pays out. */
public enum class HarvestModel {
    /**
     * Harvested over and over until the crop's *lives* run out, at which point the patch empties
     * itself. Allotments, herbs, hops, flowers and belladonna work this way, and it is the only
     * model compost still changes now that disease is switched off.
     */
    Lives,

    /**
     * Holds a fixed number of items, and the loc state says how many are left -- six fruit on a
     * fruit tree, four berries on a bush, three spines on a cactus. Some of them grow their produce
     * back ([FarmingCrop.regrowMinutes]); the rest have to be cleared and replanted.
     */
    Counted,

    /**
     * Pays out once, when the tree is checked for health, and gives nothing else. Trees, hardwoods,
     * redwood, spirit trees, crystal trees and anima seeds.
     */
    Checked,
}

/** Compost raises how much a lives-based crop yields before the patch gives out. */
public enum class CompostTier(public val extraLives: Int) {
    None(0),
    Compost(1),
    Supercompost(2),
    Ultracompost(3);

    public companion object {
        public fun from(ordinal: Int): CompostTier = entries.getOrElse(ordinal) { None }
    }
}
