package org.rsmod.content.other.consumables.configs

import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.game.type.stat.StatType

/**
 * One consumable and everything that happens when it goes down.
 *
 * Every field after [obj] has a default, so the overwhelmingly common case - a food that restores a
 * flat number of hitpoints and does nothing else - stays a single line in the family tables.
 *
 * The rows live in Kotlin rather than in obj params on purpose. Only the `food` content group needs
 * to reach the cache: it is what lets one hook cover every edible obj, and what the bank's
 * consumable extra-op reads. The numbers do not. A cache edit is additive once packed, so a heal
 * value written there can be overwritten but never removed, and these are exactly the numbers most
 * likely to want correcting. Here a wrong one is a one-line diff.
 */
public data class Edible(
    public val obj: ObjType,
    /** Flat hitpoints restored. Paired with [healPercent], which the engine sums for us. */
    public val heal: Int = 0,
    /**
     * Hitpoints restored as a percentage of the base Hitpoints level, added to [heal].
     *
     * This is not a rounding artefact of the wiki: the kebabs and the vegetables genuinely scale.
     * Cooked sweetcorn is "1 + 10%", which is the same `(constant, percent)` pair every stat
     * function in the engine already takes.
     */
    public val healPercent: Int = 0,
    /**
     * An inclusive random range rolled instead of [heal], for the foods that genuinely vary.
     *
     * Distinct from [healPercent]: cave eel rolls 8-12 flat regardless of level, whereas sweetcorn
     * scales. Rolled through [org.rsmod.api.random.GameRandom], never `kotlin.random`.
     */
    public val healRange: IntRange? = null,
    /**
     * A second helping of hitpoints applied [delayedHealTicks] later.
     *
     * The Varlamore hunter meats heal twice - "4 immediately, then 4 more after three seconds" -
     * which is the one food mechanic that cannot resolve inside the tick it started in.
     */
    public val delayedHeal: Int = 0,
    public val delayedHealTicks: Int = 5,
    /**
     * How far *above* the base Hitpoints level this food may push, given that base level.
     *
     * A function rather than a number because anglerfish's over-cap is a bracket table. Zero - the
     * default - is the ordinary "never overheal" behaviour that everything else wants.
     */
    public val overheal: (base: Int) -> Int = NO_OVERHEAL,
    /**
     * What the obj turns into rather than being deleted: `cake` -> `partial_cake` -> `cake_slice`.
     * The last stage of a chain leaves this null and the obj is consumed outright.
     */
    public val next: ObjType? = null,
    /** Karambwans and their kin, which ride their own clock so they stack with ordinary food. */
    public val combo: Boolean = false,
    public val effects: List<StatEffect> = emptyList(),
    /** Run energy restored, as a percentage of the maximum. */
    public val energy: Int = 0,
    /** Damage dealt on eating, for the rock cakes and the poisoned foods. */
    public val damage: Int = 0,
    /** Damage as a percentage of *current* hitpoints, which is how the rock cakes bite. */
    public val damagePercent: Int = 0,
    /**
     * The live game shows the op but refuses to honour it.
     *
     * A refusal consumes nothing, starts no cooldown and heals nothing - it only prints [message].
     * Rotten apples, the Hosidius servery food and the quest kebabs all behave this way, and the
     * distinction matters: without it a refusal would silently eat the item.
     */
    public val refuses: Boolean = false,
    /** Overrides the default "You eat the {name}." line. */
    public val message: String? = null,
    /** An extra line after the default one, for foods that comment on what they did. */
    public val extraMessage: String? = null,
    public val seq: SeqType = ConsumableSeqs.eat,
) {
    init {
        require(heal >= 0) { "Heal must not be negative: ${obj.internalName}" }
        require(healPercent in 0..100) { "Heal percent must be 0-100: ${obj.internalName}" }
        require(damage >= 0) { "Damage must not be negative: ${obj.internalName}" }
        require(damagePercent in 0..100) { "Damage percent must be 0-100: ${obj.internalName}" }
        require(energy in 0..100) { "Energy must be a 0-100 percentage: ${obj.internalName}" }
        require(delayedHeal == 0 || delayedHealTicks > 0) {
            "A delayed heal needs a delay: ${obj.internalName}"
        }
        require(healRange == null || heal == 0) {
            "A range and a flat heal are alternatives, not both: ${obj.internalName}"
        }
        require(healRange == null || !healRange.isEmpty()) {
            "Heal range must not be empty: ${obj.internalName}"
        }
        require(!refuses || message != null) { "A refusal has to say why: ${obj.internalName}" }
        require(!refuses || (heal == 0 && damage == 0 && next == null)) {
            "A refusal must not consume or change anything: ${obj.internalName}"
        }
    }

    public companion object {
        public val NO_OVERHEAL: (Int) -> Int = { 0 }
    }
}

/**
 * A visible-level change applied on top of the healing.
 *
 * [constant] and [percent] go straight through to `statBoost`/`statDrain`, which is why they are
 * two fields: the wiki's "reduces Attack by 3 + 2%" is literally that pair. [boost] picks the
 * direction, so both numbers stay positive.
 */
public data class StatEffect(
    public val stat: StatType,
    public val constant: Int = 0,
    public val percent: Int = 0,
    public val boost: Boolean = true,
) {
    init {
        require(constant >= 0) { "Constant must be positive; direction is carried by `boost`." }
        require(percent in 0..100) { "Percent must be an integer from 0-100." }
    }
}
