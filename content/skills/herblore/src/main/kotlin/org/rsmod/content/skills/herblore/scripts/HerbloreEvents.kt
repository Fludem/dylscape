package org.rsmod.content.skills.herblore.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** A finished potion ([product]) was mixed; unfinished potions publish nothing. */
data class MixedPotion(val player: Player, val product: ObjType) : UnboundEvent

/** [count] grimy herbs became [herb] in one click (more than one under Clean-all). */
data class CleanedHerb(val player: Player, val herb: ObjType, val count: Int) : UnboundEvent
