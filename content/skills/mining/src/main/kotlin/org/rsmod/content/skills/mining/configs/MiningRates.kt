package org.rsmod.content.skills.mining.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.builders.enums.EnumBuilder
import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.enums.EnumReferences
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.api.type.script.dsl.EnumPluginBuilder
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.obj.ObjType

object MiningParams : ParamReferences() {
    val success_rates = find<EnumType<ObjType, Int>>("mining_pickaxe_success_rates")
}

internal object MiningParamBuilder : ParamBuilder() {
    init {
        build<EnumType<ObjType, Int>>("mining_pickaxe_success_rates")
    }
}

internal object MiningEnums : EnumReferences() {
    val copper_tin_pickaxes = find<ObjType, Int>("copper_tin_rock_pickaxes")
    val clay_pickaxes = find<ObjType, Int>("clay_rock_pickaxes")
    val iron_pickaxes = find<ObjType, Int>("iron_rock_pickaxes")
    val silver_pickaxes = find<ObjType, Int>("silver_rock_pickaxes")
    val coal_pickaxes = find<ObjType, Int>("coal_rock_pickaxes")
    val gold_pickaxes = find<ObjType, Int>("gold_rock_pickaxes")
    val gem_pickaxes = find<ObjType, Int>("gem_rock_pickaxes")
    val mithril_pickaxes = find<ObjType, Int>("mithril_rock_pickaxes")
    val adamantite_pickaxes = find<ObjType, Int>("adamantite_rock_pickaxes")
    val runite_pickaxes = find<ObjType, Int>("runite_rock_pickaxes")
    val amethyst_pickaxes = find<ObjType, Int>("amethyst_rock_pickaxes")
}

/**
 * Per-rock, per-pickaxe mining success rates, in the same `(low shl 16) or high` packing upstream
 * uses for its woodcutting axe enums. `low` is the roll weight at level 1 and `high` the weight at
 * level 99; `ProtectedAccess.statRandom` interpolates between them by the player's mining level.
 *
 * These are **tuned to sit in the right ore-per-hour band, not transcribed from a scraped source**,
 * so an individual pair may be a few points off live OSRS. What they do get right is the shape:
 * every rock is strictly ordered by pickaxe tier, and rates fall off sharply as ore tier rises, so
 * the relative grind between ores is faithful even where an absolute number is not. Retuning a rock
 * is a one-line change to its baseline below.
 */
internal object MiningEnumBuilder : EnumBuilder() {
    init {
        // Copper, tin and clay share OSRS's level-1 speed band.
        build<ObjType, Int>("copper_tin_rock_pickaxes") { pickaxeLadder(64, 200) }
        build<ObjType, Int>("clay_rock_pickaxes") { pickaxeLadder(64, 200) }
        build<ObjType, Int>("iron_rock_pickaxes") { pickaxeLadder(32, 100) }
        build<ObjType, Int>("silver_rock_pickaxes") { pickaxeLadder(16, 50) }
        build<ObjType, Int>("coal_rock_pickaxes") { pickaxeLadder(12, 40) }
        build<ObjType, Int>("gold_rock_pickaxes") { pickaxeLadder(10, 34) }
        build<ObjType, Int>("gem_rock_pickaxes") { pickaxeLadder(8, 28) }
        build<ObjType, Int>("mithril_rock_pickaxes") { pickaxeLadder(6, 22) }
        build<ObjType, Int>("adamantite_rock_pickaxes") { pickaxeLadder(4, 13) }
        build<ObjType, Int>("runite_rock_pickaxes") { pickaxeLadder(2, 7) }
        build<ObjType, Int>("amethyst_rock_pickaxes") { pickaxeLadder(3, 10) }
    }

    /**
     * Fills in every pickaxe tier from a single bronze baseline, using the multiplier ladder OSRS
     * applies across all of mining. Declaring a rock's difficulty once keeps the ladder internally
     * consistent and stops a mistuned rock from needing thirteen edits.
     */
    private fun EnumPluginBuilder<ObjType, Int>.pickaxeLadder(bronzeLow: Int, bronzeHigh: Int) {
        fun tier(mul: Double): Int {
            val low = (bronzeLow * mul).toInt().coerceAtLeast(1)
            val high = (bronzeHigh * mul).toInt().coerceAtLeast(low)
            return (low shl 16) or high
        }

        this[objs.bronze_pickaxe] = tier(1.0)
        this[MiningObjs.iron_pickaxe] = tier(1.5)
        this[MiningObjs.steel_pickaxe] = tier(2.0)
        this[MiningObjs.black_pickaxe] = tier(2.25)
        this[MiningObjs.mithril_pickaxe] = tier(2.5)
        this[MiningObjs.adamant_pickaxe] = tier(3.0)
        this[MiningObjs.rune_pickaxe] = tier(3.5)
        this[MiningObjs.gilded_pickaxe] = tier(3.5)
        this[objs.dragon_pickaxe] = tier(3.75)
        this[objs.dragon_pickaxe_upgraded] = tier(3.75)
        this[objs.dragon_pickaxe_or_trailblazer] = tier(3.75)
        this[objs.dragon_pickaxe_or_zalcano] = tier(3.75)
        this[objs.third_age_pickaxe] = tier(3.75)
        this[objs.infernal_pickaxe] = tier(3.75)
        this[objs.infernal_pickaxe_or] = tier(3.75)
        this[objs.crystal_pickaxe] = tier(4.0)
    }
}
