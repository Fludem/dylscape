package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Prints the three trade interfaces and the cs2 behind them.
 *
 * `trademain` (335), `tradeside` (336) and `tradeconfirm` (334) are half named: the buttons are,
 * but most of the text and status components decode as `com_N`, and neither symbol table records
 * the arguments their setup scripts take. Those are read off the bytecode here -- in particular
 * which components carry the partner's name, the status line and the free-space text, and whether
 * the client reads the partner's offer through the mirrored `tradeoffer` inventory.
 *
 * Kept as a test rather than notes because it re-runs against whatever cache is installed.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TradeInterfaceDump {
    @Test
    fun GameTestState.`dump trade interface components and hooks`() = runBasicGameTest {
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
                    "  $id:${comp.component} ${comp.internalName} type=${comp.type} " +
                        "layer=${comp.layer}$base $opStr$text"
                )
                val hooks =
                    listOf(
                        "onLoad" to comp.onLoad,
                        "onInvTransmit" to comp.onInvTransmit,
                        "onVarTransmit" to comp.onVarTransmit,
                        "onOp" to comp.onOp,
                        "onClick" to comp.onClick,
                        "onTimer" to comp.onTimer,
                    )
                for ((label, hook) in hooks) {
                    if (hook == null) continue
                    println("      $label=${hook.contentToString()}")
                }
            }
        }
    }

    /** Every clientscript that pushes a component of one of the trade interfaces. */
    @Test
    fun GameTestState.`dump scripts referencing the trade interfaces`() = runBasicGameTest {
        val hookScripts =
            cacheTypes.components.values
                .filter { it.interfaceId in INTERFACES.keys }
                .flatMap { listOfNotNull(it.onLoad, it.onInvTransmit, it.onVarTransmit, it.onOp) }
                .mapNotNull { it.firstOrNull() as? Int }
                .toSet()
        val touching =
            cacheTypes.clientscripts
                .filter { (_, script) ->
                    script.commands.indices.any { i ->
                        script.commands[i] == PUSH_CONSTANT_INT &&
                            script.intOperands[i] > 0 &&
                            (script.intOperands[i] ushr 16) in INTERFACES.keys
                    }
                }
                .keys
        val direct = (hookScripts + touching).sorted()
        val gosubs =
            direct
                .mapNotNull { cacheTypes.clientscripts[it] }
                .flatMap { script ->
                    script.commands.indices
                        .filter { script.commands[it] == GOSUB }
                        .map { script.intOperands[it] }
                }
                .toSet()
        val all = (direct + gosubs).distinct().sorted()
        println("=== SCRIPTS hooks=$hookScripts touching=${touching.sorted()} gosubs=$gosubs")
        for (id in all) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} intLocals=${script.intLocalCount} " +
                    "strLocals=${script.stringLocalCount}"
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

    /**
     * The draw and update scripts the setup scripts register at runtime, which is why neither of
     * the tests above finds them: `trade_main_init` hooks 762 onto `tradeoffer` and `traderemoved`,
     * `trade_confirm_init` hooks 767, and both hand off to a draw proc.
     */
    @Test
    fun GameTestState.`dump the trade draw scripts`() = runBasicGameTest {
        for (id in DRAW_SCRIPTS) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} intLocals=${script.intLocalCount} " +
                    "strLocals=${script.stringLocalCount}"
            )
            for (i in script.commands.indices) {
                val str = script.stringOperands[i]
                if (str != null) {
                    println("  $i: cmd=${script.commands[i]} str=\"$str\"")
                } else {
                    println("  $i: cmd=${script.commands[i]} op=${script.intOperands[i]}")
                }
            }
        }
    }

    private companion object {
        val INTERFACES = linkedMapOf(335 to "trademain", 336 to "tradeside", 334 to "tradeconfirm")

        /** Draw procs, their inv/var transmit hooks, the tooltip, and `interface_inv_init`. */
        val DRAW_SCRIPTS = listOf(762, 763, 765, 767, 768, 1234, 149)

        const val PUSH_CONSTANT_INT = 0
        const val GOSUB = 40
    }
}
