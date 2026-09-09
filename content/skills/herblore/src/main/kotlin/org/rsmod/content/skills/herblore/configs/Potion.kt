package org.rsmod.content.skills.herblore.configs

import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType

/**
 * One dose of one potion and everything that happens when it goes down.
 *
 * This is `consumables`' [org.rsmod.content.other.consumables.configs.Edible] with the dose ladder
 * made mandatory instead of optional. `Edible.next` is nullable because the last bite of a cake
 * deletes the obj; a potion's last dose never does -- it hands back an empty vial -- so [next] is
 * non-null on every row here and the drinking script has no delete path at all.
 *
 * The numbers live in Kotlin rather than in obj params for the reason `Edible` gives: a cache edit
 * is additive once packed, and boost formulae are exactly the values most likely to want
 * correcting. The cache settled it anyway -- no potion obj carries `skill_xp`.
 */
data class Potion(
    val obj: ObjType,
    /** 4 down to 1. Drives the "You have N doses of potion left." line. */
    val dose: Int,
    /** The ladder's stem, which is how [HerbloreRecipes] names its product. */
    val family: String,
    /** The next dose down, or an empty vial for the last. Never null -- see the class comment. */
    val next: ObjType,
    val effects: List<PotionEffect> = emptyList(),
    val heal: Int = 0,
    val healPercent: Int = 0,
    /** Run energy restored, as a percentage of the maximum. */
    val energy: Int = 0,
    /**
     * Stamina duration in `varbits.stamina_duration` units, not ticks.
     *
     * That varbit is five bits wide, so a tick count does not fit in it and writing one throws.
     * Zero for everything that is not a stamina potion.
     */
    val staminaUnits: Int = 0,
    /**
     * The live game shows `Drink` but the mechanic does not exist on this server.
     *
     * Antipoison, the antidotes, antivenom and relicym's balm all cure or prevent poison, and this
     * engine has no poison; the antifire tiers protect against dragonfire, and it has no dragonfire
     * either. Those rows still carry a correct dose ladder and still hand back the vial -- they
     * just say so, rather than pretending. A fake looks implemented, which is worse than a gap you
     * can read.
     */
    val inertMessage: String? = null,
) {
    init {
        require(dose in 1..4) { "Dose must be 1..4: ${obj.internalName}" }
        require(heal >= 0) { "Heal must not be negative: ${obj.internalName}" }
        require(healPercent in 0..100) { "Heal percent must be 0-100: ${obj.internalName}" }
        require(energy in 0..100) { "Energy must be a 0-100 percentage: ${obj.internalName}" }
        require(staminaUnits in 0..31) {
            "Stamina duration must fit in five bits: ${obj.internalName}"
        }
    }
}

/**
 * A visible-level change applied on drinking.
 *
 * [constant] and [percent] go straight through to `statBoost`/`statDrain`, which is why they are
 * two fields: the wiki's "raises Attack by 10% + 3" is literally that pair. [kind] picks what is
 * done with them, so both numbers stay positive.
 *
 * Deliberately a local type rather than `consumables`' `StatEffect`. The two modules describe the
 * same idea and share no code on purpose: `consumables` is `internal` throughout, and a dependency
 * in that direction would make the food table and the potion table one compilation unit for the
 * sake of four fields.
 */
data class PotionEffect(
    val stat: StatType,
    val constant: Int = 0,
    val percent: Int = 0,
    val kind: EffectKind = EffectKind.Boost,
) {
    init {
        require(constant >= 0) { "Constant must be positive; direction is carried by `kind`." }
        require(percent in 0..100) { "Percent must be an integer from 0-100." }
    }
}

/**
 * What a [PotionEffect] does with its numbers.
 *
 * [Restore] is not [Boost] with the sign flipped. `statBoost` pushes the visible level *above* the
 * base one and `statHeal` clamps at it, and that clamp is the whole difference between a super
 * strength and a prayer potion: one is meant to overshoot, the other is meant to stop at full.
 */
enum class EffectKind {
    Boost,
    Drain,
    Restore,
}
