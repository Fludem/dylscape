package org.rsmod.content.skills.hunter.configs

import org.rsmod.api.type.builders.enums.EnumBuilder
import org.rsmod.api.type.refs.enums.EnumReferences
import org.rsmod.game.type.loc.LocType

/** Key order for the box-trap direction enums, matching `Direction`'s cardinals. */
internal const val NORTH: Int = 0
internal const val EAST: Int = 1
internal const val SOUTH: Int = 2
internal const val WEST: Int = 3

/**
 * Box-trap catching states, keyed by the compass direction the creature walked in from.
 *
 * Only box traps need this: their catching model shows the creature entering from one specific
 * side, so the loc is picked from where the npc was standing when it reached the trap. The other
 * three methods have a single catching state each and use `HunterParams.loc_trapping` instead.
 *
 * Key order is fixed at 0=north, 1=east, 2=south, 3=west, matching `Direction`'s cardinal ordinals.
 */
internal object HunterEnums : EnumReferences() {
    val box_dirs_ferret = find<Int, LocType>("hunter_boxtrap_dirs_ferret")
    val box_dirs_chinchompa = find<Int, LocType>("hunter_boxtrap_dirs_chinchompa")
    val box_dirs_chinchompa_big = find<Int, LocType>("hunter_boxtrap_dirs_chinchompa_big")
    val box_dirs_chinchompa_black = find<Int, LocType>("hunter_boxtrap_dirs_chinchompa_black")
}

internal object HunterEnumBuilder : EnumBuilder() {
    init {
        build<Int, LocType>("hunter_boxtrap_dirs_ferret") {
            this[NORTH] = HunterLocs.box_trapping_ferret_n
            this[EAST] = HunterLocs.box_trapping_ferret_e
            this[SOUTH] = HunterLocs.box_trapping_ferret_s
            this[WEST] = HunterLocs.box_trapping_ferret_w
        }
        build<Int, LocType>("hunter_boxtrap_dirs_chinchompa") {
            this[NORTH] = HunterLocs.box_trapping_chin_n
            this[EAST] = HunterLocs.box_trapping_chin_e
            this[SOUTH] = HunterLocs.box_trapping_chin_s
            this[WEST] = HunterLocs.box_trapping_chin_w
        }
        build<Int, LocType>("hunter_boxtrap_dirs_chinchompa_big") {
            this[NORTH] = HunterLocs.box_trapping_chin_big_n
            this[EAST] = HunterLocs.box_trapping_chin_big_e
            this[SOUTH] = HunterLocs.box_trapping_chin_big_s
            this[WEST] = HunterLocs.box_trapping_chin_big_w
        }
        build<Int, LocType>("hunter_boxtrap_dirs_chinchompa_black") {
            this[NORTH] = HunterLocs.box_trapping_chin_black_n
            this[EAST] = HunterLocs.box_trapping_chin_black_e
            this[SOUTH] = HunterLocs.box_trapping_chin_black_s
            this[WEST] = HunterLocs.box_trapping_chin_black_w
        }
    }
}
