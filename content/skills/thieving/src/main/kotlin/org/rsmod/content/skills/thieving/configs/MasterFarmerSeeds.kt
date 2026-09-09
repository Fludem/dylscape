package org.rsmod.content.skills.thieving.configs

import kotlin.math.roundToInt
import org.rsmod.api.random.GameRandom
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.obj.ObjType

/**
 * Every seed a master farmer can be robbed of.
 *
 * Deliberately its own reference object rather than a dependency on `content:skills:farming`. A
 * `*References` entry is only a name lookup resolved against `.data/symbols/obj.sym`, so two
 * modules finding the same name is free and resolves independently; a Gradle edge between two
 * content modules to share thirty-odd `find` calls would not be.
 */
object ThievingSeeds : ObjReferences() {
    val asgarnian_hop_seed = find("asgarnian_hop_seed")
    val avantoe_seed = find("avantoe_seed")
    val barley_seed = find("barley_seed")
    val cabbage_seed = find("cabbage_seed")
    val cadantine_seed = find("cadantine_seed")
    val cadavaberry_bush_seed = find("cadavaberry_bush_seed")
    val dwarf_weed_seed = find("dwarf_weed_seed")
    val dwellberry_bush_seed = find("dwellberry_bush_seed")
    val guam_seed = find("guam_seed")
    val hammerstone_hop_seed = find("hammerstone_hop_seed")
    val harralander_seed = find("harralander_seed")
    val irit_seed = find("irit_seed")
    val jangerberry_bush_seed = find("jangerberry_bush_seed")
    val jute_seed = find("jute_seed")
    val krandorian_hop_seed = find("krandorian_hop_seed")
    val kwuarm_seed = find("kwuarm_seed")
    val lantadyme_seed = find("lantadyme_seed")
    val limpwurt_seed = find("limpwurt_seed")
    val marigold_seed = find("marigold_seed")
    val marrentill_seed = find("marrentill_seed")
    val nasturtium_seed = find("nasturtium_seed")
    val onion_seed = find("onion_seed")
    val poisonivy_bush_seed = find("poisonivy_bush_seed")
    val potato_seed = find("potato_seed")
    val ranarr_seed = find("ranarr_seed")
    val redberry_bush_seed = find("redberry_bush_seed")
    val rosemary_seed = find("rosemary_seed")
    val snape_grass_seed = find("snape_grass_seed")
    val snapdragon_seed = find("snapdragon_seed")
    val strawberry_seed = find("strawberry_seed")
    val sweetcorn_seed = find("sweetcorn_seed")
    val tarromin_seed = find("tarromin_seed")
    val toadflax_seed = find("toadflax_seed")
    val tomato_seed = find("tomato_seed")
    val torstol_seed = find("torstol_seed")
    val watermelon_seed = find("watermelon_seed")
    val whiteberry_bush_seed = find("whiteberry_bush_seed")
    val wildblood_hop_seed = find("wildblood_hop_seed")
    val woad_seed = find("woad_seed")
    val yanillian_hop_seed = find("yanillian_hop_seed")
}

/**
 * One row of the seed table.
 *
 * [worstOneIn] and [bestOneIn] are the wiki's rarity denominators, written as they are printed so
 * the table below can be read against the article line for line. Most seeds have a single rate and
 * leave [bestOneIn] defaulted; the four that the wiki prints as a range scale with Farming level.
 */
data class SeedSlot(
    val seed: ObjType,
    val quantity: IntRange,
    val worstOneIn: Double,
    val bestOneIn: Double = worstOneIn,
) {
    /**
     * The odds at [farmingLvl], interpolated linearly between the two published endpoints.
     *
     * The wiki says only that these rates "scale with Farming level up to 85" and gives the two
     * ends; the curve between them is not documented anywhere, so a straight line is the honest
     * reading rather than a guess dressed up as a formula. Every other row is flat and returns
     * [worstOneIn] on the first branch.
     */
    fun oneIn(farmingLvl: Int): Double {
        if (bestOneIn == worstOneIn) {
            return worstOneIn
        }
        val level = farmingLvl.coerceIn(1, SCALING_MAX_LEVEL)
        val progress = (level - 1).toDouble() / (SCALING_MAX_LEVEL - 1)
        return worstOneIn + (bestOneIn - worstOneIn) * progress
    }

    /**
     * The row's share of the roll space. Weights are `1/rarity` scaled up, so [WEIGHT_SCALE] has to
     * be large enough that the rarest row -- torstol, at one in nineteen thousand -- still rounds
     * to a meaningful integer.
     */
    fun weight(farmingLvl: Int): Int = (WEIGHT_SCALE / oneIn(farmingLvl)).roundToInt()

    private companion object {
        const val SCALING_MAX_LEVEL = 85
        const val WEIGHT_SCALE = 10_000_000.0
    }
}

/**
 * The master farmer's seed table, as OSRS rolls it.
 *
 * ### One roll, one seed
 *
 * The wiki prints this as five separate groups -- allotment, hops, flowers, bushes, herbs -- which
 * reads like five independent rolls. It is not: the published rarities sum to 0.997, so every
 * successful pickpocket pays exactly one seed off one table and the groups are only a presentation
 * device. That is why the rows below are a flat list and why [roll] normalises over the actual
 * weight total: the missing 0.3% is rounding in the wiki's own printed denominators, not a
 * nothing-slot.
 *
 * ### Why the master farmer is not on the pouch ladder
 *
 * Every other rung of [ThievingTargets] pays a stackable coin pouch. Master farmers pay seeds in
 * OSRS, and when the thieving module was written there was no farming module to own that table, so
 * they were given a fat purse instead as a stopgap. `content:skills:farming` now exists and the
 * patches take these very seeds, so the stopgap is gone and the real table is here.
 *
 * ### The quantities are pre-multiplier
 *
 * Like the coin ranges, these are the raw OSRS numbers; [ThievingRates.LOOT_RATE] is applied where
 * the seed is paid out, so the table stays directly comparable to the wiki.
 */
object MasterFarmerSeeds {
    val slots: List<SeedSlot> =
        listOf(
            // Allotment.
            SeedSlot(ThievingSeeds.potato_seed, 1..4, 5.65),
            SeedSlot(ThievingSeeds.onion_seed, 1..3, 7.53),
            SeedSlot(ThievingSeeds.cabbage_seed, 1..3, 14.4),
            SeedSlot(ThievingSeeds.tomato_seed, 1..2, 15.7),
            SeedSlot(ThievingSeeds.sweetcorn_seed, 1..2, 45.2),
            SeedSlot(ThievingSeeds.strawberry_seed, 1..1, 90.4),
            SeedSlot(ThievingSeeds.watermelon_seed, 1..1, 189.0),
            SeedSlot(ThievingSeeds.snape_grass_seed, 1..1, 260.0),
            // Hops.
            SeedSlot(ThievingSeeds.barley_seed, 1..12, 18.0),
            SeedSlot(ThievingSeeds.hammerstone_hop_seed, 1..9, 18.0),
            SeedSlot(ThievingSeeds.asgarnian_hop_seed, 1..6, 23.9),
            SeedSlot(ThievingSeeds.jute_seed, 1..9, 24.1),
            SeedSlot(ThievingSeeds.yanillian_hop_seed, 1..6, 36.1),
            SeedSlot(ThievingSeeds.krandorian_hop_seed, 1..6, 72.2),
            SeedSlot(ThievingSeeds.wildblood_hop_seed, 1..3, 142.0),
            // Flowers.
            SeedSlot(ThievingSeeds.marigold_seed, 1..1, 21.8),
            SeedSlot(ThievingSeeds.nasturtium_seed, 1..1, 32.9),
            SeedSlot(ThievingSeeds.rosemary_seed, 1..1, 50.9),
            SeedSlot(ThievingSeeds.woad_seed, 1..1, 68.9),
            SeedSlot(ThievingSeeds.limpwurt_seed, 1..1, 86.3),
            // Bushes.
            SeedSlot(ThievingSeeds.redberry_bush_seed, 1..1, 25.8),
            SeedSlot(ThievingSeeds.cadavaberry_bush_seed, 1..1, 36.8),
            SeedSlot(ThievingSeeds.dwellberry_bush_seed, 1..1, 51.5),
            SeedSlot(ThievingSeeds.jangerberry_bush_seed, 1..1, 129.0),
            SeedSlot(ThievingSeeds.whiteberry_bush_seed, 1..1, 355.0),
            SeedSlot(ThievingSeeds.poisonivy_bush_seed, 1..1, 937.0),
            // Herbs. Four of these scale with Farming level; see `SeedSlot.oneIn`.
            SeedSlot(ThievingSeeds.guam_seed, 1..1, worstOneIn = 67.2, bestOneIn = 58.36),
            SeedSlot(ThievingSeeds.marrentill_seed, 1..1, 95.6),
            SeedSlot(ThievingSeeds.tarromin_seed, 1..1, 140.0),
            SeedSlot(ThievingSeeds.harralander_seed, 1..1, 206.0),
            SeedSlot(ThievingSeeds.ranarr_seed, 1..1, worstOneIn = 555.83, bestOneIn = 268.75),
            SeedSlot(ThievingSeeds.toadflax_seed, 1..1, 443.0),
            SeedSlot(ThievingSeeds.irit_seed, 1..1, 651.0),
            SeedSlot(ThievingSeeds.avantoe_seed, 1..1, 947.0),
            SeedSlot(ThievingSeeds.kwuarm_seed, 1..1, 1389.0),
            SeedSlot(ThievingSeeds.snapdragon_seed, 1..1, worstOneIn = 3835.23, bestOneIn = 1854.4),
            SeedSlot(ThievingSeeds.cadantine_seed, 1..1, 2976.0),
            SeedSlot(ThievingSeeds.lantadyme_seed, 1..1, 4167.0),
            SeedSlot(ThievingSeeds.dwarf_weed_seed, 1..1, 6944.0),
            SeedSlot(ThievingSeeds.torstol_seed, 1..1, worstOneIn = 19176.14, bestOneIn = 9271.98),
        )

    /**
     * Picks one row, weighted by its rarity at [farmingLvl].
     *
     * Recomputed per roll rather than cached per level: four rows move with Farming, the whole
     * table is forty entries, and a pickpocket happens at most once every three ticks.
     */
    fun roll(random: GameRandom, farmingLvl: Int): SeedSlot {
        val weights = IntArray(slots.size) { slots[it].weight(farmingLvl) }
        var cursor = random.of(weights.sum())
        for (index in slots.indices) {
            cursor -= weights[index]
            if (cursor < 0) {
                return slots[index]
            }
        }
        // Unreachable: `cursor` starts below the total and every weight is positive.
        return slots.last()
    }
}
