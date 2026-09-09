package org.rsmod.content.other.consumables.configs

import org.rsmod.api.config.refs.stats

/**
 * Everything with a `Drink` op that is not a Herblore potion.
 *
 * The dosed potion ladder - every `(4)` through `(1)` in the cache, 510 objs of it - is
 * deliberately absent. Doses, vials and the Herblore boosts are their own mechanic and their own
 * module, and `potion` is left as an unclaimed content group for them.
 *
 * What is here is the taverns: ales, spirits, wines, teas, the Gnome cocktails and the kegs. Ales
 * are the interesting half. Each one trades a combat stat for a non-combat one, and those numbers
 * are the live formulae rather than approximations - "boosts Strength by 2% + 1 while draining
 * Attack by 6% + 1" is exactly the `(constant, percent)` pair `statBoost` and `statDrain` take.
 *
 * Vessels are returned. A jug of wine leaves a jug, a bowl of tea leaves a bowl: that is the same
 * in-slot replacement a half-eaten cake uses, so it costs nothing extra to be correct about.
 */
internal object ConsumableDrinks : ConsumableFamily() {
    private val SPIRITS = listOf("vodka", "whisky", "gin", "brandy")

    /** Sorceress's Garden juice. Riper fruit, bigger Thieving boost. */
    private val SQIRK_JUICES =
        listOf(
            "osman_squirk_j_winter" to 1,
            "osman_squirk_j_spring" to 2,
            "osman_squirk_j_autumn" to 3,
            "osman_squirk_j_summer" to 4,
        )

    private val VARLAMORE_WINES =
        listOf(
            "ixcoztic_white",
            "tonameyo_white",
            "chichilihui_rose",
            "blackbird_red",
            "chilhuac_red",
            "principum_red",
            "metztonalli_white",
            "fortis_ash_white",
            "imperial_rose",
            "xochipaltic_rose",
        )

    private val TEAS =
        listOf(
            "cup_of_tea",
            "cup_of_nettletea_milky",
            "chinacup_of_nettletea",
            "chinacup_of_nettletea_milky",
            "poh_claycup_tea",
            "poh_claycup_tea_milky",
            "poh_chinacup_tea",
            "poh_chinacup_tea_milky",
            "poh_giltchinacup_tea",
            "poh_giltchinacup_tea_milky",
        )

    private val DAMIANA_TEAS =
        listOf(
            "bowl_damiana_tea",
            "bowl_damiana_tea_milky",
            "cup_damiana_tea",
            "cup_damiana_tea_milky",
        )

    /** The finished Gnome cocktails, and the Restaurant's delivery copies of each. */
    private val COCKTAILS =
        listOf(
            "premade_blurberry_special" to 7,
            "premade_choc_saturday" to 5,
            "premade_drunk_dragon" to 5,
            "premade_fruit_blast" to 9,
            "premade_pineapple_punch" to 9,
            "premade_sgg" to 5,
            "premade_wizard_blizzard" to 5,
            "pineapple_punch" to 9,
            "wizard_blizzard" to 5,
            "blurberry_special" to 7,
            "chocolate_saturday" to 5,
            "sgg" to 5,
            "fruit_blast" to 9,
            "drunk_dragon" to 5,
            "aluft_wizard_blizzard" to 5,
            "aluft_sgg" to 5,
            "aluft_pineapple_punch" to 9,
            "aluft_fruit_blast" to 9,
            "aluft_drunk_dragon" to 5,
            "aluft_choc_saturday" to 5,
            "aluft_blurberry_special" to 7,
        )

    private val INERT =
        listOf(
            "unfinished_pineapple_punch1",
            "unfinished_pineapple_punch2",
            "unfinished_pineapple_punch3",
            "unfinished_wizard_blizzard1",
            "unfinished_wizard_blizzard2",
            "unfinished_blurberry_special1",
            "unfinished_blurberry_special2",
            "unfinished_blurberry_special3",
            "unfinished_blurberry_special4",
            "unfinished_chocolate_saturday1",
            "unfinished_chocolate_saturday2",
            "unfinished_chocolate_saturday3",
            "unfinished_chocolate_saturday4",
            "unfinished_sgg1",
            "unfinished_sgg2",
            "unfinished_fruit_blast1",
            "unfinished_drunk_dragon1",
            "unfinished_drunk_dragon2",
            "unfinished_drunk_dragon3",
            "spoilt_cocktail",
            "spoilt_cocktail_fruity",
            "spoilt_cocktail_creamy",
            "poison_chalice",
            "mm_prepot_device",
        )

    private val QUEST_POTIONS =
        listOf(
            "bravery_pot",
            "cadava",
            "blamish_oil",
            "grim_shrinking_potion",
            "my2arm_potion",
            "inversion_potion",
            "akd_sulphur_potion",
            "akd_shielding_potion",
            "dt2_kasonde_potion",
            "dt2_stranglewood_potion",
            "easter22_hot_sauce",
            "easter22_bucket_milk",
            "easter22_potion",
        )

    init {
        // -- Ales. Boost something useful, drain something combat. ------------------------------
        ale(
            "beer",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 1, percent = 2)),
            drains = listOf(drain(stats.attack, constant = 1, percent = 6)),
            leaves = "beer_glass",
        )
        ale(
            "asgarnian_ale",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "dragon_bitter",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "dwarven_stout",
            heal = 1,
            boosts = listOf(boost(stats.mining, constant = 1), boost(stats.smithing, constant = 1)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 4),
                    drain(stats.strength, constant = 2, percent = 4),
                    drain(stats.defence, constant = 2, percent = 4),
                ),
            leaves = "beer_glass",
        )
        ale(
            "greenmans_ale",
            heal = 1,
            boosts = listOf(boost(stats.herblore, constant = 1)),
            drains =
                listOf(
                    drain(stats.attack, constant = 3),
                    drain(stats.strength, constant = 3),
                    drain(stats.defence, constant = 3),
                ),
            leaves = "beer_glass",
        )
        ale(
            "wizards_mind_bomb",
            heal = 1,
            boosts = listOf(boost(stats.magic, constant = 2)),
            drains =
                listOf(
                    drain(stats.attack, constant = 0, percent = 4),
                    drain(stats.strength, constant = 0, percent = 4),
                    drain(stats.defence, constant = 0, percent = 4),
                ),
            leaves = "beer_glass",
        )
        ale(
            "cider",
            heal = 1,
            boosts = listOf(boost(stats.farming, constant = 1)),
            drains = listOf(drain(stats.attack, constant = 2), drain(stats.strength, constant = 2)),
            leaves = "beer_glass",
        )
        ale(
            "moonlight_mead",
            heal = 4,
            drains = listOf(drain(stats.attack, constant = 2)),
            leaves = "beer_glass",
        )
        ale(
            "axemans_folly",
            heal = 1,
            boosts = listOf(boost(stats.woodcutting, constant = 1)),
            drains = listOf(drain(stats.attack, constant = 3), drain(stats.strength, constant = 3)),
            leaves = "beer_glass",
        )
        ale(
            "chefs_delight",
            heal = 1,
            boosts = listOf(boost(stats.cooking, constant = 1, percent = 5)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 5),
                    drain(stats.strength, constant = 2, percent = 5),
                ),
            leaves = "beer_glass",
        )
        ale(
            "slayers_respite",
            heal = 1,
            boosts = listOf(boost(stats.slayer, constant = 2)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 2),
                    drain(stats.strength, constant = 2, percent = 2),
                ),
            leaves = "beer_glass",
        )
        ale(
            "grog",
            heal = 3,
            boosts = listOf(boost(stats.strength, constant = 1, percent = 4)),
            drains = listOf(drain(stats.attack, constant = 3, percent = 5)),
        )
        ale(
            "prif_elven_dawn",
            heal = 1,
            boosts = listOf(boost(stats.agility, constant = 1)),
            drains = listOf(drain(stats.strength, constant = 1)),
        )
        ale(
            "slepe_bloody_bracer",
            heal = 2,
            drains = listOf(drain(stats.prayer, constant = 2, percent = 4)),
        )

        // The matured ales are the same drinks with a bigger boost; the drains are unchanged.
        ale(
            "mature_asgarnian_ale",
            heal = 2,
            boosts = listOf(boost(stats.strength, constant = 3)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "mature_dragon_bitter",
            heal = 2,
            boosts = listOf(boost(stats.strength, constant = 3)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "mature_dwarven_stout",
            heal = 2,
            boosts = listOf(boost(stats.mining, constant = 2), boost(stats.smithing, constant = 2)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 4),
                    drain(stats.strength, constant = 2, percent = 4),
                    drain(stats.defence, constant = 2, percent = 4),
                ),
            leaves = "beer_glass",
        )
        ale(
            "mature_greenmans_ale",
            heal = 2,
            boosts = listOf(boost(stats.herblore, constant = 2)),
            drains =
                listOf(
                    drain(stats.attack, constant = 3),
                    drain(stats.strength, constant = 3),
                    drain(stats.defence, constant = 3),
                ),
            leaves = "beer_glass",
        )
        ale(
            "mature_wizards_mind_bomb",
            heal = 2,
            boosts = listOf(boost(stats.magic, constant = 3)),
            drains =
                listOf(
                    drain(stats.attack, constant = 0, percent = 4),
                    drain(stats.strength, constant = 0, percent = 4),
                    drain(stats.defence, constant = 0, percent = 4),
                ),
            leaves = "beer_glass",
        )
        ale(
            "mature_moonlight_mead",
            heal = 6,
            drains = listOf(drain(stats.attack, constant = 2)),
            leaves = "beer_glass",
        )
        ale(
            "mature_axemans_folly",
            heal = 2,
            boosts = listOf(boost(stats.woodcutting, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 3), drain(stats.strength, constant = 3)),
            leaves = "beer_glass",
        )
        ale(
            "mature_chefs_delight",
            heal = 2,
            boosts = listOf(boost(stats.cooking, constant = 2, percent = 6)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 5),
                    drain(stats.strength, constant = 2, percent = 5),
                ),
            leaves = "beer_glass",
        )
        ale(
            "mature_slayers_respite",
            heal = 2,
            boosts = listOf(boost(stats.slayer, constant = 4)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 2),
                    drain(stats.strength, constant = 2, percent = 2),
                ),
            leaves = "beer_glass",
        )
        ale(
            "mature_cider",
            heal = 2,
            boosts = listOf(boost(stats.farming, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2), drain(stats.strength, constant = 2)),
            leaves = "beer_glass",
        )

        // -- Kegs. Four doses of the mature ale, counting down. ---------------------------------
        keg(
            2,
            "keg_mature_dwarven_stout_4",
            "keg_mature_dwarven_stout_3",
            "keg_mature_dwarven_stout_2",
            "keg_mature_dwarven_stout_1",
            boosts = listOf(boost(stats.mining, constant = 2), boost(stats.smithing, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 4)),
        )
        keg(
            2,
            "keg_mature_asgarnian_ale_4",
            "keg_mature_asgarnian_ale_3",
            "keg_mature_asgarnian_ale_2",
            "keg_mature_asgarnian_ale_1",
            boosts = listOf(boost(stats.strength, constant = 3)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
        )
        keg(
            2,
            "keg_mature_greenmans_ale_4",
            "keg_mature_greenmans_ale_3",
            "keg_mature_greenmans_ale_2",
            "keg_mature_greenmans_ale_1",
            boosts = listOf(boost(stats.herblore, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 3)),
        )
        keg(
            2,
            "keg_mature_wizards_mind_bomb_4",
            "keg_mature_wizards_mind_bomb_3",
            "keg_mature_wizards_mind_bomb_2",
            "keg_mature_wizards_mind_bomb_1",
            boosts = listOf(boost(stats.magic, constant = 3)),
            drains = listOf(drain(stats.attack, constant = 0, percent = 4)),
        )
        keg(
            2,
            "keg_mature_dragon_bitter_4",
            "keg_mature_dragon_bitter_3",
            "keg_mature_dragon_bitter_2",
            "keg_mature_dragon_bitter_1",
            boosts = listOf(boost(stats.strength, constant = 3)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
        )
        keg(
            6,
            "keg_mature_moonlight_mead_4",
            "keg_mature_moonlight_mead_3",
            "keg_mature_moonlight_mead_2",
            "keg_mature_moonlight_mead_1",
            drains = listOf(drain(stats.attack, constant = 2)),
        )
        keg(
            2,
            "keg_mature_cider_4",
            "keg_mature_cider_3",
            "keg_mature_cider_2",
            "keg_mature_cider_1",
            boosts = listOf(boost(stats.farming, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2), drain(stats.strength, constant = 2)),
        )
        ale(
            "keg_of_beer",
            heal = 15,
            boosts = listOf(boost(stats.strength, constant = 2, percent = 10)),
            drains = listOf(drain(stats.attack, constant = 5, percent = 50)),
        )
        ale("viking_tankard_full", heal = 4, drains = listOf(drain(stats.attack, constant = 2)))

        keg(
            2,
            "keg_mature_axemans_folly_4",
            "keg_mature_axemans_folly_3",
            "keg_mature_axemans_folly_2",
            "keg_mature_axemans_folly_1",
            boosts = listOf(boost(stats.woodcutting, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 3), drain(stats.strength, constant = 3)),
        )
        keg(
            2,
            "keg_mature_chefs_delight_4",
            "keg_mature_chefs_delight_3",
            "keg_mature_chefs_delight_2",
            "keg_mature_chefs_delight_1",
            boosts = listOf(boost(stats.cooking, constant = 2, percent = 6)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
        )
        keg(
            2,
            "keg_mature_slayers_respite_4",
            "keg_mature_slayers_respite_3",
            "keg_mature_slayers_respite_2",
            "keg_mature_slayers_respite_1",
            boosts = listOf(boost(stats.slayer, constant = 4)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 2)),
        )

        // -- The player-owned-house bar pours the same ales. ------------------------------------
        ale(
            "poh_beer",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 1, percent = 2)),
            drains = listOf(drain(stats.attack, constant = 1, percent = 6)),
            leaves = "beer_glass",
        )
        ale(
            "poh_asgarnian_ale",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "poh_greenmans_ale",
            heal = 1,
            boosts = listOf(boost(stats.herblore, constant = 1)),
            drains =
                listOf(
                    drain(stats.attack, constant = 3),
                    drain(stats.strength, constant = 3),
                    drain(stats.defence, constant = 3),
                ),
            leaves = "beer_glass",
        )
        ale(
            "poh_dragon_bitter",
            heal = 1,
            boosts = listOf(boost(stats.strength, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2, percent = 5)),
            leaves = "beer_glass",
        )
        ale(
            "poh_moonlight_mead",
            heal = 4,
            drains = listOf(drain(stats.attack, constant = 2)),
            leaves = "beer_glass",
        )
        ale(
            "poh_cider",
            heal = 1,
            boosts = listOf(boost(stats.farming, constant = 1)),
            drains = listOf(drain(stats.attack, constant = 2), drain(stats.strength, constant = 2)),
            leaves = "beer_glass",
        )
        ale(
            "poh_chefs_delight",
            heal = 1,
            boosts = listOf(boost(stats.cooking, constant = 1, percent = 5)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2, percent = 5),
                    drain(stats.strength, constant = 2, percent = 5),
                ),
            leaves = "beer_glass",
        )

        // -- Skilling ales. The reason to drink them is the boost, not the hitpoint. ------------
        ale(
            "kovacs_grog",
            heal = 1,
            boosts = listOf(boost(stats.smithing, constant = 4)),
            drains =
                listOf(
                    drain(stats.attack, constant = 2),
                    drain(stats.ranged, constant = 2),
                    drain(stats.magic, constant = 2),
                ),
        )
        ale(
            "trappers_tipple",
            heal = 0,
            boosts = listOf(boost(stats.hunter, constant = 2)),
            drains = listOf(drain(stats.attack, constant = 2), drain(stats.strength, constant = 1)),
        )
        ale(
            "fever_rum",
            heal = 1,
            drains =
                listOf(
                    drain(stats.attack, constant = 3),
                    drain(stats.strength, constant = 3),
                    drain(stats.defence, constant = 3),
                ),
        )

        // Sq'irkjuice boosts Thieving, riper fruit boosting further. No healing at all.
        for ((juice, levels) in SQIRK_JUICES) {
            ale(juice, heal = 0, boosts = listOf(boost(stats.thieving, constant = levels)))
        }

        // -- Spirits. All four are the same drink with a different label. -----------------------
        for (spirit in SPIRITS) {
            ale(
                spirit,
                heal = 5,
                boosts = listOf(boost(stats.strength, constant = 1, percent = 5)),
                drains = listOf(drain(stats.attack, constant = 3, percent = 2)),
            )
        }
        ale(
            "karamja_rum",
            heal = 5,
            boosts = listOf(boost(stats.strength, constant = 1, percent = 5)),
            drains = listOf(drain(stats.attack, constant = 3, percent = 2)),
        )

        // -- Wine. The jug is handed back, which is the whole reason `leaves` exists. ------------
        drink(
            "jug_wine",
            heal = 11,
            leaves = "jug_empty",
            effects = listOf(drain(stats.attack, constant = 2)),
        )
        drink(
            "half_full_wine_jug",
            heal = 7,
            leaves = "jug_empty",
            effects = listOf(drain(stats.attack, constant = 2)),
        )
        drink(
            "jug_bad_wine",
            heal = 0,
            leaves = "jug_empty",
            message = "You drink the wine. It tastes foul.",
            effects = listOf(drain(stats.attack, constant = 3)),
        )
        drink("rag_bottle_wine", heal = 14, effects = listOf(drain(stats.attack, constant = 2)))

        // The Varlamore wines. All sixteen hitpoints, all cost Attack.
        for (wine in VARLAMORE_WINES) {
            drink(wine, heal = 16, effects = listOf(drain(stats.attack, constant = 5)))
        }

        // -- Tea. The one drink that boosts Attack rather than draining it. ---------------------
        for (tea in TEAS) {
            drink(tea, heal = 3, effects = listOf(boost(stats.attack, constant = 2, percent = 2)))
        }
        drink("cup_of_nettletea", heal = 3, energy = 5)
        drink("bowl_nettletea", heal = 3)
        drink("bowl_nettletea_milky", heal = 3)
        drink("bowl_nettlewater", heal = 1)
        for (damiana in DAMIANA_TEAS) {
            drink(damiana, heal = 4, energy = 6)
        }
        drink("bowl_damiana_water", heal = 1)

        // -- Gnome cocktails, and the Gnome Restaurant delivery copies of them. ------------------
        for ((cocktail, heal) in COCKTAILS) {
            drink(cocktail, heal = heal)
        }

        // -- Everything else with a `Drink` op and a documented heal. ---------------------------
        drink("chocolaty_milk", heal = 4)
        drink(
            "display_tea",
            heal = 3,
            effects = listOf(boost(stats.attack, constant = 2, percent = 2)),
        )
        drink("tol_tea", heal = 3, effects = listOf(boost(stats.attack, constant = 2, percent = 2)))
        drink("kelda_stout", heal = 1)
        drink("hundred_dwarf_asgarnian_ale", heal = 1)
        drink("hundred_fruit_blast", heal = 9)
        drink("brew_red_rum", heal = 5)
        drink("brew_blue_rum", heal = 5)
        drink("rum", heal = 5)
        drink("blood_pint", heal = 2)
        drink("shayzien_lizardkicker", heal = 1)
        drink(
            "tote_cup_of_tea_strong",
            heal = 3,
            effects = listOf(boost(stats.attack, constant = 3, percent = 2)),
        )
        drink("xmas22_beer", heal = 1)
        drink("xmas22_pine", heal = 1)
        drink("xmas22_eggnog", heal = 1)
        drink("sunbeam_ale", heal = 1)
        drink("steamforge_brew", heal = 1)
        drink("eclipse_wine", heal = 16, effects = listOf(drain(stats.attack, constant = 5)))
        drink("moonlite", heal = 1)
        drink("sunshine", heal = 1)
        drink("bandit_brew", heal = 1)

        // -- Drinks the live game refuses, or that do nothing at all. ---------------------------
        // The unfinished and spoilt Gnome cocktails predate the 2006 Restaurant rework and are
        // unobtainable; the wiki records no healing for any of them.
        for (name in INERT) {
            drink(name, heal = 0)
        }
        // Quest potions carrying a `Drink` op that live does not honour outside their quest.
        for (name in QUEST_POTIONS) {
            refusedFood(name, "You don't want to drink that.")
        }
    }
}
