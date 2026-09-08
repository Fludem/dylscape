package org.rsmod.content.interfaces.prayer.tab.scripts

import com.google.inject.AbstractModule
import com.google.inject.Provides
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.stat.prayerLvl
import org.rsmod.api.player.ui.IfOverlayButton
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.gameframe.script.GameframeScript
import org.rsmod.content.interfaces.prayer.tab.Prayer
import org.rsmod.content.interfaces.prayer.tab.PrayerRepository
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.enums.EnumTypeMapResolver
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Runs single-threaded on purpose: `integration-test-suite` defaults to concurrent methods, and
 * these tests advance the shared map clock.
 */
@Execution(ExecutionMode.SAME_THREAD)
class PrayerTabScriptTest {
    @Test
    fun GameTestState.`clicking steel skin enables it and drains prayer points`() =
        runInjectedGameTest(
            PrayerRepository::class,
            PrayerRepositoryModule,
            PrayerTabScript::class,
            PrayerDrainScript::class,
        ) { repo ->
            player.stats[stats.prayer] = 99
            eventBus.publish(SessionStateEvent.Login(player))
            advance(1)

            val (component, prayer) = repo.entry("Steel Skin")
            clickPrayer(component)

            assertEquals(1, player.vars[prayer.enabled]) {
                "Steel Skin varbit was not enabled by the component click."
            }
            assertNotEquals(0, player.vars[varbits.enabled_prayers]) {
                "`prayer_allactive` did not reflect the enabled prayer."
            }

            advance(20)

            assertTrue(player.prayerLvl < 99) {
                "Prayer points did not drain after 20 ticks of Steel Skin."
            }
        }

    /**
     * Measures how long each prayer actually takes to burn one prayer point.
     *
     * Drain is driven by each prayer's `drain_effect`, and the spread is wide: Thick Skin is 1
     * while Protect from Melee is 12. A prayer that looks like it "never drains" next to an
     * overhead may simply be twelve times slower.
     */
    @Test
    fun GameTestState.`measure ticks to drain one prayer point`() =
        runInjectedGameTest(
            PrayerRepository::class,
            PrayerRepositoryModule,
            PrayerTabScript::class,
            PrayerDrainScript::class,
        ) { repo ->
            for (name in listOf("Thick Skin", "Rock Skin", "Steel Skin", "Protect from Melee")) {
                val (component, prayer) = repo.entry(name)

                player.stats[stats.prayer] = 99
                eventBus.publish(SessionStateEvent.Login(player))
                advance(1)

                clickPrayer(component)
                assertEquals(1, player.vars[prayer.enabled]) { "$name did not enable." }

                var ticks = 0
                while (player.prayerLvl == 99 && ticks < 500) {
                    advance(1)
                    ticks++
                }
                println(
                    "DRAIN $name drainEffect=${prayer.drainEffect} " +
                        "resistance=${player.vars[varbits.prayer_drain_resistance]} " +
                        "ticksToFirstPoint=$ticks (${"%.1f".format(ticks * 0.6)}s)"
                )
                assertTrue(ticks in 1..499) { "$name never drained a point within 500 ticks." }

                clickPrayer(component)
                assertEquals(0, player.vars[prayer.enabled]) { "$name did not disable." }
            }
        }

    /**
     * Guards the two checks `If3ButtonHandler` makes before it publishes anything, both of which
     * `return` without a trace when they fail.
     *
     * The client toggles the prayer varbit itself in `[clientscript,prayer_op]`, so a prayer that
     * lights up in the book proves only that the client ran its own script. If either of these
     * gates is shut, the server never sees the click and the prayer silently never drains.
     */
    @Test
    fun GameTestState.`prayer clicks pass both if3button gates`() =
        runInjectedGameTest(
            PrayerRepository::class,
            PrayerRepositoryModule,
            GameframeScript::class,
            PrayerTabScript::class,
        ) { repo ->
            eventBus.publish(SessionStateEvent.Initialize(player))
            advance(1)

            // Gate 2: If3ButtonHandler only dispatches when the interface is open server-side.
            assertTrue(player.ui.containsOverlay(interfaces.prayerbook)) {
                "`prayerbook` is not an open overlay, so every prayer click is dropped."
            }

            // Gate 1: comsub is -1 for these components, so the static cache events are checked.
            for ((component, prayer) in repo.prayerComponents) {
                val unpacked = cacheTypes.components.getValue(component.packed)
                assertTrue(unpacked.hasEvent(IfEvent.Op1)) {
                    "`${unpacked.internalName}` (${prayer.name}) has no static Op1 event, " +
                        "so its click never reaches the server."
                }
            }
        }

    /**
     * The production binding lives in [org.rsmod.content.interfaces.prayer.tab.PrayerModule], which
     * the test injector does not load. Mirror it here so the scripts can be constructed.
     */
    private object PrayerRepositoryModule : AbstractModule() {
        @Provides
        @Singleton
        fun repository(enumResolver: EnumTypeMapResolver, objTypes: ObjTypeList): PrayerRepository =
            PrayerRepository(enumResolver, objTypes).apply { load() }
    }

    private fun GameTestScope.clickPrayer(component: ComponentType) {
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
    }

    private fun PrayerRepository.entry(name: String): Pair<ComponentType, Prayer> {
        val entry =
            prayerComponents.entries.firstOrNull { it.value.name.equals(name, ignoreCase = true) }
        checkNotNull(entry) {
            "No prayer named `$name`. Loaded: ${prayerComponents.values.map(Prayer::name)}"
        }
        return entry.key to entry.value
    }
}
