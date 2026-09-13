package org.rsmod.content.custom.leagues

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.leagues.configs.LeagueTaskListEnum
import org.rsmod.content.custom.leagues.configs.league_task_params
import org.rsmod.content.custom.leagues.configs.league_task_structs
import org.rsmod.content.custom.leagues.configs.league_task_varps
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.tasks.LeagueTask
import org.rsmod.content.custom.leagues.tasks.LeagueTaskTier
import org.rsmod.content.custom.leagues.tasks.LeagueTasks
import org.rsmod.content.custom.leagues.tasks.Trigger
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * Keeps the Kotlin task table, the symbol file and the packed cache in step.
 *
 * The cache half only passes once the builders have been packed (`./gradlew packCache`) and one
 * boot has applied the editors; until then the table half still guards the invariants the ids
 * depend on.
 */
class LeagueTaskTableTest {
    @Test
    fun `ids are the declaration index and names are unique`() {
        for ((index, task) in LeagueTasks.all.withIndex()) {
            assertEquals(index, task.id, "task ${task.name}")
        }
        val names = LeagueTasks.all.map { it.name.lowercase() }
        assertEquals(names.size, names.toSet().size, "duplicate task names")
        val slugs = LeagueTasks.all.map { it.slug }
        assertEquals(slugs.size, slugs.toSet().size, "duplicate task slugs")
        assertTrue(LeagueTasks.all.size <= 32 * league_task_varps.COMPLETION_VARPS) {
            "more tasks than completion bits"
        }
    }

    @Test
    fun `counted tasks have targets and the rest complete at once`() {
        for (task in LeagueTasks.all) {
            when (val trigger = task.trigger) {
                is Trigger.KillNpc -> assertTrue(trigger.count >= 1 && trigger.names.isNotEmpty())
                is Trigger.Gather -> assertTrue(trigger.count >= 1, task.name)
                is Trigger.Equip -> assertTrue(trigger.objs.isNotEmpty(), task.name)
                else -> assertEquals(1, task.target, task.name)
            }
        }
    }

    @Test
    fun `thresholds rise and leave points to spare`() {
        assertEquals(Relic.TIER_COUNT, Relic.TIER_POINTS.size)
        assertEquals(0, Relic.TIER_POINTS.first())
        assertTrue(Relic.TIER_POINTS.zipWithNext().all { (a, b) -> a < b })
        val total = LeagueTasks.totalPoints
        assertTrue(Relic.TIER_POINTS.last() <= total * 7 / 10) {
            "tier 8 needs ${Relic.TIER_POINTS.last()} of $total points; keep it under 70%"
        }
        println("LEAGUE TASKS total=${LeagueTasks.all.size} points=$total")
        for (tier in LeagueTaskTier.entries) {
            val inTier = LeagueTasks.all.filter { it.tier == tier }
            println("  $tier: ${inTier.size} tasks, ${inTier.sumOf { it.points }} points")
        }
    }

    @Test
    fun `struct symbols match the table`() {
        val expected =
            LeagueTasks.all.map { "${LeagueTask.STRUCT_ID_BASE + it.id}\t${it.structName}" }
        val file = Path.of(".data/symbols/.local/struct.sym")
        val actual = Files.readAllLines(file).filter { it.contains("\tleague_task_") }
        if (expected != actual) {
            println("Expected .local/struct.sym task block:")
            expected.forEach(::println)
        }
        assertEquals(expected, actual, "paste the printed block into $file")

        val enumLines = Files.readAllLines(Path.of(".data/symbols/.local/enum.sym"))
        assertTrue("${LeagueTaskListEnum.ID}\t${LeagueTaskListEnum.NAME}" in enumLines) {
            "enum.sym must map ${LeagueTaskListEnum.NAME} to ${LeagueTaskListEnum.ID}"
        }
    }

    @Test
    fun GameTestState.`level tasks name released stats`() = runBasicGameTest {
        for (task in LeagueTasks.all) {
            val trigger = task.trigger as? Trigger.SkillLevel ?: continue
            val stat = cacheTypes.stats[trigger.stat]
            assertTrue(!stat.unreleased) { "${task.name} names an unreleased stat" }
            assertTrue(trigger.level in 2..stat.maxLevel) { "${task.name} has an impossible level" }
        }
    }

    @Test
    fun GameTestState.`packed cache matches the table`() = runBasicGameTest {
        val list = checkNotNull(cacheTypes.enums[LeagueTaskListEnum.ID]) { "task enum not packed" }
        assertEquals(LeagueTasks.all.size, list.primitiveMap.size, "task enum size")
        for (task in LeagueTasks.all) {
            val structId = LeagueTask.STRUCT_ID_BASE + task.id
            assertEquals(structId, list.primitiveMap[task.id], "enum entry for ${task.name}")
            val params =
                checkNotNull(cacheTypes.structs[structId]?.paramMap?.primitiveMap) {
                    "no packed struct for ${task.name}"
                }
            assertEquals(task.id, params[league_task_params.index.id], "index of ${task.name}")
            assertEquals(task.name, params[league_task_params.name.id])
            assertEquals(task.description, params[league_task_params.description.id])
            assertEquals(task.tier.cacheValue, params[league_task_params.tier.id])
            assertEquals(task.type.cacheValue, params[league_task_params.type.id])
            assertEquals(task.area.cacheValue, params[league_task_params.area.id])
            assertEquals(task.skill, params[league_task_params.skill.id])
        }

        val league = checkNotNull(cacheTypes.structs[league_task_structs.league.id]?.paramMap)
        assertEquals(
            LeagueTaskListEnum.ID,
            league.primitiveMap[league_task_params.tasks_enum.id],
            "league struct's task enum",
        )
        for ((tier, struct) in league_task_structs.tiers.withIndex()) {
            val params = checkNotNull(cacheTypes.structs[struct.id]?.paramMap)
            assertEquals(
                Relic.TIER_POINTS[tier],
                params.primitiveMap[league_task_params.tier_points.id],
                "tier ${tier + 1} threshold",
            )
        }
    }

    @Test
    fun GameTestState.`visit boxes sit on the map`() = runBasicGameTest {
        for (task in LeagueTasks.all) {
            val trigger = task.trigger as? Trigger.VisitArea ?: continue
            val box = trigger.box
            val blocked = CollisionFlag.BLOCK_WALK or CollisionFlag.BLOCK_PLAYERS
            var walkable = 0
            for (x in box.x1..box.x2) {
                for (z in box.z1..box.z2) {
                    if (!collision.isZoneAllocated(x, z, box.level)) {
                        continue
                    }
                    if (collision[CoordGrid(x, z, box.level)] and blocked == 0) {
                        walkable++
                    }
                }
            }
            assertTrue(walkable > 0) { "${task.name}: no walkable tile in $box" }
        }
    }
}
