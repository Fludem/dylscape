package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Prints the three interfaces the Crafting skill needs and the cs2 behind them.
 *
 * `crafting_gold` (446) and `silver_crafting` (6) have fully named components, so the only open
 * question there is how a quantity reaches the server: an op index, as interface 312 does, or a
 * varc set by the `make_*` buttons. `tanner` (324) is worse -- every one of its components decodes
 * as `com_N` -- so its rows have to be read off the scripts that draw it.
 *
 * Kept as a test rather than notes because it re-runs against whatever cache is installed.
 */
@Execution(ExecutionMode.SAME_THREAD)
class CraftingInterfaceDump {
    @Test
    fun GameTestState.`dump crafting interface components`() = runBasicGameTest {
        for ((id, name) in INTERFACES) {
            val comps =
                cacheTypes.components.values
                    .filter { it.interfaceId == id }
                    .sortedBy { it.component }
            println("=== INTERFACE $id ($name) components=${comps.size}")
            for (comp in comps) {
                val opStr =
                    comp.op
                        .withIndex()
                        .filter { it.value.isNotEmpty() }
                        .joinToString { "op${it.index + 1}='${it.value}'" }
                val text = comp.text.takeIf { it.isNotEmpty() }?.let { " text=\"$it\"" } ?: ""
                val base = comp.opBase.takeIf { it.isNotEmpty() }?.let { " opBase=\"$it\"" } ?: ""
                println(
                    "  $id:${comp.component} ${comp.internalName} type=${comp.type}$base $opStr$text"
                )
            }
        }
    }

    @Test
    fun GameTestState.`dump crafting interface hooks`() = runBasicGameTest {
        for ((id, name) in INTERFACES) {
            println("=== HOOKS $id ($name)")
            val comps =
                cacheTypes.components.values
                    .filter { it.interfaceId == id }
                    .sortedBy { it.component }
            for (comp in comps) {
                val hooks =
                    listOf(
                        "onLoad" to comp.onLoad,
                        "onVarTransmit" to comp.onVarTransmit,
                        "onOp" to comp.onOp,
                        "onClick" to comp.onClick,
                        "onMouseOver" to comp.onMouseOver,
                        "onMouseLeave" to comp.onMouseLeave,
                        "onTimer" to comp.onTimer,
                    )
                for ((label, hook) in hooks) {
                    if (hook == null) continue
                    println(
                        "  $id:${comp.component} ${comp.internalName} $label=${hook.contentToString()}"
                    )
                }
            }
        }
    }

    /** Every clientscript that pushes a component of one of these interfaces. */
    @Test
    fun GameTestState.`find scripts referencing the crafting interfaces`() = runBasicGameTest {
        for ((id, name) in INTERFACES) {
            println("=== SCRIPTS TOUCHING $id ($name)")
            for ((scriptId, script) in cacheTypes.clientscripts) {
                val touches =
                    script.commands.indices.any { i ->
                        script.commands[i] == PUSH_CONSTANT_INT &&
                            script.intOperands[i] > 0 &&
                            (script.intOperands[i] ushr 16) == id
                    }
                if (touches) {
                    println(
                        "  $scriptId ${script.internalName} intArgs=${script.intArgumentCount} strArgs=${script.stringArgumentCount}"
                    )
                }
            }
        }
    }

    @Test
    fun GameTestState.`dump the named crafting scripts`() = runBasicGameTest {
        for (id in SCRIPTS) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} intLocals=${script.intLocalCount}"
            )
            for (i in script.commands.indices) {
                val str = script.stringOperands[i]
                if (str != null) {
                    println("  $i: cmd=${script.commands[i]} str=\"$str\"")
                } else {
                    val op = script.intOperands[i]
                    val note =
                        if (
                            script.commands[i] == PUSH_CONSTANT_INT &&
                                op > 0 &&
                                (op ushr 16) in INTERFACES.keys
                        ) {
                            "  ; component ${op ushr 16}:${op and 0xFFFF}"
                        } else ""
                    println("  $i: cmd=${script.commands[i]} op=$op$note")
                }
            }
        }
    }

    private companion object {
        val INTERFACES =
            linkedMapOf(324 to "tanner", 446 to "crafting_gold", 6 to "silver_crafting")

        val SCRIPTS = listOf(2928, 2929)

        const val PUSH_CONSTANT_INT = 0
    }
}
