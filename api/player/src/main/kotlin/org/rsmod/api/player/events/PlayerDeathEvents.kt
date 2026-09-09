package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public class PlayerDeathEvents {
    /**
     * Published during the death sequence, after the death message and before the respawn teleport,
     * so a subscriber still sees the player at the spot they died.
     *
     * This exists because the death sequence itself is a queue handler, and a queue accepts exactly
     * one handler - `EventBus.subscribeSuspend` errors on a duplicate id, so a second
     * `onPlayerQueue(queues.death)` in a content module is a boot failure rather than a second
     * listener. Unbound events do allow multiple subscribers, so death-reactive content hangs off
     * this instead of contending for the queue.
     */
    public data class Death(val player: Player) : UnboundEvent
}
