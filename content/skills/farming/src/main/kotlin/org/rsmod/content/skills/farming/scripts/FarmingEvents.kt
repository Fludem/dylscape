package org.rsmod.content.skills.farming.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** One [produce] was picked from a patch. */
data class Harvested(val player: Player, val produce: ObjType) : UnboundEvent
