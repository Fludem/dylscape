package org.rsmod.content.custom.zulrah

import jakarta.inject.Inject
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class ZulrahDumpDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * What the cache says about Zulrah, its shrine and the poison varp, dumped rather than guessed.
 *
 * Gradle does not print test stdout: read `system-out` out of
 * `content/custom/zulrah/build/test-results/integration/TEST-*.xml`. Run with `--tests
 * '*ZulrahDump*' --rerun-tasks`.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ZulrahDump {
    @Test
    fun GameTestState.`dump zulrah and snakeling npcs`() = runBasicGameTest {
        val matches =
            cacheTypes.npcs.values
                .filter { it.internalName?.startsWith("snakeboss_") == true }
                .filter { it.id in 2042..2047 }
                .sortedBy { it.id }
        for (type in matches) {
            val ops = type.op.withIndex().filter { it.value != null }
            println(
                "NPC ${type.id} sym=${type.internalName} name='${type.name}' size=${type.size} " +
                    "hp=${type.hitpoints} att=${type.attack} str=${type.strength} " +
                    "def=${type.defence} rng=${type.ranged} magic=${type.magic} " +
                    "wander=${type.wanderRange} maxRange=${type.maxRange} " +
                    "attackRange=${type.attackRange} huntRange=${type.huntRange} " +
                    "huntMode=${type.huntMode} defaultMode=${type.defaultMode} " +
                    "respawn=${type.respawnRate} moveRestrict=${type.moveRestrict} " +
                    "readyAnim=${type.readyAnim} walkAnim=${type.walkAnim} " +
                    "category=${type.category} ops=${ops.map { "op${it.index + 1}='${it.value}'" }}"
            )
            val params = type.paramMap
            if (params == null) {
                println("  params=NONE")
                continue
            }
            for ((key, value) in params.primitiveMap) {
                println("  param $key (${cacheTypes.params[key]?.internalName ?: "?"}) = $value")
            }
        }
    }

    @Test
    fun GameTestState.`dump shrine locs and the scroll`() = runBasicGameTest {
        for (id in listOf(10068, 11698, 11699, 11700, 11701, 46241, 46242)) {
            val loc = cacheTypes.locs[id] ?: continue
            val ops = loc.op.withIndex().filter { it.value != null }
            val multi =
                if (loc.multiLoc.isNotEmpty()) {
                    " multiVarBit=${loc.multiVarBit} multiVarp=${loc.multiVarp}" +
                        " multiDefault=${loc.multiLocDefault} multiLoc=${loc.multiLoc.toList()}"
                } else {
                    ""
                }
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "size=${loc.width}x${loc.length} blockWalk=${loc.blockWalk} " +
                    "blockRange=${loc.blockRange} ops=${ops.map { "op${it.index + 1}='${it.value}'" }}$multi"
            )
        }
        val scroll = cacheTypes.objs.values.single { it.internalName == "teleportscroll_zulandra" }
        println("OBJ ${scroll.id} iop=${scroll.iop.toList()} op=${scroll.op.toList()}")
    }

    @Test
    fun GameTestState.`dump poison varps and the scripts that read them`() = runBasicGameTest {
        for (id in listOf(102, 3777)) {
            val varp = cacheTypes.varps[id] ?: continue
            println(
                "VARP $id sym=${varp.internalName} scope=${varp.scope} " +
                    "transmit=${varp.transmit} clientCode=${varp.clientCode}"
            )
        }
        val poisonBits = cacheTypes.varbits.values.filter { it.varpId == 102 || it.varpId == 3777 }
        for (bit in poisonBits) {
            println(
                "VARBIT ${bit.id} sym=${bit.internalName} varp=${bit.varpId} " +
                    "lsb=${bit.lsb} msb=${bit.msb}"
            )
        }
        val bitIds = poisonBits.map { it.id }.toSet()
        val scripts =
            cacheTypes.clientscripts.values
                .filter { script ->
                    script.commands.indices.any { i ->
                        val cmd = script.commands[i]
                        val op = script.intOperands[i]
                        (cmd == PUSH_VARP && (op == 102 || op == 3777)) ||
                            (cmd == PUSH_VARBIT && op in bitIds)
                    }
                }
                .sortedBy { it.id }
        for (script in scripts) {
            println(
                "=== SCRIPT ${script.id} ${script.internalName} " +
                    "intArgs=${script.intArgumentCount} ops=${script.commands.size}"
            )
            if (script.commands.size > MAX_PRINTED_OPS) {
                println("  (long script; printing only around the poison reads)")
            }
            for (i in script.commands.indices) {
                if (script.commands.size > MAX_PRINTED_OPS && !nearPoisonRead(script, i, bitIds)) {
                    continue
                }
                val str = script.stringOperands[i]
                if (str != null) {
                    println("  $i: cmd=${script.commands[i]} str=\"$str\"")
                } else {
                    println("  $i: cmd=${script.commands[i]} op=${script.intOperands[i]}")
                }
            }
        }
        println("TOTAL_SCRIPTS=${scripts.size}")
    }

    @Test
    fun GameTestState.`dump loc placements around the shrine and the boat`() =
        runAdvancedGameTest { advanced ->
            val registry = advanced.readOnly.locRegistry
            for ((label, box) in listOf("SHRINE" to SHRINE_BOX, "BOAT" to BOAT_BOX)) {
                println("=== PLACEMENTS $label ===")
                for (zoneX in box.minX / 8..box.maxX / 8) {
                    for (zoneZ in box.minZ / 8..box.maxZ / 8) {
                        for (loc in registry.findAll(ZoneKey(zoneX, zoneZ, 0))) {
                            val type = cacheTypes.locs[loc.id] ?: continue
                            println(
                                "PLACED ${type.internalName} (${loc.id}) '${type.name}' at " +
                                    "${loc.coords} shape=${loc.shape} angle=${loc.angle} " +
                                    "size=${type.width}x${type.length}"
                            )
                        }
                    }
                }
            }
        }

    @Test
    fun GameTestState.`render shrine and boat walkability`() =
        runInjectedGameTest(ZulrahDumpDeps::class) { deps ->
            for ((label, box) in listOf("SHRINE" to SHRINE_BOX, "BOAT" to BOAT_BOX)) {
                println("=== RENDER $label x ${box.minX}..${box.maxX} ===")
                for (z in box.maxZ downTo box.minZ) {
                    val row = StringBuilder()
                    for (x in box.minX..box.maxX) {
                        val flags = deps.collision[CoordGrid(x, z, 0)]
                        row.append(if (flags and CollisionFlag.BLOCK_WALK != 0) '#' else '.')
                    }
                    println("R ${z.toString().padStart(5)} $row")
                }
            }
        }

    private fun nearPoisonRead(
        script: org.rsmod.game.type.clientscript.UnpackedClientScriptType,
        index: Int,
        bitIds: Set<Int>,
    ): Boolean {
        val from = (index - CONTEXT).coerceAtLeast(0)
        val to = (index + CONTEXT).coerceAtMost(script.commands.size - 1)
        return (from..to).any { i ->
            val cmd = script.commands[i]
            val op = script.intOperands[i]
            (cmd == PUSH_VARP && (op == 102 || op == 3777)) || (cmd == PUSH_VARBIT && op in bitIds)
        }
    }

    private data class Box(val minX: Int, val maxX: Int, val minZ: Int, val maxZ: Int)

    private companion object {
        const val PUSH_VARP = 1
        const val PUSH_VARBIT = 25
        const val MAX_PRINTED_OPS = 400
        const val CONTEXT = 30

        val SHRINE_BOX = Box(minX = 2240, maxX = 2295, minZ = 3050, maxZ = 3090)
        val BOAT_BOX = Box(minX = 2196, maxX = 2230, minZ = 3040, maxZ = 3072)
    }
}
