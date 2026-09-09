package org.rsmod.content.skills.thieving.configs

import org.junit.jupiter.api.Assertions.assertEquals
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

    /**
     * The other direction of the same rule, and the one that actually bites.
     *
     * The ladder used to be a hand-typed list of internal names, so `al_kharid_man` -- a plain
     * `Man` carrying `Pickpocket` on op3 like every other -- silently did nothing when clicked, and
     * so did the twenty-odd other `Man`/`Woman`/`Guard` types the list happened to omit. A cache
     * the server ships is not a thing to remember by hand: if a type wears a ladder name and the op
     * the script binds, it belongs in the table.
     */
    @Test
    fun GameTestState.`every ladder-named npc carrying the op is registered`() = runBasicGameTest {
        val registered = ThievingTargets.all.keys.map { it.id }.toSet()
        val missing =
            cacheTypes.npcs.values
                .filter { it.name in LADDER_NAMES }
                .filter { it.op.getOrNull(PICKPOCKET_OP_SLOT - 1) == "Pickpocket" }
                .filter { it.id !in registered }
                .map { "${it.id} '${it.name}' (${it.internalName}) ops=${it.op.toList()}" }
        assertTrue(missing.isEmpty()) {
            "These npcs are pickpocketable in the cache but not on the ladder, so clicking " +
                "Pickpocket on them does nothing:\n" +
                missing.joinToString("\n")
        }
    }

    @Test
    fun GameTestState.`every coin pouch is stackable and pays a sane range`() = runBasicGameTest {
        for ((npc, target) in ThievingTargets.all) {
            val loot = target.loot
            if (loot !is PickpocketLoot.Pouch) {
                continue
            }
            val pouch = cacheTypes.objs[loot.pouch.id]
            assertNotNull(pouch) { "'${npc.internalName}' pays an unresolvable pouch." }
            checkNotNull(pouch)

            // The entire "an AFK session never fills the inventory" premise rests on this.
            assertTrue(pouch.stackable) {
                "Pouch '${pouch.internalName}' is not stackable, so pickpocketing would fill the " +
                    "inventory one slot at a time."
            }

            // `GameRandom.of` throws on a non-positive bound, and it would throw inside a
            // protected-access coroutine, which force-disconnects the player.
            assertTrue(loot.coins.first >= 1 && loot.coins.first <= loot.coins.last) {
                "'${npc.internalName}' has a nonsensical coin range ${loot.coins}."
            }
        }
    }

    @Test
    fun GameTestState.`every target has a sane stun`() = runBasicGameTest {
        for ((npc, target) in ThievingTargets.all) {
            assertTrue(target.stunDamage.first >= 1) {
                "'${npc.internalName}' has a nonsensical damage range ${target.stunDamage}."
            }
        }
    }

    /**
     * The master farmer's table is forty seeds typed off the wiki, which is exactly the kind of
     * data that is wrong in small ways, and every way it can be wrong is caught here rather than by
     * a player finding a seed that does not exist.
     *
     * Stackability is the one that would not look like a bug: a non-stacking seed turns a 5x haul
     * into five inventory slots and ends the session in a handful of pickpockets.
     */
    @Test
    fun GameTestState.`every master farmer seed is a real stackable seed`() = runBasicGameTest {
        assertTrue(MasterFarmerSeeds.slots.isNotEmpty()) { "The seed table is empty." }

        val seen = mutableSetOf<Int>()
        for (slot in MasterFarmerSeeds.slots) {
            val obj = cacheTypes.objs[slot.seed.id]
            assertNotNull(obj) { "Seed '${slot.seed.internalName}' is not in the cache." }
            checkNotNull(obj)

            assertTrue(obj.stackable) {
                "Seed '${obj.internalName}' is not stackable, so a 5x roll would cost five slots."
            }
            assertTrue(seen.add(slot.seed.id)) {
                "Seed '${obj.internalName}' is listed twice, which silently doubles its odds."
            }

            // `GameRandom.of` throws on a non-positive bound, and it would throw inside a
            // protected-access coroutine, which force-disconnects the player.
            assertTrue(slot.quantity.first >= 1 && slot.quantity.first <= slot.quantity.last) {
                "'${obj.internalName}' has a nonsensical quantity ${slot.quantity}."
            }
            assertTrue(slot.worstOneIn >= 1.0 && slot.bestOneIn >= 1.0) {
                "'${obj.internalName}' has a rarity below one in one."
            }

            // The wiki prints the ranges best-first; typing them the other way round would make
            // high Farming quietly worse than low.
            assertTrue(slot.bestOneIn <= slot.worstOneIn) {
                "'${obj.internalName}' gets rarer as Farming rises: ${slot.worstOneIn} -> " +
                    "${slot.bestOneIn}."
            }
        }
    }

    /**
     * The claim the flat table rests on: OSRS pays exactly one seed per successful pickpocket, and
     * the wiki's five groups are presentation rather than five independent rolls. If the published
     * rarities did not sum to one, that reading would be wrong and the table would need a
     * nothing-slot.
     */
    @Test
    fun GameTestState.`the seed rarities sum to one roll`() = runBasicGameTest {
        for (farmingLvl in intArrayOf(1, 85, 99)) {
            val total = MasterFarmerSeeds.slots.sumOf { 1.0 / it.oneIn(farmingLvl) }
            assertTrue(total > 0.98 && total < 1.02) {
                "At Farming $farmingLvl the seed rarities sum to $total, not ~1. Either a rarity " +
                    "is mistyped or this is not a single-roll table after all."
            }
        }
    }

    /** The four scaling rows, pinned at both published endpoints and in between. */
    @Test
    fun GameTestState.`herb seed odds improve with farming and then stop`() = runBasicGameTest {
        val ranarr = MasterFarmerSeeds.slots.first { it.seed.id == ThievingSeeds.ranarr_seed.id }

        assertEquals(555.83, ranarr.oneIn(1), 0.01) { "Farming 1 is not the worst published rate." }
        assertEquals(268.75, ranarr.oneIn(85), 0.01) {
            "Farming 85 is not the best published rate."
        }
        assertEquals(268.75, ranarr.oneIn(99), 0.01) {
            "The rate keeps improving past 85, which the wiki says it does not."
        }
        assertTrue(ranarr.oneIn(43) < ranarr.oneIn(1) && ranarr.oneIn(43) > ranarr.oneIn(85)) {
            "Farming 43 sits outside its two endpoints: ${ranarr.oneIn(43)}."
        }

        // A flat row must ignore the level entirely.
        val torstol = MasterFarmerSeeds.slots.first { it.seed.id == ThievingSeeds.torstol_seed.id }
        val toadflax =
            MasterFarmerSeeds.slots.first { it.seed.id == ThievingSeeds.toadflax_seed.id }
        assertTrue(torstol.oneIn(99) < torstol.oneIn(1)) { "Torstol should scale with Farming." }
        assertEquals(toadflax.oneIn(1), toadflax.oneIn(99), 0.0) {
            "Toadflax is not a scaling row and must not move with Farming."
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

        /**
         * The display names that make an npc a rung of the ladder rather than its own content.
         * Anything else that carries `Pickpocket` -- H.A.M. members, cave goblins, bandits,
         * TzHaar-Hur, elves, vampyres, Varlamore's citizens -- drops its own loot and is not this
         * module's to bind. See `ThievingTargetNpcs`.
         */
        val LADDER_NAMES =
            setOf(
                "Man",
                "Woman",
                "Drunken man",
                "Farmer",
                "Warrior",
                "Al Kharid warrior",
                "Rogue",
                "Master Farmer",
                "Martin the Master Gardener",
                "Guard",
                "Head Guard",
                "Knight of Ardougne",
                "Watchman",
                "Paladin",
                "Hero",
            )

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
