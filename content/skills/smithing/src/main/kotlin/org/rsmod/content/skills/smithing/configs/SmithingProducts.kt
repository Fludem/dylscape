package org.rsmod.content.skills.smithing.configs

import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.obj.ObjType

/**
 * What each button on the smithing interface makes, per bar tier.
 *
 * This mapping is the *only* part of anvil smithing this server authors. Everything numeric comes
 * straight out of the cache's own enums, which carry Jagex's full 181-product table:
 * - `smithing_product_to_requirement` (846) -- the Smithing level needed
 * - `smithing_product_to_bars_required` (845) -- how many bars it costs
 * - `smithing_product_to_quantity` (844) -- how many items come out
 *
 * So a rune platebody needing 99 and five bars, or arrowheads coming out fifteen at a time, are not
 * numbers written here and liable to drift -- they are read at runtime. `SmithingConfigTest`
 * asserts every entry below is present in enum 846, which is Jagex's own statement that the item is
 * smithable at all.
 *
 * Xp is the one exception: it is `bars used * the tier's per-bar xp`, which the cache does not
 * store. Those per-bar figures are the real OSRS ones.
 */
object SmithingProducts {
    /** Bar tiers in ascending order; used to pick the best bar a player is carrying. */
    val tiers: List<Tier> =
        listOf(
            Tier(SmithingObjs.bronze_bar, 1, 12.5),
            Tier(SmithingObjs.iron_bar, 2, 25.0),
            Tier(SmithingObjs.steel_bar, 3, 37.5),
            Tier(SmithingObjs.mithril_bar, 4, 50.0),
            Tier(SmithingObjs.adamantite_bar, 5, 62.5),
            Tier(SmithingObjs.runite_bar, 6, 75.0),
        )

    /**
     * @param barType the value `smithing_bar_type` takes for this tier -- the key enum 1253 maps to
     *   [bar]. It is a 1-based index, not the bar's obj id; `SmithingConfigTest` asserts every one
     *   of these against the cache's own copy of that enum.
     */
    data class Tier(val bar: ObjType, val barType: Int, val xpPerBar: Double)

    /** The tier the interface is currently drawing, or `null` if [barType] is not one of ours. */
    fun tierOf(barType: Int): Tier? = tiers.firstOrNull { it.barType == barType }

    private val products: Map<ObjType, Map<ComponentType, ObjType>> =
        mapOf(
            SmithingObjs.bronze_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.bronze_dagger,
                    SmithingComponents.sword to SmithingProductObjs.bronze_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.bronze_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.bronze_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.bronze_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.bronze_axe,
                    SmithingComponents.mace to SmithingProductObjs.bronze_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.bronze_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.bronze_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.bronze_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.bronze_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.bronze_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.bronze_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.bronze_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.bronze_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.bronze_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.bronze_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.bronze_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.bronze_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.bronze_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.bronze_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails_bronze,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_bronze_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_bronze,
                ),
            SmithingObjs.iron_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.iron_dagger,
                    SmithingComponents.sword to SmithingProductObjs.iron_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.iron_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.iron_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.iron_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.iron_axe,
                    SmithingComponents.mace to SmithingProductObjs.iron_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.iron_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.iron_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.iron_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.iron_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.iron_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.iron_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.iron_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.iron_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.iron_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.iron_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.iron_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.iron_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.iron_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.iron_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails_iron,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_iron_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_iron,
                ),
            SmithingObjs.steel_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.steel_dagger,
                    SmithingComponents.sword to SmithingProductObjs.steel_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.steel_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.steel_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.steel_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.steel_axe,
                    SmithingComponents.mace to SmithingProductObjs.steel_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.steel_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.steel_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.steel_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.steel_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.steel_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.steel_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.steel_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.steel_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.steel_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.steel_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.steel_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.steel_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.steel_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.steel_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_steel_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_steel,
                ),
            SmithingObjs.mithril_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.mithril_dagger,
                    SmithingComponents.sword to SmithingProductObjs.mithril_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.mithril_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.mithril_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.mithril_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.mithril_axe,
                    SmithingComponents.mace to SmithingProductObjs.mithril_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.mithril_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.mithril_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.mithril_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.mithril_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.mithril_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.mithril_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.mithril_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.mithril_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.mithril_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.mithril_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.mithril_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.mithril_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.mithril_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.mithril_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails_mithril,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_mithril_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_mithril,
                ),
            SmithingObjs.adamantite_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.adamant_dagger,
                    SmithingComponents.sword to SmithingProductObjs.adamant_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.adamant_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.adamant_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.adamant_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.adamant_axe,
                    SmithingComponents.mace to SmithingProductObjs.adamant_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.adamnt_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.adamant_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.adamant_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.adamant_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.adamant_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.adamant_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.adamant_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.adamant_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.adamant_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.adamant_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.adamant_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.adamant_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.adamant_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.adamant_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails_adamant,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_adamantite_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_adamantite,
                ),
            SmithingObjs.runite_bar to
                mapOf(
                    SmithingComponents.dagger to SmithingProductObjs.rune_dagger,
                    SmithingComponents.sword to SmithingProductObjs.rune_sword,
                    SmithingComponents.scimitar to SmithingProductObjs.rune_scimitar,
                    SmithingComponents.longsword to SmithingProductObjs.rune_longsword,
                    SmithingComponents.two_hand to SmithingProductObjs.rune_2h_sword,
                    SmithingComponents.axe to SmithingProductObjs.rune_axe,
                    SmithingComponents.mace to SmithingProductObjs.rune_mace,
                    SmithingComponents.warhammer to SmithingProductObjs.rune_warhammer,
                    SmithingComponents.battleaxe to SmithingProductObjs.rune_battleaxe,
                    SmithingComponents.claws to SmithingProductObjs.rune_claws,
                    SmithingComponents.chainbody to SmithingProductObjs.rune_chainbody,
                    SmithingComponents.platelegs to SmithingProductObjs.rune_platelegs,
                    SmithingComponents.plateskirt to SmithingProductObjs.rune_plateskirt,
                    SmithingComponents.platebody to SmithingProductObjs.rune_platebody,
                    SmithingComponents.med_helm to SmithingProductObjs.rune_med_helm,
                    SmithingComponents.full_helm to SmithingProductObjs.rune_full_helm,
                    SmithingComponents.sq_shield to SmithingProductObjs.rune_sq_shield,
                    SmithingComponents.kiteshield to SmithingProductObjs.rune_kiteshield,
                    SmithingComponents.dart_tips to SmithingProductObjs.rune_dart_tip,
                    SmithingComponents.arrowheads to SmithingProductObjs.rune_arrowheads,
                    SmithingComponents.knives to SmithingProductObjs.rune_knife,
                    SmithingComponents.nails to SmithingProductObjs.nails_rune,
                    SmithingComponents.bolts to
                        SmithingProductObjs.xbows_crossbow_bolts_runite_unfeathered,
                    SmithingComponents.limbs to SmithingProductObjs.xbows_crossbow_limbs_runite,
                ),
        )

    /**
     * Keyed by raw id rather than by type.
     *
     * A `find(...)` reference resolves to a `HashedObjType`, while anything handed to us at runtime
     * is an `UnpackedObjType`; the two are different classes and never compare equal, so a map
     * keyed by [ObjType] would silently miss every lookup.
     */
    private val byId: Map<Int, Map<Int, ObjType>> by lazy {
        products.entries.associate { (bar, byComponent) ->
            bar.id to
                byComponent.entries.associate { (component, product) ->
                    component.packed to product
                }
        }
    }

    /** The item [barId] makes when [component] is clicked, or `null` if that slot is not ours. */
    fun find(barId: Int, component: ComponentType): ObjType? = byId[barId]?.get(component.packed)

    /** Every (bar, component, product) triple, for tests and for enabling interface events. */
    fun entries(): Sequence<Triple<ObjType, ComponentType, ObjType>> = sequence {
        for ((bar, byComponent) in products) {
            for ((component, product) in byComponent) {
                yield(Triple(bar, component, product))
            }
        }
    }

    /** The distinct product buttons this module drives. */
    val components: Set<ComponentType> = products.values.flatMapTo(mutableSetOf()) { it.keys }
}
