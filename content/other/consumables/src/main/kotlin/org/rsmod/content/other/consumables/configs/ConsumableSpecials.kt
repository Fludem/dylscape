package org.rsmod.content.other.consumables.configs

/**
 * Everything that is not simply "eat it, gain N hitpoints".
 *
 * Seven mechanics live here, and every row picks exactly one, which is what stops the long tail of
 * odd foods from quietly defaulting to the wrong behaviour:
 * - **scaled** - a flat amount plus a percentage of the base Hitpoints level. The kebabs and the
 *   vegetables genuinely work this way; it is not the wiki rounding a number off.
 * - **random** - an inclusive roll, flat, regardless of level. Cave eel and the snail meats.
 * - **delayed** - the Varlamore hunter meats heal twice, the second helping a few ticks later.
 * - **overheal** - anglerfish and the honey locust push past the base level.
 * - **combo** - karambwans ride their own clock, so they stack with an ordinary food.
 * - **harmful** - the rock cakes, the nightshades and the poison karambwan cost hitpoints.
 * - **refused** - the obj carries an `Eat` op the live game will not honour. These are not gaps
 *   waiting to be filled: rotten apples, the Hosidius servery food and the quest kebabs all print a
 *   refusal in the real game, and the row exists to say so rather than let the click fall through
 *   to "Nothing interesting happens."
 *
 * The `food(name, 0)` rows are the unobtainable leftovers - 2005 Hallowe'en sweets, 2020 candy and
 * the historical Gnome Restaurant intermediates from before the 2006 rework. They carry an `Eat` op
 * and the wiki records no healing for any of them, so they are consumed for nothing rather than
 * handed an invented number.
 */
internal object ConsumableSpecials : ConsumableFamily() {
    private const val HONEY_LOCUST_OVERHEAL = 20

    init {
        energyFood(
            "macro_triffidfruit",
            energy = 30,
            extraMessage = "It tastes great, some of your energy is restored!",
        )
        scaledFood("kebab", heal = 3, percent = 7)
        refusedFood("rottenapples", "You don't want to eat that.")
        food("equa_toads_legs", 0)
        food("spicy_toads_legs", 0)
        food("seasoned_toads_legs", 0)
        food("spicy_worm", 0)
        food("spoilt_gnomebowl", 0)
        food("unfinished_chocolate_bomb1", 0)
        food("unfinished_chocolate_bomb2", 0)
        food("unfinished_chocolate_bomb3", 0)
        food("unfinished_worm_hole", 0)
        food("unfinished_veg_ball", 0)
        food("spoilt_crunchies", 0)
        food("unfinished_chocchip_crunchies", 0)
        food("unfinished_spicy_crunchies", 0)
        food("unfinished_toad_crunchies", 0)
        food("spoilt_batta", 0)
        food("unfinished_worm_batta", 0)
        food("unfinished_cheese+tom_batta", 0)
        food("fruitless_batta", 0)
        food("fruit_batta_lime", 0)
        food("fruit_batta_orange", 0)
        food("fruit_batta_pineapple", 0)
        food("fruit_batta_limeorange", 0)
        food("fruit_batta_limepineapple", 0)
        food("fruit_batta_orangepineapple", 0)
        food("unfinished_fruit_batta", 0)
        food("unfinished_vegetable_batta", 0)
        harmfulFood("rockcake", damage = 1, message = "You take a bite of the rock cake. Ouch!")
        harmfulFood("nightshade", damage = 15, message = "Ahhhh! What have I done!")
        comboFood("tbwt_cooked_karambwan", 18)
        harmfulFood(
            "tbwt_poorly_cooked_karambwan",
            damage = 5,
            message = "That food was undercooked!",
            combo = true,
        )
        refusedFood("tbwt_seaweed_in_monkey_skin_sandwich", "It's not for eating.")
        randomFood("snail_corpse_cooked1", 5, 7)
        randomFood("snail_corpse_cooked2", 5, 8)
        randomFood("snail_corpse_cooked3", 7, 9)
        randomFood("mort_slimey_eel_cooked", 6, 10)
        food("easter_egg_2005_lightblue", 0)
        food("easter_egg_2005_darkblue", 0)
        food("easter_egg_2005_white", 0)
        energyFood(
            "easter_egg_2005_purple",
            energy = 10,
            healRange = 1..3,
            extraMessage = "The sugary goodness heals some energy.",
        )
        food("easter_egg_2005_red", 0)
        food("easter_egg_2005_green", 0)
        food("easter_egg_2005_pink", 0)
        scaledFood("super_kebab", heal = 3, percent = 7)
        randomFood("cave_eel", 8, 12)
        randomFood("giant_frogspawn", 3, 6)
        randomFood("strawberry", 1, 6)
        refusedFood("rotten_potato", "You don't want to eat that.")
        randomFood("watermelon", 1, 5)
        randomFood("watermelon_slice", 1, 5)
        randomFood("sweetcorn_cooked", 1, 10)
        refusedFood("evil_bob_cooked_fish_correct", "You don't feel like eating that right now.")
        refusedFood("evil_bob_cooked_fish_incorrect", "You don't feel like eating that right now.")
        randomFood("tbw_spider_on_stick_cooked", 7, 10)
        randomFood("tbw_spider_on_shaft_cooked", 7, 10)
        refusedFood("ratcatchers_poisonedcheese", "Um... let me think about this one... no.")
        scaledFood("bowl_sweetcorn", heal = 1, percent = 10)
        harmfulFood(
            "hundred_dwarf_hot_rockcake",
            damage = 1,
            message = "You take a bite of the rock cake. Ouch!",
        )
        harmfulFood(
            "hundred_dwarf_cool_rockcake",
            damage = 1,
            message = "You take a bite of the rock cake. Ouch!",
        )
        energyFood(
            "aluft_gnome_mint_cake",
            energy = 50,
            extraMessage = "It tastes great, some of your energy is restored!",
        )
        energyFood(
            "trail_sweets",
            energy = 10,
            healRange = 1..3,
            extraMessage = "The sugary goodness heals some energy.",
        )
        refusedFood("grim_turnip", "You don't want to eat that.")
        refusedFood("snakeboss_eel", "You don't want to eat that.")
        refusedFood("hosidius_servery_meat_pie", "That's not for you - it's for the soldiers.")
        refusedFood("hosidius_servery_plain_pizza", "That's not for you - it's for the soldiers.")
        refusedFood(
            "hosidius_servery_pineapple_pizza",
            "That's not for you - it's for the soldiers.",
        )
        refusedFood("hosidius_servery_cooked_meat", "That's not for you - it's for the soldiers.")
        refusedFood("hosidius_servery_potato", "That's not for you - it's for the soldiers.")
        refusedFood("hosidius_servery_stew", "That's not for you - it's for the soldiers.")
        scaledFood("hosidius_tithe_fruit_a", heal = 1, percent = 0)
        scaledFood("hosidius_tithe_fruit_b", heal = 2, percent = 0)
        scaledFood("hosidius_tithe_fruit_c", heal = 3, percent = 0)
        overhealFood("anglerfish", heal = 22, overheal = ::anglerfishBonus)
        refusedFood("infernal_eel", "You don't want to eat that.")
        comboFood("br_tbwt_cooked_karambwan", 18)
        overhealFood("blighted_anglerfish", heal = 22, overheal = ::anglerfishBonus)
        comboFood("blighted_karambwan", 18)
        food("hw20_candy_brown", 0)
        food("hw20_candy_blue", 0)
        food("hw20_candy_white", 0)
        food("hw20_candy_purple", 0)
        food("hw20_candy_red", 0)
        food("hw20_candy_green", 0)
        food("hw20_candy_black", 0)
        food("hw20_candy_orange", 0)
        food("hw20_candy_pink", 0)
        refusedFood("easter22_special_kebab", "Nope, I am not going to eat that!")
        overhealFood("toa_honey_locust", heal = 20, overheal = ::honeyLocustBonus)
        harmfulFood("stackable_nightshade", damage = 15, message = "Ahhhh! What have I done!")
        scaledFood("cooked_lizard", heal = 0, percent = 33)
        delayedFood("wildkebbit_cooked", heal = 4, delayed = 4)
        delayedFood("barbkebbit_cooked", heal = 7, delayed = 5)
        delayedFood("dashingkebbit_cooked", heal = 13, delayed = 10)
        delayedFood("fennecfox_cooked", heal = 11, delayed = 8)
        delayedFood("antelopesun_cooked", heal = 12, delayed = 9)
        delayedFood("antelopemoon_cooked", heal = 14, delayed = 12)
        delayedFood("larupia_cooked", heal = 6, delayed = 5)
        delayedFood("graahk_cooked", heal = 8, delayed = 6)
        delayedFood("kyatt_cooked", heal = 9, delayed = 8)
        scaledFood("bream_fish_cooked", heal = 0, percent = 33)
        harmfulFood("araxyte_venom_sack", damage = 4, message = "The venom burns as it goes down.")
        scaledFood("varlamorian_kebab", heal = 4, percent = 10)
    }

    /**
     * How far anglerfish may push hitpoints above the base level.
     *
     * A bracket table rather than a formula, exactly as live: the bonus steps at Hitpoints 10, 25,
     * 50, 75 and 93 rather than scaling smoothly.
     */
    private fun anglerfishBonus(base: Int): Int =
        when {
            base < 10 -> 2
            base < 25 -> 4
            base < 50 -> 6
            base < 75 -> 8
            base < 93 -> 10
            else -> 13
        }

    /** The honey locust always heals 20 and always overheals, so the cap is simply the heal. */
    private fun honeyLocustBonus(base: Int): Int = HONEY_LOCUST_OVERHEAL
}
