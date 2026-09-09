package org.rsmod.content.skills.farming.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.timer.TimerReferences

/** The tools a farmer carries, and the buckets that go on a patch. */
object FarmingObjs : ObjReferences() {
    val rake = find("rake")
    val spade = find("spade")
    val dibber = find("dibber")
    val secateurs = find("secateurs")
    val gardening_trowel = find("gardening_trowel")

    val bucket_empty = find("bucket_empty")
    val bucket_compost = find("bucket_compost")
    val bucket_supercompost = find("bucket_supercompost")
    val bucket_ultracompost = find("bucket_ultracompost")

    /** Full watering cans, `watering_can_1` through `_8`; `watering_can_0` is the empty one. */
    val watering_can_0 = find("watering_can_0")
    val watering_cans =
        listOf(
            find("watering_can_1"),
            find("watering_can_2"),
            find("watering_can_3"),
            find("watering_can_4"),
            find("watering_can_5"),
            find("watering_can_6"),
            find("watering_can_7"),
            find("watering_can_8"),
        )

    val weeds = find("weeds")

    // The plant pot chain that turns a tree seed into a sapling.
    val plantpot_empty = find("plantpot_empty")
    val plantpot_compost = find("plantpot_compost")
    val plantpot_supercompost = find("plantpot_supercompost")
}

/**
 * Animations, all of them the cache's own farming set.
 *
 * The picking animations come in three heights because the client has one per crop height; a herb
 * is picked at [picking_low], fruit off a tree at [picking_high].
 */
object FarmingSeqs : SeqReferences() {
    val raking = find("farming_raking")
    val dibbing = find("farming_seed_dibbing")
    val dig = find("human_dig")
    val trowel_dig = find("farming_trowel_dig")
    val fill_plantpot = find("farming_filling_plantpot")
    val watering = find("farming_watering")
    val picking_low = find("picking_low")
    val picking_mid = find("picking_mid")
    val picking_high = find("picking_high")
    val pick_mushroom = find("farming_pick_mushroom")
}

object FarmingTimers : TimerReferences() {
    /** Keeps the transmit varbits in step with the patches around the player. */
    val sync = find("farming_patch_sync")

    /** Counts a seed in a plant pot down to a sapling. */
    val sapling = find("farming_sapling_growth")
}
