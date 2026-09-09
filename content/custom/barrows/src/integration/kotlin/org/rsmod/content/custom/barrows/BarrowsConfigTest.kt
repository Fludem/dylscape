package org.rsmod.content.custom.barrows

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.barrows.configs.BarrowsInvs
import org.rsmod.content.custom.barrows.configs.BarrowsRewards
import org.rsmod.content.custom.barrows.configs.barrows_locs
import org.rsmod.content.custom.barrows.configs.barrows_npcs
import org.rsmod.content.custom.barrows.configs.barrows_varbits

/**
 * That everything the module names resolves, and that the editor actually filled the gaps the cache
 * left. A `find` that does not resolve is already a hard boot failure; these cover the quieter kind
 * of wrong, where a type resolves but carries nothing useful.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BarrowsConfigTest {
    @Test
    fun GameTestState.`every brother resolves and keeps the cache's own stats`() =
        runBasicGameTest {
            for (brother in Brother.all) {
                val type = cacheTypes.npcs[checkNotNull(barrows_npcs.brothers[brother])]
                assertNotNull(type, "$brother did not resolve")
                // The cache already carries these and the editor deliberately does not restate
                // them; if that ever stops being true, the fight silently becomes trivial.
                assertEquals(100, type.hitpoints, "$brother lost its hitpoints")
                assertTrue(type.param(params.attackrate) > 0, "$brother has no attack speed")
            }
        }

    @Test
    fun GameTestState.`the editor supplies what the cache leaves out`() = runBasicGameTest {
        for (brother in Brother.all) {
            val type = cacheTypes.npcs[checkNotNull(barrows_npcs.brothers[brother])]
            // `NvPMeleeAccuracy` reads this and nothing else for the npc's attack roll.
            assertTrue(type.param(params.attack_melee) > 0, "$brother has no melee attack bonus")
            assertTrue(type.param(params.defence_ranged) > 0, "$brother has no ranged defence")
            // Without this every brother throws unarmed punches: `params.attack_anim` defaults to
            // `human_unarmedpunch`.
            assertNotNull(type.paramOrNull(params.attack_anim), "$brother has no attack animation")
            // Raised angry, and pinned to the sarcophagus rather than wandering off.
            assertEquals(0, type.wanderRange, "$brother still wanders")
            assertNotNull(type.huntMode, "$brother has no hunt mode")
        }
    }

    @Test
    fun GameTestState.`the chest is the multiloc the module thinks it is`() = runBasicGameTest {
        val chest = cacheTypes.locs[barrows_locs.stone_chest]
        assertEquals(
            barrows_varbits.chest_open.id,
            chest.multiVarBit,
            "The chest no longer switches on barrows_chest_open",
        )
        assertEquals(
            listOf(barrows_locs.stone_chest_closed.id, barrows_locs.stone_chest_open.id),
            chest.multiLoc.map(Short::toInt),
            "The chest's two faces are not the closed and open types",
        )
        // The ops the chest script binds. A decorative twin with no ops would bind silently.
        assertTrue(
            cacheTypes.locs[barrows_locs.stone_chest_closed].op.any { it == "Open" },
            "The closed chest has no Open op",
        )
        assertTrue(
            cacheTypes.locs[barrows_locs.stone_chest_open].op.any { it == "Search" },
            "The open chest has no Search op",
        )
    }

    @Test
    fun GameTestState.`every sarcophagus and staircase carries the op we bind`() =
        runBasicGameTest {
            for (brother in Brother.all) {
                val sarcophagus = cacheTypes.locs[checkNotNull(barrows_locs.sarcophagi[brother])]
                assertEquals("Search", sarcophagus.op[0], "$brother sarcophagus has no Search")
                val stairs = cacheTypes.locs[checkNotNull(barrows_locs.staircases[brother])]
                assertEquals("Climb-up", stairs.op[0], "$brother staircase has no Climb-up")
            }
        }

    @Test
    fun GameTestState.`the reward tables resolve`() = runBasicGameTest {
        assertEquals(24, BarrowsRewards.equipment.size, "Expected four pieces per brother")
        for (piece in BarrowsRewards.equipment) {
            assertTrue(cacheTypes.objs[piece].name.isNotBlank(), "Unnamed equipment piece $piece")
        }
        // `dropTable` asserts weights sum to their denominator at class-load, so reaching here at
        // all proves the consolation table adds up; this only checks it pays something.
        assertTrue(BarrowsRewards.consolation.tables.single().slots.isNotEmpty())
        assertTrue(cacheTypes.invs[BarrowsInvs.reward].size > 0, "The reward inv has no size")
    }

    @Test
    fun GameTestState.`roll counts follow the brothers killed`() = runBasicGameTest {
        assertEquals(1, BarrowsRewards.rollCount(0))
        assertEquals(7, BarrowsRewards.rollCount(6))
    }
}
