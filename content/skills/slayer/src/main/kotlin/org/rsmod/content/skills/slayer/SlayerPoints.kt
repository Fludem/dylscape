package org.rsmod.content.skills.slayer

/**
 * What finishing a task pays.
 *
 * Points are per master and rise steeply up the ladder, which is the whole reason to graduate from
 * Turael: he pays nothing at all. On top of that the streak pays a multiple on round-numbered
 * tasks, so the incentive is to keep taking tasks from the same master rather than skipping down to
 * an easier one.
 */
object SlayerPoints {
    /** `master_id` -> points for one completed task off the streak milestones. */
    private val BASE: Map<Int, Int> =
        mapOf(TURAEL to 0, MAZCHNA to 2, VANNAKA to 4, CHAELDAR to 10, DURADEL to 15, NIEVE to 12)

    /**
     * Streak milestones, richest first. Order matters: task 1000 is also a multiple of 250, 100, 50
     * and 10, and only the best applies.
     */
    private val MILESTONES: List<Pair<Int, Int>> =
        listOf(1000 to 50, 250 to 35, 100 to 50, 50 to 15, 10 to 5)

    /**
     * Points for completing a task from [masterId] as the [streak]-th task in a row.
     *
     * [streak] is the count *including* the task just finished, so the tenth task is the one that
     * pays the ten-task multiple.
     */
    fun award(masterId: Int, streak: Int): Int {
        val base = BASE[masterId] ?: 0
        if (base == 0) {
            return 0
        }
        val multiplier = MILESTONES.firstOrNull { (every, _) -> streak % every == 0 }?.second ?: 1
        return base * multiplier
    }

    private const val TURAEL = 1
    private const val MAZCHNA = 2
    private const val VANNAKA = 3
    private const val CHAELDAR = 4
    private const val DURADEL = 5
    private const val NIEVE = 6
}
