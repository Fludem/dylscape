package org.rsmod.content.skills.farming.data

/**
 * What one player has done to one patch.
 *
 * Growth is *not* ticked. Every timestamp here is wall-clock epoch milliseconds, and the current
 * growth stage is worked out from the difference whenever anybody asks. That is what lets a crop
 * keep growing while its owner is logged out -- there is no per-tick work to miss, and a patch
 * planted before a server restart comes back at exactly the stage it should be at.
 */
public class PatchState {
    /** Obj id of the planted seed, or -1 for an empty patch. */
    public var seedId: Int = -1

    /** When the seed went in. Meaningless while [seedId] is -1. */
    public var plantedAt: Long = 0L

    /** When the patch was last emptied, which is when weeds start creeping back. */
    public var clearedAt: Long = 0L

    /** Lives already spent on a [HarvestModel.Lives] crop. */
    public var livesUsed: Int = 0

    /** Produce already taken from a [HarvestModel.Counted] crop since it last filled up. */
    public var picked: Int = 0

    /** When the last piece of produce regrew, for crops that regrow. */
    public var regrewAt: Long = 0L

    /** Whether `Check-health` has been done, which is what unlocks a tree's produce and its xp. */
    public var checked: Boolean = false

    /** Whether a grown tree has been cut down and is now a stump waiting to be dug out. */
    public var felled: Boolean = false

    public var compost: CompostTier = CompostTier.None

    public val empty: Boolean
        get() = seedId == -1

    public fun clear(now: Long) {
        seedId = -1
        plantedAt = 0L
        clearedAt = now
        livesUsed = 0
        picked = 0
        regrewAt = 0L
        checked = false
        felled = false
        compost = CompostTier.None
    }

    /**
     * Compost is deliberately carried through: a patch is treated *before* the seed goes in, which
     * is the only order the real game allows, so wiping it here would make composting a no-op.
     */
    public fun plant(seedId: Int, now: Long) {
        val treated = compost
        clear(now)
        compost = treated
        this.seedId = seedId
        plantedAt = now
    }

    /** Growth stages completed since planting, capped at the crop's total. */
    public fun stagesGrown(crop: FarmingCrop, now: Long): Int {
        val elapsed = (now - plantedAt).coerceAtLeast(0L)
        val stages = elapsed / FarmingRates.stageMillis(crop.cycleMinutes)
        return stages.coerceAtMost(crop.growthStages.toLong()).toInt()
    }

    public fun fullyGrown(crop: FarmingCrop, now: Long): Boolean =
        stagesGrown(crop, now) >= crop.growthStages

    /** Lives a lives-based crop still has, compost included. */
    public fun livesLeft(crop: FarmingCrop): Int =
        (crop.baseLives + compost.extraLives - livesUsed).coerceAtLeast(0)

    /**
     * Produce currently hanging on a counted crop.
     *
     * Regrowth is derived the same way growth is: the number of items that have grown back since
     * [regrewAt] is a division, not a timer, so a fruit tree fills up again while its owner is
     * offline.
     */
    public fun produceLeft(crop: FarmingCrop, now: Long): Int {
        if (crop.maxProduce == 0) {
            return 0
        }
        val regrown =
            if (crop.regrowMinutes <= 0) {
                0
            } else {
                val since = (now - regrewAt).coerceAtLeast(0L)
                (since / FarmingRates.stageMillis(crop.regrowMinutes)).toInt()
            }
        val taken = (picked - regrown).coerceAtLeast(0)
        return (crop.maxProduce - taken).coerceIn(0, crop.maxProduce)
    }

    /**
     * Books the regrowth clock forward before taking an item, so that partial progress towards the
     * next piece of fruit is not silently thrown away by the pick.
     */
    public fun takeProduce(crop: FarmingCrop, now: Long) {
        if (crop.regrowMinutes > 0) {
            val stage = FarmingRates.stageMillis(crop.regrowMinutes)
            val regrown = ((now - regrewAt).coerceAtLeast(0L) / stage).toInt()
            if (regrown > 0) {
                picked = (picked - regrown).coerceAtLeast(0)
                regrewAt += regrown * stage
            }
            if (picked == 0) {
                regrewAt = now
            }
        }
        picked++
    }

    /**
     * The varbit value the client should be shown for this patch right now.
     *
     * This is the only place that turns state into an appearance, and it is why nothing in this
     * module ever writes a raw state number: the loc the player sees, the op they get offered, and
     * the branch [FarmingScript] takes all come from here.
     */
    public fun displayState(kind: PatchKind, crop: FarmingCrop?, now: Long): Int {
        if (crop == null || empty) {
            return weedState(kind, now)
        }
        val grown = stagesGrown(crop, now)
        if (grown < crop.growthStages) {
            return crop.seedState + grown
        }
        if (crop.checkState != -1 && !checked) {
            return crop.checkState
        }
        if (felled && crop.stumpState != -1) {
            return crop.stumpState
        }
        val left = produceLeft(crop, now)
        if (left > 0 && crop.harvestStates.isNotEmpty()) {
            return crop.harvestStates[left - 1]
        }
        return if (crop.grownState != -1) crop.grownState else kind.cleared
    }

    /** Bare soil creeping back to weeds, one stage at a time. */
    private fun weedState(kind: PatchKind, now: Long): Int {
        if (kind.weeds.isEmpty()) {
            return kind.cleared
        }
        val elapsed = (now - clearedAt).coerceAtLeast(0L)
        val stages = (elapsed / FarmingRates.weedMillis()).coerceAtMost(kind.rakes.toLong()).toInt()
        if (stages == 0) {
            return kind.cleared
        }
        // `weeds` runs worst-first, so the last entry is the first stage of regrowth.
        return kind.weeds[kind.rakes - stages]
    }

    /**
     * Rakes one stage of weeds off, and reports whether that finished the job.
     *
     * Raking works by moving [clearedAt] forward rather than by storing a weed counter, which keeps
     * the "how weedy is this" answer in one place.
     */
    public fun rake(kind: PatchKind, now: Long): Boolean {
        val current = weedState(kind, now)
        val index = kind.weeds.indexOf(current)
        if (index == -1) {
            return true
        }
        val remaining = kind.rakes - index
        clearedAt = now - (remaining - 1) * FarmingRates.weedMillis()
        return weedState(kind, now) == kind.cleared
    }
}
