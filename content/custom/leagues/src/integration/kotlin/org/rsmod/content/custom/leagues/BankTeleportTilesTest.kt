package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.relics.effects.BankTeleport
import org.rsmod.game.map.collision.get
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * `telejump` puts a player wherever it is told, so a bank tile inside a wall or a counter fails
 * silently. Every Bank Heist destination is checked against the real collision map instead.
 */
class BankTeleportTilesTest {
    @Test
    fun GameTestState.`every bank destination is walkable`() =
        runInjectedGameTest(LeagueTestDeps::class) { deps ->
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            val stuck = BankTeleport.entries.filter { deps.collision[it.dest] and blocked != 0 }
            assertTrue(stuck.isEmpty()) {
                "Unwalkable bank tiles: " + stuck.joinToString { "${it.name} ${it.dest}" }
            }
        }
}
