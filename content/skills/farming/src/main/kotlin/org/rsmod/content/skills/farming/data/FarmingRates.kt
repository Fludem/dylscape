package org.rsmod.content.skills.farming.data

/**
 * The numbers that make this server's farming different from the real game.
 *
 * Everything else in the module is deliberately vanilla -- [FarmingCrops] carries the live OSRS
 * levels, xp and growth-stage counts -- so the whole "8x faster" decision lives here and nowhere
 * else. Change [GROWTH_SPEEDUP] and every crop in the game re-times itself.
 */
public object FarmingRates {
    /** How much faster crops grow than in OSRS. */
    public const val GROWTH_SPEEDUP: Int = 8

    /**
     * Weeds are decay, not growth, so they are left at the vanilla five minutes a stage: speeding
     * them up would only mean more raking, which is not what "faster growth" is asking for. An
     * emptied patch is fully overgrown again a quarter of an hour later.
     */
    public const val WEED_CYCLE_MINUTES: Int = 5

    /** Real time one growth stage of a `cycleMinutes` crop takes, in milliseconds. */
    public fun stageMillis(cycleMinutes: Int): Long = cycleMinutes * 60_000L / GROWTH_SPEEDUP

    /** Real time one weed stage takes, in milliseconds. */
    public fun weedMillis(): Long = WEED_CYCLE_MINUTES * 60_000L

    /**
     * Chance that harvesting a lives-based crop does *not* use one of its lives.
     *
     * OSRS gives every crop its own chance-to-save table; this is a single curve that lands in the
     * same place -- about four items from an unfertilised patch at low level, about eight from an
     * ultracomposted one at 99 -- without a table per crop.
     */
    public fun harvestSaveChance(farmingLevel: Int): Double =
        (0.10 + farmingLevel * 0.0025).coerceAtMost(0.35)
}
