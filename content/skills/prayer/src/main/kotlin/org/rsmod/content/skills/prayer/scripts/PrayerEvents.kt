package org.rsmod.content.skills.prayer.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** [bones] were buried (or ashes scattered). */
data class BuriedBones(val player: Player, val bones: ObjType) : UnboundEvent

/** [bones] were offered at an altar, whether or not the altar handed them back. */
data class OfferedBones(val player: Player, val bones: ObjType) : UnboundEvent
