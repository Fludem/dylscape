package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.config.refs.stats

/**
 * Everything that is not a fight: run energy, the skilling boosts, and the two families whose
 * mechanic this server does not have.
 *
 * The inert rows are the honest half of this file. Antipoison, the antidotes, antivenom, relicym's
 * balm and all four antifire tiers exist in the cache, are makeable, and cure or prevent something
 * the engine does not model -- there is no poison anywhere in `engine/` and no dragonfire damage
 * path. They ship with a correct dose ladder and a correct empty vial and say plainly that the
 * effect is missing, because a silent no-op reads as a bug and an invented effect reads as working.
 */
internal object PotionsUtility : PotionFamily() {
    init {
        // Run energy. The percentages are of the maximum, which is what `Edible.energy` means too.
        ladder("1energy", energy = 10)
        ladder("2energy", energy = 20)

        // Stamina is the one timed effect that is genuinely wired up:
        // `PlayerRunUpdateProcessor.decreaseRunEnergy` already reads `varbits.stamina_active` and
        // cuts the drain, so setting the varbit is the real mechanic rather than a cosmetic one.
        ladder("stamina", energy = 20, staminaUnits = STAMINA_UNITS_PER_DOSE)

        // Skilling boosts.
        ladder("1agility", effects = listOf(boost(stats.agility, 3)))
        ladder("fisherspotion", effects = listOf(boost(stats.fishing, 3)))
        ladder("hunting", effects = listOf(boost(stats.hunter, 3)))

        // A goading potion makes npcs aggressive rather than changing a level, and a surge potion
        // is a Varlamore timed effect. Neither has anything to set here yet.
        ladder("goading", inertMessage = NOT_MODELLED)
        ladder("surge", inertMessage = NOT_MODELLED)

        // Poison, which this engine does not have. See the file comment.
        ladder("antipoison", inertMessage = NO_POISON)
        ladder("2antipoison", inertMessage = NO_POISON)
        suffixLadder("antidote+", inertMessage = NO_POISON)
        suffixLadder("antidote++", inertMessage = NO_POISON)
        suffixLadder("antivenom", inertMessage = NO_POISON)
        suffixLadder("antivenom+", inertMessage = NO_POISON)
        suffixLadder("extended_antivenom+", inertMessage = NO_POISON)
        suffixLadder("relicyms_balm", inertMessage = NO_DISEASE)

        // Dragonfire, which it does not have either. The cache reserves
        // `varbits.antifire_potion` and `super_antifire_potion` and nothing in this repo reads
        // them, so there is no protection to grant.
        ladder("1antidragon", inertMessage = NO_DRAGONFIRE)
        ladder("2antidragon", inertMessage = NO_DRAGONFIRE)
        ladder("3antidragon", inertMessage = NO_DRAGONFIRE)
        ladder("4antidragon", inertMessage = NO_DRAGONFIRE)
    }

    // Local to this family.
    /**
     * Two minutes a dose, in the coarse units `varbits.stamina_duration` holds -- see
     * `PotionDrinking.applyStamina` for why this is not a tick count.
     */
    const val STAMINA_UNITS_PER_DOSE: Int = 6

    const val NOT_MODELLED: String =
        "Nothing seems to happen: this potion's effect is not implemented yet."

    const val NO_DISEASE: String = "Nothing seems to happen: disease is not implemented yet."
}
