package org.rsmod.content.skills.slayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.slayer.SlayerProgress.slayerAssigned
import org.rsmod.content.skills.slayer.SlayerProgress.slayerRemaining
import org.rsmod.content.skills.slayer.SlayerProgress.slayerTask
import org.rsmod.content.skills.slayer.scripts.SlayerKillScript
import org.rsmod.content.skills.slayer.scripts.SlayerRepositoryScript
import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

/**
 * Kill credit, driven through the real event.
 *
 * The point of publishing `NpcDeathEvents.Killed` rather than calling the handler is that the
 * wiring is the risky part: the event had to be added upstream precisely because the death *queue*
 * cannot take a second handler, and a test that bypassed the bus would still pass if the
 * subscription were never registered.
 *
 * Single-threaded because every method advances the same shared world.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SlayerKillCreditTest {
    @Test
    fun GameTestState.`a kill on task pays slayer xp and decrements the counter`() =
        runGameTest(SlayerRepositoryScript::class, SlayerKillScript::class) {
            player.stats[stats.slayer] = 99
            player.slayerTask = ABYSSAL_DEMONS
            player.slayerRemaining = 10
            player.slayerAssigned = 10
            val startXp = player.statMap.getXP(stats.slayer)

            killAbyssalDemon()

            assertEquals(9, player.slayerRemaining, "The task counter did not go down.")
            assertTrue(player.statMap.getXP(stats.slayer) > startXp) { "No slayer xp was paid." }
        }

    /**
     * The rule that stops Slayer training itself as a side effect of ordinary combat: an abyssal
     * demon killed while something else is assigned is worth combat experience and nothing more.
     */
    @Test
    fun GameTestState.`a kill off task pays nothing`() =
        runGameTest(SlayerRepositoryScript::class, SlayerKillScript::class) {
            player.stats[stats.slayer] = 99
            player.slayerTask = GARGOYLES
            player.slayerRemaining = 10
            val startXp = player.statMap.getXP(stats.slayer)

            killAbyssalDemon()

            assertEquals(10, player.slayerRemaining, "An off-task kill decremented the counter.")
            assertEquals(
                startXp,
                player.statMap.getXP(stats.slayer),
                "An off-task kill paid slayer xp.",
            )
        }

    @Test
    fun GameTestState.`a kill with no task at all pays nothing`() =
        runGameTest(SlayerRepositoryScript::class, SlayerKillScript::class) {
            player.stats[stats.slayer] = 99
            player.slayerTask = SlayerProgress.NO_TASK
            player.slayerRemaining = 0
            val startXp = player.statMap.getXP(stats.slayer)

            killAbyssalDemon()

            assertEquals(startXp, player.statMap.getXP(stats.slayer), "Paid xp with no task.")
        }

    @Test
    fun GameTestState.`the counter never goes below zero`() =
        runGameTest(SlayerRepositoryScript::class, SlayerKillScript::class) {
            player.stats[stats.slayer] = 99
            player.slayerTask = ABYSSAL_DEMONS
            player.slayerRemaining = 1
            player.slayerAssigned = 1

            killAbyssalDemon()
            killAbyssalDemon()

            assertEquals(0, player.slayerRemaining, "The counter went past zero.")
        }

    /** Publishes the kill the same way `NpcDeath` does, with this player as the hero. */
    private fun GameTestScope.killAbyssalDemon() {
        val type = npcTypes.getValue(ABYSSAL_DEMON_NPC)
        val coords = CoordGrid(0, 50, 50, 32, 32)
        allocZoneCollision(coords)
        val npc: Npc = spawnNpc(coords, type)
        eventBus.publish(NpcDeathEvents.Killed(npc, player))
    }

    private companion object {
        const val ABYSSAL_DEMONS = 42
        const val GARGOYLES = 46
        const val ABYSSAL_DEMON_NPC = 415
    }
}
