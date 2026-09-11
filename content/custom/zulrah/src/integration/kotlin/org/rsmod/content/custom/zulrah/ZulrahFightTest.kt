package org.rsmod.content.custom.zulrah

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.npc.queueCombatRetaliate
import org.rsmod.api.combat.scripts.NpcRetaliateScript
import org.rsmod.api.combat.scripts.NvPCombatScript
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.refs.stats
import org.rsmod.api.death.plugin.NpcDeathScript
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.api.toxins.Toxins
import org.rsmod.api.toxins.ToxinsScript
import org.rsmod.content.custom.zulrah.configs.ZulrahShrine
import org.rsmod.content.custom.zulrah.configs.ZulrahVarps
import org.rsmod.content.custom.zulrah.configs.zulrah_npcs
import org.rsmod.content.custom.zulrah.scripts.ZulrahScript
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.entity.player.SessionStateEvent

/** See `DagannothCombatModule`: the accuracy formulae need both weapon singletons scoped. */
private object ZulrahCombatModule : AbstractModule() {
    override fun configure() {
        bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
    }
}

class ZulrahTestDeps @Inject constructor(val fight: ZulrahFight, val toxins: Toxins)

/**
 * The fight, driven through its real entry point.
 *
 * Most tests put the fight straight into the state they care about - a magma phase, a cloud under
 * the player - rather than playing the rotation up to it, because the rotation is random-free but
 * long, and a test that waits through four phases for one whip is mostly testing the wait.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ZulrahFightTest {
    @Test
    fun GameTestState.`boarding raises zulrah in a private shrine`() = zulrahTest { deps ->
        val state = enter(deps)

        assertEquals(state.region.normal[ZulrahShrine.arrival], player.coords) {
            "The player was not moved onto the platform."
        }
        val middle = state.region.normal[ZulrahShrine.spots.getValue(ZulrahSpot.Middle)]
        assertEquals(middle, state.npc.coords) { "Zulrah did not rise in the middle." }
        assertEquals(npcTypes[zulrah_npcs.serpentine].id, state.npc.visType.id)
        deps.fight.end(state)
    }

    @Test
    fun GameTestState.`a dive changes form and keeps the damage already done`() =
        zulrahTest { deps ->
            val state = enter(deps)
            state.npc.hitpoints = WOUNDED

            var ticks = 0
            while (!(state.phaseIndex == 1 && state.stage == ZulrahStage.Acting) && ticks < 120) {
                // Phase one is all clouds; they are not what this test is about.
                state.pending.clear()
                state.clouds.clear()
                advance(1)
                ticks++
            }

            assertEquals(1, state.phaseIndex) { "The first phase never ended." }
            assertEquals(npcTypes[zulrah_npcs.magma].id, state.npc.visType.id) {
                "Zulrah should surface in its magma form for phase two."
            }
            assertEquals(WOUNDED, state.npc.hitpoints) { "The form change healed Zulrah." }
            deps.fight.end(state)
        }

    @Test
    fun GameTestState.`zulrah never retaliates when hit`() = zulrahTest { deps ->
        val state = enter(deps)
        state.npc.queueCombatRetaliate(player)
        advance(3)

        assertTrue(state.npc.mode != NpcMode.OpPlayer2 && state.npc.mode != NpcMode.ApPlayer2) {
            "Zulrah picked the player up as a target: ${state.npc.mode}"
        }
        deps.fight.end(state)
    }

    @Test
    fun GameTestState.`the tail whip lands on a player who stays put`() = zulrahTest { deps ->
        val state = enter(deps)
        primeMagma(state)
        val before = player.hitpoints

        awaitWhip(state)
        advance(2)

        val taken = before - player.hitpoints
        assertTrue(taken in ZulrahFight.WHIP_MIN_HIT..ZulrahFight.WHIP_MAX_HIT) {
            "A whip that connects deals 20-30; the player took $taken."
        }
        deps.fight.end(state)
    }

    @Test
    fun GameTestState.`the tail whip misses a player who steps away`() = zulrahTest { deps ->
        val state = enter(deps)
        primeMagma(state)
        val before = player.hitpoints

        var ticks = 0
        while (state.whip == null && ticks++ < 10) {
            advance(1)
        }
        val whip = checkNotNull(state.whip) { "Zulrah never stared." }
        player.withProtectedAccess { telejump(whip.target.translateX(2)) }
        awaitWhip(state)
        advance(2)

        assertEquals(before, player.hitpoints) { "Two tiles should dodge the tail." }
        deps.fight.end(state)
    }

    @Test
    fun GameTestState.`standing in a toxic cloud envenoms`() = zulrahTest { deps ->
        val state = enter(deps)
        // Parked under water so nothing else happens while the cloud does its work.
        state.stage = ZulrahStage.Diving
        state.countdown = Int.MAX_VALUE
        state.clouds += ZulrahCloud(player.coords.translate(-1, -1), Int.MAX_VALUE)

        advance(2)

        assertTrue(deps.toxins.isVenomed(player)) { "The cloud did not envenom the player." }
        deps.toxins.clear(player)
        deps.fight.end(state)
    }

    @Test
    fun GameTestState.`venom hits 6 then 8, every 30 ticks`() = zulrahTest { deps ->
        deps.toxins.clear(player)
        deps.toxins.envenom(player)
        val before = player.hitpoints

        advance(2)
        assertEquals(before - 6, player.hitpoints) { "The first venom hit should be 6." }

        advance(27)
        val beforeSecond = player.hitpoints
        advance(3)
        assertEquals(beforeSecond - 8, player.hitpoints) { "The second venom hit should be 8." }
        deps.toxins.clear(player)
    }

    @Test
    fun GameTestState.`killing zulrah counts the kill and sends the player home`() =
        zulrahTest { deps ->
            val state = enter(deps)
            val kills = player.vars[ZulrahVarps.kills]
            // The death sequence drops loot owned by the hero, and an owned obj needs the
            // receiver's `observerUUID`. Production sets it on login; the harness player has none.
            player.observerUUID = 1L

            state.npc.heroPoints(player, points = state.npc.hitpoints)
            state.npc.queueDeath()
            advance(DEATH_SEQUENCE_TICKS)

            assertNull(deps.fight.fightOf(state.npc)) { "The fight outlived Zulrah." }
            assertEquals(kills + 1, player.vars[ZulrahVarps.kills]) { "The kill was not counted." }

            advance(RETURN_TICKS)
            assertEquals(ZulrahShrine.zulAndra, player.coords) {
                "The player was left in the shrine after the kill."
            }
        }

    @Test
    fun GameTestState.`logging out inside the shrine lands the player at zul-andra`() =
        zulrahTest { deps ->
            val state = enter(deps)

            eventBus.publish(SessionStateEvent.Logout(player))

            assertEquals(ZulrahShrine.zulAndra, player.coords) {
                "A player saved inside a region would log back in to nothing."
            }
            assertNull(deps.fight.fightOf(player)) { "The fight survived the logout." }
            assertTrue(!state.npc.isSlotAssigned) { "Zulrah was left behind in the shrine." }
        }

    private fun GameTestState.zulrahTest(body: GameTestScope.(ZulrahTestDeps) -> Unit) =
        runInjectedGameTest(
            ZulrahTestDeps::class,
            ZulrahCombatModule,
            ZulrahScript::class,
            ToxinsScript::class,
            PlayerHitScript::class,
            WeaponAttackStylesScript::class,
            NpcDeathScript::class,
            NvPCombatScript::class,
            NpcRetaliateScript::class,
        ) { deps ->
            player.ifClose()
            player.statMap.setBaseLevel(stats.hitpoints, 99)
            player.statMap.setCurrentLevel(stats.hitpoints, 99)
            body(deps)
        }

    /** Drives the real boat path's back half, so a broken template fails here and not in game. */
    private fun GameTestScope.enter(deps: ZulrahTestDeps): ZulrahFightState {
        var state: ZulrahFightState? = null
        player.withProtectedAccess { state = deps.fight.enter(this) }
        advance(1)
        return checkNotNull(state) { "The shrine refused to open." }
    }

    /** Skips straight to the first magma phase, with nothing left over from the clouds. */
    private fun primeMagma(state: ZulrahFightState) {
        state.phaseIndex = MAGMA_PHASE
        state.actionIndex = 0
        state.stage = ZulrahStage.Acting
        state.countdown = 1
        state.pending.clear()
        state.clouds.clear()
    }

    private fun GameTestScope.awaitWhip(state: ZulrahFightState) {
        var ticks = 0
        while (state.whip == null && ticks++ < 10) {
            advance(1)
        }
        ticks = 0
        while (state.whip != null && ticks++ < 10) {
            advance(1)
        }
    }

    private companion object {
        const val WOUNDED = 321
        const val MAGMA_PHASE = 1

        /** The death walk, animation and despawn. */
        const val DEATH_SEQUENCE_TICKS = 8

        /** `ZulrahScript`'s minute, and a little. */
        const val RETURN_TICKS = 105
    }
}
