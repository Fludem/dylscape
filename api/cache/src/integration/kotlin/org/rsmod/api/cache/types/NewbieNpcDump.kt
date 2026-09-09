package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState

/**
 * Throwaway: combat levels of the tutorial's rats and the boss the void import confused them for.
 */
@Execution(ExecutionMode.SAME_THREAD)
class NewbieNpcDump {
    @Test
    fun GameTestState.`dump rat levels`() = runBasicGameTest {
        val names =
            listOf(
                "newbiegiantrat",
                "newbiegiantrat2",
                "newbiegiantrat3",
                "newbiechicken",
                "rat_boss_giant_rat",
                "giant_rat",
            )
        for (type in cacheTypes.npcs.values.sortedBy { it.id }) {
            val name = type.internalName ?: continue
            if (name !in names) continue
            println(
                "  ${type.id} $name '${type.name}' level=${type.vislevel} size=${type.size} " +
                    "wander=${type.wanderRange} ops=${type.op.filterNotNull().filter{it.isNotEmpty()}}"
            )
        }
    }
}
