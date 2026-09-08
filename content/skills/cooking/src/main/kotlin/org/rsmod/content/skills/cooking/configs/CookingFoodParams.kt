package org.rsmod.content.skills.cooking.configs

import org.rsmod.api.config.objParam
import org.rsmod.api.config.objXpParam
import org.rsmod.api.config.refs.params
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.UnpackedObjType

/** Typed views over the params [CookingFoodEditor] stamps onto every raw food. */
val UnpackedObjType.cookingLevel: Int by objParam(params.levelrequire)
val UnpackedObjType.cookingXp: Double by objXpParam(params.skill_xp)
val UnpackedObjType.cookedProduct: ObjType by objParam(params.skill_productitem)
val UnpackedObjType.burntProduct: ObjType by objParam(CookingParams.burnt)
val UnpackedObjType.stopBurnFire: Int by objParam(CookingParams.stop_burn_fire)
val UnpackedObjType.stopBurnRange: Int by objParam(CookingParams.stop_burn_range)
val UnpackedObjType.rangeOnly: Boolean by objParam(CookingParams.range_only)
val UnpackedObjType.cookingName: String by objParam(CookingParams.name)
