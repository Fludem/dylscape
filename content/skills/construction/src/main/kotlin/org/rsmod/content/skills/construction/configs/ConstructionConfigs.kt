package org.rsmod.content.skills.construction.configs

import org.rsmod.api.type.refs.loc.LocReferences
import org.rsmod.api.type.refs.obj.ObjReferences
import org.rsmod.api.type.refs.seq.SeqReferences
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.game.type.loc.LocType

/**
 * The house portals scattered around the world. Every one of them carries the same four ops --
 * `Enter`, `Home`, `Build mode`, `Friend's house` -- so the location a portal stands in is the only
 * thing that distinguishes them. Six are the classic estate-agent locations; Kourend and Aldarin
 * were added later and behave identically.
 */
internal object ConstructionLocs : LocReferences() {
    val taverley_portal = find("poh_taverly_portal")
    val rimmington_portal = find("poh_rimmington_portal")
    val pollnivneach_portal = find("poh_pollnivneach_portal")
    val rellekka_portal = find("poh_rellekka_portal")
    val brimhaven_portal = find("poh_brimhaven_portal")
    val yanille_portal = find("poh_yanille_portal")
    val kourend_portal = find("poh_kourend_portal")
    val aldarin_portal = find("poh_aldarin_portal")

    /** The portal that stands in every garden and leads back out of the house. */
    val exit_portal = find("poh_exit_portal")

    val world_portals: List<LocType> =
        listOf(
            taverley_portal,
            rimmington_portal,
            pollnivneach_portal,
            rellekka_portal,
            brimhaven_portal,
            yanille_portal,
            kourend_portal,
            aldarin_portal,
        )
}

internal object ConstructionObjs : ObjReferences() {
    val saw = find("poh_saw")
    val hammer = find("hammer")

    val plank = find("woodplank")
    val oak_plank = find("plank_oak")
    val teak_plank = find("plank_teak")
    val mahogany_plank = find("plank_mahogany")
    val bolt_of_cloth = find("cloth")
    val steel_nails = find("nails")
    val limestone_brick = find("limestonebrick")
    val gold_leaf = find("gold_leaf")
    val marble_block = find("marble_block")
    val magic_stone = find("poh_magic_crystal")
}

internal object ConstructionSeqs : SeqReferences() {
    /**
     * The saw-and-hammer animation, played both while a piece of furniture goes up and while one
     * comes back down. The cache carries no separate removal sequence -- `human_poh_build_floor`
     * and `human_poh_build_wall` are the only other variants, and both are build animations for
     * furniture that sits flat or against a wall.
     */
    val build = find("human_poh_build")
}

internal object ConstructionVarBits : VarBitReferences() {
    /** Set while the player is inside their own house with building mode switched on. */
    val building_mode = find("poh_building_mode")

    /** Which of the twelve wall-and-door themes the house is decorated in. */
    val house_style = find("poh_house_style")

    /** Which of the six world portals the house is reached from. */
    val house_location = find("poh_house_location")
}
