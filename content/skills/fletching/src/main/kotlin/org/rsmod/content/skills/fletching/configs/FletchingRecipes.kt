package org.rsmod.content.skills.fletching.configs

import org.rsmod.content.skills.fletching.configs.FletchingObjs as obj
import org.rsmod.content.skills.fletching.configs.FletchingSeqs as seq
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.seq.SeqType

/**
 * One product on the "what would you like to make?" menu a knife opens over a stack of logs.
 *
 * [logs] is the number of logs consumed, which is 1 for everything except the wooden shields, and
 * [count] the number of items handed back -- 15 for arrow shafts, 1 for everything else.
 */
data class CutProduct(
    val product: ObjType,
    val levelReq: Int,
    val xp: Double,
    val count: Int = 1,
    val logs: Int = 1,
)

/** The menu one type of log offers, in the order the buttons are drawn. */
data class CutLogRecipe(val log: ObjType, val products: List<CutProduct>)

/** Adding a string to an unstrung bow or crossbow. */
data class StringRecipe(
    val unstrung: ObjType,
    val string: ObjType,
    val strung: ObjType,
    val levelReq: Int,
    val xp: Double,
)

/**
 * Attaching one obj to a stack of another: feathers onto shafts, heads onto headless arrows,
 * feathers onto dart tips and bolts, heads onto javelin shafts.
 *
 * All four families share a shape, so they share a row type. [batch] is how many are finished in a
 * single action -- 15 for arrows, bolts and javelins, 10 for darts -- and [xp] is per *item*, not
 * per batch, because that is how the numbers are quoted and how partial batches have to be paid.
 */
data class AttachRecipe(
    val base: ObjType,
    val tip: ObjType,
    val product: ObjType,
    val levelReq: Int,
    val xp: Double,
    val batch: Int,
    val seq: SeqType,
)

/** Fitting limbs to a stock. The result still needs a crossbow string. */
data class CrossbowRecipe(
    val limb: ObjType,
    val stock: ObjType,
    val unstrung: ObjType,
    val levelReq: Int,
    val xp: Double,
    val seq: SeqType,
)

/**
 * The whole Fletching table, at live OSRS levels and experience.
 *
 * Authored rather than read out of the cache: unlike anvil smithing, which has `smithing_product_*`
 * enums to lean on, the cache carries nothing that maps a knife and a log to a bow.
 *
 * Every list is validated in `init` -- no duplicate ingredient pair, no level outside 1..99, no
 * non-positive xp -- so a mistyped row fails at class-load rather than quietly paying the wrong
 * experience for the life of the server.
 */
object FletchingRecipes {
    // ---------------------------------------------------------------- knife on logs

    val cutting: List<CutLogRecipe> =
        listOf(
            CutLogRecipe(
                obj.logs,
                listOf(
                    CutProduct(obj.arrow_shaft, levelReq = 1, xp = 5.0, count = 15),
                    CutProduct(obj.unstrung_shortbow, levelReq = 5, xp = 5.0),
                    CutProduct(obj.unstrung_longbow, levelReq = 10, xp = 10.0),
                    CutProduct(obj.stock_wood, levelReq = 9, xp = 6.0),
                ),
            ),
            CutLogRecipe(
                obj.oak_logs,
                listOf(
                    CutProduct(obj.unstrung_oak_shortbow, levelReq = 20, xp = 16.5),
                    CutProduct(obj.unstrung_oak_longbow, levelReq = 25, xp = 25.0),
                    CutProduct(obj.oak_shield, levelReq = 27, xp = 50.0, logs = 2),
                    CutProduct(obj.stock_oak, levelReq = 24, xp = 16.0),
                ),
            ),
            CutLogRecipe(
                obj.willow_logs,
                listOf(
                    CutProduct(obj.unstrung_willow_shortbow, levelReq = 35, xp = 33.3),
                    CutProduct(obj.unstrung_willow_longbow, levelReq = 40, xp = 41.5),
                    CutProduct(obj.willow_shield, levelReq = 42, xp = 83.0, logs = 2),
                    CutProduct(obj.stock_willow, levelReq = 39, xp = 22.0),
                ),
            ),
            CutLogRecipe(
                obj.teak_logs,
                listOf(CutProduct(obj.stock_teak, levelReq = 46, xp = 27.0)),
            ),
            CutLogRecipe(
                obj.maple_logs,
                listOf(
                    CutProduct(obj.unstrung_maple_shortbow, levelReq = 50, xp = 50.0),
                    CutProduct(obj.unstrung_maple_longbow, levelReq = 55, xp = 58.3),
                    CutProduct(obj.maple_shield, levelReq = 62, xp = 116.5, logs = 2),
                    CutProduct(obj.stock_maple, levelReq = 54, xp = 32.0),
                ),
            ),
            CutLogRecipe(
                obj.mahogany_logs,
                listOf(CutProduct(obj.stock_mahogany, levelReq = 61, xp = 41.0)),
            ),
            CutLogRecipe(
                obj.yew_logs,
                listOf(
                    CutProduct(obj.unstrung_yew_shortbow, levelReq = 65, xp = 67.5),
                    CutProduct(obj.unstrung_yew_longbow, levelReq = 70, xp = 75.0),
                    CutProduct(obj.yew_shield, levelReq = 77, xp = 150.0, logs = 2),
                    CutProduct(obj.stock_yew, levelReq = 69, xp = 50.0),
                ),
            ),
            CutLogRecipe(
                obj.magic_logs,
                listOf(
                    CutProduct(obj.unstrung_magic_shortbow, levelReq = 80, xp = 83.3),
                    CutProduct(obj.unstrung_magic_longbow, levelReq = 85, xp = 91.5),
                    CutProduct(obj.magic_shield, levelReq = 92, xp = 183.0, logs = 2),
                    CutProduct(obj.stock_magic, levelReq = 78, xp = 70.0),
                ),
            ),
            CutLogRecipe(
                obj.redwood_logs,
                listOf(CutProduct(obj.redwood_shield, levelReq = 92, xp = 216.0, logs = 2)),
            ),
        )

    // ---------------------------------------------------------------- stringing

    val stringing: List<StringRecipe> =
        listOf(
            string(obj.unstrung_shortbow, obj.shortbow, 5, 5.0),
            string(obj.unstrung_longbow, obj.longbow, 10, 10.0),
            string(obj.unstrung_oak_shortbow, obj.oak_shortbow, 20, 16.5),
            string(obj.unstrung_oak_longbow, obj.oak_longbow, 25, 25.0),
            string(obj.unstrung_willow_shortbow, obj.willow_shortbow, 35, 33.3),
            string(obj.unstrung_willow_longbow, obj.willow_longbow, 40, 41.5),
            string(obj.unstrung_maple_shortbow, obj.maple_shortbow, 50, 50.0),
            string(obj.unstrung_maple_longbow, obj.maple_longbow, 55, 58.3),
            string(obj.unstrung_yew_shortbow, obj.yew_shortbow, 65, 67.5),
            string(obj.unstrung_yew_longbow, obj.yew_longbow, 70, 75.0),
            string(obj.unstrung_magic_shortbow, obj.magic_shortbow, 80, 83.3),
            string(obj.unstrung_magic_longbow, obj.magic_longbow, 85, 91.5),
            // Crossbows take their own string, not a bowstring.
            crossbowString(obj.unstrung_bronze_crossbow, obj.bronze_crossbow, 9, 6.0),
            crossbowString(obj.unstrung_blurite_crossbow, obj.blurite_crossbow, 24, 16.0),
            crossbowString(obj.unstrung_iron_crossbow, obj.iron_crossbow, 39, 22.0),
            crossbowString(obj.unstrung_steel_crossbow, obj.steel_crossbow, 46, 27.0),
            crossbowString(obj.unstrung_mithril_crossbow, obj.mithril_crossbow, 54, 32.0),
            crossbowString(obj.unstrung_adamantite_crossbow, obj.adamantite_crossbow, 61, 41.0),
            crossbowString(obj.unstrung_runite_crossbow, obj.runite_crossbow, 69, 50.0),
            crossbowString(obj.unstrung_dragon_crossbow, obj.dragon_crossbow, 78, 70.0),
        )

    // ---------------------------------------------------------------- attaching

    /** Feathering shafts is the one recipe whose "tip" is also its only ingredient by name. */
    val headlessArrows: AttachRecipe =
        AttachRecipe(
            base = obj.arrow_shaft,
            tip = obj.feather,
            product = obj.headless_arrow,
            levelReq = 1,
            xp = 1.0,
            batch = ARROW_BATCH,
            seq = seq.add_feather,
        )

    val arrows: List<AttachRecipe> =
        listOf(
            arrow(obj.bronze_arrowheads, obj.bronze_arrow, 1, 1.3),
            arrow(obj.iron_arrowheads, obj.iron_arrow, 15, 2.5),
            arrow(obj.steel_arrowheads, obj.steel_arrow, 30, 5.0),
            arrow(obj.mithril_arrowheads, obj.mithril_arrow, 45, 7.5),
            arrow(obj.adamant_arrowheads, obj.adamant_arrow, 60, 10.0),
            arrow(obj.rune_arrowheads, obj.rune_arrow, 75, 12.5),
            arrow(obj.amethyst_arrowheads, obj.amethyst_arrow, 82, 13.5),
            arrow(obj.dragon_arrowheads, obj.dragon_arrow, 90, 15.0),
        )

    val darts: List<AttachRecipe> =
        listOf(
            dart(obj.bronze_dart_tip, obj.bronze_dart, 1, 1.8, seq.dart_feathers_bronze),
            dart(obj.iron_dart_tip, obj.iron_dart, 22, 3.8, seq.dart_feathers_iron),
            dart(obj.steel_dart_tip, obj.steel_dart, 37, 7.5, seq.dart_feathers_steel),
            dart(obj.mithril_dart_tip, obj.mithril_dart, 52, 11.2, seq.dart_feathers_mithril),
            dart(obj.adamant_dart_tip, obj.adamant_dart, 67, 15.0, seq.dart_feathers_adamant),
            dart(obj.rune_dart_tip, obj.rune_dart, 81, 18.8, seq.dart_feathers_rune),
            dart(obj.amethyst_dart_tip, obj.amethyst_dart, 90, 21.0, seq.dart_feathers_amethyst),
            dart(obj.dragon_dart_tip, obj.dragon_dart, 95, 25.0, seq.dart_feathers_dragon),
        )

    val bolts: List<AttachRecipe> =
        listOf(
            bolt(obj.bronze_bolts_unf, obj.bronze_bolts, 9, 0.5, seq.bolt_feathers_bronze),
            bolt(obj.blurite_bolts_unf, obj.blurite_bolts, 24, 1.0, seq.bolt_feathers_blurite),
            bolt(obj.iron_bolts_unf, obj.iron_bolts, 39, 1.5, seq.bolt_feathers_iron),
            bolt(obj.silver_bolts_unf, obj.silver_bolts, 43, 2.5, seq.bolt_feathers_silver),
            bolt(obj.steel_bolts_unf, obj.steel_bolts, 46, 3.5, seq.bolt_feathers_steel),
            bolt(obj.mithril_bolts_unf, obj.mithril_bolts, 54, 5.0, seq.bolt_feathers_mithril),
            bolt(
                obj.adamantite_bolts_unf,
                obj.adamantite_bolts,
                61,
                7.0,
                seq.bolt_feathers_adamant,
            ),
            bolt(obj.runite_bolts_unf, obj.runite_bolts, 69, 10.0, seq.bolt_feathers_rune),
            bolt(obj.dragon_bolts_unf, obj.dragon_bolts, 84, 12.0, seq.bolt_feathers_dragon),
        )

    val javelins: List<AttachRecipe> =
        listOf(
            javelin(obj.bronze_javelin_head, obj.bronze_javelin, 3, 1.0),
            javelin(obj.iron_javelin_head, obj.iron_javelin, 17, 2.0),
            javelin(obj.steel_javelin_head, obj.steel_javelin, 32, 5.0),
            javelin(obj.mithril_javelin_head, obj.mithril_javelin, 47, 8.0),
            javelin(obj.adamant_javelin_head, obj.adamant_javelin, 62, 10.0),
            javelin(obj.rune_javelin_head, obj.rune_javelin, 77, 12.4),
            javelin(obj.amethyst_javelin_head, obj.amethyst_javelin, 84, 13.4),
            javelin(obj.dragon_javelin_head, obj.dragon_javelin, 92, 15.0),
        )

    /** Every "use A on B and get C" row, which is what the item-on-item bindings iterate. */
    val attaching: List<AttachRecipe> = listOf(headlessArrows) + arrows + darts + bolts + javelins

    // ---------------------------------------------------------------- crossbow limbs

    val crossbows: List<CrossbowRecipe> =
        listOf(
            CrossbowRecipe(
                obj.limbs_bronze,
                obj.stock_wood,
                obj.unstrung_bronze_crossbow,
                9,
                12.0,
                seq.crossbow_bronze,
            ),
            CrossbowRecipe(
                obj.limbs_blurite,
                obj.stock_oak,
                obj.unstrung_blurite_crossbow,
                24,
                32.0,
                seq.crossbow_blurite,
            ),
            CrossbowRecipe(
                obj.limbs_iron,
                obj.stock_willow,
                obj.unstrung_iron_crossbow,
                39,
                44.0,
                seq.crossbow_iron,
            ),
            CrossbowRecipe(
                obj.limbs_steel,
                obj.stock_teak,
                obj.unstrung_steel_crossbow,
                46,
                54.0,
                seq.crossbow_steel,
            ),
            CrossbowRecipe(
                obj.limbs_mithril,
                obj.stock_maple,
                obj.unstrung_mithril_crossbow,
                54,
                64.0,
                seq.crossbow_mithril,
            ),
            CrossbowRecipe(
                obj.limbs_adamantite,
                obj.stock_mahogany,
                obj.unstrung_adamantite_crossbow,
                61,
                82.0,
                seq.crossbow_adamantite,
            ),
            CrossbowRecipe(
                obj.limbs_runite,
                obj.stock_yew,
                obj.unstrung_runite_crossbow,
                69,
                100.0,
                seq.crossbow_runite,
            ),
            CrossbowRecipe(
                obj.limbs_dragon,
                obj.stock_magic,
                obj.unstrung_dragon_crossbow,
                78,
                135.0,
                seq.crossbow_runite,
            ),
        )

    // ---------------------------------------------------------------- lookups

    /**
     * Cut menus keyed by the log's raw id.
     *
     * Keyed by `Int` on purpose: `find(...)` hands back a `HashedObjType` while the runtime hands
     * the script an `UnpackedObjType`, and the two never compare equal.
     */
    val cuttingByLog: Map<Int, CutLogRecipe> = cutting.associateBy { it.log.id }

    val stringingByUnstrung: Map<Int, StringRecipe> = stringing.associateBy { it.unstrung.id }

    private fun string(unstrung: ObjType, strung: ObjType, levelReq: Int, xp: Double) =
        StringRecipe(unstrung, obj.bow_string, strung, levelReq, xp)

    private fun crossbowString(unstrung: ObjType, strung: ObjType, levelReq: Int, xp: Double) =
        StringRecipe(unstrung, obj.crossbow_string, strung, levelReq, xp)

    private fun arrow(heads: ObjType, product: ObjType, levelReq: Int, xp: Double) =
        AttachRecipe(
            base = obj.headless_arrow,
            tip = heads,
            product = product,
            levelReq = levelReq,
            xp = xp,
            batch = ARROW_BATCH,
            seq = seq.add_arrow_tips,
        )

    private fun dart(tip: ObjType, product: ObjType, levelReq: Int, xp: Double, anim: SeqType) =
        AttachRecipe(
            base = tip,
            tip = obj.feather,
            product = product,
            levelReq = levelReq,
            xp = xp,
            batch = DART_BATCH,
            seq = anim,
        )

    private fun bolt(unf: ObjType, product: ObjType, levelReq: Int, xp: Double, anim: SeqType) =
        AttachRecipe(
            base = unf,
            tip = obj.feather,
            product = product,
            levelReq = levelReq,
            xp = xp,
            batch = ARROW_BATCH,
            seq = anim,
        )

    private fun javelin(head: ObjType, product: ObjType, levelReq: Int, xp: Double) =
        AttachRecipe(
            base = obj.javelin_shaft,
            tip = head,
            product = product,
            levelReq = levelReq,
            xp = xp,
            batch = ARROW_BATCH,
            seq = seq.add_arrow_tips,
        )

    init {
        val cutProducts = cutting.flatMap { it.products }
        for (recipe in cutting) {
            require(recipe.products.isNotEmpty()) { "No products for log: ${recipe.log}" }
            require(recipe.products.size <= MENU_SLOTS) {
                "A log offers more products than the make-menu can draw: ${recipe.log}"
            }
        }
        require(cutting.distinctBy { it.log.id }.size == cutting.size) { "Duplicate log recipe." }
        require(stringing.distinctBy { it.unstrung.id }.size == stringing.size) {
            "Duplicate stringing recipe."
        }

        // The item-on-item bindings key on the ordered pair, so a repeat would be a startup crash
        // inside the event bus rather than anything this class could explain.
        val pairs =
            attaching.map { it.base.id to it.tip.id } + crossbows.map { it.limb.id to it.stock.id }
        require(pairs.distinct().size == pairs.size) {
            "Two recipes claim the same ingredient pair."
        }

        val levels =
            cutProducts.map { it.levelReq } +
                stringing.map { it.levelReq } +
                attaching.map { it.levelReq } +
                crossbows.map { it.levelReq }
        require(levels.all { it in 1..99 }) { "A recipe has a level outside 1..99." }

        val xp =
            cutProducts.map { it.xp } +
                stringing.map { it.xp } +
                attaching.map { it.xp } +
                crossbows.map { it.xp }
        require(xp.all { it > 0.0 }) { "A recipe pays no experience." }

        require(cutProducts.all { it.count >= 1 && it.logs >= 1 }) {
            "A cut product consumes or yields nothing."
        }
        require(attaching.all { it.batch >= 1 }) { "An attach recipe has an empty batch." }
    }

    /** How many arrows, bolts or javelins one action finishes. */
    const val ARROW_BATCH: Int = 15

    /** Darts come ten at a time. */
    const val DART_BATCH: Int = 10

    /** The make-menu draws at most ten buttons; no log comes close, but the table is checked. */
    private const val MENU_SLOTS: Int = 10
}
