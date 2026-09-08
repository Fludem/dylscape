package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.params
import org.rsmod.api.type.editors.loc.LocEditor
import org.rsmod.api.type.editors.obj.ObjEditor
import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.content.skills.mining.configs.MiningParams.success_rates
import org.rsmod.game.stat.PlayerStatMap
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

private typealias ores = MiningObjs

private typealias rocks = MiningRockLocs

private typealias rates = MiningEnums

/**
 * The ore rocks and the spent rocks they turn into.
 *
 * Every ore has two cache variants (`copperrock1`/`copperrock2` and so on) which are two model
 * orientations of the same rock, not a full/empty pair. The spent rock is a separate loc:
 * `rocks1`/`rocks2`/`rocks3`, all named "Rocks". Ops were decoded from the cache rather than
 * assumed — every ore rock carries `Mine` on **op1** and `hidden` on op3.
 */
internal object MiningRockLocs : LocReferences() {
    val copper_1 = find("copperrock1")
    val copper_2 = find("copperrock2")
    val tin_1 = find("tinrock1")
    val tin_2 = find("tinrock2")
    val clay_1 = find("clayrock1")
    val clay_2 = find("clayrock2")
    val iron_1 = find("ironrock1")
    val iron_2 = find("ironrock2")
    val silver_1 = find("silverrock1")
    val silver_2 = find("silverrock2")
    val coal_1 = find("coalrock1")
    val coal_2 = find("coalrock2")
    val gold_1 = find("goldrock1")
    val gold_2 = find("goldrock2")
    val mithril_1 = find("mithrilrock1")
    val mithril_2 = find("mithrilrock2")
    val adamantite_1 = find("adamantiterock1")
    val adamantite_2 = find("adamantiterock2")
    val runite_1 = find("runiterock1")
    val runite_2 = find("runiterock2")
    val amethyst_1 = find("amethystrock1")
    val amethyst_2 = find("amethystrock2")

    val spent_1 = find("rocks1")
    val spent_2 = find("rocks2")
}

/**
 * Levels, XP and respawn timings per ore.
 *
 * Levels and XP are the real OSRS values. Respawn times are **tuned, not scraped** — they are in
 * the right band and correctly ordered (copper seconds, runite minutes), but a given rock may be
 * off by a few ticks against live.
 *
 * `deplete_chance = 0` makes a rock always deplete on a successful swing, which is how mining works
 * in OSRS: one ore per rock, then it turns to rubble. The comparison upstream's skilling code
 * performs is `random.of(1, 255) > deplete_chance`, so zero means "always".
 *
 * Gem rocks are deliberately absent: they roll a random gem from a weighted table rather than
 * yielding one fixed product, which the shared skilling script has no notion of.
 */
internal object MiningRocksEditor : LocEditor() {
    init {
        rock(
            rocks.clay_1,
            rocks.spent_1,
            ores.clay,
            level = 1,
            xp = 5.0,
            respawn = 4,
            rates.clay_pickaxes,
        )
        rock(
            rocks.clay_2,
            rocks.spent_2,
            ores.clay,
            level = 1,
            xp = 5.0,
            respawn = 4,
            rates.clay_pickaxes,
        )
        rock(rocks.copper_1, rocks.spent_1, ores.copper_ore, 1, 17.5, 4, rates.copper_tin_pickaxes)
        rock(rocks.copper_2, rocks.spent_2, ores.copper_ore, 1, 17.5, 4, rates.copper_tin_pickaxes)
        rock(rocks.tin_1, rocks.spent_1, ores.tin_ore, 1, 17.5, 4, rates.copper_tin_pickaxes)
        rock(rocks.tin_2, rocks.spent_2, ores.tin_ore, 1, 17.5, 4, rates.copper_tin_pickaxes)
        rock(rocks.iron_1, rocks.spent_1, ores.iron_ore, 15, 35.0, 9, rates.iron_pickaxes)
        rock(rocks.iron_2, rocks.spent_2, ores.iron_ore, 15, 35.0, 9, rates.iron_pickaxes)
        rock(rocks.silver_1, rocks.spent_1, ores.silver_ore, 20, 40.0, 100, rates.silver_pickaxes)
        rock(rocks.silver_2, rocks.spent_2, ores.silver_ore, 20, 40.0, 100, rates.silver_pickaxes)
        rock(rocks.coal_1, rocks.spent_1, ores.coal, 30, 50.0, 50, rates.coal_pickaxes)
        rock(rocks.coal_2, rocks.spent_2, ores.coal, 30, 50.0, 50, rates.coal_pickaxes)
        rock(rocks.gold_1, rocks.spent_1, ores.gold_ore, 40, 65.0, 100, rates.gold_pickaxes)
        rock(rocks.gold_2, rocks.spent_2, ores.gold_ore, 40, 65.0, 100, rates.gold_pickaxes)
        rock(
            rocks.mithril_1,
            rocks.spent_1,
            ores.mithril_ore,
            55,
            80.0,
            200,
            rates.mithril_pickaxes,
        )
        rock(
            rocks.mithril_2,
            rocks.spent_2,
            ores.mithril_ore,
            55,
            80.0,
            200,
            rates.mithril_pickaxes,
        )
        rock(
            rocks.adamantite_1,
            rocks.spent_1,
            ores.adamantite_ore,
            70,
            95.0,
            400,
            rates.adamantite_pickaxes,
        )
        rock(
            rocks.adamantite_2,
            rocks.spent_2,
            ores.adamantite_ore,
            70,
            95.0,
            400,
            rates.adamantite_pickaxes,
        )
        rock(rocks.runite_1, rocks.spent_1, ores.runite_ore, 85, 125.0, 500, rates.runite_pickaxes)
        rock(rocks.runite_2, rocks.spent_2, ores.runite_ore, 85, 125.0, 500, rates.runite_pickaxes)
        rock(
            rocks.amethyst_1,
            rocks.spent_1,
            ores.amethyst,
            92,
            240.0,
            200,
            rates.amethyst_pickaxes,
        )
        rock(
            rocks.amethyst_2,
            rocks.spent_2,
            ores.amethyst,
            92,
            240.0,
            200,
            rates.amethyst_pickaxes,
        )
    }

    private fun rock(
        type: LocType,
        spent: LocType,
        ore: ObjType,
        level: Int,
        xp: Double,
        respawn: Int,
        successRates: EnumType<ObjType, Int>,
    ) {
        edit(type) {
            contentGroup = MiningContent.mining_rock
            param[params.levelrequire] = level
            param[params.skill_xp] = PlayerStatMap.toFineXP(xp).toInt()
            param[params.skill_productitem] = ore
            param[params.next_loc_stage] = spent
            param[params.respawn_time] = respawn
            param[params.deplete_chance] = 0
            param[success_rates] = successRates
        }
    }
}

/**
 * Puts the ores into upstream's `content.ore` group.
 *
 * The group already exists in `content.sym` and Lumbridge's `SmithingApprentice` already tests
 * `content.ore in player.inv`, but nothing was ever tagged into it — so that check could never
 * fire. Tagging here makes the existing dialogue work as written.
 */
internal object MiningOres : ObjEditor() {
    init {
        for (ore in
            listOf(
                ores.clay,
                ores.copper_ore,
                ores.tin_ore,
                ores.iron_ore,
                ores.silver_ore,
                ores.coal,
                ores.gold_ore,
                ores.mithril_ore,
                ores.adamantite_ore,
                ores.runite_ore,
            )) {
            edit(ore) { contentGroup = content.ore }
        }
    }
}
