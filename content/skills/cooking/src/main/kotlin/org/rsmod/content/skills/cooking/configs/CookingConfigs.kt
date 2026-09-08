package org.rsmod.content.skills.cooking.configs

import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.game.type.obj.ObjType

/**
 * Content groups this module owns. Resolved from `.data/symbols/.local/content.sym`; an unknown
 * name fails the boot outright, so the two must stay in step.
 */
object CookingContent : ContentReferences() {
    /** Anything that can be put on a fire or range. Carries the params in [CookingParams]. */
    val cooking_raw = find("cooking_raw")

    /** Ranges, stoves and ovens: `Cook` on op1, and the only place bread can be baked. */
    val cooking_range = find("cooking_range")

    /** Fires, including the one firemaking lights. */
    val cooking_fire = find("cooking_fire")
}

/**
 * Objs this module needs.
 *
 * The cache's five "Burnt fish" are anonymous -- `burntfish1` to `burntfish5`, all named "Burnt
 * fish" with only the examine text telling them apart -- so which raw fish burns into which is
 * recorded in [CookingFoodEditor] rather than being readable off the obj. Note also that the raw
 * shrimps are `raw_shrimp`, singular, while the cooked ones are `shrimp`, and that `newbieshrimp`
 * (2512) exists but has no name: Tutorial Island's raw shrimps cook into the ordinary [shrimp] and
 * burn into [burnt_shrimp], which is what the live game does too.
 */
object CookingObjs : ObjReferences() {
    // Fish, raw.
    val raw_shrimp = find("raw_shrimp")
    val raw_anchovies = find("raw_anchovies")
    val raw_sardine = find("raw_sardine")
    val raw_herring = find("raw_herring")
    val raw_mackerel = find("raw_mackerel")
    val raw_trout = find("raw_trout")
    val raw_cod = find("raw_cod")
    val raw_pike = find("raw_pike")
    val raw_salmon = find("raw_salmon")
    val raw_tuna = find("raw_tuna")
    val raw_lobster = find("raw_lobster")
    val raw_bass = find("raw_bass")
    val raw_swordfish = find("raw_swordfish")
    val raw_shark = find("raw_shark")

    // Fish, cooked.
    val shrimp = find("shrimp")
    val anchovies = find("anchovies")
    val sardine = find("sardine")
    val herring = find("herring")
    val mackerel = find("mackerel")
    val trout = find("trout")
    val cod = find("cod")
    val pike = find("pike")
    val salmon = find("salmon")
    val tuna = find("tuna")
    val lobster = find("lobster")
    val bass = find("bass")
    val swordfish = find("swordfish")
    val shark = find("shark")

    // Fish, burnt.
    val burnt_shrimp = find("burnt_shrimp")
    val burnt_fish_anchovies = find("burntfish1")
    val burnt_fish_trout = find("burntfish2")
    val burnt_fish_herring = find("burntfish3")
    val burnt_fish_tuna = find("burntfish4")
    val burnt_fish_sardine = find("burntfish5")
    val burnt_swordfish = find("burnt_swordfish")
    val burnt_lobster = find("burnt_lobster")
    val burnt_shark = find("burnt_shark")

    // Meat.
    val raw_beef = find("raw_beef")
    val raw_rat_meat = find("raw_rat_meat")
    val raw_bear_meat = find("raw_bear_meat")
    val cooked_meat = find("cooked_meat")
    val burnt_meat = find("burnt_meat")
    val raw_chicken = find("raw_chicken")
    val cooked_chicken = find("cooked_chicken")
    val burnt_chicken = find("burnt_chicken")

    // Bread.
    val bread_dough = find("bread_dough")
    val bread = find("bread")
    val burnt_bread = find("burnt_bread")

    // Dough. Pastry dough and pizza bases are made here but need pie dishes and toppings this
    // module does not implement before they can be cooked.
    val pot_flour = find("pot_flour")
    val pot_empty = find("pot_empty")
    val bucket_water = find("bucket_water")
    val bucket_empty = find("bucket_empty")
    val jug_water = find("jug_water")
    val jug_empty = find("jug_empty")
    val bowl_water = find("bowl_water")
    val bowl_empty = find("bowl_empty")
    val pastry_dough = find("pastry_dough")
    val pizza_base = find("pizza_base")

    /**
     * Tutorial Island's own shrimps and flour. They exist so the tutorial can hand out items that
     * cannot be carried off the island and sold.
     */
    val newbie_raw_shrimp = find("newbieraw_shrimp")
    val newbie_pot_flour = find("newbie_pot_flour")
}

object CookingSeqs : SeqReferences() {
    val range = find("human_cooking")
    val fire = find("human_firecooking")
}

/**
 * Per-food data, stored as params on the raw obj so that everything about a food lives on its type.
 * Level and xp reuse upstream's `levelrequire` and `skill_xp`, and the cooked result reuses
 * `skill_productitem`, exactly as the mining rocks and woodcutting trees do.
 */
object CookingParams : ParamReferences() {
    /** What the food turns into on a failed roll. */
    val burnt = find<ObjType>("cooking_burnt")

    /** The Cooking level at which this food stops burning on a fire. */
    val stop_burn_fire = find<Int>("cooking_stop_burn_fire")

    /** The Cooking level at which this food stops burning on a range. Never above the fire's. */
    val stop_burn_range = find<Int>("cooking_stop_burn_range")

    /** Bread and its relatives cannot be baked over an open fire. */
    val range_only = find<Boolean>("cooking_range_only")

    /** How the chat refers to the food: "the shrimps", "the bread". */
    val name = find<String>("cooking_name")
}

internal object CookingParamBuilder : ParamBuilder() {
    init {
        build<ObjType>("cooking_burnt")
        build<Int>("cooking_stop_burn_fire")
        build<Int>("cooking_stop_burn_range")
        build<Boolean>("cooking_range_only")
        build<String>("cooking_name")
    }
}
