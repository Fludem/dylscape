package org.rsmod.content.other.consumables.configs

import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.stat.StatType

/**
 * Base for the family tables.
 *
 * Each family is an [ObjReferences] so that its `find` calls happen inside the object's `init`,
 * which is when the reference loader drains the cache. A mistyped internal name is therefore a hard
 * boot failure naming the typo, exactly as it is for a hand-written `val`.
 *
 * The objs are named by string rather than declared one `val` each. At this scale a typed reference
 * file would be several hundred lines duplicating the table sitting beside it, and would buy
 * nothing: `find` fails the boot on an unknown name either way.
 *
 * Abstract on purpose - `TypeReferencesLoader` skips a class with no `objectInstance`, so this base
 * is never loaded itself, only its family subclasses.
 */
internal abstract class ConsumableFamily : ObjReferences() {
    val rows: MutableList<Edible> = mutableListOf()

    /** The ordinary case: one bite, one flat heal, nothing else. */
    protected fun food(name: String, heal: Int, message: String? = null) {
        rows += Edible(obj = find(name), heal = heal, message = message)
    }

    /** A food whose heal scales with the eater: "1 + 10%" for cooked sweetcorn. */
    protected fun scaledFood(name: String, heal: Int, percent: Int, message: String? = null) {
        rows += Edible(obj = find(name), heal = heal, healPercent = percent, message = message)
    }

    /** A food that rolls an inclusive range, flat, regardless of level. */
    protected fun randomFood(name: String, low: Int, high: Int, message: String? = null) {
        rows += Edible(obj = find(name), healRange = low..high, message = message)
    }

    /**
     * A food eaten in stages: each bite heals [heal] and turns the obj into the next name, the last
     * leaving nothing behind. One call so a chain cannot be left half-wired.
     */
    protected fun chain(heal: Int, vararg names: String) {
        require(names.size > 1) { "A chain needs at least two stages: ${names.toList()}" }
        val types = names.map { find(it) }
        for ((index, type) in types.withIndex()) {
            rows += Edible(obj = type, heal = heal, next = types.getOrNull(index + 1))
        }
    }

    /** The Varlamore hunter meats: heals twice, the second helping a few ticks later. */
    protected fun delayedFood(name: String, heal: Int, delayed: Int) {
        rows += Edible(obj = find(name), heal = heal, delayedHeal = delayed)
    }

    /** Anglerfish and friends: heals past the base level, by an amount that scales with it. */
    protected fun overhealFood(name: String, heal: Int, overheal: (Int) -> Int) {
        rows += Edible(obj = find(name), heal = heal, overheal = overheal)
    }

    /** Karambwans, which ride their own clock so they stack with an ordinary food. */
    protected fun comboFood(name: String, heal: Int) {
        rows += Edible(obj = find(name), heal = heal, combo = true)
    }

    /** Food that costs hitpoints instead of giving them. */
    protected fun harmfulFood(
        name: String,
        damage: Int = 0,
        damagePercent: Int = 0,
        message: String? = null,
        combo: Boolean = false,
    ) {
        rows +=
            Edible(
                obj = find(name),
                damage = damage,
                damagePercent = damagePercent,
                message = message,
                combo = combo,
            )
    }

    /**
     * An obj carrying an `Eat` or `Drink` op that live refuses to let you use.
     *
     * These are not oversights to be filled in later. Rotten apples, servery food and the quest
     * kebabs all print a refusal in the real game, and the cache still offers the option, so the
     * row exists to say the refusal out loud rather than to leave the click falling through to
     * "Nothing interesting happens."
     */
    protected fun refusedFood(name: String, message: String) {
        rows += Edible(obj = find(name), refuses = true, message = message)
    }

    /**
     * Beer, wine and the rest: heals a little, says something, and moves stats around.
     *
     * [leaves] is the vessel left behind - an empty jug, a bowl, a cup. It is the same field a
     * multi-bite food's next stage uses, because it is the same mechanic: the obj is replaced in
     * its own slot rather than deleted. The only difference is that a vessel is not itself
     * drinkable, so it never gets a row of its own.
     */
    protected fun drink(
        name: String,
        heal: Int = 0,
        message: String? = null,
        extraMessage: String? = null,
        energy: Int = 0,
        leaves: String? = null,
        effects: List<StatEffect> = emptyList(),
    ) {
        rows +=
            Edible(
                obj = find(name),
                heal = heal,
                effects = effects,
                energy = energy,
                next = leaves?.let { find(it) },
                message = message,
                extraMessage = extraMessage,
                seq = ConsumableSeqs.drink,
            )
    }

    /**
     * An ale: one hitpoint or so, a boost to some non-combat skill, and a combat stat drained for
     * the privilege. The drain is what makes them a real mechanic rather than flavour - a dwarven
     * stout costs you Attack, Strength and Defence to gain a Mining level.
     */
    protected fun ale(
        name: String,
        heal: Int,
        boosts: List<StatEffect> = emptyList(),
        drains: List<StatEffect> = emptyList(),
        leaves: String? = null,
    ) {
        rows +=
            Edible(
                obj = find(name),
                heal = heal,
                effects = boosts + drains,
                next = leaves?.let { find(it) },
                seq = ConsumableSeqs.drink,
            )
    }

    /** A keg drunk down through its four doses, each dose the same size. */
    protected fun keg(
        heal: Int,
        vararg names: String,
        boosts: List<StatEffect> = emptyList(),
        drains: List<StatEffect> = emptyList(),
    ) {
        val types = names.map { find(it) }
        for ((index, type) in types.withIndex()) {
            rows +=
                Edible(
                    obj = type,
                    heal = heal,
                    effects = boosts + drains,
                    next = types.getOrNull(index + 1),
                    seq = ConsumableSeqs.drink,
                )
        }
    }

    /** Food whose point is the run energy: purple sweets, mint cake, strange fruit. */
    protected fun energyFood(
        name: String,
        energy: Int,
        heal: Int = 0,
        healRange: IntRange? = null,
        message: String? = null,
        extraMessage: String? = null,
    ) {
        rows +=
            Edible(
                obj = find(name),
                heal = heal,
                healRange = healRange,
                energy = energy,
                message = message,
                extraMessage = extraMessage,
            )
    }

    protected fun boost(stat: StatType, constant: Int = 0, percent: Int = 0): StatEffect =
        StatEffect(stat, constant, percent, boost = true)

    protected fun drain(stat: StatType, constant: Int = 0, percent: Int = 0): StatEffect =
        StatEffect(stat, constant, percent, boost = false)
}
