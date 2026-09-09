package org.rsmod.content.other.consumables.configs

/**
 * Food eaten in stages.
 *
 * Cakes, pizzas and pies are not consumed in one click: each bite heals the same amount and turns
 * the obj into the next stage, the last leaving nothing behind. The heal on each [chain] call is
 * therefore the value *per bite*, not the total - a whole cake is three bites of four.
 *
 * Stage order is the order of the arguments and is not derivable from obj id: the cache numbers
 * `half_botanical_pie` (19659) below `botanical_pie` (19662), and the five stages of cooked crab
 * meat count *down* from `_5`. Getting the order backwards would let a slice be eaten back into a
 * whole cake, so it is written out rather than computed.
 *
 * Not here, deliberately: pineapples, tenti pineapples and watermelons. The wiki prices them the
 * same way ("2 x 4"), but they are cut apart with a knife into separate objs rather than bitten
 * through stages, so each of their pieces is its own single-bite row.
 */
internal object ConsumableChains : ConsumableFamily() {
    init {
        // Admiral pie
        chain(8, "admiral_pie", "half_admiral_pie")
        // Anchovy pizza
        chain(9, "anchovie_pizza", "half_anchovie_pizza")
        // Apple pie
        chain(7, "apple_pie", "half_an_apple_pie")
        // Botanical pie
        chain(7, "botanical_pie", "half_botanical_pie")
        // Cake
        chain(4, "cake", "partial_cake", "cake_slice")
        // Chocolate cake
        chain(5, "chocolate_cake", "partial_chocolate_cake", "chocolate_slice")
        // Cooked giant crab meat
        chain(
            2,
            "hundred_pirate_giant_crab_meat_5",
            "hundred_pirate_giant_crab_meat_4",
            "hundred_pirate_giant_crab_meat_3",
            "hundred_pirate_giant_crab_meat_2",
            "hundred_pirate_giant_crab_meat_1",
        )
        // Dragonfruit pie
        chain(10, "dragonfruit_pie", "half_dragonfruit_pie")
        // Fish pie
        chain(6, "fish_pie", "half_fish_pie")
        // Garden pie
        chain(6, "garden_pie", "half_garden_pie")
        // Meat pie
        chain(6, "meat_pie", "half_a_meat_pie")
        // Meat pizza
        chain(8, "meat_pizza", "half_meat_pizza")
        // Mushroom pie
        chain(8, "mushroom_pie", "half_mushroom_pie")
        // Pineapple pizza
        chain(11, "pineapple_pizza", "half_pineapple_pizza")
        // Plain pizza
        chain(7, "plain_pizza", "half_plain_pizza")
        // Redberry pie
        chain(5, "redberry_pie", "half_a_redberry_pie")
        // Summer pie
        chain(11, "summer_pie", "half_summer_pie")
        // Wild pie
        chain(11, "wild_pie", "half_wild_pie")
    }
}
