package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

public class PlayerHitEvents {
    /**
     * Published by `StandardPlayerHitProcessor` once a hit has taken its hitpoints, before any
     * death is queued, so a subscriber sees the damage that actually landed.
     *
     * This exists for the same reason as [PlayerDeathEvents.Death]: the hit itself is delivered
     * through a queue with exactly one handler, so anything that reacts to being hit (Vengeance,
     * recoil effects) has to hang off an unbound event rather than contend for that queue.
     */
    public data class Impact(val player: Player, val hit: Hit) : UnboundEvent
}
