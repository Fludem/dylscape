package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints the signatures of the `league_relics` clientscripts, and the full opcode stream of the
 * small ones.
 *
 * `[clientscript,...]` entries are the server-invocable ones (RUNCLIENTSCRIPT); `[proc,...]` are
 * only reachable from other cs2. The relics screen sits on "Loading..." until the server answers a
 * grid click, so the question this answers is which of these the server is expected to run, and
 * with what arguments.
 */
class LeagueRelicsScriptDump {
    @Test
    fun GameTestState.`dump relic script signatures`() = runBasicGameTest {
        for ((id, name) in SCRIPTS) {
            val script = cacheTypes.clientscripts[id]
            if (script == null) {
                println("SCRIPT $id ($name) -> MISSING")
                continue
            }
            println(
                "SCRIPT $id ($name) intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount} intLocals=${script.intLocalCount} " +
                    "ops=${script.commands.size}"
            )
        }
    }

    @Test
    fun GameTestState.`dump loading and draw scripts`() = runBasicGameTest {
        for (id in listOf(3188, 3191, 733, 3196, 3194)) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "strArgs=${script.stringArgumentCount}"
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

    @Test
    fun GameTestState.`dump every hook on interface 655`() = runBasicGameTest {
        val comps =
            cacheTypes.components.values.filter { it.interfaceId == 655 }.sortedBy { it.component }
        for (comp in comps) {
            val hooks =
                listOf(
                    "onLoad" to comp.onLoad,
                    "onMouseOver" to comp.onMouseOver,
                    "onMouseLeave" to comp.onMouseLeave,
                    "onVarTransmit" to comp.onVarTransmit,
                    "onTimer" to comp.onTimer,
                    "onOp" to comp.onOp,
                    "onMouseRepeat" to comp.onMouseRepeat,
                    "onClick" to comp.onClick,
                    "onClickRepeat" to comp.onClickRepeat,
                    "onRelease" to comp.onRelease,
                    "onHold" to comp.onHold,
                    "onScrollWheel" to comp.onScrollWheel,
                )
            for ((label, hook) in hooks) {
                if (hook == null) continue
                println(
                    "HOOK 655:${comp.component} ${comp.internalName} $label=" +
                        hook.contentToString()
                )
            }
        }
    }

    private companion object {
        private val SCRIPTS =
            listOf(
                733 to "clientscript,league_relics_draw_selections",
                3188 to "clientscript,league_relics_init",
                3189 to "proc,league_relics_draw_selections",
                3190 to "clientscript,league_relic_hover",
                3191 to "clientscript,league_relics_loading",
                3193 to "clientscript,league_relic_expanded_view",
                3194 to "clientscript,league_relic_not_available",
                3196 to "clientscript,league_relic_back",
                3694 to "proc,league_relic_hover",
                3697 to "proc,league_relic_active",
            )
    }
}
