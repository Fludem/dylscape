package org.rsmod.content.skills.crafting.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/**
 * [count] of [product] was crafted: leather stitched, wool spun, jewellery cast or a gem cut. A
 * crushed gem pays nothing and publishes nothing.
 */
data class Crafted(val player: Player, val product: ObjType, val count: Int) : UnboundEvent
