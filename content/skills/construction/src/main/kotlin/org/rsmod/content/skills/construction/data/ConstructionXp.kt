package org.rsmod.content.skills.construction.data

/**
 * How much experience a piece of furniture is worth.
 *
 * This is the one part of construction the cache does not carry: `furniture` has the materials, the
 * level and the name, but no experience column, because on the real server experience is
 * server-side config. It is not arbitrary data, though -- construction was designed around a fixed
 * rate per material, and summing the materials reproduces the live value for the overwhelming
 * majority of furniture:
 * ```
 * Crude wooden chair   2 planks                  2 x 29  =   58   (live: 58)
 * Wooden chair         3 planks                  3 x 29  =   87   (live: 87)
 * Oak armchair         3 oak planks              3 x 60  =  180   (live: 180)
 * Mahogany armchair    2 mahogany planks         2 x 140 =  280   (live: 280)
 * Rug                  4 bolts of cloth          4 x 15  =   60   (live: 60)
 * Opulent rug          4 bolts + 1 gold leaf     60 + 300 = 360   (live: 360)
 * Marble fireplace     1 marble block            1 x 500 =  500   (live: 500)
 * Gilded four-poster   3 mahogany + 2 gold leaf 420 + 600 = 1020  (live: 1020)
 * ```
 *
 * It is not exact everywhere -- a wooden bookcase is 115 live against 116 here, and the clocks come
 * out high because their clockwork mechanism carries experience the sum cannot see. Rather than
 * transcribe five hundred numbers that would drift out of date, the sum stands, with the handful of
 * furniture that is worth checking pinned by [OVERRIDES]. Anything built from a material with no
 * known rate still earns [MINIMUM_XP] so no build is worth nothing.
 */
public object ConstructionXp {
    /** Experience per unit of each material, by obj id. */
    private val MATERIAL_XP: Map<Int, Double> =
        mapOf(
            960 to 29.0, // woodplank
            8778 to 60.0, // plank_oak
            8780 to 90.0, // plank_teak
            8782 to 140.0, // plank_mahogany
            8790 to 15.0, // cloth
            3420 to 40.0, // limestonebrick
            8784 to 300.0, // gold_leaf
            8786 to 500.0, // marble_block
            8788 to 1000.0, // poh_magic_crystal
            1539 to 0.0, // nails
        )

    /**
     * Furniture whose live value is known to differ from the material sum, keyed by the db row's
     * internal name. Kept deliberately short: every entry here is one the sum gets wrong.
     */
    private val OVERRIDES: Map<String, Double> =
        mapOf(
            "poh_bookcase_1" to 115.0, // 4 planks; the sum says 116
            "poh_bookcase_2" to 180.0,
            "poh_bookcase_3" to 420.0,
            "poh_clock_1" to 142.0, // clockwork mechanisms carry no plank rate
            "poh_clock_2" to 202.0,
            "poh_clock_3" to 142.0,
        )

    public const val MINIMUM_XP: Double = 1.0

    public fun of(furniture: FurnitureData): Double {
        OVERRIDES[furniture.internalName]?.let {
            return it
        }
        val sum =
            furniture.materials.sumOf { material ->
                (MATERIAL_XP[material.obj] ?: 0.0) * material.count
            }
        return if (sum > 0.0) sum else MINIMUM_XP
    }
}
