package org.rsmod.api.npc.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player

public class NpcDeathEvents {
    /**
     * Published at the start of the death sequence, before the npc is walked, has its ops hidden
     * and is despawned, so a subscriber still sees it alive on the tile it died on.
     *
     * This exists for the same reason `PlayerDeathEvents.Death` does: the death sequence is a queue
     * handler, and a queue accepts exactly one handler - `EventBus.subscribeSuspend` errors on a
     * duplicate id. Npc queues are worse than the player case, because they resolve `type ->
     * content group -> default` and stop at the first match, and `DropTableScript` already claims a
     * type-specific `queues.death` for nearly every npc that has a generated table. So a second
     * `onNpcQueue(type, queues.death)` is not an override, it is a boot failure. Unbound events do
     * allow multiple subscribers, so kill-reactive content - slayer task credit, and anything
     * counting kills later - hangs off this instead of contending for the queue.
     *
     * [killer] is the npc's hero: the player owed the kill by the engine's damage tracking. It is
     * null when nobody is - an npc killed by another npc, by a script, or by an admin command.
     */
    public data class Killed(val npc: Npc, val killer: Player?) : UnboundEvent
}
