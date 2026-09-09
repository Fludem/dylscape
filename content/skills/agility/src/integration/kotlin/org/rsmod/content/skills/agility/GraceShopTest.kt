package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.agility.configs.AgilityInvs
import org.rsmod.content.skills.agility.configs.AgilityNpcs
import org.rsmod.content.skills.agility.configs.AgilityObjs
import org.rsmod.content.skills.agility.map.GraceNpcSpawns
import org.rsmod.content.skills.agility.shop.GracefulPrices
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class GraceCollisionDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * Holds together the four halves of Grace that can each be right on their own and still leave the
 * shop broken: the npc, the op slots the script binds, the priced stock, and the tile she spawns
 * on.
 */
@Execution(ExecutionMode.SAME_THREAD)
class GraceShopTest {
    @Test
    fun GameTestState.`grace carries talk-to on op1 and trade on op3`() = runBasicGameTest {
        val grace = cacheTypes.npcs[AgilityNpcs.grace.id]
        assertNotNull(grace) { "Npc ${AgilityNpcs.grace.id} is not in the cache." }
        // An op bound to a slot whose cache text is absent is dropped silently by the handler, so
        // the shop would open for nobody and report nothing.
        assertEquals("Talk-to", grace!!.op.getOrNull(0))
        assertEquals("Trade", grace.op.getOrNull(2))
    }

    @Test
    fun GameTestState.`every priced line is stocked and every stocked line is priced`() =
        runBasicGameTest {
            val inv = cacheTypes.invs[AgilityInvs.roguesden_shop.id]
            assertNotNull(inv) { "Inv ${AgilityInvs.roguesden_shop.id} is not in the cache." }

            val stocked = inv!!.stock?.filterNotNull()?.map { it.obj }?.toSet().orEmpty()
            assertTrue(stocked.isNotEmpty()) {
                "Grace's shop has no stock -- `GraceShopInv` did not apply."
            }
            // A stocked line with no price cannot be bought and a priced line with no stock cannot
            // be found; either way the mismatch is invisible until someone opens the shop.
            assertEquals(GracefulPrices.objIds, stocked) {
                "Priced lines and stocked lines disagree."
            }
        }

    @Test
    fun `the graceful set costs what the live game charges`() {
        val set =
            listOf(
                AgilityObjs.graceful_hood,
                AgilityObjs.graceful_top,
                AgilityObjs.graceful_legs,
                AgilityObjs.graceful_gloves,
                AgilityObjs.graceful_boots,
                AgilityObjs.graceful_cape,
            )
        val total = set.sumOf { GracefulPrices[it.id] ?: 0 }
        assertEquals(GracefulPrices.FULL_SET_COST, total, "Full graceful set cost")
    }

    @Test
    fun GameTestState.`grace is spawned on a tile she can stand on`() =
        runInjectedGameTest(GraceCollisionDeps::class) { deps ->
            // Parsed out of the packed resource rather than restated here, so this test and the
            // packer cannot drift. `city-shops` shipped 17 correct shops and zero spawns because
            // every test it had asserted on cache types, and a spawn file is not a cache type.
            val spawn = parseSpawn()
            assertEquals("rooftops_grace", spawn.npc) { "`npcs.toml` spawns the wrong npc." }

            val flags = deps.collision[spawn.coords.x, spawn.coords.z, spawn.coords.level]
            assertEquals(0, flags and BLOCKS_STANDING) {
                "Grace's spawn tile ${spawn.coords.toConventionalString()} is blocked."
            }
        }

    private fun parseSpawn(): Spawn {
        val text =
            GraceNpcSpawns::class
                .java
                .getResourceAsStream(GraceNpcSpawns.FILE)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: error("`${GraceNpcSpawns.FILE}` is not on the classpath.")
        val match =
            SPAWN_PATTERN.find(text) ?: error("Parsed no spawns out of `${GraceNpcSpawns.FILE}`.")
        val (npc, level, msqX, msqZ, localX, localZ) = match.destructured
        return Spawn(
            npc,
            CoordGrid(level.toInt(), msqX.toInt(), msqZ.toInt(), localX.toInt(), localZ.toInt()),
        )
    }

    private data class Spawn(val npc: String, val coords: CoordGrid)

    private companion object {
        /**
         * Stricter than the walkability check the courses use. An npc spawn tile also has to be
         * free of the loc and ground decoration that a player could not click her through.
         */
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
