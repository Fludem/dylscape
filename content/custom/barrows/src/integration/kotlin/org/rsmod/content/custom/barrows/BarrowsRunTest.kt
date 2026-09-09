package org.rsmod.content.custom.barrows

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.barrows.BarrowsProgress.brothersKilled
import org.rsmod.content.custom.barrows.BarrowsProgress.chestOpen
import org.rsmod.content.custom.barrows.BarrowsProgress.hasKilled
import org.rsmod.content.custom.barrows.BarrowsProgress.resetRun
import org.rsmod.content.custom.barrows.BarrowsProgress.rewardPotential
import org.rsmod.content.custom.barrows.configs.BarrowsMap
import org.rsmod.content.custom.barrows.configs.barrows_npcs
import org.rsmod.content.custom.barrows.scripts.BarrowsScript
import org.rsmod.game.entity.Npc

/**
 * The run itself: raising a brother, refusing to raise him twice, and what a kill records.
 *
 * Single-threaded on purpose - `integration-test-suite` runs methods concurrently by default and
 * these spawn npcs into the shared world at fixed crypt tiles.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsRunTest {
    @Test
    fun GameTestState.`searching a sarcophagus raises that brother`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            val brother = Brother.Torag
            assertEquals(BarrowsRun.Raise.Raised, raise(run, brother))
            assertNotNull(liveBrother(run, brother), "${brother.displayName} was not raised.")
            cleanUp(run, brother)
        }

    /**
     * Barrows is shared-world here and RSMod has no per-player npc visibility, so two players
     * searching the same sarcophagus would otherwise stack two copies of one brother on one tile.
     */
    @Test
    fun GameTestState.`a brother already on his feet is not raised again`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            val brother = Brother.Guthan
            assertEquals(BarrowsRun.Raise.Raised, raise(run, brother))
            assertEquals(BarrowsRun.Raise.AlreadyRaised, raise(run, brother))
            cleanUp(run, brother)
        }

    @Test
    fun GameTestState.`a brother already beaten stays at rest`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            val brother = Brother.Karil
            player.withProtectedAccess { run.brotherDefeated(this, brother) }
            assertEquals(BarrowsRun.Raise.AlreadyKilled, raise(run, brother))
            assertNull(liveBrother(run, brother), "A beaten brother was raised.")
        }

    @Test
    fun GameTestState.`beating a brother records the kill and pays reward potential`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            val brother = Brother.Dharok
            player.withProtectedAccess { run.brotherDefeated(this, brother) }

            assertTrue(player.hasKilled(brother), "The kill was not recorded.")
            assertEquals(1, player.brothersKilled)
            assertEquals(BarrowsProgress.BROTHER_POTENTIAL, player.rewardPotential)
        }

    /** The varbit the overlay prints saturates rather than overflowing. */
    @Test
    fun GameTestState.`reward potential saturates at the cap`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            repeat(200) { run.monsterDefeated(player) }
            for (brother in Brother.all) {
                player.withProtectedAccess { run.brotherDefeated(this, brother) }
            }
            assertEquals(BarrowsProgress.MAX_REWARD_POTENTIAL, player.rewardPotential)
        }

    @Test
    fun GameTestState.`the death handler credits the player who did the damage`() =
        runInjectedGameTest(BarrowsRun::class, null, BarrowsScript::class) { run ->
            player.resetRun()
            val brother = Brother.Verac
            player.coords = BarrowsMap.brotherSpawn(brother)
            raise(run, brother)

            val raised = checkNotNull(liveBrother(run, brother))
            raised.heroPoints(player, points = raised.hitpoints)
            raised.queueDeath()
            advance(DEATH_SEQUENCE_TICKS)

            assertTrue(player.hasKilled(brother), "The kill was not credited.")
            assertNull(liveBrother(run, brother), "The brother is still standing.")
        }

    /** Six brothers and a looted chest have to leave the next trip looking untouched. */
    @Test
    fun GameTestState.`resetting a run clears every varbit it wrote`() = runGameTest {
        for (brother in Brother.all) {
            player.withProtectedAccess { player.hasKilled(brother) }
        }
        player.rewardPotential = BarrowsProgress.MAX_REWARD_POTENTIAL
        player.chestOpen = true

        player.resetRun()

        assertEquals(0, player.brothersKilled)
        assertEquals(0, player.rewardPotential)
        assertTrue(!player.chestOpen)
    }

    private fun GameTestScope.raise(run: BarrowsRun, brother: Brother): BarrowsRun.Raise {
        var result: BarrowsRun.Raise? = null
        player.withProtectedAccess { result = run.raise(this, brother) }
        advance(1)
        return checkNotNull(result) { "The raise never ran; is the player access-protected?" }
    }

    private fun liveBrother(run: BarrowsRun, brother: Brother): Npc? =
        run.findLiveBrother(brother, BarrowsMap.brotherSpawn(brother))

    /** Leaves the shared world as it was found, since these methods run against one map. */
    private fun GameTestScope.cleanUp(run: BarrowsRun, brother: Brother) {
        val npc = liveBrother(run, brother) ?: return
        npcRepo.del(npc, duration = 0)
        advance(1)
        assertNull(
            run.findLiveBrother(brother, BarrowsMap.brotherSpawn(brother)),
            "${barrows_npcs.brothers[brother]} outlived its test.",
        )
    }

    private companion object {
        /** Long enough for the death walk, animation and despawn. */
        const val DEATH_SEQUENCE_TICKS = 8
    }
}
