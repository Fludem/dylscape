package org.rsmod.content.other.consumables.configs

/**
 * Every consumable in the game, keyed by raw obj id.
 *
 * Keyed by `Int` deliberately: `find(...)` hands back a `HashedObjType` while the runtime hands the
 * script an `UnpackedObjType`, and the two never compare equal.
 *
 * Both properties are lazy because obj ids are only filled in once the type resolver has run, which
 * is long after these family objects are constructed.
 *
 * Public rather than internal: the integration suite is a separate compilation unit, and the table
 * is the module's answer to "how much does this heal?" for anything that needs to ask later.
 */
public object Consumables {
    public val rows: List<Edible> by lazy {
        ConsumablePlainFoods.rows +
            ConsumableChains.rows +
            ConsumableSpecials.rows +
            ConsumableDrinks.rows
    }

    public val byObjId: Map<Int, Edible> by lazy { rows.associateBy { it.obj.id } }
}
