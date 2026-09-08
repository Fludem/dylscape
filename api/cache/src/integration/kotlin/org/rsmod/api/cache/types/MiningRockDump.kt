package org.rsmod.api.cache.types

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class MiningRockDump {
    @Test
    fun GameTestState.`dump mining rock locs`() = runBasicGameTest {
        val wanted =
            Regex(
                "^(copper|tin|iron|coal|silver|gold|mithril|adamantite|runite|clay|gem" +
                    "|amethyst)rock[0-9]*$|^rocks[0-9]*$|^gemrock$"
            )
        val matches =
            cacheTypes.locs.values
                .filter { it.internalName?.matches(wanted) == true }
                .sortedBy { it.internalName }
        for (loc in matches) {
            println(
                "LOC ${loc.id} sym=${loc.internalName} name='${loc.name}' " +
                    "ops=${loc.op.toList()}"
            )
        }
        println("TOTAL=${matches.size}")
    }
}
