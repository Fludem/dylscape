package org.rsmod.content.custom.knightwaves

import com.google.inject.AbstractModule
import com.google.inject.Provides
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.knightwaves.configs.KnightWavesArena
import org.rsmod.content.custom.knightwaves.scripts.KnightWavesScript
import org.rsmod.content.interfaces.prayer.tab.Prayer
import org.rsmod.content.interfaces.prayer.tab.PrayerRepository
import org.rsmod.content.interfaces.prayer.tab.scripts.PrayerTabScript
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Closes the loop this module exists for: the prayer book, not the trial, is the thing that was
 * broken.
 *
 * Chivalry and Piety were never unimplemented. `PrayerTabObjs` already declares both with `unlock =
 * kr_knightwaves_state, unlockState = 8`, and both the client and `Prayer` refuse them below that.
 * Nothing in the repo ever wrote the varbit, so it sat at `0` for every account and the prayers
 * were permanently grey. These tests assert the trial is what changes that.
 */
@Execution(ExecutionMode.SAME_THREAD)
class KnightWavesUnlockTest {
    @Test
    fun GameTestState.`chivalry and piety are locked until the trial is finished`() =
        runInjectedGameTest(PrayerRepository::class, PrayerRepositoryModule) { repo ->
            player.stats[stats.defence] = 99
            player.stats[stats.prayer] = 99

            for (name in KNIGHT_WAVE_PRAYERS) {
                val prayer = repo.byName(name)
                assertFalse(prayer.hasAllRequirements(player)) {
                    "$name is available without the trial; the unlock gate does nothing."
                }
            }
        }

    @Test
    fun GameTestState.`finishing the trial unlocks chivalry and piety`() =
        runInjectedGameTest(PrayerRepository::class, PrayerRepositoryModule) { repo ->
            player.stats[stats.defence] = 99
            player.stats[stats.prayer] = 99
            player.setVarBit(varbits.kr_knightwaves_state, KnightWavesProgress.COMPLETE)

            for (name in KNIGHT_WAVE_PRAYERS) {
                val prayer = repo.byName(name)
                assertTrue(prayer.hasAllRequirements(player)) {
                    "$name is still locked after the trial."
                }
            }
        }

    /**
     * Requirements are checked twice on the way in - once before protected access is taken and once
     * inside - so passing [Prayer.hasAllRequirements] is not by itself proof that a click works.
     */
    @Test
    fun GameTestState.`piety can actually be switched on after the trial`() =
        runInjectedGameTest(
            PrayerRepository::class,
            PrayerRepositoryModule,
            PrayerTabScript::class,
        ) { repo ->
            player.stats[stats.defence] = 99
            player.stats[stats.prayer] = 99
            player.setVarBit(varbits.kr_knightwaves_state, KnightWavesProgress.COMPLETE)
            advance(1)

            val (component, prayer) = repo.entry("Piety")
            eventBus.publish(
                IfOverlayButton(
                    player = player,
                    component = component,
                    comsub = -1,
                    obj = null,
                    op = IfButtonOp.Op1,
                )
            )
            advance(1)

            assertEquals(1, player.vars[prayer.enabled]) {
                "Clicking Piety after the trial did not enable it."
            }
        }

    /** Preserve has its own unlock varbit and is the trial's third reward. */
    @Test
    fun GameTestState.`the trial also teaches preserve`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            player.stats[stats.prayer] = 99
            assertEquals(0, player.vars[varbits.preserve_unlocked])

            player.withProtectedAccess { trial.complete(this) }
            advance(1)

            assertEquals(1, player.vars[varbits.preserve_unlocked]) {
                "Preserve was not unlocked by finishing the trial."
            }
        }

    /** The reward sends the player back out of the instance, or they are stranded in it. */
    @Test
    fun GameTestState.`the reward returns the player to the squire`() =
        runInjectedGameTest(KnightWavesTrial::class, null, KnightWavesScript::class) { trial ->
            val region = createRegion(KnightWavesArena.template)
            player.telejump(region.normal[KnightWavesArena.entrance])
            advance(1)

            player.withProtectedAccess { trial.complete(this) }
            advance(1)

            assertEquals(KnightWavesArena.squireCoords, player.coords) {
                "The player was left inside the arena after finishing."
            }
        }

    /**
     * The production binding lives in `PrayerModule`, which the test injector does not load. Mirror
     * it here so the prayer scripts can be constructed.
     */
    private object PrayerRepositoryModule : AbstractModule() {
        @Provides
        @Singleton
        fun repository(enumResolver: EnumTypeMapResolver, objTypes: ObjTypeList): PrayerRepository =
            PrayerRepository(enumResolver, objTypes).apply { load() }
    }

    private fun PrayerRepository.byName(name: String): Prayer = entry(name).second

    private fun PrayerRepository.entry(name: String): Pair<ComponentType, Prayer> {
        val entry =
            prayerComponents.entries.firstOrNull { it.value.name.equals(name, ignoreCase = true) }
        checkNotNull(entry) {
            "No prayer named `$name`. Loaded: ${prayerComponents.values.map(Prayer::name)}"
        }
        return entry.key to entry.value
    }

    private companion object {
        val KNIGHT_WAVE_PRAYERS = listOf("Chivalry", "Piety")
    }
}
