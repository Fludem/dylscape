package org.rsmod.content.custom.knightwaves

import org.rsmod.api.config.refs.varbits
import org.rsmod.game.entity.Player

/**
 * Progress through the trial, stored in `kr_knightwaves_state` - the same varbit the prayer book
 * reads to decide whether Chivalry and Piety are available.
 *
 * That is not a coincidence or a shortcut. `Prayer.hasAllRequirements` tests
 * `vars[unlocked] < unlockState`, a threshold rather than a flag, precisely so the intermediate
 * values mean something. So the varbit counts waves on the way up and lands on [COMPLETE] at the
 * end, and the two prayers unlock as a side effect of finishing.
 *
 * It also means progress is saved for free: a player who logs out after three waves comes back at
 * three waves.
 */
object KnightWavesProgress {
    /** The value `PrayerTabObjs` requires for Chivalry and Piety. */
    const val COMPLETE: Int = 8

    val Player.wavesCleared: Int
        get() = vars[varbits.kr_knightwaves_state].coerceAtMost(TOTAL_WAVES)

    val Player.trialComplete: Boolean
        get() = vars[varbits.kr_knightwaves_state] >= COMPLETE

    /** Total number of knights faced. */
    const val TOTAL_WAVES: Int = 6
}
