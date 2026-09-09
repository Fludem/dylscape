package org.rsmod.content.skills.agility.shop

import org.rsmod.content.skills.agility.configs.AgilityObjs

/**
 * What each line in Grace's shop costs in marks of grace.
 *
 * These are fixed. Every other shop in the game prices off `objType.cost` and slides that price
 * with stock, but Grace does not haggle: a graceful hood is 35 marks whether she has one left or a
 * hundred. So this table is the price, and `StandardGpCostCalculations` is deliberately unused.
 *
 * **Keyed on the raw `Int` obj id.** `ObjReferences.find` hands back a `HashedObjType` while the
 * runtime gives shop code an `UnpackedObjType`, and the two never compare equal - a map keyed on
 * `ObjType` would build fine, boot fine, and price nothing.
 *
 * Prices are OSRS's own and sum to the published 260 marks for the full set.
 */
public object GracefulPrices {
    private val byObjId: Map<Int, Int> =
        mapOf(
            AgilityObjs.graceful_hood.id to 35,
            AgilityObjs.graceful_top.id to 55,
            AgilityObjs.graceful_legs.id to 60,
            AgilityObjs.graceful_gloves.id to 30,
            AgilityObjs.graceful_boots.id to 40,
            AgilityObjs.graceful_cape.id to 40,
            AgilityObjs.amylase_pack.id to 10,
        )

    /** The full graceful set, for the test that pins this table against the published total. */
    public const val FULL_SET_COST: Int = 260

    public operator fun get(objId: Int): Int? = byObjId[objId]

    public val objIds: Set<Int>
        get() = byObjId.keys
}
