@file:Suppress("SpellCheckingInspection")

package org.rsmod.content.skills.slayer.configs

import org.rsmod.api.config.refs.objs
import org.rsmod.game.type.obj.ObjType

/**
 * The protective equipment a handful of tasks demand, keyed by `slayer_task:id`.
 *
 * This is the one part of Slayer that is authored rather than read out of the cache, and only
 * because it has to be: `slayer_task:equipment_required` is declared in `dbcol.sym` but **table 113
 * defines no type for it in this rev**, so the column carries nothing. Everything else here - the
 * tasks, the weights, the amounts, the level requirements, the npc mapping - comes from the cache.
 *
 * Slayer helmets are not listed. They are folded in at start-up by sweeping the cache for objs
 * carrying the `slayer_helm` or `slayer_helm_imbued` param, which catches every recoloured and
 * imbued variant without naming a dozen ids here. [helmSubstitutes] marks the entries a helmet
 * actually covers: it is built from the head-slot pieces, so it does not cover the mirror shield or
 * the witchwood icon, and neither does a real slayer helmet.
 */
object SlayerGear {
    data class Requirement(
        val objs: List<ObjType>,
        val message: String,
        val helmSubstitutes: Boolean,
    )

    val byTaskId: Map<Int, Requirement> =
        mapOf(
            BANSHEES to
                Requirement(
                    listOf(objs.earmuffs),
                    "The screams of the banshees would be deafening without ear protection.",
                    helmSubstitutes = true,
                ),
            ABERRANT_SPECTRES to
                Requirement(
                    listOf(objs.nose_peg),
                    "The stench of the spectres would overwhelm you without a nose peg.",
                    helmSubstitutes = true,
                ),
            DUST_DEVILS to
                Requirement(
                    listOf(objs.facemask),
                    "The dust would clog your lungs without a face mask.",
                    helmSubstitutes = true,
                ),
            SMOKE_DEVILS to
                Requirement(
                    listOf(objs.facemask),
                    "The smoke would clog your lungs without a face mask.",
                    helmSubstitutes = true,
                ),
            WALL_BEASTS to
                Requirement(
                    listOf(objs.spiny_helmet),
                    "You need a spiny helmet to fight a wall beast safely.",
                    helmSubstitutes = true,
                ),
            BASILISKS to
                Requirement(
                    listOf(slayer_objs.mirror_shield),
                    "You need a mirror shield to fight a basilisk safely.",
                    // A slayer helmet is head-slot; it is no substitute for a shield, in OSRS or
                    // here.
                    helmSubstitutes = false,
                ),
            CAVE_HORRORS to
                Requirement(
                    listOf(slayer_objs.witchwood_icon),
                    "You need a witchwood icon to withstand the cave horror's screech.",
                    helmSubstitutes = false,
                ),
        )

    private const val BANSHEES = 38
    private const val ABERRANT_SPECTRES = 41
    private const val BASILISKS = 43
    private const val DUST_DEVILS = 49
    private const val WALL_BEASTS = 61
    private const val CAVE_HORRORS = 80
    private const val SMOKE_DEVILS = 95
}
