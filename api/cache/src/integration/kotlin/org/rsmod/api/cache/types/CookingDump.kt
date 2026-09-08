package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints everything the cooking module needs to decode from the cache rather than assume: which
 * locs carry a `Cook` op, the ops on the handful of locs cooking must bind regardless, the
 * make-menu's verb table, and the display names behind the anonymous `burntfishN` symbols.
 */
class CookingDump {
    @Test
    fun GameTestState.`dump cooking locs, verbs and objs`() = runBasicGameTest {
        val cookLocs =
            cacheTypes.locs.values
                .filter { loc -> loc.op.any { it?.contains("cook", ignoreCase = true) == true } }
                .sortedBy { it.id }
        for (loc in cookLocs) {
            println(
                "COOKLOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "ops=${loc.op.toList()}"
            )
        }
        println("COOKLOC_TOTAL=${cookLocs.size}")

        val named =
            listOf(
                "fire",
                "fire_cook",
                "range",
                "newbierange",
                "fireplacecookingpot",
                "range_noop",
                "stove_clay01_talkasti01",
                "carnilleanrange",
                "fai_varrock_range",
                "rimmington_poor_range",
                "poh_stove_1",
            )
        for (name in named) {
            val loc = cacheTypes.locs.values.firstOrNull { it.internalName == name }
            println("NAMEDLOC $name -> id=${loc?.id} name='${loc?.name}' ops=${loc?.op?.toList()}")
        }

        for ((id, purpose) in
            listOf(
                1809 to "skillmulti verb",
                5178 to "no quantity buttons",
                1810 to "no X button",
            )) {
            val enum = cacheTypes.enums[id] ?: continue
            println("ENUM $id ($purpose)")
            enum.primitiveMap.toSortedMap(compareBy { it as Int }).forEach { (k, v) ->
                println("    ENUMROW $id $k -> $v")
            }
        }

        val objNames =
            listOf(
                "burntfish1",
                "burntfish2",
                "burntfish3",
                "burntfish4",
                "burntfish5",
                "burnt_shrimp",
                "burnt_swordfish",
                "burnt_lobster",
                "burnt_shark",
                "burnt_bread",
                "burnt_meat",
                "burnt_chicken",
                "burnt_pitta_bread",
                "newbieshrimp",
                "newbieraw_shrimp",
                "newbie_pot_flour",
                "pot_flour",
                "flour",
                "bread_dough",
                "pastry_dough",
                "pizza_base",
                "pitta_dough",
                "pitta_bread",
                "raw_beef",
                "raw_rat_meat",
                "raw_bear_meat",
                "cooked_meat",
                "raw_chicken",
                "cooked_chicken",
                "shrimp",
                "raw_shrimp",
                "bucket_water",
                "jug_water",
                "bowl_water",
                "bucket_empty",
                "jug_empty",
                "bowl_empty",
                "pot_empty",
            )
        for (name in objNames) {
            val obj = cacheTypes.objs.values.firstOrNull { it.internalName == name }
            println("OBJ $name -> id=${obj?.id} name='${obj?.name}' ops=${obj?.iop?.toList()}")
        }
    }
}
