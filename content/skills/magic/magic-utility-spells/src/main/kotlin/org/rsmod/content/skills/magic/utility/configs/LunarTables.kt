package org.rsmod.content.skills.magic.utility.configs

import org.rsmod.game.type.obj.ObjType

/** A one-for-one inventory swap: [from] becomes [into]. */
public data class Conversion(val from: ObjType, val into: ObjType)

/** The Lunar skilling spells' inputs and outputs, from the wiki, keyed by raw obj id at runtime. */
public object LunarTables {
    val humidify: List<Conversion> =
        listOf(
            Conversion(utility_objs.vial, utility_objs.vial_of_water),
            Conversion(utility_objs.bucket, utility_objs.bucket_of_water),
            Conversion(utility_objs.bowl, utility_objs.bowl_of_water),
            Conversion(utility_objs.jug, utility_objs.jug_of_water),
            Conversion(utility_objs.watering_can_empty, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_1, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_2, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_3, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_4, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_5, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_6, utility_objs.watering_can_full),
            Conversion(utility_objs.watering_can_7, utility_objs.watering_can_full),
        )

    val tanLeather: List<Conversion> =
        listOf(
            Conversion(utility_objs.cow_hide, utility_objs.leather),
            Conversion(utility_objs.green_dragonhide, utility_objs.green_dragon_leather),
            Conversion(utility_objs.blue_dragonhide, utility_objs.blue_dragon_leather),
            Conversion(utility_objs.red_dragonhide, utility_objs.red_dragon_leather),
            Conversion(utility_objs.black_dragonhide, utility_objs.black_dragon_leather),
        )

    val stringJewellery: List<Conversion> =
        listOf(
            Conversion(utility_objs.gold_amulet_u, utility_objs.gold_amulet),
            Conversion(utility_objs.sapphire_amulet_u, utility_objs.sapphire_amulet),
            Conversion(utility_objs.opal_amulet_u, utility_objs.opal_amulet),
            Conversion(utility_objs.emerald_amulet_u, utility_objs.emerald_amulet),
            Conversion(utility_objs.jade_amulet_u, utility_objs.jade_amulet),
            Conversion(utility_objs.ruby_amulet_u, utility_objs.ruby_amulet),
            Conversion(utility_objs.topaz_amulet_u, utility_objs.topaz_amulet),
            Conversion(utility_objs.diamond_amulet_u, utility_objs.diamond_amulet),
            Conversion(utility_objs.dragonstone_amulet_u, utility_objs.dragonstone_amulet),
            Conversion(utility_objs.onyx_amulet_u, utility_objs.onyx_amulet),
            Conversion(utility_objs.zenyte_amulet_u, utility_objs.zenyte_amulet),
        )

    val rechargeDragonstone: List<Conversion> =
        listOf(
            Conversion(utility_objs.amulet_of_glory, utility_objs.amulet_of_glory_4),
            Conversion(utility_objs.amulet_of_glory_1, utility_objs.amulet_of_glory_4),
            Conversion(utility_objs.amulet_of_glory_2, utility_objs.amulet_of_glory_4),
            Conversion(utility_objs.amulet_of_glory_3, utility_objs.amulet_of_glory_4),
            Conversion(utility_objs.ring_of_wealth, utility_objs.ring_of_wealth_5),
            Conversion(utility_objs.ring_of_wealth_1, utility_objs.ring_of_wealth_5),
            Conversion(utility_objs.ring_of_wealth_2, utility_objs.ring_of_wealth_5),
            Conversion(utility_objs.ring_of_wealth_3, utility_objs.ring_of_wealth_5),
            Conversion(utility_objs.ring_of_wealth_4, utility_objs.ring_of_wealth_5),
            Conversion(utility_objs.skills_necklace, utility_objs.skills_necklace_4),
            Conversion(utility_objs.skills_necklace_1, utility_objs.skills_necklace_4),
            Conversion(utility_objs.skills_necklace_2, utility_objs.skills_necklace_4),
            Conversion(utility_objs.skills_necklace_3, utility_objs.skills_necklace_4),
            Conversion(utility_objs.combat_bracelet, utility_objs.combat_bracelet_4),
            Conversion(utility_objs.combat_bracelet_1, utility_objs.combat_bracelet_4),
            Conversion(utility_objs.combat_bracelet_2, utility_objs.combat_bracelet_4),
            Conversion(utility_objs.combat_bracelet_3, utility_objs.combat_bracelet_4),
        )

    public data class Plank(val logs: ObjType, val plank: ObjType, val cost: Int)

    val plankMake: List<Plank> =
        listOf(
            Plank(utility_objs.logs, utility_objs.plank, 70),
            Plank(utility_objs.oak_logs, utility_objs.oak_plank, 175),
            Plank(utility_objs.teak_logs, utility_objs.teak_plank, 350),
            Plank(utility_objs.mahogany_logs, utility_objs.mahogany_plank, 1050),
        )

    public data class Pie(val raw: ObjType, val cooked: ObjType, val level: Int, val xp: Double)

    val bakePie: List<Pie> =
        listOf(
            Pie(utility_objs.uncooked_redberry_pie, utility_objs.redberry_pie, 10, 78.0),
            Pie(utility_objs.uncooked_meat_pie, utility_objs.meat_pie, 20, 110.0),
            Pie(utility_objs.uncooked_mud_pie, utility_objs.mud_pie, 29, 128.0),
            Pie(utility_objs.uncooked_apple_pie, utility_objs.apple_pie, 30, 130.0),
            Pie(utility_objs.uncooked_garden_pie, utility_objs.garden_pie, 34, 138.0),
            Pie(utility_objs.uncooked_fish_pie, utility_objs.fish_pie, 47, 164.0),
            Pie(utility_objs.uncooked_botanical_pie, utility_objs.botanical_pie, 52, 180.0),
            Pie(utility_objs.uncooked_mushroom_pie, utility_objs.mushroom_pie, 60, 200.0),
            Pie(utility_objs.uncooked_admiral_pie, utility_objs.admiral_pie, 70, 210.0),
            Pie(utility_objs.uncooked_dragonfruit_pie, utility_objs.dragonfruit_pie, 73, 220.0),
            Pie(utility_objs.uncooked_wild_pie, utility_objs.wild_pie, 85, 240.0),
            Pie(utility_objs.uncooked_summer_pie, utility_objs.summer_pie, 95, 260.0),
        )

    /** Crafting xp per product for the conversion spells that award any. */
    public const val SUPERGLASS_XP_PER_GLASS: Double = 10.0
    public const val SPIN_FLAX_XP_PER_STRING: Double = 15.0
    public const val STRING_JEWELLERY_XP_PER_AMULET: Double = 4.0
    public const val SPIN_FLAX_MAX: Int = 5
    public const val TAN_LEATHER_MAX: Int = 5
    /** Superglass Make yields an extra glass for roughly three pairs in ten. */
    public const val SUPERGLASS_BONUS_PERCENT: Int = 30
}
