package org.rsmod.content.custom.dagannothkings

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
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
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.custom.dagannothkings.configs.DagannothKingsLair
import org.rsmod.content.custom.dagannothkings.configs.dk_npcs
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.map.CoordGrid

/**
 * Binds the two weapon singletons the accuracy formulae depend on.
 *
 * `WeaponModule` is not one of the modules the test injector installs, so without this both
 * `AttackStyles` and `AttackTypes` are JIT-bound and *unscoped*: `WeaponAttackStylesScript` would
 * initialise its own copy while the formulae read a different, empty one, and the first roll dies
 * on an uninitialised lateinit.
 */
private object DagannothCombatModule : AbstractModule() {
    override fun configure() {
        bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
        bind(AttackTypes::class.java).`in`(Scopes.SINGLETON)
    }
}

class DagannothCombatDeps @Inject constructor(val attackStyles: AttackStyles)

/**
 * The half of the fight that only shows up once something is swinging.
 *
 * Everything here exercises the npc combat driver in `api/combat`, which before this module could
 * only ever build a melee attack -- so these are as much a test of that driver as of the kings. The
 * hit *timing* is the load-bearing part: a ranged or magic attack has to land on the tick its
 * projectile arrives, and getting that wrong is invisible to any config assertion.
 */
@Execution(ExecutionMode.SAME_THREAD)
class DagannothKingsCombatTest {
    @Test
    fun GameTestState.`rex lands his bite shortly after engaging`() = combatTest {
        val rex = spawnKing(dk_npcs.melee.id, MELEE_TILE, offset = 1)
        val before = player.hitpoints

        forceMaxHit()
        rex.opPlayer2(player)

        // One tick for the interaction to fire the attack, one for the queued hit to land. Melee
        // queues at a fixed delay of one, with no projectile to wait on.
        advance(2)

        assertNotEquals(before, player.hitpoints, "Rex never connected.")
    }

    @Test
    fun GameTestState.`supreme fires from range and the hit waits for the arrow`() = combatTest {
        val supreme = spawnKing(dk_npcs.supreme.id, RANGED_TILE, offset = SHOOTING_DISTANCE)
        val before = player.hitpoints

        forceMaxHit()
        supreme.apPlayer2(player)

        // The whole point of the ranged path. A melee-timed hit would have landed by now; the
        // arrow is still in the air, so the player must be untouched.
        advance(2)
        assertEquals(before, player.hitpoints, "Supreme's hit landed before the arrow did.")

        advance(PROJECTILE_FLIGHT_TICKS)
        assertNotEquals(before, player.hitpoints, "Supreme's arrow never landed.")
    }

    @Test
    fun GameTestState.`prime casts from range and the hit waits for the spell`() = combatTest {
        val prime = spawnKing(dk_npcs.magic.id, MAGIC_TILE, offset = SHOOTING_DISTANCE)
        val before = player.hitpoints

        forceMaxHit()
        prime.apPlayer2(player)

        advance(2)
        assertEquals(before, player.hitpoints, "Prime's hit landed before the spell did.")

        advance(PROJECTILE_FLIGHT_TICKS)
        assertNotEquals(before, player.hitpoints, "Prime's spell never landed.")
    }

    @Test
    fun GameTestState.`the ranged kings hold their distance instead of closing`() = combatTest {
        // `attackRange = 10` plus the ap path is what keeps them out there. If either regressed
        // they would walk into melee and swing a ranged animation at point-blank range.
        for (id in listOf(dk_npcs.supreme.id, dk_npcs.magic.id)) {
            val king = spawnKing(id, RANGED_TILE, offset = SHOOTING_DISTANCE)
            val startCoords = king.coords

            forceMaxHit()
            king.apPlayer2(player)
            advance(4)

            assertEquals(
                startCoords,
                king.coords,
                "${king.type.name} closed on the player instead of firing from range.",
            )
        }
    }

    @Test
    fun GameTestState.`a ranged king answers a hit from range rather than walking in`() =
        combatTest {
            // The bug this pins would ship silently. `NpcRetaliateScript` used to send every npc
            // through `combatDefaultRetaliateOp`, so a king struck first walked into melee range
            // and swung its ranged attack animation there like a punch. Queue the retaliation the
            // way the hit processor does, then check which mode it answers in.
            val supreme = spawnKing(dk_npcs.supreme.id, RANGED_TILE, offset = SHOOTING_DISTANCE)

            supreme.queueCombatRetaliate(player)
            advance(2)

            assertEquals(
                NpcMode.ApPlayer2,
                supreme.mode,
                "Supreme retaliated into melee mode; it should answer from range.",
            )
        }

    @Test
    fun GameTestState.`no king hits for more than its live maximum`() = combatTest {
        for (id in listOf(dk_npcs.melee.id, dk_npcs.supreme.id, dk_npcs.magic.id)) {
            player.stats[stats.hitpoints] = FULL_HEALTH
            val king = spawnKing(id, MELEE_TILE, offset = 1)

            forceMaxHit()
            king.opPlayer2(player)
            advance(TICKS_PER_EXCHANGE)

            val dealt = FULL_HEALTH - player.hitpoints
            assertTrue(
                dealt <= LIVE_MAX_HIT,
                "${king.type.name} hit for $dealt, above its live maximum of $LIVE_MAX_HIT.",
            )
        }
    }

    /**
     * Every combat test needs the same three scripts, the weapon singletons, and a map clock that
     * has moved off zero, so they are wrapped up once here.
     */
    private fun GameTestState.combatTest(body: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            DagannothCombatDeps::class,
            DagannothCombatModule,
            NvPCombatScript::class,
            NpcRetaliateScript::class,
            WeaponAttackStylesScript::class,
            // Applies queued hits to the player; without it damage is rolled and then dropped.
            PlayerHitScript::class,
        ) { _ ->
            // "Is this player in combat" is `lastcombat + combat_activecombat_delay >= mapClock`.
            // The test world's clock starts near zero, which makes that true for a player nothing
            // has ever touched, and the single-way guard then refuses every attack. A live server
            // never sees this, because its clock is always far past the window.
            advance(CLOCK_WARMUP)
            body()
        }

    private fun GameTestScope.spawnKing(id: Int, tile: CoordGrid, offset: Int) =
        spawnNpc(tile.translateX(offset), npcTypes.getValue(id)).also {
            player.telejump(tile)
            player.stats[stats.hitpoints] = FULL_HEALTH
        }

    /**
     * Forces the next attack to connect for its maximum.
     *
     * The accuracy roll is drawn first and a zero always passes it; the damage roll follows. Left
     * to the default generator these would depend on a hit chance, which is exactly the kind of
     * flake that gets a suite ignored.
     */
    private fun GameTestScope.forceMaxHit() {
        random.next = 0
        random.then = LIVE_MAX_HIT
    }

    private companion object {
        val MELEE_TILE: CoordGrid = DagannothKingsLair.spawns.getValue(DagannothKing.Rex)
        val RANGED_TILE: CoordGrid = DagannothKingsLair.spawns.getValue(DagannothKing.Supreme)
        val MAGIC_TILE: CoordGrid = DagannothKingsLair.spawns.getValue(DagannothKing.Prime)

        const val SHOOTING_DISTANCE = 5

        /**
         * Generous on purpose: `arrow` takes three ticks at this distance and `magic_spell` four,
         * so this covers both without asserting a number the flight profile owns.
         */
        const val PROJECTILE_FLIGHT_TICKS = 6

        const val CLOCK_WARMUP = 16
        const val TICKS_PER_EXCHANGE = 4
        const val FULL_HEALTH = 99
        const val LIVE_MAX_HIT = 26
    }
}
