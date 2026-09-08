package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class LeagueRelicsDump {
    private val relicNames =
        listOf(
            "Endless Harvest",
            "Bank Heist",
            "Flow State",
            "Conniving Clues",
            "Soul Harvest",
            "Flask of Fervour",
        )

    @Test
    fun GameTestState.`dump league interfaces`() = runBasicGameTest {
        val matches =
            cacheTypes.interfaces.values
                .filter { it.internalName?.contains("league", ignoreCase = true) == true }
                .sortedBy { it.id }
        for (interf in matches) {
            println(
                "INTERFACE ${interf.id} sym=${interf.internalName} components=${interf.components.size}"
            )
        }
        println("TOTAL_INTERFACES=${matches.size}")
    }

    @Test
    fun GameTestState.`find enums holding relic names`() = runBasicGameTest {
        var hits = 0
        for (enum in cacheTypes.enums.values.sortedBy { it.id }) {
            val strings = enum.primitiveMap.values.filterIsInstance<String>()
            if (strings.isEmpty()) continue
            val matched = relicNames.filter { needle -> strings.any { it == needle } }
            if (matched.isEmpty()) continue
            hits++
            println(
                "ENUM ${enum.id} sym=${enum.internalName} key=${enum.keyLiteral} " +
                    "val=${enum.valLiteral} size=${enum.primitiveMap.size} transmit=${enum.transmit} matched=$matched"
            )
            for ((k, v) in enum.primitiveMap.entries.sortedBy { it.key.toString() }) {
                println("  $k -> $v")
            }
        }
        println("TOTAL_ENUM_HITS=$hits")
    }

    @Test
    fun GameTestState.`dump all relic structs`() = runBasicGameTest {
        val matches =
            cacheTypes.structs.values
                .filter { struct ->
                    val params = struct.paramMap?.primitiveMap ?: return@filter false
                    params.containsKey(879) && params.containsKey(880) && params.containsKey(885)
                }
                .sortedBy { it.id }
        for (struct in matches) {
            val params = struct.paramMap!!.primitiveMap
            println(
                "RELIC struct=${struct.id} tier=${params[885]} name='${params[879]}' " +
                    "paramKeys=${params.keys.sorted()}"
            )
        }
        println("TOTAL_RELIC_STRUCTS=${matches.size}")
    }

    @Test
    fun GameTestState.`dump all league vars`() = runBasicGameTest {
        val varps =
            cacheTypes.varps.values
                .filter { it.internalName?.contains("league", ignoreCase = true) == true }
                .sortedBy { it.id }
        for (varp in varps) {
            println("VARP ${varp.id} sym=${varp.internalName} transmit=${varp.transmit}")
        }
        println("TOTAL_LEAGUE_VARPS=${varps.size}")

        val leagueVarpIds = varps.map { it.id }.toSet()
        val varbits =
            cacheTypes.varbits.values
                .filter {
                    it.varpId in leagueVarpIds ||
                        it.internalName?.contains("league", ignoreCase = true) == true
                }
                .sortedWith(compareBy({ it.varpId }, { it.bits.first }))
        for (varbit in varbits) {
            val bits = varbit.bits
            println(
                "VARBIT ${varbit.id} sym=${varbit.internalName} varp=${varbit.varpId} " +
                    "bits=${bits.first}..${bits.last} max=${(1L shl (bits.last - bits.first + 1)) - 1}"
            )
        }
        println("TOTAL_LEAGUE_VARBITS=${varbits.size}")
    }

    @Test
    fun GameTestState.`dump league_relics onload hooks`() = runBasicGameTest {
        val comps =
            cacheTypes.components.values.filter { it.interfaceId == 655 }.sortedBy { it.component }
        var withOnLoad = 0
        for (comp in comps) {
            val onLoad = comp.onLoad ?: continue
            withOnLoad++
            println(
                "ONLOAD 655:${comp.component} sym=${comp.internalName} " +
                    "script=${onLoad.contentToString()}"
            )
        }
        println("COMPONENTS=${comps.size} WITH_ONLOAD=$withOnLoad")
    }

    @Test
    fun GameTestState.`resolve spike component refs`() = runBasicGameTest {
        val names =
            listOf(
                "league_relics:close_button",
                "league_relics:clickzones",
                "league_relics:select_button",
                "league_relics:select_back",
                "league_relics:confirm_button",
                "league_relics:confirm_cancel",
                "emote:emote_list",
            )
        for (name in names) {
            val match = cacheTypes.components.values.firstOrNull { it.internalName == name }
            if (match == null) {
                println("COMP $name -> UNRESOLVED")
                continue
            }
            println(
                "COMP $name -> interfaceId=${match.interfaceId} component=${match.component} " +
                    "packed=${match.packed} type=${match.type} buttonType=${match.buttonType}"
            )
        }
    }

    @Test
    fun GameTestState.`dump relic varbit widths`() = runBasicGameTest {
        val matches =
            cacheTypes.varbits.values
                .filter { it.internalName?.startsWith("league_relic") == true }
                .sortedBy { it.id }
        for (varbit in matches) {
            val bits = varbit.bits
            val width = bits.last - bits.first + 1
            val max = (1L shl width) - 1
            println(
                "VARBIT ${varbit.id} sym=${varbit.internalName} varp=${varbit.varpId} " +
                    "bits=${bits.first}..${bits.last} width=$width maxValue=$max"
            )
        }
        println("TOTAL_RELIC_VARBITS=${matches.size}")
    }

    @Test
    fun GameTestState.`find enums referencing relic structs`() = runBasicGameTest {
        val relicStructIds =
            cacheTypes.structs.values
                .filter { struct ->
                    val params = struct.paramMap?.primitiveMap ?: return@filter false
                    params.containsKey(879) && params.containsKey(880) && params.containsKey(885)
                }
                .map { it.id }
                .toSet()
        var hits = 0
        for (enum in cacheTypes.enums.values.sortedBy { it.id }) {
            val ints = enum.primitiveMap.values.filterIsInstance<Int>().toSet()
            val overlap = ints intersect relicStructIds
            if (overlap.size < 3) continue
            hits++
            println(
                "ENUM ${enum.id} sym=${enum.internalName} key=${enum.keyLiteral} val=${enum.valLiteral} " +
                    "size=${enum.primitiveMap.size} transmit=${enum.transmit} relicRefs=${overlap.size}"
            )
            for ((k, v) in enum.primitiveMap.entries.sortedBy { it.key.toString() }) {
                println("  $k -> $v")
            }
        }
        println("TOTAL_RELIC_ENUMS=$hits")
    }

    @Test
    fun GameTestState.`find structs holding relic names`() = runBasicGameTest {
        var hits = 0
        for (struct in cacheTypes.structs.values.sortedBy { it.id }) {
            val params = struct.paramMap ?: continue
            val strings = params.primitiveMap.values.filterIsInstance<String>()
            val matched = relicNames.filter { needle -> strings.any { it == needle } }
            if (matched.isEmpty()) continue
            hits++
            println("STRUCT ${struct.id} sym=${struct.internalName} matched=$matched")
            for ((k, v) in params.primitiveMap) {
                println("  param $k -> $v")
            }
        }
        println("TOTAL_STRUCT_HITS=$hits")
    }
}
