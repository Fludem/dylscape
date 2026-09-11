package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.config.refs.stats
import org.rsmod.api.toxins.ToxinCure

/**
 * The potions that put levels back rather than pushing them past the top.
 *
 * Every row here uses [EffectKind.Restore], which routes to `statHeal` and clamps at the base
 * level. Using [EffectKind.Boost] instead would let a prayer potion overshoot full prayer, which is
 * the one mistake this whole distinction exists to prevent.
 */
internal object PotionsRestore : PotionFamily() {
    init {
        // A restore potion brings back the combat stats and the skills, but never Prayer or
        // Hitpoints. That exclusion is the entire difference between it and a super restore.
        ladder(
            "statrestore",
            effects =
                listOf(
                    restore(stats.attack, 10, 30),
                    restore(stats.strength, 10, 30),
                    restore(stats.defence, 10, 30),
                    restore(stats.ranged, 10, 30),
                    restore(stats.magic, 10, 30),
                ),
        )
        ladder(
            "2restore",
            effects =
                listOf(
                    restore(stats.attack, 8, 25),
                    restore(stats.strength, 8, 25),
                    restore(stats.defence, 8, 25),
                    restore(stats.ranged, 8, 25),
                    restore(stats.magic, 8, 25),
                    restore(stats.prayer, 8, 25),
                ),
        )
        ladder("prayerrestore", effects = listOf(restore(stats.prayer, 7, 25)))

        // Sanfew serum restores like a super restore and additionally cures poison and disease.
        // Poison is real; disease is not, and sanfew is the one drink where that half is minor
        // enough that saying so on every sip would be noise.
        infixLadder(
            "sanfew_salve_",
            "_dose",
            effects =
                listOf(
                    restore(stats.attack, 4, 30),
                    restore(stats.strength, 4, 30),
                    restore(stats.defence, 4, 30),
                    restore(stats.ranged, 4, 30),
                    restore(stats.magic, 4, 30),
                    restore(stats.prayer, 4, 30),
                ),
            cure = ToxinCure.Sanfew,
        )

        // Two Varlamore potions whose effects are timed rather than instant, and which no part of
        // this engine can express yet. They get a working dose ladder and an honest message rather
        // than an invented boost.
        ladder("statrenewal", inertMessage = NOT_MODELLED)
        ladder("moonlightpotion", inertMessage = NOT_MODELLED)
        ladder("1prayer_regeneration", inertMessage = NOT_MODELLED)
    }

    // Local to this family.
    const val NOT_MODELLED: String =
        "Nothing seems to happen: this potion's effect is not implemented yet."
}
