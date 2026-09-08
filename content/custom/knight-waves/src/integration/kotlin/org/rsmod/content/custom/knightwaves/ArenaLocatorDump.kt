package org.rsmod.content.custom.knightwaves

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.flag.CollisionFlag

/** Throwaway locator for the Knight Waves arena. Delete once the coords are recorded. */
class ArenaLocatorDump {
    @Test
    fun GameTestState.`dump camelot top floor collision`() = runBasicGameTest {
        val level = 2
        val minX = 2725
        val maxX = 2775
        val minZ = 3490
        val maxZ = 3525
        println("COLLISION level=$level x=$minX..$maxX z=$minZ..$maxZ ('.' walkable, '#' blocked)")
        for (z in maxZ downTo minZ) {
            val row = StringBuilder()
            row.append("MAP %4d ".format(z))
            for (x in minX..maxX) {
                val flags = collision[x, z, level]
                row.append(if (flags and CollisionFlag.BLOCK_WALK == 0) '.' else '#')
            }
            println(row)
        }
        println("MAP xxxx " + (minX..maxX).joinToString("") { ((it / 10) % 10).toString() })
        println("MAP xxxx " + (minX..maxX).joinToString("") { (it % 10).toString() })
    }

    @Test
    fun GameTestState.`dump knight stats`() = runBasicGameTest {
        val names = listOf("kr_squire") + (1..8).map { "kr_knight$it" }
        for (name in names) {
            val type = cacheTypes.npcs.values.firstOrNull { it.internalName == name } ?: continue
            println(
                "KNIGHT ${type.internalName} id=${type.id} name='${type.name}' " +
                    "size=${type.size} vislevel=${type.vislevel} hp=${type.hitpoints} " +
                    "att=${type.attack} str=${type.strength} def=${type.defence} " +
                    "wander=${type.wanderRange} maxRange=${type.maxRange} " +
                    "huntMode=${type.huntMode} huntRange=${type.huntRange} " +
                    "defaultMode=${type.defaultMode} ops=${type.op.toList()}"
            )
        }
    }

    @Test
    fun GameTestState.`dump camelot top floor locs`() = runAdvancedGameTest { advanced ->
        val registry = advanced.readOnly.locRegistry
        for (zoneZ in 436..440) {
            for (zoneX in 342..347) {
                val zone = ZoneKey(zoneX, zoneZ, 2)
                for (loc in registry.findAll(zone)) {
                    val type = cacheTypes.locs[loc.id] ?: continue
                    println(
                        "LOC ${loc.coords} id=${loc.id} shape=${loc.shape} angle=${loc.angle} " +
                            "sym=${type.internalName} name='${type.name}'"
                    )
                }
            }
        }
        println("LOCSCAN_DONE")
    }
}
