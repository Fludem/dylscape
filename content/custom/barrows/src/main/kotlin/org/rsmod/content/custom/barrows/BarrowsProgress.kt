package org.rsmod.content.custom.barrows

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.custom.barrows.configs.barrows_varbits
import org.rsmod.game.entity.Player

/**
 * A player's progress through the current Barrows run, held entirely in the vanilla varbits.
 *
 * That is not a shortcut, it is the whole reason the client half works for free. `barrows_overlay`
 * reads `barrows_killed_ahrim`..`_verac` and `barrows_killed_count` itself, and
 * `barrows_stone_chest` is a multiloc over `barrows_chest_open`, so writing these varbits both
 * *records* the state and *draws* it. It also means progress survives a relog without a line of
 * persistence code, exactly as `KnightWavesProgress` does.
 */
object BarrowsProgress {
    /**
     * Reward potential, capped at OSRS's own 1012. The cap is what the rare-item chance is measured
     * against, so a chest opened at full potential is as good as the chest gets.
     *
     * ### About these three numbers
     *
     * The *shape* is OSRS's and is not in doubt: potential accrues from brothers and from crypt
     * monsters, saturates at 1012, and drives both the roll count and the rare chance. The two
     * accrual constants are **calibrated here rather than copied from Jagex**, because the exact
     * per-kill figures are not recoverable from the cache and guessing them would quietly hand out
     * the wrong drop rates. They are set so that the six brothers alone reach 984 - close to the
     * cap, but leaving the last 28 to be earned by clearing monsters on the way to the chest, which
     * is the behaviour the cap exists to produce.
     *
     * If someone later pins the real figures, this is the only place to change them.
     */
    const val MAX_REWARD_POTENTIAL: Int = 1012

    /** Six brothers come to 984, just shy of the cap. See the note on [MAX_REWARD_POTENTIAL]. */
    const val BROTHER_POTENTIAL: Int = 164

    /** Fourteen crypt monsters top a full brother clear up to the cap. */
    const val MONSTER_POTENTIAL: Int = 2

    fun Player.hasKilled(brother: Brother): Boolean =
        vars[checkNotNull(barrows_varbits.killed[brother])] != 0

    fun Player.markKilled(brother: Brother) {
        set(checkNotNull(barrows_varbits.killed[brother]), 1)
    }

    val Player.brothersKilled: Int
        get() = Brother.all.count { hasKilled(it) }

    val Player.brothersAlive: List<Brother>
        get() = Brother.all.filterNot { hasKilled(it) }

    /**
     * Reward potential, which is also the number the overlay prints as the killcount. Saturates at
     * [MAX_REWARD_POTENTIAL] so a long stay in the crypt cannot push it past what the reward maths
     * expects.
     */
    var Player.rewardPotential: Int
        get() = vars[barrows_varbits.killed_count].coerceIn(0, MAX_REWARD_POTENTIAL)
        set(value) {
            set(barrows_varbits.killed_count, value.coerceIn(0, MAX_REWARD_POTENTIAL))
        }

    var Player.chestOpen: Boolean
        get() = vars[barrows_varbits.chest_open] != 0
        set(value) {
            set(barrows_varbits.chest_open, if (value) 1 else 0)
        }

    /**
     * Wipes the run. Called when the chest has been looted and when the player dies, so the next
     * trip starts from six live brothers and no potential.
     */
    fun Player.resetRun() {
        for (brother in Brother.all) {
            set(checkNotNull(barrows_varbits.killed[brother]), 0)
        }
        set(barrows_varbits.killed_count, 0)
        set(barrows_varbits.killed_monster, 0)
        set(barrows_varbits.chest_open, 0)
    }

    /**
     * `Player.vars` deliberately exposes no `set` operator - a varp change has to reach the client
     * in the same breath - so every write here goes through the setter that does both.
     */
    private fun Player.set(varbit: org.rsmod.game.type.varbit.VarBitType, value: Int) {
        VarPlayerIntMapSetter.set(this, varbit, value)
    }
}
