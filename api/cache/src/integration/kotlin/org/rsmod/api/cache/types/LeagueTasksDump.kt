package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints everything the vanilla `league_tasks` screen (657) needs from the server, so a task list
 * of our own can be drawn on it without guessing.
 *
 * The task structs and the enums that list them carry no symbol names upstream, so the chain is
 * walked from the one named root: `enum 2670[league_type]` gives the league struct, and its params
 * lead to the task enum and from there to each task struct. The clientscripts then say which params
 * the client reads (name, description, tier, completion varbit, ...) and what arguments the draw
 * scripts expect.
 *
 * Gradle swallows stdout; read `system-out` from `api/cache/build/test-results/integration/`.
 */
class LeagueTasksDump {
    @Test
    fun GameTestState.`dump league struct and its enums`() = runBasicGameTest {
        val leagueEnum = cacheTypes.enums[LEAGUE_TYPE_ENUM]
        println(
            "ENUM $LEAGUE_TYPE_ENUM key=${leagueEnum?.keyLiteral} val=${leagueEnum?.valLiteral}"
        )
        for ((k, v) in leagueEnum?.primitiveMap?.entries.orEmpty().sortedBy { it.key.toString() }) {
            println("  $k -> $v")
        }
        val leagueStructId =
            leagueEnum?.primitiveMap?.get(LEAGUE_TYPE) as? Int ?: return@runBasicGameTest
        val leagueStruct = cacheTypes.structs[leagueStructId] ?: return@runBasicGameTest
        println(
            "=== LEAGUE STRUCT $leagueStructId params=${leagueStruct.paramMap?.primitiveMap?.size}"
        )
        for ((param, value) in
            leagueStruct.paramMap?.primitiveMap.orEmpty().entries.sortedBy { it.key }) {
            val paramType = cacheTypes.params[param]
            println(
                "  param $param sym=${paramType?.internalName} type=${paramType?.typeLiteral} -> $value"
            )
            val enum = (value as? Int)?.let { cacheTypes.enums[it] } ?: continue
            println(
                "    ENUM ${enum.id} key=${enum.keyLiteral} val=${enum.valLiteral} " +
                    "size=${enum.primitiveMap.size} transmit=${enum.transmit} default=${enum.defaultInt}"
            )
            for ((k, v) in enum.primitiveMap.entries.sortedBy { (it.key as? Int) ?: 0 }.take(6)) {
                println("      $k -> $v")
            }
        }
    }

    @Test
    fun GameTestState.`dump task structs`() = runBasicGameTest {
        val taskEnum = findTaskEnum() ?: return@runBasicGameTest println("TASK ENUM NOT FOUND")
        println(
            "TASK ENUM ${taskEnum.id} key=${taskEnum.keyLiteral} val=${taskEnum.valLiteral} " +
                "size=${taskEnum.primitiveMap.size} transmit=${taskEnum.transmit}"
        )
        val entries = taskEnum.primitiveMap.entries.sortedBy { (it.key as? Int) ?: 0 }
        println(
            "  first keys=${entries.take(5).map { it.key }} last keys=${entries.takeLast(3).map { it.key }}"
        )

        val structIds = entries.mapNotNull { it.value as? Int }
        val paramKeys = mutableMapOf<Int, Int>()
        for (structId in structIds) {
            val params = cacheTypes.structs[structId]?.paramMap?.primitiveMap ?: continue
            for (key in params.keys) paramKeys[key] = (paramKeys[key] ?: 0) + 1
        }
        println("PARAM KEY FREQUENCY over ${structIds.size} task structs:")
        for ((key, count) in paramKeys.entries.sortedBy { it.key }) {
            val paramType = cacheTypes.params[key]
            println(
                "  param $key sym=${paramType?.internalName} type=${paramType?.typeLiteral} count=$count"
            )
        }

        for (structId in structIds.take(12)) {
            dumpStruct(structId, "TASK")
        }
        val direWolf =
            structIds.firstOrNull { id ->
                cacheTypes.structs[id]?.paramMap?.primitiveMap?.values?.any {
                    it is String && it.contains("Dire Wolf", ignoreCase = true)
                } == true
            }
        println("DIRE WOLF struct=$direWolf")
        direWolf?.let { dumpStruct(it, "TASK") }

        // Distinct values of the small int params, to tell tier/type/area apart.
        for ((key, _) in paramKeys.entries.sortedBy { it.key }) {
            val values =
                structIds.mapNotNull { cacheTypes.structs[it]?.paramMap?.primitiveMap?.get(key) }
            val ints = values.filterIsInstance<Int>()
            if (ints.isEmpty()) continue
            val distinct = ints.toSortedSet()
            if (distinct.size <= 40) {
                println("PARAM $key distinct=${distinct}")
                val histogram = ints.groupingBy { it }.eachCount().toSortedMap()
                println("  histogram=$histogram")
            } else {
                println(
                    "PARAM $key distinct=${distinct.size} min=${distinct.first()} max=${distinct.last()}"
                )
            }
        }

        for (tierStruct in TIER_STRUCTS) {
            dumpStruct(tierStruct, "TIER")
        }
    }

    @Test
    fun GameTestState.`dump task scripts`() = runBasicGameTest {
        val taskEnum = findTaskEnum()
        val taskStructIds =
            taskEnum?.primitiveMap?.values?.filterIsInstance<Int>()?.toSet().orEmpty()
        for (id in TASK_SCRIPTS) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} intLocals=${script.intLocalCount} " +
                    "strLocals=${script.stringLocalCount} ops=${script.commands.size}"
            )
            for (i in script.commands.indices) {
                val command = script.commands[i]
                val operand = script.intOperands[i]
                val str = script.stringOperands[i]
                val note =
                    when {
                        str != null -> "str=\"$str\""
                        command == PUSH_VARBIT || command == POP_VARBIT ->
                            "op=$operand ; VARBIT ${cacheTypes.varbits[operand]?.internalName ?: "?"}"
                        command == PUSH_VARP || command == POP_VARP ->
                            "op=$operand ; VARP ${cacheTypes.varps[operand]?.internalName ?: "?"}"
                        command == PUSH_INT -> {
                            val notes = mutableListOf<String>()
                            cacheTypes.params[operand]?.let {
                                notes += "param?${it.internalName ?: ""}"
                            }
                            if (operand == taskEnum?.id) notes += "TASK_ENUM"
                            cacheTypes.enums[operand]?.let { notes += "enum?" }
                            if (operand in taskStructIds) notes += "taskstruct?"
                            cacheTypes.components[operand]?.let {
                                notes += "comp?${it.internalName}"
                            }
                            if (notes.isEmpty()) "op=$operand"
                            else "op=$operand ; ${notes.joinToString(" ")}"
                        }
                        else -> "op=$operand"
                    }
                println("  $i: cmd=$command $note")
            }
            if (script.switches.isNotEmpty()) {
                println("  switches=${script.switches.map { it.entries.sortedBy { e -> e.key } }}")
            }
        }
    }

    @Test
    fun GameTestState.`dump every hook on the task and side panel interfaces`() = runBasicGameTest {
        for (interfaceId in listOf(657, 656)) {
            val comps =
                cacheTypes.components.values
                    .filter { it.interfaceId == interfaceId }
                    .sortedBy { it.component }
            println("=== INTERFACE $interfaceId components=${comps.size}")
            for (comp in comps) {
                val hooks =
                    listOf(
                        "onLoad" to comp.onLoad,
                        "onMouseOver" to comp.onMouseOver,
                        "onMouseLeave" to comp.onMouseLeave,
                        "onVarTransmit" to comp.onVarTransmit,
                        "onInvTransmit" to comp.onInvTransmit,
                        "onStatTransmit" to comp.onStatTransmit,
                        "onTimer" to comp.onTimer,
                        "onOp" to comp.onOp,
                        "onMouseRepeat" to comp.onMouseRepeat,
                        "onClick" to comp.onClick,
                        "onClickRepeat" to comp.onClickRepeat,
                        "onRelease" to comp.onRelease,
                        "onHold" to comp.onHold,
                        "onDrag" to comp.onDrag,
                        "onDragComplete" to comp.onDragComplete,
                        "onScrollWheel" to comp.onScrollWheel,
                    )
                val notes = mutableListOf<String>()
                for ((label, hook) in hooks) {
                    if (hook == null) continue
                    notes += "$label=${hook.contentToString()}"
                }
                comp.onVarTransmitList?.let { notes += "varTransmitList=${it.contentToString()}" }
                if (comp.events != 0) notes += "events=0x%x".format(comp.events)
                if (comp.hide) notes += "hidden"
                notes += "type=${comp.type} buttonType=${comp.buttonType}"
                println(
                    "  $interfaceId:${comp.component} ${comp.internalName}: ${notes.joinToString(" ")}"
                )
            }
        }
    }

    @Test
    fun GameTestState.`dump completion varbits`() = runBasicGameTest {
        val completionVarps =
            cacheTypes.varps.values
                .filter { it.internalName?.startsWith("league_task_completed_") == true }
                .sortedBy { it.id }
        println("COMPLETION VARPS=${completionVarps.size} ids=${completionVarps.map { it.id }}")
        val varpIds = completionVarps.map { it.id }.toSet()
        val varbits =
            cacheTypes.varbits.values
                .filter { it.varpId in varpIds }
                .sortedWith(compareBy({ it.varpId }, { it.bits.first }))
        var wide = 0
        for (varbit in varbits) {
            val width = varbit.bits.last - varbit.bits.first + 1
            if (width != 1) {
                wide++
                println(
                    "WIDE VARBIT ${varbit.id} sym=${varbit.internalName} varp=${varbit.varpId} width=$width"
                )
            }
        }
        println("COMPLETION VARBITS=${varbits.size} wide=$wide")
        // Bits that no varbit covers, per varp, in case a gap is what the client expects.
        for (varp in completionVarps) {
            val covered =
                varbits.filter { it.varpId == varp.id }.flatMap { it.bits.toList() }.toSet()
            val free = (0..31).filterNot { it in covered }
            if (free.isNotEmpty()) println("VARP ${varp.id} ${varp.internalName} free bits=$free")
        }
        println("=== FIRST 256 (id, sym, varp, bit)")
        for ((index, varbit) in varbits.take(256).withIndex()) {
            println(
                "DONE %03d\t%d\t%s\tvarp=%d bit=%d"
                    .format(index, varbit.id, varbit.internalName, varbit.varpId, varbit.bits.first)
            )
        }
        val total =
            cacheTypes.varbits.values.firstOrNull {
                it.internalName == "league_total_tasks_completed"
            }
        println(
            "TOTAL_TASKS_COMPLETED varbit=${total?.id} varp=${total?.varpId} bits=${total?.bits} " +
                "varpSym=${total?.let { cacheTypes.varps[it.varpId]?.internalName }}"
        )
    }

    @Test
    fun GameTestState.`dump task lookup enums`() = runBasicGameTest {
        // 2671 tier -> points, 3411 type names, 3412 area names, 3415 area filter -> area value,
        // 2729 skill names, then the filter dropdown labels.
        for (id in listOf(2671, 3411, 3412, 3415, 2729, 4105, 2676, 2677, 3413, 3414)) {
            val enum = cacheTypes.enums[id] ?: continue
            println(
                "ENUM $id key=${enum.keyLiteral} val=${enum.valLiteral} size=${enum.primitiveMap.size} " +
                    "default=${enum.defaultInt}/${enum.defaultStr}"
            )
            for ((k, v) in enum.primitiveMap.entries.sortedBy { (it.key as? Int) ?: 0 }) {
                println("  $k -> $v")
            }
        }
    }

    private fun GameTestState.dumpStruct(structId: Int, label: String) {
        val struct =
            cacheTypes.structs[structId] ?: return println("$label struct=$structId MISSING")
        println("$label struct=$structId sym=${struct.internalName}")
        for ((param, value) in
            struct.paramMap?.primitiveMap.orEmpty().entries.sortedBy { it.key }) {
            val paramType = cacheTypes.params[param]
            val extra =
                when {
                    paramType?.typeLiteral?.name == "VARBIT" ||
                        (value is Int &&
                            cacheTypes.varbits[value]?.internalName?.startsWith("league_task_") ==
                                true) ->
                        " ; varbit ${cacheTypes.varbits[value as Int]?.internalName}"
                    else -> ""
                }
            println(
                "  param $param sym=${paramType?.internalName} type=${paramType?.typeLiteral} -> $value$extra"
            )
        }
    }

    /** The enum under the league struct with the most struct-valued entries. */
    private fun GameTestState.findTaskEnum(): org.rsmod.game.type.enums.UnpackedEnumType<*, *>? {
        val leagueEnum = cacheTypes.enums[LEAGUE_TYPE_ENUM] ?: return null
        val leagueStructId = leagueEnum.primitiveMap[LEAGUE_TYPE] as? Int ?: return null
        val leagueStruct = cacheTypes.structs[leagueStructId] ?: return null
        val candidates =
            leagueStruct.paramMap
                ?.primitiveMap
                .orEmpty()
                .values
                .filterIsInstance<Int>()
                .mapNotNull { cacheTypes.enums[it] }
        return candidates.maxByOrNull { it.primitiveMap.size }
    }

    private companion object {
        const val LEAGUE_TYPE_ENUM = 2670
        const val LEAGUE_TYPE = 5
        val TIER_STRUCTS = 1136..1143

        val TASK_SCRIPTS =
            listOf(
                3202,
                3203,
                3204,
                2434,
                3699,
                3216,
                3207,
                3208,
                3209,
                3215,
                3225,
                3226,
                3227,
                1125,
                3181,
                3182,
            )

        const val PUSH_INT = 0
        const val PUSH_VARP = 1
        const val POP_VARP = 2
        const val PUSH_VARBIT = 25
        const val POP_VARBIT = 27
    }
}
