package org.rsmod.content.skills.fletching.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/**
 * [count] of [product] was fletched: a bow cut from logs, a bow strung, a crossbow assembled, or a
 * batch of arrows tipped.
 */
data class Fletched(val player: Player, val product: ObjType, val count: Int) : UnboundEvent
