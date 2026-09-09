@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.agility.configs

import org.rsmod.api.type.editors.inv.InvEditor
import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.currency.CurrencyReferences
import org.rsmod.api.type.refs.inv.InvReferences
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

/**
 * Marks of grace as a shop currency.
 *
 * A `CurrencyType` is a **name-only server type**: it resolves through `NameTypeReferenceResolver`
 * against `.data/symbols/.local/currency.sym` and has no cache encoder at all, so unlike the npc
 * spawn below this needs no `packCache`. The id is not the obj id - the obj a player actually hands
 * over is [AgilityObjs.mark_of_grace], and the link between the two lives in
 * [org.rsmod.content.skills.agility.shop.MarkOfGraceShopOperations].
 */
public object AgilityCurrencies : CurrencyReferences() {
    val mark_of_grace = find("mark_of_grace")
}

/** Grace's shop inv. The cache names it after the Rogues' Den, which is where she stands. */
public object AgilityInvs : InvReferences() {
    val roguesden_shop = find("roguesden_shop")
}

public object AgilityNpcs : NpcReferences() {
    val grace = find("rooftops_grace")
}

/**
 * Stocks Grace's shop.
 *
 * The cache names the inv and gives it a size, but stock is an RSMod extension vanilla never
 * writes, so without this the shop opens empty. `restock = true` with a large `restockCycles` is
 * the shape every other shop here uses; the counts are OSRS's own (100 of each graceful piece,
 * 1,000 amylase packs), which in practice means it never runs dry.
 *
 * `InvScope.Shared` is required by `Shops.open` for a shop this size, and it is also correct: one
 * global stock rather than a copy per player.
 */
internal object GraceShopInv : InvEditor() {
    init {
        edit(AgilityInvs.roguesden_shop) {
            scope = InvScope.Shared
            stack = InvStackType.Always
            autoSize = true
            restock = true
            stock += stock(AgilityObjs.graceful_hood, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.graceful_top, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.graceful_legs, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.graceful_gloves, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.graceful_boots, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.graceful_cape, count = 100, restockCycles = 100)
            stock += stock(AgilityObjs.amylase_pack, count = 1000, restockCycles = 100)
        }
    }
}

/**
 * Pins Grace to her spawn tile.
 *
 * Out of the cache she carries the default wander range of 5, which would walk her away from the
 * bank chest she stands beside. `wanderRange = 0` makes the wander processor skip her entirely.
 * This is a type edit, applied by the boot-time config sync rather than `packCache`.
 */
internal object GraceNpcEditor : NpcEditor() {
    init {
        edit(AgilityNpcs.grace) { wanderRange = 0 }
    }
}
