package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Prints every clientscript that touches the world map's marker varcs, and the opcode stream of the
 * one the server is meant to call.
 *
 * The world map opened but never showed the player, and nothing in the symbol tables says why:
 * `varc,worldmap_youarehere` (188) is just a number. The scan below shows that exactly one script
 * writes it, `[clientscript,worldmap_transmitdata]` -- a `[clientscript,...]`, so RUNCLIENTSCRIPT
 * is the only thing that can reach it. Its whole body is three pushes and three pops:
 * ```
 *   push $int0; push $int1; push $int2
 *   pop varc,worldmap_gravestone   ; $int2
 *   pop varc,worldmap_clue         ; $int1
 *   pop varc,worldmap_youarehere   ; $int0
 * ```
 *
 * so the signature is `(youAreHere, clue, gravestone)`, all three written together.
 *
 * The values are packed coords: `[proc,worldmap_findcoordinmap]`, which every reader passes them
 * to, compares against `-1` for "absent" and otherwise walks the coord's level up or down looking
 * for a plane the current map covers. Nothing else is server-driven -- `[clientscript,
 * worldmap_overlay]` re-registers itself on a client-side timer and redraws from the varcs -- so
 * keeping 188 in step with the player is the entire server side of the feature.
 */
class WorldMapDump {
    @Test
    fun GameTestState.`dump the world map marker scripts`() = runBasicGameTest {
        for ((id, script) in cacheTypes.clientscripts.entries.sortedBy { it.key }) {
            val hits =
                script.commands.indices
                    .filter { script.commands[it] in VARC_OPS }
                    .filter { script.intOperands[it] in MARKER_VARCS }
                    .map { "${if (script.commands[it] == PUSH_VARC) "read" else "write"}@$it" }
            if (hits.isNotEmpty()) {
                println("SCRIPT $id ${script.internalName} -> ${hits.joinToString()}")
            }
        }

        for (id in listOf(TRANSMIT_DATA, FIND_COORD_IN_MAP)) {
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
                    println("  $i: cmd=${script.commands[i]} op=${script.intOperands[i]}")
                }
            }
        }
    }

    private companion object {
        const val TRANSMIT_DATA = 1749
        const val FIND_COORD_IN_MAP = 1715

        const val PUSH_VARC = 42
        const val POP_VARC = 43
        val VARC_OPS = setOf(PUSH_VARC, POP_VARC)

        /** `worldmap_youarehere`, `worldmap_gravestone` and `worldmap_clue`. */
        val MARKER_VARCS = setOf(188, 401, 1078)
    }
}
