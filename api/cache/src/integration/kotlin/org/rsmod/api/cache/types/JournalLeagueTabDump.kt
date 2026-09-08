package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints what the journal side panel needs in order to show its fifth tab.
 *
 * `side_journal` reserves five list slots -- summary, quests, tasks, adventure paths and leagues --
 * but the module only wired the first three. This is the derivation behind the leagues tab:
 * - `side_journal_tab` is four bits wide, so a fifth value fits. `[proc,side_journal_switchtab]`
 *   switches on it in slot order, which puts leagues at 4.
 * - Every list component carries `events=0x2` (Op1) in the cache, so clicks reach the server with
 *   no `ifSetEvents` from us. The same is true of the four `league_side_panel` footer buttons.
 * - That proc draws the fifth tab only when `[proc,league_world]` returns 1 *and*
 *   `league_tutorial_completed` reads 3 or higher.
 * - `[proc,league_world]` returns 1 when bit 30 of `map_flags_cached` is set and bit 29 is clear.
 *   No clientscript writes that varp -- 27 read it, none pop it -- so the server owns it and can
 *   flip the flag on login.
 *
 * Kept as a test rather than notes because it re-runs against whatever cache is actually installed,
 * which notes cannot.
 */
class JournalLeagueTabDump {
    @Test
    fun GameTestState.`dump the tab gate vars`() = runBasicGameTest {
        val varp = cacheTypes.varps[WORLD_FLAGS_VARP]
        println("VARP $WORLD_FLAGS_VARP sym=${varp?.internalName} transmit=${varp?.transmit}")

        var readers = 0
        var writers = 0
        for (script in cacheTypes.clientscripts.values.sortedBy { it.id }) {
            for (i in script.commands.indices) {
                if (script.intOperands[i] != WORLD_FLAGS_VARP) continue
                when (script.commands[i]) {
                    PUSH_VARP -> {
                        readers++
                        println("READS ${script.id} ${script.internalName}")
                    }
                    POP_VARP -> {
                        writers++
                        println("WRITES ${script.id} ${script.internalName}")
                    }
                }
            }
        }
        println("READERS=$readers WRITERS=$writers")

        for (name in GATE_VARBITS) {
            val varbit = cacheTypes.varbits.values.firstOrNull { it.internalName == name }
            val bits = varbit?.bits
            val width = if (bits == null) 0 else bits.last - bits.first + 1
            println(
                "VARBIT ${varbit?.id} $name varp=${varbit?.baseVar?.internalName} " +
                    "bits=$bits max=${if (width == 0) 0 else (1 shl width) - 1}"
            )
        }
    }

    @Test
    fun GameTestState.`dump the journal and league side panel components`() = runBasicGameTest {
        dumpInterface("side_journal")
        dumpInterface("league_side_panel")
    }

    private fun GameTestState.dumpInterface(name: String) {
        val interf =
            cacheTypes.interfaces.values.firstOrNull { it.internalName == name }
                ?: return println("INTERFACE $name = MISSING")
        val comps =
            cacheTypes.components.values
                .filter { it.interfaceId == interf.id }
                .sortedBy { it.component }
        println("=== INTERFACE ${interf.id} $name components=${comps.size}")
        for (comp in comps) {
            val notes = mutableListOf<String>()
            comp.onLoad?.let { notes += "onLoad=${it.contentToString()}" }
            comp.onOp?.let { notes += "onOp=${it.contentToString()}" }
            if (comp.hide) notes += "hidden"
            if (comp.events != 0) notes += "events=0x%x".format(comp.events)
            if (notes.isEmpty()) continue
            println("  ${comp.component} ${comp.internalName}: ${notes.joinToString(" ")}")
        }
    }

    @Test
    fun GameTestState.`dump the tab bar layout procs`() = runBasicGameTest {
        for (id in LAYOUT_PROCS) {
            val script = cacheTypes.clientscripts[id] ?: continue
            println(
                "=== $id ${script.internalName} intArgs=${script.intArgumentCount} " +
                    "ops=${script.commands.size}"
            )
            for (i in script.commands.indices) {
                val command = script.commands[i]
                val operand = script.intOperands[i]
                val str = script.stringOperands[i]
                when {
                    str != null -> println("  $i: cmd=$command str=\"$str\"")
                    command == PUSH_VARBIT || command == POP_VARBIT ->
                        println(
                            "  $i: cmd=$command op=$operand" +
                                "  ; VARBIT ${cacheTypes.varbits[operand]?.internalName ?: "?"}"
                        )
                    command == PUSH_VARP || command == POP_VARP ->
                        println(
                            "  $i: cmd=$command op=$operand" +
                                "  ; VARP ${cacheTypes.varps[operand]?.internalName ?: "?"}"
                        )
                    else -> println("  $i: cmd=$command op=$operand")
                }
            }
        }
    }

    private companion object {
        /** `side_journal_switchtab`, then the two world predicates it consults. */
        val LAYOUT_PROCS = listOf(2800, 3160, 1138, 206)

        val GATE_VARBITS =
            listOf("side_journal_tab", "league_tutorial_completed", "league_account", "league_type")

        const val WORLD_FLAGS_VARP = 3717
        const val PUSH_VARP = 1
        const val POP_VARP = 2
        const val PUSH_VARBIT = 25
        const val POP_VARBIT = 27
    }
}
