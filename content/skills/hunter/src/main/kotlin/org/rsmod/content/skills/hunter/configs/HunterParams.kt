package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.builders.param.ParamBuilder
import org.rsmod.api.type.refs.param.ParamReferences
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.loc.LocType
import org.rsmod.game.type.obj.ObjType

/**
 * Per-creature trapping data, hung off the creature's npc type so the scripts stay generic.
 *
 * Upstream's `params.levelrequire`, `params.skill_xp` and `params.skill_productitem` are reused
 * rather than duplicated; these are the pieces hunter needs that no existing param covers.
 *
 * A creature carries either [loc_trapping] (one loc, for snares, nets and deadfalls) or
 * [loc_trapping_dirs] (an enum of four, for box traps, whose model shows which side the creature
 * came in from). Never both.
 */
object HunterParams : ParamReferences() {
    val trap_kind = find<Int>("hunter_trap_kind")
    val rate_low = find<Int>("hunter_rate_low")
    val rate_high = find<Int>("hunter_rate_high")
    val creature_respawn = find<Int>("hunter_creature_respawn")
    val loc_trapping = find<LocType>("hunter_loc_trapping")
    val loc_trapping_dirs = find<EnumType<Int, LocType>>("hunter_loc_trapping_dirs")
    val loc_full = find<LocType>("hunter_loc_full")
    val secondary_product = find<ObjType>("hunter_secondary_product")
    val secondary_count = find<Int>("hunter_secondary_count")
    val trap_return = find<ObjType>("hunter_trap_return")
}

internal object HunterParamBuilder : ParamBuilder() {
    init {
        build<Int>("hunter_trap_kind") { default = -1 }
        build<Int>("hunter_rate_low") { default = 0 }
        build<Int>("hunter_rate_high") { default = 0 }
        build<Int>("hunter_creature_respawn") { default = 50 }
        build<LocType>("hunter_loc_trapping")
        build<EnumType<Int, LocType>>("hunter_loc_trapping_dirs")
        build<LocType>("hunter_loc_full")
        build<ObjType>("hunter_secondary_product")
        build<Int>("hunter_secondary_count") { default = 0 }
        build<ObjType>("hunter_trap_return")
    }
}
