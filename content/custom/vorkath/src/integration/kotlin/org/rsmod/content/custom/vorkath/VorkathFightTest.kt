package org.rsmod.content.custom.vorkath

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
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
import org.rsmod.content.custom.vorkath.configs.VorkathArena
import org.rsmod.content.custom.vorkath.configs.vorkath_npcs
import org.rsmod.content.custom.vorkath.configs.vorkath_varps
import org.rsmod.content.custom.vorkath.scripts.VorkathScript
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.map.CoordGrid

/** See `DagannothCombatModule`: the accuracy formulae need both weapon singletons scoped. */
private object VorkathCombatModule : AbstractModule() {
    override fun configure() {
        bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
    }
}

class VorkathFightDeps @Inject constructor(val attackStyles: AttackStyles)

/**
 * The fight, driven through its real entry point: a poke.
 *
 * Every test spawns the dragon on its real platform and stands the player in the real crater, so a
 * hit that lands here has also proved the platform's blocked tiles do not break line of sight - the
 * one geometry risk in putting an npc on ground the map marks as unwalkable.
 */
@Execution(ExecutionMode.SAME_THREAD)
class VorkathFightTest {
    @Test
    fun GameTestState.`a poke swaps the sleeper for a fighting dragon aimed at the poker`() =
        fightTest {
            val sleeper = spawnSleeping()
            val awake = poke(sleeper)

            assertFalse(sleeper.isSlotAssigned, "The sleeper is still on the map.")
            assertEquals(VorkathArena.spawn, awake.coords, "The dragon woke off its platform.")
            assertEquals(AWAKE_HITPOINTS, awake.hitpoints)
            assertEquals(NpcMode.ApPlayer2, awake.mode, "Vorkath is awake but not fighting.")
        }

    @Test
    fun GameTestState.`the first shot waits for the wake-up and then the projectile`() = fightTest {
        val sleeper = spawnSleeping()
        val before = player.hitpoints
        forceRanged()
        poke(sleeper)

        // Still rearing up: nothing fired yet.
        advance(2)
        assertEquals(before, player.hitpoints, "Vorkath hit before it had finished waking.")

        advance(FIRST_HIT_WINDOW)
        assertTrue(player.hitpoints < before, "Vorkath never connected.")
    }

    @Test
    fun GameTestState.`ranged hits for the derived thirty two and magic for the authored thirty`() =
        fightTest {
            val sleeper = spawnSleeping()
            forceRanged()
            poke(sleeper)
            assertEquals(RANGED_MAX_HIT, awaitDamage(RANGED_MAX_HIT), "Ranged max hit.")

            // Armed only now: the landing tick draws from the random source too (the player's
            // retaliation), and values queued before it are eaten before the next attack rolls.
            player.stats[stats.hitpoints] = FULL_HEALTH
            forceMagic()
            assertEquals(MAGIC_MAX_HIT, awaitDamage(MAGIC_MAX_HIT), "Magic max hit.")
        }

    @Test
    fun GameTestState.`a kill is counted and the sleeper returns to the platform`() = fightTest {
        val sleeper = spawnSleeping()
        val awake = poke(sleeper)
        val kills = player.vars[vorkath_varps.kills]
        // The death sequence credits whoever holds the most hero points, and drops loot owned by
        // them; an owned obj needs the receiver's `observerUUID`, which production sets on login.
        awake.heroPoints(player, 1)
        player.observerUUID = 1L

        awake.queueDeath()
        advance(DEATH_SEQUENCE_TICKS)
        assertEquals(kills + 1, player.vars[vorkath_varps.kills], "The kill was not counted.")
        assertFalse(awake.isSlotAssigned, "The dead dragon is still on the map.")
        assertFalse(sleeper.isSlotAssigned, "The sleeper came back before the pause.")

        advance(RESPAWN_TICKS)
        assertTrue(sleeper.isSlotAssigned, "The sleeper never came back.")
        assertEquals(VorkathArena.spawn, sleeper.coords, "The sleeper came back off the platform.")
        assertEquals(SLEEPING_HITPOINTS, sleeper.hitpoints)
    }

    @Test
    fun GameTestState.`an awake dragon nobody is fighting goes back to sleep`() = fightTest {
        val sleeper = spawnSleeping()
        val awake = poke(sleeper)
        // Leave the island entirely, so nothing keeps either side "in combat".
        player.telejump(VorkathArena.rellekkaArrival)

        advance(SLEEP_CHECK_TICKS + 2)
        assertFalse(awake.isSlotAssigned, "Still awake.")
        assertTrue(sleeper.isSlotAssigned, "The sleeper never came back.")
        assertEquals(VorkathArena.spawn, sleeper.coords)
    }

    @Test
    fun GameTestState.`torfinn sails the player across`() = fightTest {
        val torfinn =
            spawnNpc(VorkathArena.torfinnRellekka, npcTypes[vorkath_npcs.torfinn_rellekka])
        player.telejump(VorkathArena.rellekkaArrival)

        player.opNpc3(torfinn)
        advance(3)
        assertEquals(VorkathArena.ungaelArrival, player.coords, "The boat never left Rellekka.")
    }

    private fun GameTestState.fightTest(body: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            VorkathFightDeps::class,
            VorkathCombatModule,
            VorkathScript::class,
            NvPCombatScript::class,
            NpcRetaliateScript::class,
            WeaponAttackStylesScript::class,
            // Applies queued hits to the player; without it damage is rolled and then dropped.
            PlayerHitScript::class,
            // The real death sequence, which publishes `Killed` and deletes the dead npc.
            NpcDeathScript::class,
        ) { _ ->
            // "In combat" is `lastcombat + combat_activecombat_delay >= mapClock`, which is true
            // for an untouched player while the clock sits near zero, and the single-way guard
            // then refuses every attack. A live server never sees this.
            advance(CLOCK_WARMUP)
            player.ifClose()
            player.statMap.setBaseLevel(stats.hitpoints, FULL_HEALTH.toByte())
            player.statMap.setCurrentLevel(stats.hitpoints, FULL_HEALTH.toByte())
            body()
        }

    /** The sleeping dragon on its real platform. */
    private fun GameTestScope.spawnSleeping(): Npc =
        spawnNpc(VorkathArena.spawn, npcTypes[vorkath_npcs.sleeping])

    /**
     * Stands the player against the platform and pokes, letting the interaction resolve, then
     * returns the fighting dragon that took the sleeper's place.
     */
    private fun GameTestScope.poke(sleeper: Npc): Npc {
        player.telejump(POKE_TILE)
        player.opNpc1(sleeper)
        advance(2)
        return checkNotNull(
            npcRepo.findAll(VorkathArena.spawn).firstOrNull { it.id == vorkath_npcs.awake.id }
        ) {
            "No awake Vorkath on the platform after the poke."
        }
    }

    /**
     * Advances a tick at a time until the player has taken at least [total] damage, or a full
     * attack cycle has passed with nothing landing. Returns what was actually taken, so a hit that
     * overshoots fails on the number rather than on a timeout.
     */
    private fun GameTestScope.awaitDamage(total: Int): Int {
        var ticks = 0
        while (FULL_HEALTH - player.hitpoints < total && ticks++ < FIRST_HIT_WINDOW) {
            advance(1)
        }
        return FULL_HEALTH - player.hitpoints
    }

    /**
     * Forces the next attack: the style coin flip first, then the accuracy roll (a zero always
     * passes), then the damage roll.
     */
    private fun GameTestScope.forceRanged() {
        random.next = 1
        random.then = 0
        random.then = RANGED_MAX_HIT
    }

    private fun GameTestScope.forceMagic() {
        random.next = 0
        random.then = 0
        random.then = MAGIC_MAX_HIT
    }

    private companion object {
        /** Open floor directly south of the platform, in reach of a poke. */
        val POKE_TILE = CoordGrid(2272, 4061, 0)

        const val CLOCK_WARMUP = 16
        const val FULL_HEALTH = 99
        const val AWAKE_HITPOINTS = 750
        const val SLEEPING_HITPOINTS = 10
        const val RANGED_MAX_HIT = 32
        const val MAGIC_MAX_HIT = 30

        /** `VorkathScript.WAKE_TICKS` plus a generous projectile flight. */
        const val FIRST_HIT_WINDOW = 10
        const val ATTACK_RATE = 5
        const val PROJECTILE_FLIGHT_TICKS = 6
        const val DEATH_SEQUENCE_TICKS = 10

        /** `VorkathScript.RESPAWN_TICKS` plus the tick the delayed add lands on. */
        const val RESPAWN_TICKS = 52

        /** `VorkathScript.SLEEP_CHECK_TICKS`. */
        const val SLEEP_CHECK_TICKS = 50
    }
}
