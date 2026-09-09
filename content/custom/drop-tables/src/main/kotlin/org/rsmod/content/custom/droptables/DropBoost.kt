package org.rsmod.content.custom.droptables

import org.rsmod.game.entity.player.XpRateTier

/**
 * A multiplier expressed as an exact integer fraction.
 *
 * Deliberately not a `Double`. Drop rolls have to stay reproducible under test, and integer
 * arithmetic lets a boosted roll consume exactly the same number of
 * [org.rsmod.api.random.GameRandom] draws as an unboosted one, so a scripted-roll test written
 * before the boost existed keeps asserting the same thing.
 */
data class Ratio(val num: Int, val den: Int) {
    init {
        require(den > 0) { "Denominator must be positive: $this." }
        require(num >= den) { "A boost may only improve a rate: $this." }
    }

    companion object {
        val ONE: Ratio = Ratio(1, 1)
    }
}

/**
 * How much a player's experience-rate tier improves their drop rates.
 *
 * The tiers trade experience against loot: the slowest tier boosts everything, the middle tier
 * boosts only what is rare, and the fastest tier boosts nothing.
 */
data class DropBoost(val all: Ratio, val rare: Ratio) {
    /**
     * The multiplier that applies to one slot: the better of [all] and [rare] when the slot is
     * rare, otherwise [all].
     *
     * Taking the maximum rather than composing them is what makes "+20% to all rolls" mean exactly
     * that, without a rare slot compounding into +32%.
     */
    fun forSlot(rare: Boolean): Ratio =
        if (rare && this.rare.num * all.den > all.num * this.rare.den) this.rare else all

    companion object {
        val NONE: DropBoost = DropBoost(Ratio.ONE, Ratio.ONE)

        /** 10x experience: every non-empty slot is 20% more likely, rares included. */
        val PLUS_20_ALL: DropBoost = DropBoost(all = Ratio(6, 5), rare = Ratio.ONE)

        /** 16x experience: only rare slots and the shared rare drop table are boosted. */
        val PLUS_10_RARE: DropBoost = DropBoost(all = Ratio.ONE, rare = Ratio(11, 10))

        /** A player who has not picked a tier yet gets no boost. */
        fun forTier(tier: XpRateTier?): DropBoost =
            when (tier) {
                XpRateTier.Rate10 -> PLUS_20_ALL
                XpRateTier.Rate16 -> PLUS_10_RARE
                XpRateTier.Rate30,
                null -> NONE
            }
    }
}
