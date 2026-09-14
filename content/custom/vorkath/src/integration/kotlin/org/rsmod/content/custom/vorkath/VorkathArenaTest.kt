package org.rsmod.content.custom.vorkath

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.vorkath.configs.VorkathArena
import org.rsmod.content.custom.vorkath.configs.vorkath_locs
import org.rsmod.map.CoordGrid
import org.rsmod.map.zone.ZoneKey
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class VorkathArenaDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * Guards the coordinates, which is the half of this module a compiler cannot check.
 *
 * `telejump` does not fail on a bad destination - it puts the player inside a wall and reports
 * nothing - and a spawn on the wrong tile is a dragon in the sea. Every game test is handed the
 * real collision map and, on the advanced scope, the real map locs, so the whole table is swept.
 */
@Execution(ExecutionMode.SAME_THREAD)
class VorkathArenaTest {
    @Test
    fun GameTestState.`every tile a player is put on is standable`() =
        runInjectedGameTest(VorkathArenaDeps::class) { deps ->
            val blocked =
                VorkathArena.standable.filter { (_, coords) ->
                    deps.collision[coords.x, coords.z, coords.level] and BLOCKS_STANDING != 0
                }
            assertTrue(blocked.isEmpty()) {
                "Tiles nothing can stand on: " +
                    blocked.entries.joinToString { (name, coords) ->
                        "$name $coords (flags ${deps.collision[coords.x, coords.z, coords.level]})"
                    }
            }
        }

    @Test
    fun GameTestState.`the dragon's platform is the blocked seven by seven in the crater`() =
        runInjectedGameTest(VorkathArenaDeps::class) { deps ->
            // The map reserves the platform: every tile of the footprint is blocked and every tile
            // of the ring around it is open. If the spawn drifts a tile, one of the two fails.
            val spawn = VorkathArena.spawn
            for (dx in 0 until VorkathArena.VORKATH_SIZE) {
                for (dz in 0 until VorkathArena.VORKATH_SIZE) {
                    val flags = deps.collision[spawn.x + dx, spawn.z + dz, spawn.level]
                    assertTrue(flags and CollisionFlag.BLOCK_WALK != 0) {
                        "Platform tile (${spawn.x + dx}, ${spawn.z + dz}) is open floor."
                    }
                }
            }
            for (dx in -1..VorkathArena.VORKATH_SIZE) {
                for (dz in -1..VorkathArena.VORKATH_SIZE) {
                    val onEdge = dx == -1 || dz == -1
                    val onFarEdge =
                        dx == VorkathArena.VORKATH_SIZE || dz == VorkathArena.VORKATH_SIZE
                    if (!onEdge && !onFarEdge) {
                        continue
                    }
                    val flags = deps.collision[spawn.x + dx, spawn.z + dz, spawn.level]
                    assertEquals(0, flags and BLOCKS_STANDING) {
                        "Ring tile (${spawn.x + dx}, ${spawn.z + dz}) is blocked; the platform is " +
                            "not where the spawn says."
                    }
                }
            }
        }

    @Test
    fun GameTestState.`the crater lips and the boat are on the map where the script binds them`() =
        runAdvancedGameTest { advanced ->
            val registry = advanced.readOnly.locRegistry

            val entrance = registry.locAt(VorkathArena.craterEntrance)
            assertEquals(
                vorkath_locs.crater_entrance.id,
                entrance,
                "The south lip is not `ungael_crater_entrance`.",
            )

            // The north lip is deliberately unbound: its multiloc resolves to the op-less child
            // for every value of the varbit in this cache. This pins that, so the day a cache
            // update gives the exit an op, someone comes back and binds it.
            val exit = registry.locAt(CRATER_EXIT)
            val exitType = cacheTypes.locs.getValue(exit)
            val children = exitType.multiLoc.map { it.toInt() }.toSet()
            assertEquals(setOf(CRATER_EXIT_NO_OP), children) {
                "The north lip (${exitType.internalName}) now resolves to something with an op; " +
                    "bind it in VorkathScript."
            }

            val boat = registry.locAt(UNGAEL_BOAT)
            assertEquals(vorkath_locs.ungael_boat.id, boat, "No `ungael_boat` on the shore.")
        }

    private fun org.rsmod.api.registry.loc.LocRegistry.locAt(coords: CoordGrid): Int {
        val loc = findAll(ZoneKey.from(coords)).firstOrNull { it.coords == coords }
        return checkNotNull(loc) { "No loc at all on $coords." }.id
    }

    private companion object {
        /** Stricter than a walk check: a loc or a rock on the tile counts as unusable. */
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or
                CollisionFlag.BLOCK_PLAYERS or
                CollisionFlag.LOC or
                CollisionFlag.GROUND_DECOR

        /** The placed `ungael_boat`, a 6x2 with `Travel`, read off the loc placement dump. */
        val UNGAEL_BOAT = CoordGrid(2275, 4032, 0)

        /** The north lip: `ungael_crater_exit` (25337), whose children are all 31991. */
        val CRATER_EXIT = CoordGrid(2272, 4077, 0)
        const val CRATER_EXIT_NO_OP = 31991
    }
}
