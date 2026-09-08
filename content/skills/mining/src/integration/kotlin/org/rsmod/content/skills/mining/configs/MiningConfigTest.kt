package org.rsmod.content.skills.mining.configs

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeMiningAnim
import org.rsmod.content.skills.mining.scripts.Mining.Companion.pickaxeMiningReq
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockLevelReq
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockOre
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockRespawnTime
import org.rsmod.content.skills.mining.scripts.Mining.Companion.rockSpent

/**
 * Guards the things the type resolver cannot: a rock can resolve perfectly and still be unmineable
 * because a param is missing, or point at an ore that does not exist.
 */
/**
 * Runs single-threaded on purpose: `integration-test-suite` sets
 * `junit.jupiter.execution.parallel.mode.default=concurrent`, so methods in one class share a
 * single game world. Tests that place locs or move items race each other otherwise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class MiningConfigTest {
    @Test
    fun GameTestState.`every rock is fully configured`() = runBasicGameTest {
        val rocks = cacheTypes.locs.values.filter { it.isContentType(MiningContent.mining_rock) }
        assertTrue(rocks.isNotEmpty()) { "No locs were tagged into the mining_rock group." }
        for (rock in rocks) {
            val ore = cacheTypes.objs[rock.rockOre.id]
            assertNotNull(ore) { "Rock '${rock.internalName}' yields an unresolvable ore." }
            assertTrue(rock.rockLevelReq in 1..99) {
                "Rock '${rock.internalName}' has an out-of-range level: ${rock.rockLevelReq}."
            }
            assertTrue(rock.rockRespawnTime > 0) {
                "Rock '${rock.internalName}' would never respawn."
            }
            // A rock whose spent stage is itself would be permanently mineable.
            assertTrue(rock.rockSpent.id != rock.id) {
                "Rock '${rock.internalName}' depletes into itself."
            }
        }
    }

    @Test
    fun GameTestState.`every pickaxe is usable`() = runBasicGameTest {
        val pickaxes =
            cacheTypes.objs.values.filter { it.isContentType(MiningContent.mining_pickaxe) }
        assertTrue(pickaxes.isNotEmpty()) { "No objs were tagged into the mining_pickaxe group." }
        for (pickaxe in pickaxes) {
            assertTrue(pickaxe.pickaxeMiningReq in 1..99) {
                "Pickaxe '${pickaxe.internalName}' has an out-of-range requirement."
            }
            assertNotNull(pickaxe.pickaxeMiningAnim) {
                "Pickaxe '${pickaxe.internalName}' has no mining animation."
            }
        }
        // A level-1 player must always have something to mine with, or the skill is unstartable.
        assertTrue(pickaxes.any { it.pickaxeMiningReq == 1 }) { "No pickaxe is usable at level 1." }
    }

    @Test
    fun GameTestState.`ores are tagged into the shared ore group`() = runBasicGameTest {
        // Lumbridge's SmithingApprentice tests `content.ore in player.inv`; that check was dead
        // until this module tagged the ores, so it is worth pinning.
        val ores = cacheTypes.objs.values.filter { it.isContentType(content.ore) }
        val names = ores.mapNotNull { it.internalName }.toSet()
        for (expected in
            listOf(
                "copper_ore",
                "tin_ore",
                "iron_ore",
                "silver_ore",
                "coal",
                "gold_ore",
                "mithril_ore",
                "adamantite_ore",
                "runite_ore",
                "clay",
            )) {
            assertTrue(expected in names) { "'$expected' is missing from the content.ore group." }
        }
    }
}
