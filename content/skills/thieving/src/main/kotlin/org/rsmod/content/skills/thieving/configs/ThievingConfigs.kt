package org.rsmod.content.skills.thieving.configs

import kotlin.math.roundToInt
import org.rsmod.api.type.refs.content.ContentReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.spot.SpotanimReferences

/**
 * Animations the skill uses. Both ship in the rev 233 cache; neither is declared in `BaseSeqs`, so
 * they are picked up here.
 */
object ThievingSeqs : SeqReferences() {
    val pickpocket = find("human_pickpocket")
    val stunned = find("stunned_thieving")
}

/** The stars-over-the-head graphic a caught thief wears. */
object ThievingSpotanims : SpotanimReferences() {
    val stunned = find("stunned_thieving")
}

/**
 * Content groups this module owns.
 *
 * Only the coin pouches get one. The pickpocket targets deliberately do **not**: `contentGroup` is
 * a single field on a type, `man`/`woman` are already tagged into upstream's `content.person` for
 * their Talk-to dialogue, and `TypeUpdaterConfigs.merge` folds competing edits in ClassGraph scan
 * order — so tagging them here would clobber that dialogue nondeterministically. The pickpocket
 * handlers register per npc type instead. See [ThievingTargets].
 *
 * Resolved from `.data/symbols/.local/content.sym`. An unknown name fails the boot outright, so the
 * two must stay in step.
 */
object ThievingContent : ContentReferences() {
    val coin_pouch = find("thieving_coin_pouch")
}

/**
 * The two knobs that make this RS3-flavoured rather than OSRS-faithful.
 *
 * Both are applied at the point of use rather than baked into the tables, so the tables stay
 * readable as the real OSRS numbers and can be checked against the wiki at a glance.
 */
object ThievingRates {
    /** Xp multiplier. OSRS values times this is roughly RS3's pace. */
    const val XP_RATE = 2.5

    /** Loot multiplier. Requested outright: five times the normal haul. */
    const val LOOT_RATE = 5

    /**
     * Success rate at 99, on the engine's 0..255 scale. `SkillingSuccessRate` interpolates linearly
     * from the low rate at level 1 to this at level 99, so pinning it near the top of the scale is
     * what makes a maxed thief essentially never fail.
     */
    private const val RATE_AT_MAX = 248

    /** Success rate a player standing exactly at a target's level requirement should see. */
    private const val RATE_AT_REQUIREMENT = 0.70

    private const val MAX_LEVEL = 99

    /** [RATE_AT_MAX] is the same for every tier; only the level-1 end of the line moves. */
    fun rateHigh(): Int = RATE_AT_MAX

    /**
     * Solves for the level-1 rate that puts the interpolated curve at [RATE_AT_REQUIREMENT] when
     * the player is exactly at [requirement].
     *
     * The engine's curve is `(low * (99 - lvl) + high * (lvl - 1)) / 98`, so this is that equation
     * rearranged for `low`. For the highest tiers the `high` end alone already clears the target
     * rate, which is why the result clamps at zero rather than going negative — a level 80 thief
     * pickpocketing heroes starts at about 78% and climbs from there.
     */
    fun rateLow(requirement: Int): Int {
        if (requirement <= 1) {
            return (RATE_AT_REQUIREMENT * 256).roundToInt().coerceAtMost(255)
        }
        val needed = RATE_AT_REQUIREMENT * 256 * (MAX_LEVEL - 1)
        val contributedByHigh = RATE_AT_MAX.toDouble() * (requirement - 1)
        val remaining = needed - contributedByHigh
        if (remaining <= 0) {
            return 0
        }
        return (remaining / (MAX_LEVEL - requirement)).roundToInt().coerceIn(0, 255)
    }
}
