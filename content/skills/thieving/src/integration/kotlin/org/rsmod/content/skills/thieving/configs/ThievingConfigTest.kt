package org.rsmod.content.skills.thieving.configs

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.thieving.map.ThievingNpcSpawns
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Pins the parts of the module that are statements about the cache rather than about behaviour.
 *
 * Runs single-threaded for the same reason every other suite here does: `integration-test-suite`
 * defaults to concurrent execution and methods in one class share a game world.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ThievingConfigTest {
    @Test
    fun GameTestState.`every pickpocket target carries the op the script binds`() =
        runBasicGameTest {
            assertTrue(ThievingTargets.all.isNotEmpty()) { "The pickpocket ladder is empty." }
            for ((npc, target) in ThievingTargets.all) {
                val type = cacheTypes.npcs[npc.id]
                assertNotNull(type) { "Target '${npc.internalName}' is not in the cache." }
                checkNotNull(type)

                // The script registers `onOpNpc3`, so the op has to be in that exact slot. Without
                // it `OpNpcHandler` refuses the click and the target silently does nothing.
                val op = type.op.getOrNull(PICKPOCKET_OP_SLOT - 1)
                assertTrue(op == "Pickpocket") {
                    "'${npc.internalName}' has '$op' on op$PICKPOCKET_OP_SLOT, not 'Pickpocket'. " +
                        "Ops are ${type.op.toList()}."
                }
                assertTrue(target.level in 1..99) {
                    "'${npc.internalName}' requires level ${target.level}."
                }
                assertTrue(target.baseXp > 0.0) { "'${npc.internalName}' awards no xp." }
            }
        }

    @Test
    fun GameTestState.`every coin pouch is stackable and pays a sane range`() = runBasicGameTest {
        for ((npc, target) in ThievingTargets.all) {
            val pouch = cacheTypes.objs[target.pouch.id]
            assertNotNull(pouch) { "'${npc.internalName}' pays an unresolvable pouch." }
            checkNotNull(pouch)

            // The entire "an AFK session never fills the inventory" premise rests on this.
            assertTrue(pouch.stackable) {
                "Pouch '${pouch.internalName}' is not stackable, so pickpocketing would fill the " +
                    "inventory one slot at a time."
            }

            // `GameRandom.of` throws on a non-positive bound, and it would throw inside a
            // protected-access coroutine, which force-disconnects the player.
            assertTrue(target.coins.first >= 1 && target.coins.first <= target.coins.last) {
                "'${npc.internalName}' has a nonsensical coin range ${target.coins}."
            }
            assertTrue(target.stunDamage.first >= 1) {
                "'${npc.internalName}' has a nonsensical damage range ${target.stunDamage}."
            }
        }
    }

    @Test
    fun GameTestState.`the pickpocket ladder is monotone`() = runBasicGameTest {
        val rungs = ThievingTargets.all.values.distinct().sortedBy { it.level }
        val xp = rungs.map { it.baseXp }
        assertTrue(xp == xp.sorted()) {
            "Sorting the ladder by level does not sort it by xp, which means a typo in the table: " +
                rungs.joinToString { "lvl ${it.level} -> ${it.baseXp}xp" }
        }
        assertTrue(ThievingTargets.lowestLevel == 1) {
            "A fresh account has nothing to pickpocket; lowest rung is ${ThievingTargets.lowestLevel}."
        }
    }

    @Test
    fun GameTestState.`every stall carries the steal op and notable loot`() = runBasicGameTest {
        assertTrue(ThievingStalls.all.isNotEmpty()) { "No stalls are configured." }
        for ((loc, stall) in ThievingStalls.all) {
            val type = cacheTypes.locs[loc.id]
            assertNotNull(type) { "Stall '${loc.internalName}' is not in the cache." }
            checkNotNull(type)

            val op = type.op.getOrNull(STEAL_OP_SLOT - 1)
            assertTrue(op != null && op.startsWith("Steal")) {
                "'${loc.internalName}' has '$op' on op$STEAL_OP_SLOT. Ops are ${type.op.toList()}."
            }

            // The "already robbed" variants would be an infinitely stealable empty stall.
            val name = type.internalName.orEmpty()
            assertTrue(!name.endsWith("_noop") && !name.endsWith("_nothieving")) {
                "'$name' is a depleted-stall variant and must not be tagged."
            }

            assertTrue(stall.loot.isNotEmpty()) { "'${loc.internalName}' drops nothing." }
            for (product in stall.loot) {
                val obj = cacheTypes.objs[product.id]
                assertNotNull(obj) { "'${loc.internalName}' drops an unresolvable obj." }
                checkNotNull(obj)

                // Stalls pay noted so the 5x haul stacks instead of filling the bag. An obj with no
                // cert link cannot be noted, and would silently be added loose instead.
                assertTrue(obj.certlink > 0) {
                    "'${obj.internalName}' has no cert link, so the stall cannot pay it noted."
                }
            }
        }
    }

    /**
     * The spawn file is coordinates typed from knowledge of OSRS, which is exactly the kind of data
     * that is wrong in small ways. The test harness copies the real game collision map, so a tile
     * an npc could never stand on is catchable here rather than by walking to Ardougne in-game.
     *
     * Parsed straight out of the packed resource so this and [ThievingNpcSpawns] cannot drift.
     */
    @Test
    fun GameTestState.`every authored spawn stands on a walkable tile`() = runBasicGameTest {
        val resource =
            ThievingNpcSpawns::class.java.getResourceAsStream(SPAWN_RESOURCE)?.bufferedReader()
                ?: error("Could not read $SPAWN_RESOURCE from the classpath.")
        val text = resource.use { it.readText() }

        val entries = SPAWN_PATTERN.findAll(text).toList()
        assertTrue(entries.isNotEmpty()) { "Parsed no spawns out of $SPAWN_RESOURCE." }

        val blocked = mutableListOf<String>()
        for (entry in entries) {
            val (npc, level, msqX, msqZ, localX, localZ) = entry.destructured
            val coords =
                CoordGrid(level.toInt(), msqX.toInt(), msqZ.toInt(), localX.toInt(), localZ.toInt())
            val flags = collision[coords.x, coords.z, coords.level]
            if (flags and BLOCKS_STANDING != 0) {
                blocked +=
                    "$npc at $coords (${coords.x}, ${coords.z}) flags=0x${flags.toString(16)}"
            }
        }
        assertTrue(blocked.isEmpty()) {
            "These spawns are on tiles nothing can stand on:\n" + blocked.joinToString("\n")
        }
    }

    private companion object {
        /** Decoded by `ThievingOpsDump`; `Pickpocketing` registers `onOpNpc3` to match. */
        const val PICKPOCKET_OP_SLOT = 3

        /** Likewise; `StallThieving` registers `onOpLoc2`. */
        const val STEAL_OP_SLOT = 2

        const val SPAWN_RESOURCE = "npcs.toml"

        /** A tile an npc cannot occupy: solid ground furniture, a blocking loc, or unwalkable. */
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
