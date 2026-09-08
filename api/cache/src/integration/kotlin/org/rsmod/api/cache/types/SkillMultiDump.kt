package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints the make-menu's setup script and the enums it reads.
 *
 * `[clientscript,skillmulti_setup]` takes thirteen ints and a string, and the symbol tables record
 * none of their meanings -- which is why this module originally shipped without the menu at all.
 * The meanings are recoverable, though:
 * [org.rsmod.api.cache.types.clientscript .ClientScriptTypeDecoder] already exposes each script's
 * opcode stream, and the argument order can simply be read off it.
 *
 * The load-bearing parts of the output, for anyone re-deriving this after a cache update:
 * - a run of `push $intN; push -1; branch_eq` over `$int3`..`$int11` counting how many buttons to
 *   draw -- so `$int2`..`$int11` are the ten item slots, `-1` meaning empty
 * - `$int1` clamped into `1..28` and used as the ceiling for `$int12`, which lands in
 *   `varc,skillmulti_quantity` -- so `$int1` is the maximum and `$int12` the starting quantity
 * - `$int0` used only as a key into enums 1809, 5178, 1810 and 3623 -- the verb, whether quantity
 *   buttons are drawn at all, and which of them appear
 *
 * Kept as a test rather than notes because it re-runs against whatever cache is actually installed,
 * which notes cannot.
 */
class SkillMultiDump {
    @Test
    fun GameTestState.`dump the skillmulti setup script`() = runBasicGameTest {
        val setup = cacheTypes.clientscripts[SETUP_SCRIPT]
        if (setup != null) {
            println(
                "=== ${setup.internalName} intArgs=${setup.intArgumentCount} " +
                    "strArgs=${setup.stringArgumentCount} intLocals=${setup.intLocalCount}"
            )
            for (i in setup.commands.indices) {
                val str = setup.stringOperands[i]
                val note = componentNote(setup.commands[i], setup.intOperands[i])
                if (str != null) {
                    println("  $i: cmd=${setup.commands[i]} str=\"$str\"")
                } else {
                    println("  $i: cmd=${setup.commands[i]} op=${setup.intOperands[i]}$note")
                }
            }
        }

        for ((id, purpose) in ENUMS) {
            val enum = cacheTypes.enums[id] ?: continue
            println("=== ENUM $id ($purpose) default='${enum.defaultStr ?: enum.defaultInt}'")
            enum.primitiveMap.toSortedMap(compareBy { it as Int }).forEach { (key, value) ->
                println("    $key -> $value")
            }
        }
    }

    /** Annotates a pushed constant that decodes as a component of the make-menu. */
    private fun componentNote(command: Int, operand: Int): String {
        if (command != PUSH_CONSTANT_INT || operand <= 0) {
            return ""
        }
        val interfaceId = operand ushr 16
        return if (interfaceId == SKILLMULTI) "  ; component $interfaceId:${operand and 0xFFFF}"
        else ""
    }

    private companion object {
        const val SETUP_SCRIPT = 2046
        const val SKILLMULTI = 270
        const val PUSH_CONSTANT_INT = 0

        val ENUMS =
            listOf(
                1809 to "skillmulti type -> the verb on every button",
                5178 to "skillmulti type -> 0 when the quantity buttons are suppressed",
                1810 to "skillmulti type -> 0 when the 'X' quantity button is suppressed",
                1812 to "button index -> its keyboard hotkey label",
            )
    }
}
