package org.rsmod.content.skills.crafting.configs

import org.rsmod.content.skills.crafting.configs.CraftingObjs as obj
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

/** Cutting an uncut gem with a chisel. */
data class GemRecipe(
    val uncut: ObjType,
    val cut: ObjType,
    val levelReq: Int,
    val xp: Double,
    /**
     * Percent chance the gem shatters instead. Opal and jade are the only gems that can, and only
     * below their "stop failing" level, which is why the odds are a level curve rather than a
     * constant -- see [crushChance].
     */
    val crushes: Boolean = false,
)

/** One product on the menu a needle opens over a hide. */
data class LeatherProduct(
    val product: ObjType,
    val levelReq: Int,
    val xp: Double,
    val hides: Int = 1,
)

/** A hide and everything a needle and thread can turn it into. */
data class LeatherRecipe(val hide: ObjType, val products: List<LeatherProduct>)

/** One row of a jewellery panel: a bar, an optional gem, a mould and what comes out. */
data class JewelleryRecipe(
    val component: ComponentType,
    val product: ObjType,
    val bar: ObjType,
    val mould: ObjType,
    val gem: ObjType?,
    val levelReq: Int,
    val xp: Double,
)

/** Spinning wheel work: one obj becomes another. */
data class SpinRecipe(
    val material: ObjType,
    val product: ObjType,
    val levelReq: Int,
    val xp: Double,
)

/** One row of the tanner's panel. */
data class TanRecipe(val hide: ObjType, val leather: ObjType, val name: String, val cost: Int)

/**
 * The Crafting tables, at live OSRS levels and experience.
 *
 * Authored rather than decoded: the jewellery panels carry their gem and mould in the cache, but
 * nothing there records a level requirement or an experience value.
 *
 * Validated in `init` on the same terms as `FletchingRecipes`, so a mistyped row fails at
 * class-load.
 */
object CraftingRecipes {
    // ---------------------------------------------------------------- chisel on gem

    val gems: List<GemRecipe> =
        listOf(
            GemRecipe(obj.uncut_opal, obj.opal, 1, 15.0, crushes = true),
            GemRecipe(obj.uncut_jade, obj.jade, 13, 20.0, crushes = true),
            GemRecipe(obj.uncut_red_topaz, obj.red_topaz, 16, 25.0, crushes = true),
            GemRecipe(obj.uncut_sapphire, obj.sapphire, 20, 50.0),
            GemRecipe(obj.uncut_emerald, obj.emerald, 27, 67.5),
            GemRecipe(obj.uncut_ruby, obj.ruby, 34, 85.0),
            GemRecipe(obj.uncut_diamond, obj.diamond, 43, 107.5),
            GemRecipe(obj.uncut_dragonstone, obj.dragonstone, 55, 137.5),
            GemRecipe(obj.uncut_onyx, obj.onyx, 67, 167.5),
            GemRecipe(obj.uncut_zenyte, obj.zenyte, 89, 200.0),
        )

    /**
     * Odds of shattering a semi-precious gem, in percent.
     *
     * Only opal, jade and red topaz can shatter, and the chance falls away as the level climbs past
     * the requirement, reaching zero well before the gem stops being worth cutting. The curve is
     * tuned rather than measured: the live rates are not published, only the fact that they taper.
     */
    fun crushChance(recipe: GemRecipe, level: Int): Int {
        if (!recipe.crushes) {
            return 0
        }
        val above = (level - recipe.levelReq).coerceAtLeast(0)
        return (BASE_CRUSH_PERCENT - above * CRUSH_FALLOFF).coerceAtLeast(0)
    }

    // ---------------------------------------------------------------- needle on hide

    val leather: List<LeatherRecipe> =
        listOf(
            LeatherRecipe(
                obj.leather,
                listOf(
                    LeatherProduct(obj.leather_gloves, 1, 13.8),
                    LeatherProduct(obj.leather_boots, 7, 16.25),
                    LeatherProduct(obj.leather_cowl, 9, 18.5),
                    LeatherProduct(obj.leather_vambraces, 11, 22.0),
                    LeatherProduct(obj.leather_armour, 14, 25.0),
                    LeatherProduct(obj.leather_chaps, 18, 27.0),
                    LeatherProduct(obj.coif, 38, 37.0),
                ),
            ),
            LeatherRecipe(obj.hard_leather, listOf(LeatherProduct(obj.hardleather_body, 28, 35.0))),
            LeatherRecipe(
                obj.dragon_leather,
                listOf(
                    LeatherProduct(obj.green_dhide_vambraces, 57, 62.0),
                    LeatherProduct(obj.green_dhide_chaps, 60, 124.0, hides = 2),
                    LeatherProduct(obj.green_dhide_body, 63, 186.0, hides = 3),
                ),
            ),
            LeatherRecipe(
                obj.dragon_leather_blue,
                listOf(
                    LeatherProduct(obj.blue_dhide_vambraces, 66, 70.0),
                    LeatherProduct(obj.blue_dhide_chaps, 68, 140.0, hides = 2),
                    LeatherProduct(obj.blue_dhide_body, 71, 210.0, hides = 3),
                ),
            ),
            LeatherRecipe(
                obj.dragon_leather_red,
                listOf(
                    LeatherProduct(obj.red_dhide_vambraces, 73, 78.0),
                    LeatherProduct(obj.red_dhide_chaps, 75, 156.0, hides = 2),
                    LeatherProduct(obj.red_dhide_body, 77, 234.0, hides = 3),
                ),
            ),
            LeatherRecipe(
                obj.dragon_leather_black,
                listOf(
                    LeatherProduct(obj.black_dhide_vambraces, 79, 86.0),
                    LeatherProduct(obj.black_dhide_chaps, 82, 172.0, hides = 2),
                    LeatherProduct(obj.black_dhide_body, 84, 258.0, hides = 3),
                ),
            ),
        )

    /** How many items one reel of thread finishes before it runs out. */
    const val ITEMS_PER_THREAD: Int = 5

    // ---------------------------------------------------------------- jewellery

    val goldJewellery: List<JewelleryRecipe> =
        listOf(
            gold(CraftingGoldComponents.gold_ring, obj.gold_ring, obj.ring_mould, null, 5, 15.0),
            gold(
                CraftingGoldComponents.sapphire_ring,
                obj.sapphire_ring,
                obj.ring_mould,
                obj.sapphire,
                20,
                40.0,
            ),
            gold(
                CraftingGoldComponents.emerald_ring,
                obj.emerald_ring,
                obj.ring_mould,
                obj.emerald,
                27,
                55.0,
            ),
            gold(
                CraftingGoldComponents.ruby_ring,
                obj.ruby_ring,
                obj.ring_mould,
                obj.ruby,
                34,
                70.0,
            ),
            gold(
                CraftingGoldComponents.diamond_ring,
                obj.diamond_ring,
                obj.ring_mould,
                obj.diamond,
                43,
                85.0,
            ),
            gold(
                CraftingGoldComponents.dragon_ring,
                obj.dragonstone_ring,
                obj.ring_mould,
                obj.dragonstone,
                55,
                100.0,
            ),
            gold(
                CraftingGoldComponents.onyx_ring,
                obj.onyx_ring,
                obj.ring_mould,
                obj.onyx,
                67,
                115.0,
            ),
            gold(
                CraftingGoldComponents.zenyte_ring,
                obj.zenyte_ring,
                obj.ring_mould,
                obj.zenyte,
                89,
                150.0,
            ),
            gold(
                CraftingGoldComponents.gold_necklace,
                obj.gold_necklace,
                obj.necklace_mould,
                null,
                6,
                20.0,
            ),
            gold(
                CraftingGoldComponents.sapphire_necklace,
                obj.sapphire_necklace,
                obj.necklace_mould,
                obj.sapphire,
                22,
                55.0,
            ),
            gold(
                CraftingGoldComponents.emerald_necklace,
                obj.emerald_necklace,
                obj.necklace_mould,
                obj.emerald,
                29,
                60.0,
            ),
            gold(
                CraftingGoldComponents.ruby_necklace,
                obj.ruby_necklace,
                obj.necklace_mould,
                obj.ruby,
                40,
                75.0,
            ),
            gold(
                CraftingGoldComponents.diamond_necklace,
                obj.diamond_necklace,
                obj.necklace_mould,
                obj.diamond,
                56,
                90.0,
            ),
            gold(
                CraftingGoldComponents.dragon_necklace,
                obj.dragonstone_necklace,
                obj.necklace_mould,
                obj.dragonstone,
                72,
                105.0,
            ),
            gold(
                CraftingGoldComponents.onyx_necklace,
                obj.onyx_necklace,
                obj.necklace_mould,
                obj.onyx,
                82,
                120.0,
            ),
            gold(
                CraftingGoldComponents.zenyte_necklace,
                obj.zenyte_necklace,
                obj.necklace_mould,
                obj.zenyte,
                92,
                165.0,
            ),
            gold(
                CraftingGoldComponents.gold_amulet,
                obj.unstrung_gold_amulet,
                obj.amulet_mould,
                null,
                8,
                30.0,
            ),
            gold(
                CraftingGoldComponents.sapphire_amulet,
                obj.unstrung_sapphire_amulet,
                obj.amulet_mould,
                obj.sapphire,
                24,
                65.0,
            ),
            gold(
                CraftingGoldComponents.emerald_amulet,
                obj.unstrung_emerald_amulet,
                obj.amulet_mould,
                obj.emerald,
                31,
                70.0,
            ),
            gold(
                CraftingGoldComponents.ruby_amulet,
                obj.unstrung_ruby_amulet,
                obj.amulet_mould,
                obj.ruby,
                50,
                85.0,
            ),
            gold(
                CraftingGoldComponents.diamond_amulet,
                obj.unstrung_diamond_amulet,
                obj.amulet_mould,
                obj.diamond,
                70,
                100.0,
            ),
            gold(
                CraftingGoldComponents.dragon_amulet,
                obj.unstrung_dragonstone_amulet,
                obj.amulet_mould,
                obj.dragonstone,
                80,
                150.0,
            ),
            gold(
                CraftingGoldComponents.onyx_amulet,
                obj.unstrung_onyx_amulet,
                obj.amulet_mould,
                obj.onyx,
                90,
                165.0,
            ),
            gold(
                CraftingGoldComponents.zenyte_amulet,
                obj.unstrung_zenyte_amulet,
                obj.amulet_mould,
                obj.zenyte,
                98,
                200.0,
            ),
            gold(
                CraftingGoldComponents.gold_bracelet,
                obj.gold_bracelet,
                obj.bracelet_mould,
                null,
                7,
                25.0,
            ),
            gold(
                CraftingGoldComponents.sapphire_bracelet,
                obj.sapphire_bracelet,
                obj.bracelet_mould,
                obj.sapphire,
                23,
                60.0,
            ),
            gold(
                CraftingGoldComponents.emerald_bracelet,
                obj.emerald_bracelet,
                obj.bracelet_mould,
                obj.emerald,
                30,
                65.0,
            ),
            gold(
                CraftingGoldComponents.ruby_bracelet,
                obj.ruby_bracelet,
                obj.bracelet_mould,
                obj.ruby,
                42,
                80.0,
            ),
            gold(
                CraftingGoldComponents.diamond_bracelet,
                obj.diamond_bracelet,
                obj.bracelet_mould,
                obj.diamond,
                58,
                95.0,
            ),
            gold(
                CraftingGoldComponents.dragon_bracelet,
                obj.dragonstone_bracelet,
                obj.bracelet_mould,
                obj.dragonstone,
                74,
                110.0,
            ),
            gold(
                CraftingGoldComponents.onyx_bracelet,
                obj.onyx_bracelet,
                obj.bracelet_mould,
                obj.onyx,
                84,
                125.0,
            ),
            gold(
                CraftingGoldComponents.zenyte_bracelet,
                obj.zenyte_bracelet,
                obj.bracelet_mould,
                obj.zenyte,
                95,
                180.0,
            ),
        )

    val silverJewellery: List<JewelleryRecipe> =
        listOf(
            silver(
                SilverCraftingComponents.opal_ring,
                obj.opal_ring,
                obj.ring_mould,
                obj.opal,
                1,
                10.0,
            ),
            silver(
                SilverCraftingComponents.jade_ring,
                obj.jade_ring,
                obj.ring_mould,
                obj.jade,
                13,
                32.0,
            ),
            silver(
                SilverCraftingComponents.topaz_ring,
                obj.topaz_ring,
                obj.ring_mould,
                obj.red_topaz,
                16,
                35.0,
            ),
            silver(
                SilverCraftingComponents.opal_necklace,
                obj.opal_necklace,
                obj.necklace_mould,
                obj.opal,
                16,
                35.0,
            ),
            silver(
                SilverCraftingComponents.jade_necklace,
                obj.jade_necklace,
                obj.necklace_mould,
                obj.jade,
                25,
                54.0,
            ),
            silver(
                SilverCraftingComponents.topaz_necklace,
                obj.topaz_necklace,
                obj.necklace_mould,
                obj.red_topaz,
                32,
                70.0,
            ),
            silver(
                SilverCraftingComponents.opal_amulet,
                obj.unstrung_opal_amulet,
                obj.amulet_mould,
                obj.opal,
                27,
                55.0,
            ),
            silver(
                SilverCraftingComponents.jade_amulet,
                obj.unstrung_jade_amulet,
                obj.amulet_mould,
                obj.jade,
                34,
                70.0,
            ),
            silver(
                SilverCraftingComponents.topaz_amulet,
                obj.unstrung_topaz_amulet,
                obj.amulet_mould,
                obj.red_topaz,
                45,
                80.0,
            ),
            silver(
                SilverCraftingComponents.opal_bracelet,
                obj.opal_bracelet,
                obj.bracelet_mould,
                obj.opal,
                22,
                45.0,
            ),
            silver(
                SilverCraftingComponents.jade_bracelet,
                obj.jade_bracelet,
                obj.bracelet_mould,
                obj.jade,
                29,
                60.0,
            ),
            silver(
                SilverCraftingComponents.topaz_bracelet,
                obj.topaz_bracelet,
                obj.bracelet_mould,
                obj.red_topaz,
                38,
                75.0,
            ),
            silver(
                SilverCraftingComponents.holy_symbol,
                obj.unstrung_holy_symbol,
                obj.holy_symbol_mould,
                null,
                16,
                50.0,
            ),
            silver(
                SilverCraftingComponents.unholy_symbol,
                obj.unstrung_unholy_symbol,
                obj.unholy_symbol_mould,
                null,
                17,
                50.0,
            ),
            silver(
                SilverCraftingComponents.sickle,
                obj.silver_sickle,
                obj.sickle_mould,
                null,
                18,
                50.0,
            ),
            silver(SilverCraftingComponents.tiara, obj.tiara, obj.tiara_mould, null, 23, 52.5),
        )

    /** Both panels, for the lookups the script needs. */
    val jewellery: List<JewelleryRecipe> = goldJewellery + silverJewellery

    val jewelleryByComponent: Map<Int, JewelleryRecipe> =
        jewellery.associateBy { it.component.packed }

    // ---------------------------------------------------------------- spinning wheel

    val spinning: List<SpinRecipe> =
        listOf(
            SpinRecipe(obj.wool, obj.ball_of_wool, 1, 2.5),
            SpinRecipe(obj.flax, obj.bow_string, 10, 15.0),
        )

    /**
     * Stringing an amulet with a ball of wool. Not a Crafting action: no level, no experience.
     *
     * A list of pairs rather than a map because it is only ever iterated to register bindings, and
     * the bindings want the `ObjType` itself. The "key by raw id" rule applies to lookups against
     * an obj the runtime hands back, which this is not.
     */
    val amuletStringing: List<Pair<ObjType, ObjType>> =
        listOf(
            obj.unstrung_gold_amulet to obj.gold_amulet,
            obj.unstrung_sapphire_amulet to obj.sapphire_amulet,
            obj.unstrung_emerald_amulet to obj.emerald_amulet,
            obj.unstrung_ruby_amulet to obj.ruby_amulet,
            obj.unstrung_diamond_amulet to obj.diamond_amulet,
            obj.unstrung_dragonstone_amulet to obj.dragonstone_amulet,
            obj.unstrung_onyx_amulet to obj.onyx_amulet,
            obj.unstrung_zenyte_amulet to obj.zenyte_amulet,
            obj.unstrung_opal_amulet to obj.opal_amulet,
            obj.unstrung_jade_amulet to obj.jade_amulet,
            obj.unstrung_topaz_amulet to obj.topaz_amulet,
            obj.unstrung_holy_symbol to obj.holy_symbol,
        )

    // ---------------------------------------------------------------- tanning

    /**
     * What the tanner offers, in panel-row order.
     *
     * The panel has eight rows; this list fills seven and the eighth is hidden. Soft and hard
     * leather are the same cowhide at two prices, which is why the hide repeats.
     */
    val tanning: List<TanRecipe> =
        listOf(
            TanRecipe(obj.cowhide, obj.leather, "Soft leather", 1),
            TanRecipe(obj.cowhide, obj.hard_leather, "Hard leather", 3),
            TanRecipe(obj.snake_hide, obj.snakeskin, "Snakeskin", 15),
            TanRecipe(obj.dragonhide_green, obj.dragon_leather, "Green d'hide", 20),
            TanRecipe(obj.dragonhide_blue, obj.dragon_leather_blue, "Blue d'hide", 20),
            TanRecipe(obj.dragonhide_red, obj.dragon_leather_red, "Red d'hide", 20),
            TanRecipe(obj.dragonhide_black, obj.dragon_leather_black, "Black d'hide", 20),
        )

    private fun gold(
        component: ComponentType,
        product: ObjType,
        mould: ObjType,
        gem: ObjType?,
        levelReq: Int,
        xp: Double,
    ) = JewelleryRecipe(component, product, obj.gold_bar, mould, gem, levelReq, xp)

    private fun silver(
        component: ComponentType,
        product: ObjType,
        mould: ObjType,
        gem: ObjType?,
        levelReq: Int,
        xp: Double,
    ) = JewelleryRecipe(component, product, obj.silver_bar, mould, gem, levelReq, xp)

    init {
        val leatherProducts = leather.flatMap { it.products }
        for (recipe in leather) {
            require(recipe.products.isNotEmpty()) { "No products for hide: ${recipe.hide}" }
            require(recipe.products.size <= MENU_SLOTS) {
                "A hide offers more products than the make-menu can draw: ${recipe.hide}"
            }
        }
        require(leather.distinctBy { it.hide.id }.size == leather.size) { "Duplicate hide recipe." }
        require(gems.distinctBy { it.uncut.id }.size == gems.size) { "Duplicate gem recipe." }
        require(spinning.distinctBy { it.material.id }.size == spinning.size) {
            "Duplicate spinning recipe."
        }
        require(jewellery.distinctBy { it.component.packed }.size == jewellery.size) {
            "Two jewellery recipes share a button."
        }
        require(tanning.size <= MENU_SLOTS) { "More tanning rows than the make-menu can draw." }

        val levels =
            gems.map { it.levelReq } +
                leatherProducts.map { it.levelReq } +
                jewellery.map { it.levelReq } +
                spinning.map { it.levelReq }
        require(levels.all { it in 1..99 }) { "A recipe has a level outside 1..99." }

        val xp =
            gems.map { it.xp } +
                leatherProducts.map { it.xp } +
                jewellery.map { it.xp } +
                spinning.map { it.xp }
        require(xp.all { it > 0.0 }) { "A recipe pays no experience." }

        require(leatherProducts.all { it.hides >= 1 }) { "A leather product consumes no hide." }
        require(tanning.all { it.cost >= 0 }) { "A tanning row costs less than nothing." }
    }

    /** Shatter chance at the exact level requirement, in percent. */
    private const val BASE_CRUSH_PERCENT: Int = 20

    /** Percentage points the shatter chance drops per level above the requirement. */
    private const val CRUSH_FALLOFF: Int = 2

    private const val MENU_SLOTS: Int = 10
}
