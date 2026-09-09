package org.rsmod.content.skills.herblore.configs

/**
 * Every dose of every potion in the game, and the two lookups the rest of the module needs.
 *
 * Both properties are lazy, and that is load-bearing rather than an optimisation. Obj ids are
 * filled in by the reference resolver long after the family objects are constructed, and
 * `CacheType.id` throws on a type it has not reached yet -- so `associateBy { it.obj.id }` at
 * construction time would crash the boot. `Consumables` is lazy for exactly this reason.
 *
 * Public because the integration suite is a separate compilation unit.
 */
object Potions {
    val rows: List<Potion> by lazy {
        PotionsCombat.rows + PotionsRestore.rows + PotionsUtility.rows
    }

    val byObjId: Map<Int, Potion> by lazy { rows.associateBy { it.obj.id } }

    /**
     * The four-dose head of every ladder, keyed by family stem.
     *
     * This is how [HerbloreRecipes] names what a recipe produces: the recipe table stores a stem
     * and looks the obj up here, so the recipes and the dose ladder cannot end up naming different
     * potions. A stem with no ladder throws at startup, naming the recipe.
     */
    val heads: Map<String, Potion> by lazy {
        rows.filter { it.dose == MAX_DOSE }.associateBy { it.family }
    }

    private const val MAX_DOSE: Int = 4
}
