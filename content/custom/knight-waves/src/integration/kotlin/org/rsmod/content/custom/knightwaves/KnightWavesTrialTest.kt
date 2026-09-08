package org.rsmod.content.custom.knightwaves

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.trialComplete
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.wavesCleared
import org.rsmod.content.custom.knightwaves.configs.KnightWavesArena
import org.rsmod.content.custom.knightwaves.configs.knightwaves_npcs
import org.rsmod.content.custom.knightwaves.scripts.KnightWavesScript
import org.rsmod.game.entity.Npc
import org.rsmod.game.region.Region

/**
 * Runs single-threaded on purpose: `integration-test-suite` defaults to concurrent methods, and
 * these advance the shared map clock and allocate regions.
 */
@Execution(ExecutionMode.SAME_THREAD)
class KnightWavesTrialTest {
    /**
     * The trial stores its progress in the same varbit the prayer book gates on, counting up to
     * `8`. An overflow here would not misbehave quietly - `VarPlayerIntMap.assertVarBitBounds`
     * throws, and an exception on the reward path force-disconnects the player instead of logging.
     */
    @Test
    fun GameTestState.`kr_knightwaves_state holds every value the trial writes`() = runGameTest {
        for (value in 0..KnightWavesProgress.COMPLETE) {
            player.setVarBit(varbits.kr_knightwaves_state, value)
            assertEquals(value, player.vars[varbits.kr_knightwaves_state])
        }
    }

    @Test
    fun GameTestState.`entering the arena spawns the first knight`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            val region = enterArena(trial)

            val knight = knightOnFloor(region)
            assertNotNull(knight) { "No knight was spawned in the arena." }
            assertEquals(
                npcTypes[knightwaves_npcs.waves[0]].id,
                knight?.id,
                "First wave should be the first knight in the ladder.",
            )
        }

    /**
     * The knights are the reason `wanderRange` is set to `0`. Every npc roams five tiles from its
     * spawn by default, and `NpcExtensions.retaliate` sends any npc with a non-zero wander range
     * jogging home the moment a fight drifts past its leash - which in a duel means it disengages.
     */
    @Test
    fun GameTestState.`knights hold their ground and charge on sight`() = runBasicGameTest {
        for (knight in knightwaves_npcs.waves) {
            val type = cacheTypes.npcs.getValue(knight.id)
            assertEquals(0, type.wanderRange) { "${type.name} would wander off, or flee home." }
            assertTrue(type.huntRange > 0) { "${type.name} will never notice the player." }
            assertNotNull(type.huntMode) { "${type.name} has no hunt mode, so it never charges." }
        }
    }

    @Test
    fun GameTestState.`beating a knight advances the wave`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            val region = enterArena(trial)

            val first = knightOnFloor(region)
            assertNotNull(first)
            defeat(first!!)

            assertEquals(1, player.wavesCleared) { "The kill did not count." }
            val second = knightOnFloor(region)
            assertNotNull(second) { "The next wave did not appear." }
            assertEquals(npcTypes[knightwaves_npcs.waves[1]].id, second?.id)
        }

    @Test
    fun GameTestState.`the sixth knight completes the trial`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            player.stats[stats.defence] = 70
            player.stats[stats.prayer] = 70
            val region = enterArena(trial)

            repeat(KnightWavesProgress.TOTAL_WAVES) { wave ->
                val knight = knightOnFloor(region)
                assertNotNull(knight) { "Wave ${wave + 1} never appeared." }
                assertEquals(
                    npcTypes[knightwaves_npcs.waves[wave]].id,
                    knight?.id,
                    "Wave ${wave + 1} was the wrong knight.",
                )
                defeat(knight!!)
            }

            assertTrue(player.trialComplete) {
                "Six kills left the state at ${player.vars[varbits.kr_knightwaves_state]}."
            }
            assertEquals(1, player.vars[varbits.preserve_unlocked]) { "Preserve was not taught." }
            assertTrue(player.statMap.getXP(stats.defence) > 0.0) { "No Defence xp was paid." }
            assertTrue(player.statMap.getXP(stats.prayer) > 0.0) { "No Prayer xp was paid." }
            assertTrue(knightOnFloor(region) == null) { "A seventh knight was spawned." }
        }

    /** Progress lives in a saved varbit, so a player who leaves picks up where they stopped. */
    @Test
    fun GameTestState.`a part-finished trial resumes at the next knight`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            player.setVarBit(varbits.kr_knightwaves_state, 3)
            val region = enterArena(trial)

            val knight = knightOnFloor(region)
            assertNotNull(knight) { "Nothing spawned for the resumed trial." }
            assertEquals(
                npcTypes[knightwaves_npcs.waves[3]].id,
                knight?.id,
                "Resuming after three waves should start the fourth knight.",
            )
        }

    private fun GameTestScope.enterArena(trial: KnightWavesTrial): Region {
        val region = createRegion(KnightWavesArena.template)
        player.telejump(region.normal[KnightWavesArena.entrance])
        advance(1)
        player.withProtectedAccess { trial.spawnKnight(region.arenaTile, player.wavesCleared) }
        advance(1)
        return region
    }

    /** Kills [knight] with the player credited, the way the trial's death handler expects. */
    private fun GameTestScope.defeat(knight: Npc) {
        knight.heroPoints(player, points = knight.hitpoints)
        knight.queueDeath()
        advance(DEATH_SEQUENCE_TICKS)
    }

    private val Region.arenaTile
        get() = normal[KnightWavesArena.knightSpawn]

    private fun GameTestScope.knightOnFloor(region: Region): Npc? =
        npcRepo.findAll(region.arenaTile).firstOrNull()

    private companion object {
        /** Long enough for the death walk, animation and despawn, plus the next spawn. */
        const val DEATH_SEQUENCE_TICKS = 8
    }
}
