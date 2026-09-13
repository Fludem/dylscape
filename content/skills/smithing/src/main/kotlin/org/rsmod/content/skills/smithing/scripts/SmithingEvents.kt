package org.rsmod.content.skills.smithing.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/**
 * [count] of [product] came off the anvil in one strike. Published for anything counting output.
 */
data class Smithed(val player: Player, val product: ObjType, val count: Int) : UnboundEvent

/** One [bar] came out of the furnace; failed iron pays nothing and publishes nothing. */
data class Smelted(val player: Player, val bar: ObjType) : UnboundEvent
