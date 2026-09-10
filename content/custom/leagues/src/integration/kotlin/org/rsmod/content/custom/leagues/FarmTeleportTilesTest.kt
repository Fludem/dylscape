package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.relics.effects.FarmTeleport
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Fairy's Flight lands beside a patch, not on it - the patch itself is a blocking loc - so each
 * farm needs at least one walkable tile within the landing radius, checked against the real
 * collision map.
 */
class FarmTeleportTilesTest {
    @Test
    fun GameTestState.`every farm has a walkable landing tile`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            val stuck =
                FarmTeleport.entries.filter { farm ->
                    val around =
                        (-RADIUS..RADIUS).flatMap { dx ->
                            (-RADIUS..RADIUS).map { dz -> farm.patch.translate(dx, dz) }
                        }
                    around.none { tile: CoordGrid -> deps.collision[tile] and blocked == 0 }
                }
            assertTrue(stuck.isEmpty()) {
                "Farms with no walkable landing tile: " +
                    stuck.joinToString { "${it.name} ${it.patch}" }
            }
        }

    private companion object {
        /** Matches `FairysFlightScript`'s landing radius. */
        const val RADIUS = 3
    }
}
