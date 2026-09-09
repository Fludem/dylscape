package org.rsmod.content.skills.slayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.slayer.configs.SlayerNpcs
import org.rsmod.content.skills.slayer.data.SlayerTaskRepository

/**
 * Proves the cache's own slayer database reads back correctly.
 *
 * These assertions are about *content*, not merely about the code running. The whole skill rests on
 * the claim that npc param `slayer_task` holds a `slayer_task:id`, and that claim was inferred from
 * the data rather than read off a Jagex name - the param is unnamed in the cache. If it is ever
 * wrong it should be wrong here, loudly, rather than silently in a player's task counter.
 */
class SlayerTaskRepositoryTest {
    @Test
    fun GameTestState.`tasks load with their requirements`() =
        runInjectedGameTest(SlayerTaskRepository::class) { repo ->
            repo.load()

            val abyssal = repo.task(ABYSSAL_DEMONS)
            assertNotNull(abyssal, "Task $ABYSSAL_DEMONS (Abyssal Demons) is missing.")
            assertEquals("Abyssal Demons", abyssal!!.displayName)
            assertEquals(85, abyssal.slayerLevel)

            assertEquals(75, repo.task(GARGOYLES)?.slayerLevel, "Gargoyles need 75 slayer.")
            assertEquals(70, repo.task(KURASK)?.slayerLevel, "Kurask need 70 slayer.")
            assertEquals(90, repo.task(DARK_BEASTS)?.slayerLevel, "Dark beasts need 90 slayer.")
        }

    /**
     * The load-bearing inference, asserted by name so a wrong param reads as a wrong monster rather
     * than as a number nobody can check.
     */
    @Test
    fun GameTestState.`the npc param maps monsters onto their task`() =
        runInjectedGameTest(SlayerTaskRepository::class) { repo ->
            repo.load()
            val expected =
                mapOf(
                    415 to "Abyssal Demons",
                    412 to "Gargoyles",
                    410 to "Kurask",
                    7250 to "Dark Beasts",
                    498 to "Smoke Devils",
                )
            for ((npcId, taskName) in expected) {
                val taskId = repo.taskIdForNpc(npcId)
                assertTrue(taskId != 0, "Npc $npcId carries no slayer task.")
                assertEquals(taskName, repo.task(taskId)?.displayName, "Npc $npcId maps wrong.")
            }
        }

    @Test
    fun GameTestState.`a monster that is nobody's task reads as no task`() =
        runInjectedGameTest(SlayerTaskRepository::class) { repo ->
            repo.load()
            // A slayer master is not a slayer monster. Zero is the param's own cache default, which
            // is what lets "not a task monster" need no sentinel of ours.
            assertEquals(0, repo.taskIdForNpc(13618), "Turael should not count toward a task.")
        }

    @Test
    fun GameTestState.`every master has a weighted task list`() =
        runInjectedGameTest(SlayerTaskRepository::class) { repo ->
            repo.load()
            for ((masterId, npc) in SlayerNpcs.byMasterId) {
                val assignments = repo.assignments(masterId)
                assertTrue(assignments.isNotEmpty(), "Master $masterId ($npc) assigns nothing.")
                assertTrue(
                    assignments.all { it.weight > 0 },
                    "Master $masterId has a zero-weight assignment.",
                )
                assertTrue(
                    assignments.all { it.maxAmount >= it.minAmount || it.maxAmount <= 0 },
                    "Master $masterId has an inverted amount range.",
                )
            }
        }

    /**
     * A few rows name tasks nothing in this rev counts for. They are excluded from assignment
     * rather than asserted away, because a player handed one could never finish it. This test pins
     * the list so it is noticed if it grows.
     */
    @Test
    fun GameTestState.`only the known tasks lack an npc`() =
        runInjectedGameTest(SlayerTaskRepository::class) { repo ->
            repo.load()
            val unmapped =
                SlayerNpcs.byMasterId.keys
                    .flatMap(repo::assignments)
                    .map { it.task }
                    .distinct()
                    .filterNot { repo.hasNpcs(it.id) }
                    .map { it.displayName }
                    .toSet()
            assertEquals(
                setOf("Brine Rats", "Fever Spiders", "Boss", "Metal Dragons", "Araxytes"),
                unmapped,
                "The set of unfinishable tasks changed.",
            )
        }

    private companion object {
        const val ABYSSAL_DEMONS = 42
        const val GARGOYLES = 46
        const val KURASK = 45
        const val DARK_BEASTS = 66
    }
}
