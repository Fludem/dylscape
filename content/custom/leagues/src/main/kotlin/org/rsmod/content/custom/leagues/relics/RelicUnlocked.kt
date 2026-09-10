package org.rsmod.content.custom.leagues.relics

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

/**
 * Published when a player commits a relic pick. [previous] is the relic it replaced in the same
 * tier, or `null` for a tier's first pick. Relics with a running effect (a timer, say) start it
 * from here rather than waiting for the next login.
 */
data class RelicUnlocked(val player: Player, val relic: Relic, val previous: Relic?) : UnboundEvent
