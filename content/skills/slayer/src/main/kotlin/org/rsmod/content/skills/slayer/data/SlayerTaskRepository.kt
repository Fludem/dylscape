package org.rsmod.content.skills.slayer.data

import com.github.michaelbull.logging.InlineLogger
import it.unimi.dsi.fastutil.ints.Int2IntMap
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.params
import org.rsmod.api.config.refs.stats
import org.rsmod.content.skills.slayer.SlayerAssignment
import org.rsmod.content.skills.slayer.SlayerTask
import org.rsmod.content.skills.slayer.configs.slayer_columns
import org.rsmod.content.skills.slayer.configs.slayer_tables
import org.rsmod.game.dbtable.DbRow
import org.rsmod.game.dbtable.DbTableResolver
import org.rsmod.game.type.TypeResolver
import org.rsmod.game.type.npc.NpcTypeList

/**
 * Everything Slayer knows, read once out of the cache's own dbtables.
 *
 * This is modelled on `MusicRepository`, the only other runtime dbtable consumer in the repo: an
 * injected [DbTableResolver], a [load] called at game startup, and immutable maps afterwards.
 *
 * The npc index is the piece with no dbtable behind it. `slayer_task` has no npc column, but npc
 * types carry the task id in their `slayer_task` param (vanilla param 50), so the index is built by
 * sweeping every npc type once at startup rather than authoring a mapping by hand.
 */
@Singleton
class SlayerTaskRepository
@Inject
constructor(private val dbTables: DbTableResolver, private val npcTypes: NpcTypeList) {
    private lateinit var tasksById: Int2ObjectMap<SlayerTask>
    private lateinit var assignmentsByMaster: Int2ObjectMap<List<SlayerAssignment>>
    private lateinit var taskByNpc: Int2IntMap
    private lateinit var tasksWithNpcs: IntSet

    /** [load] is called from more than one script's start-up, and doing it twice is waste. */
    private var loaded = false

    /** The task with this `slayer_task:id`, or null if nothing defines it. */
    fun task(id: Int): SlayerTask? = tasksById[id]

    /** Everything [masterId] can assign, before any per-player filtering. */
    fun assignments(masterId: Int): List<SlayerAssignment> =
        assignmentsByMaster[masterId] ?: emptyList()

    /**
     * The task this npc counts toward, or `0` when it counts toward none.
     *
     * Zero is the cache's own default for the param, so "not a slayer monster" needs no sentinel of
     * our own.
     */
    fun taskIdForNpc(npcId: Int): Int = taskByNpc.get(npcId)

    /**
     * Whether anything in this cache counts toward [taskId].
     *
     * A handful of rows in `slayer_master_task` name tasks with no npc behind them in this rev, and
     * a player handed one could never finish it - so assignment checks this. Kept as a prebuilt set
     * because it is asked once per candidate on every roll.
     */
    fun hasNpcs(taskId: Int): Boolean = tasksWithNpcs.contains(taskId)

    /** Npc type id -> task id, for everything that counts toward a task. */
    fun allNpcTasks(): Map<Int, Int> = taskByNpc

    /** Every npc that counts toward [taskId]. The scan is fine for tests and task interfaces. */
    fun npcsForTask(taskId: Int): List<Int> =
        taskByNpc.int2IntEntrySet().filter { it.intValue == taskId }.map { it.intKey }

    fun load() {
        if (loaded) {
            return
        }
        val unlockBits = loadUnlockBits()

        val tasksByRow = Int2ObjectOpenHashMap<SlayerTask>()
        val tasksById = Int2ObjectOpenHashMap<SlayerTask>()
        for (row in dbTables[slayer_tables.task]) {
            val task = row.toTask() ?: continue
            tasksByRow[TypeResolver[row.type]] = task
            tasksById[task.id] = task
        }
        this.tasksById = tasksById

        val assignments = Int2ObjectOpenHashMap<MutableList<SlayerAssignment>>()
        var orphaned = 0
        for (row in dbTables[slayer_tables.masterTask]) {
            val masterId = row.getOrNull(slayer_columns.masterId) ?: continue
            val taskRow = row.getOrNull(slayer_columns.masterTaskRow) ?: continue
            val task = tasksByRow[TypeResolver[taskRow]]
            if (task == null) {
                orphaned++
                continue
            }
            val weight = row.getOrNull(slayer_columns.masterWeight) ?: 0
            if (weight <= 0) {
                continue
            }
            val unlockRow = row.getOrNull(slayer_columns.masterTaskUnlock)
            val assignment =
                SlayerAssignment(
                    task = task,
                    weight = weight,
                    minAmount = row.getOrNull(slayer_columns.masterMinAmount) ?: 0,
                    maxAmount = row.getOrNull(slayer_columns.masterMaxAmount) ?: 0,
                    unlockBit =
                        unlockRow?.let { unlockBits[TypeResolver[it]] }?.takeIf { it != NO_BIT },
                )
            assignments.computeIfAbsent(masterId) { mutableListOf() } += assignment
        }
        this.assignmentsByMaster =
            Int2ObjectOpenHashMap(assignments.mapValues { it.value.toList() })

        val taskByNpc = Int2IntOpenHashMap()
        taskByNpc.defaultReturnValue(NO_TASK)
        for ((npcId, type) in npcTypes) {
            val taskId = type.param(params.slayer_task)
            if (taskId != NO_TASK && tasksById.containsKey(taskId)) {
                taskByNpc[npcId] = taskId
            }
        }
        this.taskByNpc = taskByNpc
        this.tasksWithNpcs = IntOpenHashSet(taskByNpc.values)
        this.loaded = true

        logger.info {
            "Loaded ${tasksById.size} slayer tasks, " +
                "${assignments.values.sumOf(List<SlayerAssignment>::size)} master assignments " +
                "across ${assignments.size} masters, and ${taskByNpc.size} task npcs" +
                if (orphaned > 0) " ($orphaned assignments had no task row)." else "."
        }
    }

    private fun DbRow.toTask(): SlayerTask? {
        val id = getOrNull(slayer_columns.taskId) ?: return null
        val statReqs = getOrNull(slayer_columns.taskStatReqs).orEmpty()
        val slayerLevel =
            statReqs.firstOrNull { TypeResolver[it.stat] == TypeResolver[stats.slayer] }?.level ?: 1
        return SlayerTask(
            id = id,
            name = getOrNull(slayer_columns.taskName) ?: return null,
            displayName = getOrNull(slayer_columns.taskDisplayName) ?: return null,
            minCombat = getOrNull(slayer_columns.taskMinCombat) ?: 1,
            slayerLevel = slayerLevel,
        )
    }

    /** Every `slayer_unlock` row's own dbrow id -> the bit it occupies in the unlock bitset. */
    private fun loadUnlockBits(): Int2IntMap {
        val bits = Int2IntOpenHashMap()
        bits.defaultReturnValue(NO_BIT)
        for (row in dbTables[slayer_tables.unlock]) {
            val bit = row.getOrNull(slayer_columns.unlockBit) ?: continue
            bits[TypeResolver[row.type]] = bit
        }
        return bits
    }

    private companion object {
        /** The `slayer_task` param's own cache default, reused as "no task". */
        const val NO_TASK = 0

        /** Distinct from [NO_TASK]: bit 0 is a real unlock, so absence needs its own value. */
        const val NO_BIT = -1

        private val logger = InlineLogger()
    }
}
