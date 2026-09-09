package org.rsmod.content.custom.barrows.configs

import org.rsmod.content.custom.barrows.Brother
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.dropTable
import org.rsmod.game.type.obj.ObjType

/**
 * What the chest pays out.
 *
 * OSRS resolves a Barrows chest in two stages, and both are reproduced here:
 * 1. **How many rolls.** One per brother killed, plus one, so a full clear rolls seven times and a
 *    chest opened having killed nobody still rolls once.
 * 2. **What each roll hits.** Each roll independently tests the reward potential against the cap. A
 *    hit takes one of the twenty-four equipment pieces, uniformly; a miss falls through to
 *    [consolation], the runes-and-bolt-racks table that makes up the bulk of a chest.
 *
 * [RARE_SCALE] is the one number that is calibrated rather than copied - see its note. Everything
 * else here is structural.
 */
object BarrowsRewards {
    /**
     * Divides the per-roll rare chance, which is otherwise `potential / MAX_REWARD_POTENTIAL`.
     *
     * At full potential a roll therefore hits the equipment table at `1 in 45`, and a seven-roll
     * chest clears roughly one item in six - which is the observable this is calibrated against,
     * the commonly quoted "about one item per six or seven full runs". Jagex's own constant is not
     * recoverable from the cache, so this is the honest way to land on the right rate: pick the
     * shape from the game and the scale from the outcome.
     */
    const val RARE_SCALE: Int = 45

    /** One roll per brother killed, plus one. */
    fun rollCount(brothersKilled: Int): Int = brothersKilled + 1

    /**
     * Every equipment piece the chest can hand out, flattened. Uniform: which brothers you killed
     * does not narrow the table, matching live.
     */
    val equipment: List<ObjType> = Brother.all.flatMap { barrows_objs.equipment.getValue(it) }

    /**
     * The non-equipment half of the chest. Weights are out of 1000 and the builder asserts they sum
     * to it, so a typo fails at class-load instead of quietly skewing every chest forever.
     *
     * There is no empty slot: a roll that misses the equipment table always pays something, which
     * is what makes the chest feel worth opening at low potential.
     */
    val consolation: DropTable = dropTable {
        table(outOf = 1000) {
            drop(280, barrows_objs.bolt_rack, 35..35)
            drop(140, barrows_objs.coins, 494..8000)
            drop(100, barrows_objs.mind_rune, 200..500)
            drop(90, barrows_objs.chaos_rune, 92..195)
            drop(80, barrows_objs.death_rune, 74..148)
            drop(70, barrows_objs.blood_rune, 55..111)
            drop(60, barrows_objs.bronze_arrow, 60..122)
            drop(55, barrows_objs.iron_arrow, 60..122)
            drop(50, barrows_objs.steel_arrow, 60..122)
            drop(45, barrows_objs.mithril_arrow, 60..122)
            drop(15, barrows_objs.loop_half_key)
            drop(15, barrows_objs.tooth_half_key)
        }
    }
}
