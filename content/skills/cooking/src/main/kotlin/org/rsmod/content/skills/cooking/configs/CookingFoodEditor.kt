package org.rsmod.content.skills.cooking.configs

import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.obj.ObjType

/**
 * The cooking table.
 *
 * Levels and xp are the real OSRS values. The stop-burn levels are the live ones as well, to the
 * best of the wiki's knowledge: a fire first, then a range, which is always a few levels kinder.
 * How *often* a food burns below those levels is the one tuned number, and it lives in
 * [org.rsmod.content.skills.cooking.scripts.CookingRolls].
 *
 * Which raw fish burns into which anonymous "Burnt fish" is the one thing here the cache does not
 * say. The assignment follows the item-id blocks the fish were created in: `burntfish1` (323)
 * closes the shrimp/anchovy block, `burntfish2` (343) the sardine-to-cod block that trout, salmon
 * and cod share, `burntfish3` (357) the herring/pike/mackerel block and `burntfish4` (367) the
 * tuna/bass block, with `burntfish5` (369) left for the sardine. All five are named "Burnt fish" in
 * game, so a wrong guess would only ever show in the examine text.
 *
 * Not here, on purpose: pies, pizzas, cakes and stews are multi-step recipes with their own dishes
 * and toppings, and karambwan and the Wintertodt/Tempoross foods have mechanics of their own. They
 * belong to modules that do not exist yet.
 */
internal object CookingFoodEditor : ObjEditor() {
    init {
        // Fish.
        food(
            CookingObjs.raw_shrimp,
            CookingObjs.shrimp,
            CookingObjs.burnt_shrimp,
            level = 1,
            xp = 30.0,
            fire = 34,
            range = 33,
            name = "the shrimps",
        )
        food(
            CookingObjs.raw_anchovies,
            CookingObjs.anchovies,
            CookingObjs.burnt_fish_anchovies,
            level = 1,
            xp = 30.0,
            fire = 34,
            range = 33,
            name = "the anchovies",
        )
        food(
            CookingObjs.raw_sardine,
            CookingObjs.sardine,
            CookingObjs.burnt_fish_sardine,
            level = 1,
            xp = 40.0,
            fire = 38,
            range = 35,
            name = "the sardine",
        )
        food(
            CookingObjs.raw_herring,
            CookingObjs.herring,
            CookingObjs.burnt_fish_herring,
            level = 5,
            xp = 50.0,
            fire = 41,
            range = 38,
            name = "the herring",
        )
        food(
            CookingObjs.raw_mackerel,
            CookingObjs.mackerel,
            CookingObjs.burnt_fish_herring,
            level = 10,
            xp = 60.0,
            fire = 45,
            range = 42,
            name = "the mackerel",
        )
        food(
            CookingObjs.raw_trout,
            CookingObjs.trout,
            CookingObjs.burnt_fish_trout,
            level = 15,
            xp = 70.0,
            fire = 50,
            range = 47,
            name = "the trout",
        )
        food(
            CookingObjs.raw_cod,
            CookingObjs.cod,
            CookingObjs.burnt_fish_trout,
            level = 18,
            xp = 75.0,
            fire = 52,
            range = 49,
            name = "the cod",
        )
        food(
            CookingObjs.raw_pike,
            CookingObjs.pike,
            CookingObjs.burnt_fish_herring,
            level = 20,
            xp = 80.0,
            fire = 55,
            range = 52,
            name = "the pike",
        )
        food(
            CookingObjs.raw_salmon,
            CookingObjs.salmon,
            CookingObjs.burnt_fish_trout,
            level = 25,
            xp = 90.0,
            fire = 58,
            range = 55,
            name = "the salmon",
        )
        food(
            CookingObjs.raw_tuna,
            CookingObjs.tuna,
            CookingObjs.burnt_fish_tuna,
            level = 30,
            xp = 100.0,
            fire = 63,
            range = 60,
            name = "the tuna",
        )
        food(
            CookingObjs.raw_lobster,
            CookingObjs.lobster,
            CookingObjs.burnt_lobster,
            level = 40,
            xp = 120.0,
            fire = 74,
            range = 70,
            name = "the lobster",
        )
        food(
            CookingObjs.raw_bass,
            CookingObjs.bass,
            CookingObjs.burnt_fish_tuna,
            level = 43,
            xp = 130.0,
            fire = 80,
            range = 75,
            name = "the bass",
        )
        food(
            CookingObjs.raw_swordfish,
            CookingObjs.swordfish,
            CookingObjs.burnt_swordfish,
            level = 45,
            xp = 140.0,
            fire = 86,
            range = 81,
            name = "the swordfish",
        )
        // Sharks burn right up to 99 without cooking gauntlets, which do not exist here yet.
        food(
            CookingObjs.raw_shark,
            CookingObjs.shark,
            CookingObjs.burnt_shark,
            level = 80,
            xp = 210.0,
            fire = 99,
            range = 99,
            name = "the shark",
        )

        // Meat. Beef, bear and rat all cook into the same "Cooked meat".
        for (raw in
            listOf(CookingObjs.raw_beef, CookingObjs.raw_bear_meat, CookingObjs.raw_rat_meat)) {
            food(
                raw,
                CookingObjs.cooked_meat,
                CookingObjs.burnt_meat,
                level = 1,
                xp = 30.0,
                fire = 34,
                range = 33,
                name = "the meat",
            )
        }
        food(
            CookingObjs.raw_chicken,
            CookingObjs.cooked_chicken,
            CookingObjs.burnt_chicken,
            level = 1,
            xp = 30.0,
            fire = 34,
            range = 33,
            name = "the chicken",
        )

        // Bread. Range only: there is no baking over a campfire.
        food(
            CookingObjs.bread_dough,
            CookingObjs.bread,
            CookingObjs.burnt_bread,
            level = 1,
            xp = 40.0,
            fire = 37,
            range = 37,
            name = "the bread",
            rangeOnly = true,
        )

        // Tutorial Island's shrimps cook into the ordinary ones.
        food(
            CookingObjs.newbie_raw_shrimp,
            CookingObjs.shrimp,
            CookingObjs.burnt_shrimp,
            level = 1,
            xp = 30.0,
            fire = 34,
            range = 33,
            name = "the shrimps",
        )
    }

    private fun food(
        raw: ObjType,
        cooked: ObjType,
        burnt: ObjType,
        level: Int,
        xp: Double,
        fire: Int,
        range: Int,
        name: String,
        rangeOnly: Boolean = false,
    ) {
        edit(raw) {
            contentGroup = CookingContent.cooking_raw
            param[params.levelrequire] = level
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
            param[params.skill_productitem] = cooked
            param[CookingParams.burnt] = burnt
            param[CookingParams.stop_burn_fire] = fire
            param[CookingParams.stop_burn_range] = range
            param[CookingParams.range_only] = rangeOnly
            param[CookingParams.name] = name
        }
    }
}
