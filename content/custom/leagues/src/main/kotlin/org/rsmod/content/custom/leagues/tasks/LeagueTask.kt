package org.rsmod.content.custom.leagues.tasks

import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.stat.StatType
import org.rsmod.map.CoordGrid

/** The five difficulties the vanilla list knows. [points] is what enum 2671 pays for the tier. */
enum class LeagueTaskTier(val points: Int, val cacheValue: Int) {
    Easy(10, 1),
    Medium(30, 2),
    Hard(80, 3),
    Elite(200, 4),
    Master(400, 5),
}

/** The "Type:" line and the type filter, from enum 3411. */
enum class LeagueTaskType(val cacheValue: Int) {
    Skill(1),
    Combat(2),
    Quest(3),
    Achievement(4),
    Minigame(5),
    Other(6),
}

/** The "Area:" line and the area filter, from enum 3412 (the filter maps onto these values). */
enum class LeagueTaskArea(val cacheValue: Int) {
    Global(0),
    Misthalin(1),
    Karamja(2),
    Asgarnia(3),
    Kandarin(4),
    Morytania(5),
    Desert(6),
    Tirannwn(7),
    Fremennik(8),
    Kourend(10),
    Wilderness(11),
    Varlamore(21),
}

/**
 * A counted skilling action. [skill] is the skill's index in enum 2729, which is what the list's
 * skill filter is keyed on (1 attack ... 23 hunter).
 */
enum class GatherKind(val skill: Int) {
    Mine(13),
    Chop(18),
    Fish(15),
    Cook(16),
    Hunt(23),
    Pickpocket(10),
    Stall(10),
    Smith(14),
    Smelt(14),
    Craft(11),
    Fletch(19),
    Firemake(17),
    Potion(9),
    CleanHerb(9),
    AgilityLap(8),
    Harvest(21),
    BuryBones(7),
    OfferBones(7),
}

/** An inclusive tile box on one level. */
data class Box(val x1: Int, val z1: Int, val x2: Int, val z2: Int, val level: Int = 0) {
    init {
        require(x1 <= x2 && z1 <= z2) { "Box corners must be south-west then north-east: $this" }
    }

    val centre: CoordGrid
        get() = CoordGrid((x1 + x2) / 2, (z1 + z2) / 2, level)

    fun contains(coords: CoordGrid): Boolean =
        coords.level == level && coords.x in x1..x2 && coords.z in z1..z2
}

/** What completes a task. Counted triggers carry a [count]; the rest complete on first sight. */
sealed interface Trigger {
    val count: Int
        get() = 1

    data class SkillLevel(val stat: StatType, val level: Int) : Trigger

    data class TotalLevel(val level: Int) : Trigger

    data class CombatLevel(val level: Int) : Trigger

    /** Kills of any npc whose display name is in [names], compared case-insensitively. */
    data class KillNpc(val names: Set<String>, override val count: Int) : Trigger

    /** [products] empty means any product of that [kind] counts. */
    data class Gather(val kind: GatherKind, val products: Set<ObjType>, override val count: Int) :
        Trigger

    /** Wearing any one of [objs]. */
    data class Equip(val objs: Set<ObjType>) : Trigger

    /** Unlocking a relic in [minTier] (0-based) or above; `null` for any relic at all. */
    data class UnlockRelic(val minTier: Int?) : Trigger

    data class VisitArea(val box: Box) : Trigger
}

/**
 * One league task. [id] is frozen for life: it is the struct's id offset, the completion bit the
 * client reads, and the key the counters are saved under. Retire a task by leaving its slot empty
 * in [LeagueTasks], never by renumbering.
 */
class LeagueTask(
    val id: Int,
    val name: String,
    val description: String,
    val tier: LeagueTaskTier,
    val trigger: Trigger,
    val type: LeagueTaskType,
    val area: LeagueTaskArea,
    val skill: Int,
) {
    val points: Int
        get() = tier.points

    val target: Int
        get() = trigger.count

    /** Whether progress accumulates across events (kills, ores) rather than completing at once. */
    val counted: Boolean
        get() = target > 1

    val slug: String = slugOf(name)

    /** The struct's symbol name in `.data/symbols/.local/struct.sym`. */
    val structName: String = "league_task_$slug"

    /** Where [id] is stored: `league_task_completed_<varp>`, bit [bit]. */
    val completionVarp: Int
        get() = id / 32

    val completionBit: Int
        get() = id % 32

    override fun toString(): String = "LeagueTask(id=$id, name='$name', tier=$tier)"

    companion object {
        /** Task structs are packed at `STRUCT_ID_BASE + id`; see `.local/struct.sym`. */
        const val STRUCT_ID_BASE: Int = 50000

        fun slugOf(name: String): String =
            name.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
    }
}
