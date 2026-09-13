package org.rsmod.content.skills.firemaking.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType

/** A fire caught from [logs]; a tile that could not take the fire publishes nothing. */
data class LitFire(val player: Player, val logs: ObjType) : UnboundEvent
