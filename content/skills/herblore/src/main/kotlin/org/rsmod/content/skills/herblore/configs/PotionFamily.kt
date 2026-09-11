package org.rsmod.content.skills.herblore.configs

import org.rsmod.api.toxins.ToxinCure
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.game.type.stat.StatType

/**
 * Base for the potion families, and the thing that keeps five hundred objs off the page.
 *
 * Each family is an [ObjReferences] so that its `find` calls happen inside the object's `init`,
 * which is when the reference loader drains the cache. A mistyped internal name is therefore a hard
 * boot failure naming the typo, exactly as it is for a hand-written `val`. That is the same
 * arrangement `ConsumableFamily` uses, for the same reason.
 *
 * **Nothing in a subclass may touch `ObjType.id`.** `find` returns a `HashedObjType` whose
 * `internalId` is still null here, and `CacheType.id` errors on null. Anything keyed by id belongs
 * in [Potions], behind `by lazy`.
 *
 * Abstract on purpose -- `TypeReferencesLoader` skips a class with no `objectInstance`, so this
 * base is never loaded itself, only its family subclasses.
 */
internal abstract class PotionFamily : ObjReferences() {
    val rows: MutableList<Potion> = mutableListOf()

    /**
     * The regular ladder: `4dose<stem>` down to `1dose<stem>`, then an empty vial.
     *
     * Forty-six of the cache's forty-seven `Ndose` stems are complete four-rung ladders, so the
     * names are computed rather than listed. The single exception is the strength potion, whose
     * four-dose the cache calls `strength4` and not `4dose1strength`; [head] exists for exactly
     * that row and is used nowhere else. Do not generalise it away -- the point of generating the
     * rest is that the one irregularity stays visible at the one call site that has it.
     */
    protected fun ladder(
        stem: String,
        head: String? = null,
        effects: List<PotionEffect> = emptyList(),
        heal: Int = 0,
        healPercent: Int = 0,
        energy: Int = 0,
        staminaUnits: Int = 0,
        cure: ToxinCure? = null,
        inertMessage: String? = null,
    ) {
        val names = listOf(head ?: "4dose$stem", "3dose$stem", "2dose$stem", "1dose$stem")
        build(stem, names, effects, heal, healPercent, energy, staminaUnits, cure, inertMessage)
    }

    /**
     * The families the cache numbers with a trailing digit instead: `antidote+4`, `antivenom4`.
     * Same four rungs, different spelling.
     */
    protected fun suffixLadder(
        stem: String,
        effects: List<PotionEffect> = emptyList(),
        heal: Int = 0,
        healPercent: Int = 0,
        energy: Int = 0,
        cure: ToxinCure? = null,
        inertMessage: String? = null,
    ) {
        val names = (4 downTo 1).map { "$stem$it" }
        build(stem, names, effects, heal, healPercent, energy, 0, cure, inertMessage)
    }

    /** `sanfew_salve_4_dose` and its kin, where the digit sits in the middle. */
    protected fun infixLadder(
        prefix: String,
        suffix: String,
        effects: List<PotionEffect> = emptyList(),
        cure: ToxinCure? = null,
        inertMessage: String? = null,
    ) {
        val names = (4 downTo 1).map { "$prefix$it$suffix" }
        build("$prefix$suffix", names, effects, 0, 0, 0, 0, cure, inertMessage)
    }

    private fun build(
        family: String,
        names: List<String>,
        effects: List<PotionEffect>,
        heal: Int,
        healPercent: Int,
        energy: Int,
        staminaUnits: Int,
        cure: ToxinCure?,
        inertMessage: String?,
    ) {
        require(names.size == DOSES) { "A ladder has exactly $DOSES rungs: $names" }
        val types = names.map { find(it) }
        val empty = find("vial_empty")
        for ((index, type) in types.withIndex()) {
            rows +=
                Potion(
                    obj = type,
                    dose = DOSES - index,
                    family = family,
                    next = types.getOrNull(index + 1) ?: empty,
                    effects = effects,
                    heal = heal,
                    healPercent = healPercent,
                    energy = energy,
                    staminaUnits = staminaUnits,
                    cure = cure,
                    inertMessage = inertMessage,
                )
        }
    }

    protected fun boost(stat: StatType, constant: Int = 0, percent: Int = 0): PotionEffect =
        PotionEffect(stat, constant, percent, EffectKind.Boost)

    protected fun drain(stat: StatType, constant: Int = 0, percent: Int = 0): PotionEffect =
        PotionEffect(stat, constant, percent, EffectKind.Drain)

    /** Clamped at the base level by `statHeal`, unlike [boost]. See [EffectKind]. */
    protected fun restore(stat: StatType, constant: Int = 0, percent: Int = 0): PotionEffect =
        PotionEffect(stat, constant, percent, EffectKind.Restore)

    protected companion object {
        const val DOSES: Int = 4

        /** Dragonfire is not implemented on this server; see [Potion.inertMessage]. */
        const val NO_DRAGONFIRE: String =
            "Nothing seems to happen: dragonfire is not implemented yet."
    }
}
