package org.rsmod.content.skills.construction.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Which loc goes up when a piece of furniture is built.
 *
 * This is the one link the cache does not spell out. `furniture` names the obj the build menu draws
 * and the materials it eats, but not the scenery it leaves behind -- on the real server that lives
 * in server-side config. It is recoverable, though, because the built loc always shares the
 * furniture obj's model and, nearly as often, its internal name. Three rules are tried in turn:
 * 1. a loc named exactly like the furniture row (`poh_stove_1`);
 * 2. a loc named like the row with the last underscore squeezed out (`poh_armchair_4` ->
 *    `poh_chair4`'s sibling `poh_bookcase2` shape);
 * 3. the `poh_`-prefixed locs that share the furniture obj's inventory model, preferring the one
 *    whose name is shortest -- state variants such as `poh_fireplace_1_lit` and
 *    `poh_stove_4_kettle` are always longer than the base loc.
 *
 * Hotspots that cover several tiles -- rugs, window runs -- place a different loc per piece, named
 * with the piece as a word: `poh_rugcorner2`, `poh_rugside2`, `poh_rugmiddle2`. The template
 * hotspot carries the same word as its own suffix, so [resolve] swaps one for the other.
 *
 * **Anything that cannot be resolved is not offered.** A wrong guess would put the wrong scenery in
 * someone's house, so the resolver fails closed and [unresolved] reports what it could not place.
 */
@Singleton
public class FurnitureLocs
@Inject
constructor(
    private val locTypes: LocTypeList,
    private val objTypes: ObjTypeList,
    private val tables: ConstructionTables,
) {
    private val byName: Map<String, UnpackedLocType> by lazy {
        locTypes.values.mapNotNull { type -> type.internalName?.let { it to type } }.toMap()
    }

    private val pohLocsByModel: Map<Int, List<UnpackedLocType>> by lazy {
        buildMap<Int, MutableList<UnpackedLocType>> {
            for (type in locTypes.values) {
                if (type.internalName?.startsWith(POH_PREFIX) != true) continue
                for (model in type.models) {
                    getOrPut(model) { mutableListOf() }.add(type)
                }
            }
        }
    }

    private val resolved = HashMap<Long, Int>()
    private val failed = HashSet<Int>()

    /** Furniture rows no loc could be found for, reported once the tables have been walked. */
    public val unresolved: Set<Int>
        get() = failed

    /**
     * The loc to place for [furnitureRow] on a hotspot piece carrying [variant], or `null` when the
     * furniture cannot be placed and so must not be offered.
     */
    public fun resolve(furnitureRow: Int, variant: String?): Int? {
        val key =
            (furnitureRow.toLong() shl 32) or
                (variant?.hashCode()?.toLong()?.and(0xFFFFFFFFL) ?: 0L)
        resolved[key]?.let {
            return it
        }
        if (furnitureRow in failed) {
            return null
        }
        val furniture = tables.furniture(furnitureRow) ?: return null
        val base = baseLoc(furniture)
        if (base == null) {
            failed += furnitureRow
            return null
        }
        val piece = if (variant == null) base else pieceLoc(base, variant) ?: return null
        resolved[key] = piece.id
        return piece.id
    }

    /** Whether every piece [variants] asks for can be placed. */
    public fun canPlace(furnitureRow: Int, variants: Collection<String?>): Boolean =
        variants.all { resolve(furnitureRow, it) != null }

    private fun baseLoc(furniture: FurnitureData): UnpackedLocType? {
        val name = furniture.internalName
        if (name != null) {
            byName[name]?.let {
                return it
            }
            byName[name.replaceFirst(TRAILING_INDEX, "$1")]?.let {
                return it
            }
        }
        val model = objTypes[furniture.modelObj]?.model ?: return null
        val candidates = pohLocsByModel[model].orEmpty()
        return candidates.minWithOrNull(
            compareBy({ it.internalName?.length ?: Int.MAX_VALUE }, { it.id })
        )
    }

    /**
     * Finds the sibling of [base] that covers [variant]. `poh_rugcorner2` with variant `middle`
     * becomes `poh_rugmiddle2`; a base with no piece word in its name gets the variant appended.
     */
    private fun pieceLoc(base: UnpackedLocType, variant: String): UnpackedLocType? {
        val name = base.internalName ?: return null
        for (word in PIECE_WORDS) {
            if (word == variant || !name.contains(word)) continue
            byName[name.replace(word, variant)]?.let {
                return it
            }
        }
        if (name.contains(variant)) {
            return base
        }
        val stem = name.dropLastWhile(Char::isDigit)
        val index = name.takeLastWhile(Char::isDigit)
        byName["$stem$variant$index"]?.let {
            return it
        }
        return byName["${name}_$variant"]
    }

    private companion object {
        const val POH_PREFIX = "poh_"

        /** `poh_armchair_4` -> `poh_armchair4`. */
        val TRAILING_INDEX = Regex("_(\\d+)$")

        /**
         * The words the cache uses for the pieces of a multi-tile hotspot, longest first so
         * `corner` is never matched inside a longer word.
         */
        val PIECE_WORDS = listOf("middle", "corner", "side", "left", "right", "top", "bottom")
    }
}
