package org.rsmod.content.other.consumables.configs

import org.rsmod.api.type.refs.queue.QueueReferences

/**
 * Server-only queue types this module owns.
 *
 * Queues are name-only types with no cache encoder, so these need no `packCache` - but `find` will
 * not mint one either, so the name has to exist in `.data/symbols/.local/queue.sym` first.
 */
internal object ConsumableQueues : QueueReferences() {
    /** The Varlamore hunter meats' second helping, a few ticks after the first. */
    val delayed_heal = find("consumable_delayed_heal")
}
