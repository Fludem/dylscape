package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class PrayerDump {
    @Test
    fun GameTestState.`dump bones objs`() = runBasicGameTest {
        val matches =
            cacheTypes.objs.values
                .filter { name ->
                    val sym = name.internalName ?: return@filter false
                    sym.isNotEmpty() &&
                        (name.iop.getOrNull(0) == "Bury" || name.iop.getOrNull(0) == "Scatter")
                }
                .sortedBy { it.id }
        for (obj in matches) {
            println(
                "OBJ ${obj.id} sym=${obj.internalName} name='${obj.name}' op=${obj.op.toList()} iop=${obj.iop.toList()}"
            )
        }
        println("TOTAL_OBJS=${matches.size}")
    }

    @Test
    fun GameTestState.`dump altar locs`() = runBasicGameTest {
        val matches =
            cacheTypes.locs.values
                .filter { it.internalName?.contains("altar", ignoreCase = true) == true }
                .sortedBy { it.id }
        for (loc in matches) {
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' ops=${loc.op.toList()}"
            )
        }
        println("TOTAL_LOCS=${matches.size}")
    }
}
