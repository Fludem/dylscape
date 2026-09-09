package org.rsmod.content.areas.misc.tutorial

/**
 * The tutorial as a linear list of stages, stored in the `tutorial` varp as the stage's [value].
 *
 * `value` is deliberately spaced by tens rather than by `ordinal`, so extra sub-steps can be added
 * between two stages later without renumbering a player's saved progress. A brand-new account reads
 * the varp as `0`, which is [NOT_STARTED]; [COMPLETE] is the sentinel the whole script checks to
 * stay out of a finished player's way.
 */
enum class TutorialStage(val value: Int) {
    NOT_STARTED(0),
    GUIDE(10),
    SURVIVAL_CHOP(20),
    SURVIVAL_FIRE(30),
    SURVIVAL_FISH(40),
    SURVIVAL_COOK(45),
    COOKING(50),
    QUEST(60),
    MINING_MINE(70),
    MINING_SMELT(80),
    MINING_SMITH(90),
    COMBAT(100),
    BANK(110),
    PRAYER(115),
    MAGIC(120),
    COMPLETE(1000);

    fun atLeast(other: TutorialStage): Boolean = value >= other.value

    fun isBefore(other: TutorialStage): Boolean = value < other.value

    companion object {
        private val byValue = entries.associateBy { it.value }

        /**
         * The stage a saved varp value represents, rounding an unknown value down to the nearest.
         */
        fun of(value: Int): TutorialStage =
            byValue[value]
                ?: entries.filter { it.value <= value }.maxByOrNull { it.value }
                ?: NOT_STARTED
    }
}
