package org.rsmod.api.cache.types

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.map.collision.get
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.literal.CacheVarLiteral
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Prints everything the magic modules are authored from, so that nothing in them is guessed.
 *
 * What each section settles:
 * - **Spells**: every spell obj in the four spellbook enums with all its params decoded, plus the
 *   `events` bitmask of the spellbook component it lives on. The bitmask decides which hook a spell
 *   can be bound to at all: `TgtCom` for "cast on an inventory item", `TgtLoc` for obelisks and
 *   farming patches, `TgtNpc`/`TgtPlayer` for combat. Level, max hit, cast xp, teleport coordinate,
 *   rune requirements and the curse drain stat all come out of the same params.
 * - **Enchant table**: dbtable 86 `magic_enchant` row by row, with the obj/seq/spotanim/synth
 *   columns resolved to names. The whole jewellery enchanting recipe book lives here.
 * - **Art**: name round-trips for the seq/spotanim/synth ids the modules reference. An `UNNAMED`
 *   here means the symbol tables do not know the id and it must not be `find()`ed.
 * - **Locs**: ops and footprint of the spellbook altars and charge-orb obelisks.
 * - **Npc params**: how many npcs carry `undead` (Crumble Undead) and `freeze_resistance`.
 * - **Vars**: bit widths of the spellbook and autocast varbits.
 * - **Objs**: the non-spell objs the lunar and utility spells consume or produce.
 *
 * Kept as a test because it re-runs against whatever cache is installed. Read `system-out` from
 * `api/cache/build/test-results/integration/TEST-*MagicDump*.xml`; Gradle prints none of it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class MagicDump {
    @Test
    fun GameTestState.`dump every spell in every spellbook`() = runBasicGameTest {
        val books = cacheTypes.enums[SPELLBOOKS_ENUM] ?: return@runBasicGameTest
        for ((bookKey, bookEnumId) in books.primitiveMap.toSortedMap(compareBy { it as Int })) {
            val bookEnum = cacheTypes.enums[bookEnumId as Int] ?: continue
            println("=== SPELLBOOK key=$bookKey enum=$bookEnumId (${bookEnum.internalName})")
            for ((slot, objId) in bookEnum.primitiveMap.toSortedMap(compareBy { it as Int })) {
                val obj = cacheTypes.objs[objId as Int] ?: continue
                println("  [$slot] obj=${obj.id} sym=${obj.internalName} name='${obj.name}'")
                val params = obj.paramMap?.primitiveMap.orEmpty().toSortedMap()
                for ((paramId, value) in params) {
                    val paramName = cacheTypes.params[paramId]?.internalName ?: "?"
                    println(
                        "      $paramName($paramId) = ${cacheTypes.decodeParam(paramName, value)}"
                    )
                }
            }
        }
    }

    @Test
    fun GameTestState.`dump the magic_enchant dbtable`() = runBasicGameTest {
        val rows = cacheTypes.dbRows.values.filter { it.table == ENCHANT_TABLE }.sortedBy { it.id }
        println("=== DBTABLE $ENCHANT_TABLE magic_enchant rows=${rows.size}")
        val table = cacheTypes.dbTables[ENCHANT_TABLE]
        println("  column types: ${table?.types}")
        for (row in rows) {
            val cells =
                row.data.toSortedMap().map { (column, values) ->
                    val types = row.types[column].orEmpty()
                    val decoded =
                        values.mapIndexed { i, value ->
                            cacheTypes.decodeLiteral(types.getOrNull(i), value)
                        }
                    "${ENCHANT_COLUMNS.getOrNull(column) ?: column}=$decoded"
                }
            println("  row ${row.id} ${row.internalName}: ${cells.joinToString(" ")}")
        }
    }

    @Test
    fun GameTestState.`round-trip the art ids the modules reference`() = runBasicGameTest {
        println("=== SEQS")
        for (id in SEQ_IDS) println(
            "  seq $id -> ${cacheTypes.seqs[id]?.internalName ?: "UNNAMED"}"
        )
        println("=== SPOTANIMS")
        for (id in SPOT_IDS) {
            println("  spotanim $id -> ${cacheTypes.spotanims[id]?.internalName ?: "UNNAMED"}")
        }
        println("=== SYNTHS")
        for (id in SYNTH_IDS) {
            println("  synth $id -> ${cacheTypes.synths[id]?.internalName ?: "UNNAMED"}")
        }
        println("=== SEQ NAMES containing cast/lunar/vengeance/zaros/teleport (human_ only)")
        cacheTypes.seqs.values
            .filter { seq ->
                val n = seq.internalName ?: return@filter false
                n.startsWith("human_") &&
                    (n.contains("cast") ||
                        n.contains("lunar") ||
                        n.contains("veng") ||
                        n.contains("zaros") ||
                        n.contains("teleport") ||
                        n.contains("enchant"))
            }
            .sortedBy { it.id }
            .forEach { println("  seq ${it.id} ${it.internalName}") }
        println("=== SPOTANIM NAMES for ancient/lunar/god/curse spells")
        cacheTypes.spotanims.values
            .filter { spot ->
                val n = spot.internalName ?: return@filter false
                ANCIENT_WORDS.any { n.contains(it) } ||
                    n.startsWith("quest_lunar_spellbook") ||
                    n.startsWith("dream_") ||
                    n.startsWith("lunar_") ||
                    CURSE_WORDS.any { n.startsWith(it) }
            }
            .sortedBy { it.id }
            .forEach { println("  spotanim ${it.id} ${it.internalName}") }
        println("=== SYNTH NAMES for spells")
        cacheTypes.synths.values
            .filter { synth ->
                val n = synth.internalName ?: return@filter false
                ANCIENT_WORDS.any { n.contains(it) } ||
                    CURSE_WORDS.any { n.startsWith(it) } ||
                    n.contains("teleport") ||
                    n.contains("alch") ||
                    n.contains("enchant") ||
                    n.contains("superheat") ||
                    n.contains("bones") ||
                    n.contains("orb") ||
                    n.contains("iban") ||
                    n.contains("dart") ||
                    n.contains("crumble") ||
                    n.contains("charge") ||
                    n.contains("lunar") ||
                    n.contains("veng") ||
                    n.contains("humid") ||
                    n.contains("glass") ||
                    n.contains("plank") ||
                    n.contains("spell")
            }
            .sortedBy { it.id }
            .forEach { println("  synth ${it.id} ${it.internalName}") }
    }

    @Test
    fun GameTestState.`dump altar and obelisk locs`() = runBasicGameTest {
        println("=== LOCS")
        for (name in LOC_NAMES) {
            val loc = cacheTypes.locs.values.firstOrNull { it.internalName == name }
            if (loc == null) {
                println("  $name: MISSING")
                continue
            }
            println(
                "  ${loc.id} $name name='${loc.name}' size=${loc.width}x${loc.length} " +
                    "ops=${loc.op.toList()} params=${loc.paramMap?.primitiveMap}"
            )
        }
    }

    @Test
    fun GameTestState.`count npcs tagged undead or freeze resistant`() = runBasicGameTest {
        for ((paramName, paramId) in
            listOf("undead" to UNDEAD_PARAM, "freeze_resistance" to FREEZE_PARAM)) {
            val tagged =
                cacheTypes.npcs.values
                    .filter { it.paramMap?.primitiveMap?.containsKey(paramId) == true }
                    .sortedBy { it.id }
            println("=== NPCS with $paramName($paramId): ${tagged.size}")
            for (npc in tagged.take(25)) {
                val value = npc.paramMap?.primitiveMap?.get(paramId)
                println("  ${npc.id} ${npc.internalName} '${npc.name}' = $value")
            }
        }
    }

    @Test
    fun GameTestState.`dump spellbook and autocast varbits`() = runBasicGameTest {
        println("=== VARBITS")
        for (id in VARBIT_IDS) {
            val varbit = cacheTypes.varbits[id]
            if (varbit == null) {
                println("  $id MISSING")
                continue
            }
            println(
                "  $id ${varbit.internalName} varp=${varbit.baseVar.id} " +
                    "bits=${varbit.lsb}..${varbit.msb}"
            )
        }
    }

    @Test
    fun GameTestState.`dump the objs the utility spells touch`() = runBasicGameTest {
        println("=== OBJS")
        val wanted =
            cacheTypes.objs.values
                .filter { obj ->
                    val n = obj.internalName ?: return@filter false
                    OBJ_PATTERNS.any { it.matches(n) }
                }
                .sortedBy { it.id }
        for (obj in wanted) {
            println(
                "  ${obj.id} ${obj.internalName} '${obj.name}' cost=${obj.cost} " +
                    "stackable=${obj.stackable} params=${obj.paramMap?.primitiveMap}"
            )
        }
    }

    @Test
    fun GameTestState.`dump the enchant sub-list plumbing`() = runBasicGameTest {
        println("=== DBTABLES count=${cacheTypes.dbTables.size} rows=${cacheTypes.dbRows.size}")
        for (id in listOf(ENCHANT_TABLE, MUSIC_TABLE)) {
            val table = cacheTypes.dbTables[id]
            println(
                "  table $id ${table?.internalName} types=${table?.types} defaults=${table?.defaults}"
            )
            val rows = cacheTypes.dbRows.values.filter { it.table == id }
            println("    rows=${rows.size} firstRowData=${rows.firstOrNull()?.data}")
        }
        println("=== VARBIT 9730")
        cacheTypes.varbits[SUBLIST_VARBIT]?.let {
            println("  ${it.internalName} varp=${it.baseVar.id} bits=${it.lsb}..${it.msb}")
        }
        println(
            "=== SPELLBOOK COMPONENTS 218:4 (back), 218:12 (enchant_jewellery), 218:13 (enchant_1), 218:10 (xbows)"
        )
        for (child in listOf(4, 12, 13, 10, 3)) {
            val comp = cacheTypes.components[(SPELLBOOK shl 16) or child] ?: continue
            println(
                "  218:$child ${comp.internalName} type=${comp.type} events=${comp.events} " +
                    "op=${comp.op.toList()} onLoad=${comp.onLoad?.contentToString()} " +
                    "onOp=${comp.onOp?.contentToString()} onVarTransmit=${comp.onVarTransmit?.contentToString()}"
            )
        }
        println(
            "=== CLIENTSCRIPTS referencing varbit 9730 (get=25/set=27) or enum/param of sublist"
        )
        for (script in cacheTypes.clientscripts.values.sortedBy { it.id }) {
            val hits =
                script.commands.indices.filter { i ->
                    (script.commands[i] == 25 || script.commands[i] == 27) &&
                        script.intOperands[i] == SUBLIST_VARBIT
                }
            if (hits.isEmpty()) continue
            println(
                "  script ${script.id} ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "hits=$hits ops=${script.commands.size}"
            )
            for (i in hits) {
                val from = maxOf(0, i - 12)
                val to = minOf(script.commands.size - 1, i + 12)
                for (j in from..to) {
                    val str = script.stringOperands[j]
                    if (str != null) println("    $j: cmd=${script.commands[j]} str=\"$str\"")
                    else println("    $j: cmd=${script.commands[j]} op=${script.intOperands[j]}")
                }
                println("    --")
            }
        }
        println("=== ENCHANT SPELL OBJS")
        for (id in ENCHANT_SPELL_OBJS) {
            val obj = cacheTypes.objs[id] ?: continue
            println("  obj=${obj.id} sym=${obj.internalName}")
            for ((paramId, value) in obj.paramMap?.primitiveMap.orEmpty().toSortedMap()) {
                val paramName = cacheTypes.params[paramId]?.internalName ?: "?"
                println("      $paramName($paramId) = ${cacheTypes.decodeParam(paramName, value)}")
            }
        }
    }

    /**
     * Walkability south and east of the Edgeville bank, where the spellbook altars are placed. `#`
     * is blocked, `.` open. The bank building is the block in the top-left of the render.
     */
    @Test
    fun GameTestState.`render edgeville for the altar placement`() =
        runInjectedGameTest(MagicCollision::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            println(
                "=== EDGEVILLE level 0, x $EDGE_X0..${EDGE_X0 + EDGE_W - 1}, z $EDGE_Z0..${EDGE_Z0 + EDGE_H - 1}"
            )
            println(
                "      " +
                    (0 until EDGE_W).joinToString("") { ((EDGE_X0 + it) / 10 % 10).toString() }
            )
            println(
                "      " + (0 until EDGE_W).joinToString("") { ((EDGE_X0 + it) % 10).toString() }
            )
            for (z in EDGE_Z0 + EDGE_H - 1 downTo EDGE_Z0) {
                val row = StringBuilder()
                for (x in EDGE_X0 until EDGE_X0 + EDGE_W) {
                    val flags = deps.collision[CoordGrid(x, z, 0)]
                    val symbol =
                        when {
                            flags == 0 -> '.'
                            flags and blocked != 0 -> '#'
                            flags and CollisionFlag.LOC != 0 -> 'L'
                            else -> 'w' // walls, decoration, projectile blockers only
                        }
                    row.append(symbol)
                }
                println("  $z $row")
            }
        }

    private fun TypeListMap.decodeParam(name: String, value: Any): String =
        when {
            name == "spell_button" && value is Int -> {
                val component = components[value]
                val events =
                    IfEvent.entries.filter { component?.hasEvent(it) == true }.map { it.name }
                "${component?.internalName ?: value} events=$events"
            }
            name == "spell_telecoord" && value is Int -> {
                val coord = CoordGrid(value)
                "$coord (x=${coord.x}, z=${coord.z}, level=${coord.level})"
            }
            name.startsWith("spell_runetype") && value is Int -> {
                "${objs[value]?.internalName ?: "?"}($value)"
            }
            name == "spell_drain_stat" && value is Int -> {
                "${stats[value]?.internalName ?: "?"}($value)"
            }
            else -> value.toString()
        }

    private fun TypeListMap.decodeLiteral(typeId: Int?, value: Any): String {
        val literal = CacheVarLiteral.entries.firstOrNull { it.id == typeId }
        val name =
            when (literal) {
                CacheVarLiteral.OBJ,
                CacheVarLiteral.NAMEDOBJ -> objs[value as Int]?.internalName
                CacheVarLiteral.SEQ -> seqs[value as Int]?.internalName
                CacheVarLiteral.SPOTANIM,
                CacheVarLiteral.GRAPHIC -> spotanims[value as Int]?.internalName
                CacheVarLiteral.SYNTH -> synths[value as Int]?.internalName
                else -> null
            }
        return if (name != null) "$name($value)" else "$value:${literal?.name ?: typeId}"
    }

    private companion object {
        const val SPELLBOOKS_ENUM = 1981
        const val ENCHANT_TABLE = 86
        const val MUSIC_TABLE = 44
        const val SUBLIST_VARBIT = 9730
        const val SPELLBOOK = 218
        const val EDGE_X0 = 3076
        const val EDGE_W = 40
        const val EDGE_Z0 = 3470
        const val EDGE_H = 45
        val ENCHANT_SPELL_OBJS = listOf(3276, 3287, 3298, 3305, 3318, 6567, 19475, 27089, 5000)
        const val UNDEAD_PARAM = 65434
        const val FREEZE_PARAM = 65399

        val ENCHANT_COLUMNS =
            listOf(
                "base",
                "output",
                "spell",
                "members",
                "castxp",
                "anim",
                "spotanim",
                "sound",
                "special",
                "failmes",
            )

        val SEQ_IDS =
            listOf(
                707,
                708,
                709,
                710,
                711,
                712,
                713,
                714,
                715,
                716,
                717,
                718,
                719,
                720,
                721,
                722,
                723,
                724,
                725,
                726,
                727,
                728,
                729,
                793,
                811,
                812,
                931,
                1161,
                1162,
                1163,
                1164,
                1165,
                1166,
                1167,
                1168,
                1169,
                1576,
                1819,
                1820,
                1978,
                1979,
                1988,
                4409,
                4410,
                4411,
                4412,
                4413,
                4414,
                4415,
                4416,
                4417,
                4418,
                4422,
                4423,
                4432,
                6293,
                6294,
                6295,
                6296,
                6297,
                6298,
                6299,
                6300,
                6301,
                6302,
                6303,
                6304,
                6305,
                8316,
            )

        val SPOT_IDS =
            listOf(
                76,
                77,
                78,
                87,
                88,
                89,
                102,
                103,
                104,
                105,
                106,
                107,
                108,
                109,
                110,
                111,
                112,
                113,
                114,
                115,
                116,
                141,
                142,
                143,
                144,
                145,
                146,
                147,
                148,
                149,
                150,
                151,
                152,
                153,
                154,
                167,
                168,
                169,
                170,
                171,
                172,
                173,
                174,
                175,
                177,
                178,
                179,
                180,
                181,
                238,
                282,
                322,
                328,
                329,
                345,
            ) + (360..392).toList() + (723..748).toList() + listOf(452, 1061, 1063, 1296)

        val SYNTH_IDS =
            (97..232).toList() +
                listOf(1653, 1654, 1655, 1656, 1657, 1658, 1659, 1660) +
                (4025..4032).toList()

        val ANCIENT_WORDS =
            listOf("_rush", "_burst", "_blitz", "_barrage", "ice_", "smoke_", "shadow_", "blood_")
        val CURSE_WORDS =
            listOf(
                "confuse",
                "weaken",
                "curse",
                "vulnerab",
                "enfeeble",
                "stun",
                "bind",
                "snare",
                "entangle",
                "saradomin",
                "guthix",
                "zamorak",
                "iban",
                "magic_dart",
                "magicdart",
                "crumble",
                "teleport",
                "tele_",
                "alch",
                "enchant",
                "superheat",
                "bones",
                "orb",
                "charge",
            )

        val LOC_NAMES =
            listOf(
                "dt_zaros_altar",
                "astral_altar",
                "arceuus_altar",
                "obelisk_air",
                "obelisk_water",
                "obelisk_earth",
                "obelisk_fire",
            )

        val VARBIT_IDS = listOf(4070, 275, 276, 2668, 4823, 3617)

        val OBJ_PATTERNS =
            listOf(
                Regex("plank.*"),
                Regex(".*hunter_kit"),
                Regex("amulet_of_glory.*"),
                Regex("ring_of_wealth.*"),
                Regex("jewl_.*"),
                Regex("uncooked_.*pie"),
                Regex(".*pie"),
                Regex(".*_?hide.*"),
                Regex("leather.*"),
                Regex(".*_leather"),
                Regex("stafforb"),
                Regex("(air|water|earth|fire)_orb"),
                Regex("(saradomin|guthix|zamorak)_cape.*"),
                Regex("(saradomin|guthix|zamorak)_staff"),
                Regex("bucket.*"),
                Regex("bowl.*"),
                Regex("jug.*"),
                Regex("vial.*"),
                Regex("watering_can.*"),
                Regex("seaweed.*"),
                Regex("molten_glass"),
                Regex("flax"),
                Regex("bow_string"),
                Regex(".*bones"),
                Regex("banana"),
                Regex("peach"),
                Regex("coins"),
                Regex("logs|oak_logs|teak_logs|mahogany_logs"),
                Regex("(unstrung|strung)_.*"),
                Regex(".*_amulet_u|.*_amulet"),
                Regex("dragonstone.*"),
            )
    }
}

private class MagicCollision @Inject constructor(val collision: CollisionFlagMap)
