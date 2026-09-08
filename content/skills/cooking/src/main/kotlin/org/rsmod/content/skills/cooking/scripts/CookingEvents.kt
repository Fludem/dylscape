package org.rsmod.content.skills.cooking.scripts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * One item finished cooking, well or badly. Published for anything that needs to react to cooking
 * without being cooking -- Tutorial Island's survival section is the first customer.
 */
data class CookedFood(
    val player: Player,
    val raw: UnpackedObjType,
    val product: ObjType,
    val burnt: Boolean,
) : UnboundEvent

/** Flour and water became dough. */
data class MixedDough(val player: Player, val dough: ObjType) : UnboundEvent
