package org.rsmod.content.areas.misc.tutorial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.areas.misc.tutorial.configs.TutorialConstants
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignComponents
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignData.StyleSlot
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignInterfaces
import org.rsmod.content.areas.misc.tutorial.configs.TutorialKitObjs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialLocs
import org.rsmod.content.areas.misc.tutorial.configs.TutorialNpcs
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * The half of this module that lives on the map rather than in the cache.
 *
 * A spawn file is not a cache type, so nothing else in the suite would notice an instructor
 * authored into a wall, onto the wrong floor of the world, or into an open field two rooms from the
 * lesson they teach. These read the packed resource and the real collision map back. They matter
 * more here than anywhere else on the server: `ROUTE_NEW_ACCOUNTS` sends every new account to
 * [TutorialConstants.START_COORD], and finishing the course is the only way off the island.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TutorialMapTest {
    @Test
    fun GameTestState.`every spawn names an npc the cache knows`() = runBasicGameTest {
        val known = cacheTypes.npcs.values.mapTo(hashSetOf()) { it.internalName }
        val unknown = parseSpawns().filter { it.npc !in known }
        assertTrue(unknown.isEmpty()) {
            "`packCache` hard-errors on these; they are typos in the spawn file: $unknown"
        }
    }

    @Test
    fun GameTestState.`every instructor is spawned`() = runBasicGameTest {
        val names = cacheTypes.npcs.values.filter { it.id in TutorialNpcs.all.map { n -> n.id } }
        val spawned = parseSpawns().map { it.npc }.toSet()
        val missing = names.filter { it.internalName !in spawned }.map { it.internalName }
        assertTrue(missing.isEmpty()) {
            "These instructors have dialogue and a stage but no tile to stand on: $missing"
        }
    }

    @Test
    fun GameTestState.`every instructor stands on a tile a player can reach`() = runBasicGameTest {
        val instructors = TutorialNpcs.all.map { it.id }.toSet()
        val ids = cacheTypes.npcs.values.filter { it.id in instructors }.associateBy { it.id }
        val names = ids.values.mapNotNull { it.internalName }.toSet()
        val blocked = parseSpawns().filter { it.npc in names }.filter { blocksStanding(it.coords) }
        assertTrue(blocked.isEmpty()) {
            "These instructors are authored into scenery, so a player sent to them finds nothing " +
                "but a hint arrow over a wall: $blocked"
        }
    }

    /**
     * Fishing spots are the one exception to the rule above. They are npcs that sit *on* the water,
     * which is deliberately un-standable — so the check is inverted, plus the tile a player fishes
     * from has to exist next to it.
     */
    @Test
    fun GameTestState.`the fishing spots sit on water a player can fish from`() = runBasicGameTest {
        val spots = parseSpawns().filter { it.npc.endsWith("newbiefishing") }
        assertTrue(spots.isNotEmpty()) { "The survival step has nothing to fish." }
        for (spot in spots) {
            assertTrue(blocksStanding(spot.coords)) {
                "The fishing spot at ${spot.coords} is on dry land."
            }
            val bank = CARDINAL.map { (dx, dz) -> spot.coords.translateX(dx).translateZ(dz) }
            assertTrue(bank.any { !blocksStanding(it) }) {
                "Nothing can stand next to the fishing spot at ${spot.coords}."
            }
        }
    }

    @Test
    fun GameTestState.`a new account is placed somewhere it can stand`() = runBasicGameTest {
        assertTrue(!blocksStanding(TutorialConstants.START_COORD)) {
            "New accounts are routed to ${TutorialConstants.START_COORD}, which is not standable."
        }
        assertTrue(!blocksStanding(TutorialConstants.LUMBRIDGE)) {
            "The tutorial ends by teleporting to ${TutorialConstants.LUMBRIDGE}, inside scenery."
        }
    }

    /**
     * The mining, smelting, smithing and combat steps all happen in the cave under the island, at
     * the standard `+6400` z offset. An instructor authored onto the surface by mistake reads as a
     * plausible coordinate and fails only in game, which is exactly what happened before.
     */
    @Test
    fun GameTestState.`the mining and combat instructors are underground`() = runBasicGameTest {
        val underground =
            setOf(TutorialNpcs.mining.id, TutorialNpcs.combat.id).mapNotNull {
                cacheTypes.npcs[it]?.internalName
            }
        val spawns = parseSpawns().filter { it.npc in underground }
        assertEquals(underground.size, spawns.size) { "A cave instructor is missing a spawn." }
        for (spawn in spawns) {
            assertTrue(spawn.coords.z >= CAVE_Z) {
                "${spawn.npc} is authored at ${spawn.coords}, on the surface, not in the cave."
            }
        }
    }

    /**
     * Every loc the module binds has to resolve, and — more usefully — has to actually be on the
     * map. `find()` only looks a name up in the symbol table; a door or ladder that resolves but is
     * placed nowhere would leave the route through the island silently broken.
     */
    @Test
    fun GameTestState.`the doors, gate and ladders the module binds all resolve`() =
        runBasicGameTest {
            val bound =
                TutorialLocs.allDoors +
                    TutorialLocs.laddersDown +
                    TutorialLocs.laddersUp +
                    TutorialLocs.bank_booth
            val unresolved = bound.filter { cacheTypes.locs[it.id] == null }
            assertTrue(unresolved.isEmpty()) { "These tutorial locs do not resolve: $unresolved" }

            // The route out of the first room, west to the kitchen, and down to the cave.
            for (loc in
                listOf(
                    TutorialLocs.door1,
                    TutorialLocs.gate_left,
                    TutorialLocs.gate_right,
                    TutorialLocs.ladder_down_quest,
                    TutorialLocs.ladder_up_combat,
                )) {
                val type = cacheTypes.locs[loc.id]
                assertTrue(type?.op?.firstOrNull()?.isNotEmpty() == true) {
                    "$loc carries no op1, so nothing this module binds to it can ever fire."
                }
            }
        }

    /**
     * The starter kit is written by symbol name, and several of those names are not what the item
     * is called in game -- the wizard set reads "Blue wizard hat (g)" in this revision, and
     * "Leather body" is `leather_armour`. A typo would resolve to a real but wrong item and nothing
     * else would notice, so the display names are asserted here.
     */
    @Test
    fun GameTestState.`the starter kit is the items it claims to be`() = runBasicGameTest {
        val named =
            TutorialKitObjs.all.associate { (obj, count) ->
                (cacheTypes.objs[obj.id]?.name ?: "<unresolved ${obj.id}>") to count
            }
        for ((obj, _) in TutorialKitObjs.all) {
            assertNotNull(cacheTypes.objs[obj.id]) { "Starter kit obj $obj does not resolve." }
        }
        val expected =
            mapOf(
                "Iron full helm" to 1,
                "Iron platebody" to 1,
                "Iron platelegs" to 1,
                "Iron kiteshield" to 1,
                "Bronze scimitar" to 1,
                "Iron scimitar" to 1,
                "Steel scimitar" to 1,
                "Black scimitar" to 1,
                "Mithril scimitar" to 1,
                "Adamant scimitar" to 1,
                "Rune scimitar" to 1,
                "Shortbow" to 1,
                "Bronze arrow" to 100,
                "Leather body" to 1,
                "Leather chaps" to 1,
                "Blue wizard hat (g)" to 1,
                "Blue wizard robe (g)" to 1,
                "Blue skirt (g)" to 1,
                "Staff of air" to 1,
                "Air rune" to 200,
                "Water rune" to 200,
                "Earth rune" to 200,
                "Fire rune" to 200,
                "Mind rune" to 200,
                "Coins" to 10_000,
            )
        assertEquals(expected, named)
    }

    /**
     * The design screen is bound button-by-button, so a component that failed to resolve would go
     * unnoticed until a player clicked a dead arrow. Every style slot must also offer options for
     * both genders, or cycling that row would divide by zero.
     */
    @Test
    fun GameTestState.`the character design screen resolves and every slot has options`() =
        runBasicGameTest {
            assertNotNull(cacheTypes.interfaces[TutorialDesignInterfaces.player_design.id]) {
                "The character design interface does not resolve."
            }
            val buttons =
                listOf(
                    TutorialDesignComponents.head_left to "head_left",
                    TutorialDesignComponents.head_right to "head_right",
                    TutorialDesignComponents.hair_left to "hair_left",
                    TutorialDesignComponents.hair_right to "hair_right",
                    TutorialDesignComponents.gender_male to "gender_male",
                    TutorialDesignComponents.gender_female to "gender_female",
                    TutorialDesignComponents.confirm to "confirm",
                )
            for ((button, name) in buttons) {
                val comp = cacheTypes.components[button.packed]
                assertNotNull(comp) { "Design button $name does not resolve." }
            }
            for (slot in StyleSlot.entries) {
                assertTrue(slot.options(female = false).isNotEmpty()) {
                    "$slot has no male styles."
                }
                assertTrue(slot.options(female = true).isNotEmpty()) {
                    "$slot has no female styles."
                }
            }
            // The two genders' kit ids are entirely separate ranges; an overlap would mean a style
            // carried across a gender switch could silently mean a different body part.
            for (slot in StyleSlot.entries) {
                val shared = slot.options(false).intersect(slot.options(true).toSet())
                assertTrue(shared.isEmpty()) { "$slot shares kit ids across genders: $shared" }
            }
        }

    /**
     * The rats are the point of the combat step, so a rat authored into the cave wall is a step the
     * player cannot finish. They are checked separately from the instructors because they are not
     * in [TutorialNpcs.all].
     */
    @Test
    fun GameTestState.`the giant rats stand in the combat chamber`() = runBasicGameTest {
        val rats = parseSpawns().filter { it.npc.startsWith("newbiegiantrat") }
        assertTrue(rats.size >= 8) { "The combat step has almost nothing to fight: ${rats.size}." }
        val blocked = rats.filter { blocksStanding(it.coords) }
        assertTrue(blocked.isEmpty()) { "These rats are authored into scenery: $blocked" }
        // Scurrius, the level-46 boss, shares the display name "Giant rat" and was bridged here by
        // a name match once already.
        val boss = parseSpawns().filter { it.npc == "rat_boss_giant_rat" }
        assertTrue(boss.isEmpty()) { "The level-46 boss rat is spawned on Tutorial Island." }
    }

    private fun GameTestState.blocksStanding(coords: CoordGrid): Boolean =
        collision[coords.x, coords.z, coords.level] and BLOCKS_STANDING != 0

    private data class Spawn(val npc: String, val coords: CoordGrid)

    /** Parsed straight out of the packed resource, so the test and the packer cannot drift. */
    private fun parseSpawns(): List<Spawn> {
        // Anchored on `TutorialIsland`, which shares the resource's package -- the same class
        // `TutorialNpcSpawns.onPackMapTask` hands to `resourceFile`, so the test and the packer
        // read the identical file.
        val text =
            TutorialIsland::class.java.getResourceAsStream("npcs.toml")?.bufferedReader()?.use {
                it.readText()
            } ?: error("`npcs.toml` is not on the classpath next to TutorialIsland.")
        return SPAWN_PATTERN.findAll(text)
            .map { match ->
                val (npc, level, msqX, msqZ, localX, localZ) = match.destructured
                Spawn(
                    npc,
                    CoordGrid(
                        level.toInt(),
                        msqX.toInt(),
                        msqZ.toInt(),
                        localX.toInt(),
                        localZ.toInt(),
                    ),
                )
            }
            .toList()
    }

    private companion object {
        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        /** The lowest z of the cave under the island; the surface tops out far below this. */
        const val CAVE_Z = 9000

        val CARDINAL = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1)

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
