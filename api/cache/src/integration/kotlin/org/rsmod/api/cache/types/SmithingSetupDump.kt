package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Prints how the anvil interface decides which bar tier to draw.
 *
 * Interface 312's component 0 runs `[proc,smithing_setup]` (clientscript 430) from its `onLoad`.
 * The first thing that proc does is read the varbit `smithing_bar_type`, hand it to enum 1253 and
 * switch on the **bar obj** that comes back -- which is why it looks, from the case labels alone,
 * as though the server should be storing a bar's obj id. It should not. `smithing_bar_type` is bits
 * 0..2 of the varp `smithbars` (210), so an obj id written there truncates silently: `bronze_bar`
 * (2349) keeps only `2349 and 7 == 5`, and enum 1253 turns 5 into `adamantite_bar`. Every tier drew
 * the wrong menu that way until this was decoded.
 *
 * The output to read is enum 1253's `1..7` -> bar mapping, and the varbit's `bits=0..2`.
 *
 * Kept as a test rather than notes because it re-runs against whatever cache is actually installed.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SmithingSetupDump {
    @Test
    fun GameTestState.`dump the smithing bar-tier varbit and enum`() = runBasicGameTest {
        println("=== varbit $BAR_TYPE_VARBIT = ${cacheTypes.varbits[BAR_TYPE_VARBIT]}")

        val enum = cacheTypes.enums[BAR_TYPE_TO_BAR]
        println("=== enum $BAR_TYPE_TO_BAR (smithing_bar_type -> bar obj)")
        enum?.primitiveMap?.toSortedMap(compareBy { it as Int })?.forEach { (key, value) ->
            println("    $key -> $value (${cacheTypes.objs[value as Int]?.internalName})")
        }

        val setup = cacheTypes.clientscripts[SETUP_SCRIPT] ?: return@runBasicGameTest
        println("=== script $SETUP_SCRIPT ${setup.internalName}")
        for (i in 0 until minOf(setup.commands.size, HEAD_INSTRUCTIONS)) {
            val str = setup.stringOperands[i]
            if (str != null) {
                println("  $i: cmd=${setup.commands[i]} str=\"$str\"")
            } else {
                println("  $i: cmd=${setup.commands[i]} op=${setup.intOperands[i]}")
            }
        }
    }

    private companion object {
        const val SETUP_SCRIPT = 430
        const val BAR_TYPE_VARBIT = 3216
        const val BAR_TYPE_TO_BAR = 1253

        /** Enough of the proc to cover the varbit read, the enum call and the switch. */
        const val HEAD_INSTRUCTIONS = 17
    }
}
