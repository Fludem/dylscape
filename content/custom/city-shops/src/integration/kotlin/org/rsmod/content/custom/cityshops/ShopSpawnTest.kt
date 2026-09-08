package org.rsmod.content.custom.cityshops

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.cityshops.map.CityShopNpcSpawns
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Holds the two halves of a shop together.
 *
 * A shop needs a stocked inv *and* a shopkeeper standing somewhere a player can reach. This module
 * shipped with 17 correct shops and zero spawns, and every existing test passed, because they all
 * assert on cache types and a spawn file is not a cache type. These tests read the packed resource
 * back and check it against [ShopAssignments].
 */
class ShopSpawnTest {
    @Test
    fun `every shop npc is spawned somewhere`() {
        val spawned = parseSpawns().map { it.npc }.toSet()
        val missing =
            ShopAssignments.all
                .flatMap { assignment -> assignment.npcs.map { assignment.title to it } }
                .filter { (_, npc) -> npc.internalNameValue !in spawned }
                .filterNot { (_, npc) -> npc.internalNameValue in DELIBERATELY_UNSPAWNED }
        assertTrue(missing.isEmpty()) {
            "These shops are unreachable — nothing places their shopkeeper on the map:\n" +
                missing.joinToString("\n") { (title, npc) ->
                    "  $title <- ${npc.internalNameValue}"
                }
        }
    }

    @Test
    fun `every spawned npc opens a shop`() {
        val assigned = ShopAssignments.all.flatMap { it.npcs }.map { it.internalNameValue }.toSet()
        val orphans = parseSpawns().map { it.npc }.toSet() - assigned
        assertTrue(orphans.isEmpty()) {
            "This module spawns npcs that open no shop, so they are just scenery: $orphans"
        }
    }

    @Test
    fun `no two shopkeepers are authored onto the same tile`() {
        val stacked = parseSpawns().groupBy { it.coords }.filter { it.value.size > 1 }
        assertTrue(stacked.isEmpty()) {
            "Two npcs on one tile — one of them will be standing inside the other:\n" +
                stacked.entries.joinToString("\n") { (coords, at) ->
                    "  $coords <- ${at.map(Spawn::npc)}"
                }
        }
    }

    @Test
    fun GameTestState.`every spawn names an npc the cache knows`() = runBasicGameTest {
        val known = cacheTypes.npcs.values.mapTo(hashSetOf()) { it.internalName }
        val unknown = parseSpawns().filter { it.npc !in known }
        assertTrue(unknown.isEmpty()) {
            "`packCache` hard-errors on these; they are typos in a spawn file:\n" +
                unknown.joinToString("\n") { "  ${it.file}: ${it.npc}" }
        }
    }

    /**
     * The coordinates come from the OSRS Wiki's map templates, which give a shop's marker rather
     * than a walkable tile, so a mistyped or misread one lands in a wall or on a counter. Reading
     * the real collision map is what catches that before anyone walks to Catherby to find out.
     */
    @Test
    fun GameTestState.`every spawn stands on a walkable tile`() = runBasicGameTest {
        val blocked =
            parseSpawns().filter { spawn ->
                val flags = collision[spawn.coords.x, spawn.coords.z, spawn.coords.level]
                flags and BLOCKS_STANDING != 0
            }
        assertTrue(blocked.isEmpty()) {
            "These shopkeepers are authored onto tiles nothing can stand on:\n" +
                blocked.joinToString("\n") { "  ${it.file}: ${it.npc} at ${it.coords}" }
        }
    }

    private data class Spawn(val file: String, val npc: String, val coords: CoordGrid)

    /** Parsed straight out of the packed resources, so the tests and the packer cannot drift. */
    private fun parseSpawns(): List<Spawn> =
        CityShopNpcSpawns.FILES.flatMap { file ->
            val text =
                CityShopNpcSpawns::class.java.getResourceAsStream(file)?.bufferedReader()?.use {
                    it.readText()
                } ?: error("`$file` is listed in CityShopNpcSpawns but is not on the classpath.")
            val spawns =
                SPAWN_PATTERN.findAll(text).map { match ->
                    val (npc, level, msqX, msqZ, localX, localZ) = match.destructured
                    val coords =
                        CoordGrid(
                            level.toInt(),
                            msqX.toInt(),
                            msqZ.toInt(),
                            localX.toInt(),
                            localZ.toInt(),
                        )
                    Spawn(file, npc, coords)
                }
            spawns.toList().also {
                assertTrue(it.isNotEmpty()) { "Parsed no spawns out of `$file`." }
            }
        }

    private companion object {
        /**
         * Bound to a shop but intentionally left off the map. Both are alternate forms of an npc
         * that *is* spawned: `aubury_3op` is the post-Rune Mysteries Aubury whose extra op
         * teleports to an essence mine this server does not have, and `thessalia_league` is the
         * Leagues variant. They stay wired so they trade if anything ever spawns them.
         */
        val DELIBERATELY_UNSPAWNED = setOf("aubury_3op", "thessalia_league")

        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR

        val SPAWN_PATTERN =
            Regex(
                """npc\s*=\s*'([^']+)'\s*\n\s*coords\s*=\s*'(\d+)_(\d+)_(\d+)_(\d+)_(\d+)'""",
                RegexOption.MULTILINE,
            )
    }
}
