package org.rsmod.game.entity.player

/**
 * The experience-rate tier a player picked during first-login setup, permanent once chosen.
 *
 * Each tier trades experience rate against drop rate: the faster the levelling, the smaller the
 * loot bonus. The drop half is applied by the drop-tables module, which maps a tier onto its boost;
 * only the rate lives here, so the engine stays free of content policy.
 *
 * The tier is the authoritative value, not [xpRate]. `Player.xpRate` is regenerated from it on
 * every load so the two cannot drift apart, and [id] is the multiplier itself so a stored row reads
 * for itself.
 *
 * A `null` tier means the chooser has not run yet - that absence *is* the "has chosen" flag.
 */
public enum class XpRateTier(public val id: Int, public val xpRate: Double) {
    Rate10(10, 10.0),
    Rate16(16, 16.0),
    Rate30(30, 30.0);

    public companion object {
        /**
         * Stored in `characters.xp_rate_tier` when the chooser has not run yet. No tier uses it, so
         * it can never collide with a real choice.
         */
        public const val UNCHOSEN_ID: Int = 0

        /** Returns `null` for [UNCHOSEN_ID], and for any unrecognised value. */
        public fun of(id: Int): XpRateTier? = entries.firstOrNull { it.id == id }
    }
}
