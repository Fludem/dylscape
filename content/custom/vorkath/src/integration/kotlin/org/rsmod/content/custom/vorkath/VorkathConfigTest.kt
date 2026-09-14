package org.rsmod.content.custom.vorkath

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.maxhit.npc.NpcRangedMaxHit
import org.rsmod.api.config.refs.categories
import org.rsmod.api.config.refs.huntmodes
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.droptables.data.DropTableResourceLoader
import org.rsmod.content.custom.vorkath.configs.VorkathArena
import org.rsmod.content.custom.vorkath.configs.vorkath_npcs
import org.rsmod.content.custom.vorkath.configs.vorkath_seqs
import org.rsmod.content.custom.vorkath.map.VorkathSpawns
import org.rsmod.game.entity.npc.NpcMode
import org.rsmod.game.type.TypeResolver
import org.rsmod.game.type.category.isType
import org.rsmod.map.CoordGrid

class VorkathDropDeps @Inject constructor(val dropTables: DropTableResourceLoader)

/**
 * Guards the parts of the fight that live entirely in configuration.
 *
 * Almost everything that could go wrong here goes wrong silently: a missing ranged defence makes
 * Vorkath trivially shot rather than throwing, a stray strength bonus quietly buffs it, a missing
 * animation makes a dragon throw punches, and a spawn file the packer never sees leaves an empty
 * platform. None of that fails a build on its own, so it is asserted.
 */
@Execution(ExecutionMode.SAME_THREAD)
class VorkathConfigTest {
    @Test
    fun GameTestState.`the editor landed on both forms`() = runBasicGameTest {
        val sleeping = cacheTypes.npcs.getValue(vorkath_npcs.sleeping.id)
        assertEquals(0, sleeping.wanderRange, "The sleeping dragon would wander off its platform.")
        assertEquals(NpcMode.None, sleeping.defaultMode)
        assertTrue(sleeping.op.any { it == "Poke" }, "Nothing to poke.")

        val awake = cacheTypes.npcs.getValue(vorkath_npcs.awake.id)
        assertEquals(0, awake.wanderRange)
        assertEquals(32, awake.maxRange, "Vorkath would leash too early.")
        assertEquals(NpcMode.None, awake.defaultMode)
        assertEquals(TypeResolver[huntmodes.aggressive_ranged], awake.huntMode)
        assertEquals(16, awake.huntRange)
        assertEquals(16, awake.attackRange, "Vorkath cannot cover the crater from its platform.")
        assertTrue(
            awake.paramOrNull(params.npc_attack_type).isType(categories.attacktype_ranged),
            "A struck Vorkath would try to walk into melee range.",
        )
        assertEquals(vorkath_seqs.attack.id, awake.param(params.attack_anim).id)
        assertEquals(vorkath_seqs.death.id, awake.param(params.death_anim).id)
        for (param in listOf(params.defence_light, params.defence_standard, params.defence_heavy)) {
            assertEquals(26, awake.param(param), "Vorkath is trivially shot.")
        }
        assertEquals(1, awake.param(params.poison_immunity))
        // The exact predicate `DropTableResourceLoader` filters on.
        assertTrue(awake.op.any { it == "Attack" }, "Vorkath cannot be attacked.")
    }

    @Test
    fun GameTestState.`vorkath keeps the levels the cache gave it and hits for thirty two`() =
        runBasicGameTest {
            val awake = cacheTypes.npcs.getValue(vorkath_npcs.awake.id)
            assertEquals(750, awake.hitpoints)
            assertEquals(308, awake.ranged)
            assertEquals(150, awake.magic)
            assertEquals(7, awake.size)
            assertEquals(5, awake.param(params.attackrate), "Vorkath attacks at the wrong speed.")
            assertEquals(26, awake.param(params.defence_stab))
            assertEquals(240, awake.param(params.defence_magic))

            // Max hit is derived, not stored, and the editor sets no ranged strength - so the live
            // 32 falls straight out of the level. The day anyone adds a bonus this fails instead of
            // quietly buffing the fight.
            val maxHit =
                NpcRangedMaxHit.calculateBaseDamage(
                    NpcRangedMaxHit.calculateEffectiveRanged(awake.ranged),
                    awake.param(params.ranged_strength),
                )
            assertEquals(LIVE_RANGED_MAX_HIT, maxHit)

            // The shared driver must never fire for Vorkath: its two styles are rolled by the
            // script, and a declared projectile here would mean a third, wrong one.
            assertNull(awake.paramOrNull(params.proj_travel))
            assertNull(awake.paramOrNull(params.proj_type))
        }

    @Test
    fun GameTestState.`the generated drop table claims the awake form`() =
        runInjectedGameTest(VorkathDropDeps::class) { deps ->
            val result = deps.dropTables.load()
            val claimed = result.assignments.map { it.first.id }
            assertTrue(vorkath_npcs.awake.id in claimed, "Vorkath would die with no loot.")
            // The sleeping form is on the same wiki row but has no `Attack` op, so the loader
            // sets it aside rather than erroring - which is exactly right, it never dies.
            assertTrue("vorkath_sleeping" in result.unattackable)
        }

    @Test
    fun `the spawn file places the dragon and both ferrymen where the arena says`() {
        val spawns = parseSpawns()
        val expected =
            mapOf(
                "vorkath_sleeping" to VorkathArena.spawn,
                "torfinn_travel_rellekka" to VorkathArena.torfinnRellekka,
                "torfinn_travel_ungael" to VorkathArena.torfinnUngael,
            )
        assertEquals(expected, spawns, "npcs.toml and VorkathArena have drifted apart.")
    }

    /** Parsed straight out of the packed resource, so the test and the packer cannot drift. */
    private fun parseSpawns(): Map<String, CoordGrid> {
        val text =
            VorkathSpawns::class
                .java
                .getResourceAsStream(VorkathSpawns.FILE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("`${VorkathSpawns.FILE}` is not on the classpath beside VorkathSpawns.")
        return SPAWN_PATTERN.findAll(text)
            .associate { match ->
                val (npc, level, msqX, msqZ, localX, localZ) = match.destructured
                npc to
                    CoordGrid(
                        level.toInt(),
                        msqX.toInt(),
                        msqZ.toInt(),
                        localX.toInt(),
                        localZ.toInt(),
                    )
            }
            .also { assertTrue(it.isNotEmpty()) { "Parsed no spawns out of the file." } }
    }

    private companion object {
        const val LIVE_RANGED_MAX_HIT = 32

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
