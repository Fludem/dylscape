package org.rsmod.content.custom.cityshops.configs

import org.rsmod.api.config.Constants
import org.rsmod.api.config.refs.objs
import org.rsmod.api.type.script.dsl.InvPluginBuilder
import org.rsmod.game.type.inv.InvScope
import org.rsmod.game.type.inv.InvStackType

/**
 * A shop that only deals in what it stocks. `autoSize` keeps the interface exactly as long as the
 * stock list, so an unsold slot never shows up as an empty square.
 */
internal fun InvPluginBuilder.specialistShop() {
    scope = InvScope.Shared
    stack = InvStackType.Always
    autoSize = true
    restock = true
}

/**
 * A general store buys anything (`allStock`), so unlike a specialist shop it needs the free slots
 * to put those items in and cannot use `autoSize`.
 */
internal fun InvPluginBuilder.generalStore() {
    scope = InvScope.Shared
    stack = InvStackType.Always
    size = Constants.shop_default_size
    restock = true
    allStock = true
    stock += stock(objs.pot_empty, count = 5, restockCycles = 10)
    stock += stock(objs.jug_empty, count = 2, restockCycles = 100)
    stock += stock(objs.pack_jug_empty, count = 5, restockCycles = 20)
    stock += stock(objs.shears, count = 2, restockCycles = 100)
    stock += stock(objs.knife, count = 5, restockCycles = 100)
    stock += stock(objs.bucket_empty, count = 3, restockCycles = 10)
    stock += stock(objs.pack_bucket, count = 15, restockCycles = 10)
    stock += stock(objs.bowl_empty, count = 2, restockCycles = 50)
    stock += stock(objs.cake_tin, count = 2, restockCycles = 50)
    stock += stock(objs.tinderbox, count = 2, restockCycles = 100)
    stock += stock(objs.chisel, count = 2, restockCycles = 100)
    stock += stock(objs.spade, count = 5, restockCycles = 100)
    stock += stock(objs.hammer, count = 5, restockCycles = 100)
    stock += stock(objs.newcomer_map, count = 5, restockCycles = 100)
    stock += stock(objs.sos_security_book, count = 5, restockCycles = 100)
}
