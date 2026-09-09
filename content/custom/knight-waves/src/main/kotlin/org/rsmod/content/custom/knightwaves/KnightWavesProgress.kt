package org.rsmod.content.custom.knightwaves

import org.rsmod.api.config.refs.varbits
import org.rsmod.game.entity.Player

/**
 * Progress through the trial, stored in `kr_knightwaves_state` - the same varbit the prayer book
 * reads to decide whether Chivalry and Piety are available.
 *
 * That is not a shortcut. `Prayer.hasAllRequirements` tests `vars[unlocked] < unlockState`, a
 * threshold rather than a flag, precisely so the values on the way up mean something. So the varbit
 * counts knights beaten and lands on [COMPLETE] at the end, and the two prayers unlock as a
 * consequence of finishing rather than as a separate bookkeeping step that could drift from it.
 *
 * It also means progress is saved for free: a player who logs out after three knights comes back
 * facing the fourth.
 */
object KnightWavesProgress {
    /** Number of knights faced. */
    const val TOTAL_WAVES: Int = 6

    /** The value `PrayerTabObjs` requires before it will hand over Chivalry and Piety. */
    const val COMPLETE: Int = 8

    /** Knights already beaten, and so also the index of the one still to face. */
    val Player.wavesCleared: Int
        get() = vars[varbits.kr_knightwaves_state].coerceIn(0, TOTAL_WAVES)

    val Player.trialComplete: Boolean
        get() = vars[varbits.kr_knightwaves_state] >= COMPLETE
}
