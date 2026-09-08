package org.rsmod.content.skills.smithing.configs

import org.rsmod.game.type.obj.ObjType

data class Ingredient(val obj: ObjType, val count: Int)

/**
 * One furnace recipe.
 *
 * [successPercent] exists for exactly one recipe -- iron. Smelting iron ore without a ring of
 * forging fails half the time in the real game, and the failure is flavour players recognise ("The
 * ore is too impure..."), so it is modelled rather than smoothed away.
 */
data class SmeltRecipe(
    val bar: ObjType,
    val levelReq: Int,
    val xp: Double,
    val ingredients: List<Ingredient>,
    val successPercent: Int = 100,
) {
    /** The ore that identifies this recipe when a player uses an ore on a furnace. */
    val primaryOre: ObjType
        get() = ingredients.first().obj
}

/**
 * The smelting table.
 *
 * Levels and xp are the real OSRS values. Unlike the anvil table -- which is read wholesale out of
 * the cache's own enums -- the cache carries no smelting enum, so this one is authored.
 */
object SmeltingRecipes {
    private fun recipe(
        bar: ObjType,
        levelReq: Int,
        xp: Double,
        vararg ingredients: Pair<ObjType, Int>,
        successPercent: Int = 100,
    ) =
        SmeltRecipe(
            bar = bar,
            levelReq = levelReq,
            xp = xp,
            ingredients = ingredients.map { Ingredient(it.first, it.second) },
            successPercent = successPercent,
        )

    val all: List<SmeltRecipe> =
        listOf(
            recipe(
                SmithingObjs.bronze_bar,
                1,
                6.2,
                SmithingObjs.copper_ore to 1,
                SmithingObjs.tin_ore to 1,
            ),
            recipe(
                SmithingObjs.iron_bar,
                15,
                12.5,
                SmithingObjs.iron_ore to 1,
                successPercent = 50,
            ),
            recipe(SmithingObjs.silver_bar, 20, 13.7, SmithingObjs.silver_ore to 1),
            recipe(
                SmithingObjs.steel_bar,
                30,
                17.5,
                SmithingObjs.iron_ore to 1,
                SmithingObjs.coal to 2,
            ),
            recipe(SmithingObjs.gold_bar, 40, 22.5, SmithingObjs.gold_ore to 1),
            recipe(
                SmithingObjs.mithril_bar,
                50,
                30.0,
                SmithingObjs.mithril_ore to 1,
                SmithingObjs.coal to 4,
            ),
            recipe(
                SmithingObjs.adamantite_bar,
                70,
                37.5,
                SmithingObjs.adamantite_ore to 1,
                SmithingObjs.coal to 6,
            ),
            recipe(
                SmithingObjs.runite_bar,
                85,
                50.0,
                SmithingObjs.runite_ore to 1,
                SmithingObjs.coal to 8,
            ),
        )

    /**
     * Recipes keyed by the ore a player would sensibly use on the furnace.
     *
     * Bronze is reachable from either half of its pair, so copper and tin both map to it. Iron ore
     * maps to two recipes -- iron and steel -- which is the one genuine ambiguity, resolved with a
     * dialogue choice. Coal is deliberately not a key: it is a secondary in four recipes at once
     * and offers no useful default.
     */
    val byOre: Map<Int, List<SmeltRecipe>> by lazy {
        buildMap {
            for (recipe in all) {
                for (ingredient in recipe.ingredients) {
                    if (ingredient.obj.id == SmithingObjs.coal.id) {
                        continue
                    }
                    merge(ingredient.obj.id, listOf(recipe)) { a, b -> a + b }
                }
            }
        }
    }
}
