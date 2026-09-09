package org.rsmod.content.skills.slayer.configs

import org.rsmod.api.type.refs.dbcol.DbColumnReferences
import org.rsmod.api.type.refs.dbtable.DbTableReferences
import org.rsmod.game.dbtable.DbColumnCodec
import org.rsmod.game.stat.StatRequirement
import org.rsmod.game.type.TypeListMap
import org.rsmod.game.type.literal.CacheVarLiteral

internal typealias slayer_tables = SlayerDbTables

internal typealias slayer_columns = SlayerDbColumns

/**
 * Jagex's own Slayer database, which ships in this cache and is what the whole skill is built on
 * rather than a hand-authored table.
 *
 * `slayer_task` (113) defines the tasks, `slayer_master_task` (114) is every master's weighted
 * assignment list with its amount ranges, and `slayer_unlock` (117) is the reward table with costs
 * and descriptions. `slayer_area` (115) and `slayer_task_sublist` (116) also exist but are only
 * needed by Konar's location-locked tasks and the sublist indirection, neither of which the six
 * masters here use.
 */
object SlayerDbTables : DbTableReferences() {
    val task = find("slayer_task")
    val masterTask = find("slayer_master_task")
    val unlock = find("slayer_unlock")
}

/**
 * The columns actually read. Several more are declared in `dbcol.sym` than this rev's cache defines
 * a type for - `slayer_task:equipment_required`, `:slayer_tip`, `:task_sublist`,
 * `:quests_required_all` and `:twisted_min_comlevel` all have **no type entry in table 113**, so
 * they carry no data here and cannot be bound. The protective-gear requirements are authored in
 * [SlayerGear] instead, for exactly that reason.
 */
object SlayerDbColumns : DbColumnReferences() {
    val taskId = int("slayer_task:id")
    val taskMinCombat = int("slayer_task:min_comlevel")
    val taskStatReqs = list("slayer_task:min_stat_requirement_all", SlayerStatReqCodec)
    val taskName = string("slayer_task:name_lowercase")
    val taskDisplayName = string("slayer_task:name_uppercase")

    val masterId = int("slayer_master_task:master_id")
    val masterTaskRow = dbRow("slayer_master_task:task")
    val masterWeight = int("slayer_master_task:weight")
    val masterMinAmount = int("slayer_master_task:min_amount")
    val masterMaxAmount = int("slayer_master_task:max_amount")

    /**
     * The unlock a row is gated behind, when it has one. "Boss" on Duradel's list is gated on "Like
     * a Boss", for instance, so an unfiltered weighted pick would assign tasks the player has not
     * bought.
     */
    val masterTaskUnlock = dbRow("slayer_master_task:task_unlock")

    val unlockBit = int("slayer_unlock:bit")
    val unlockCost = int("slayer_unlock:cost")
    val unlockName = string("slayer_unlock:name")
    val unlockDescription = string("slayer_unlock:description")
}

/**
 * Reads a `(level, stat)` pair out of a `min_stat_requirement_*` column.
 *
 * This exists because [DbColumnCodec.StatReqCodec] cannot be used here: it declares `[STAT, INT]`
 * and reads the stat first, but these columns are `[INT, STAT]` and store the level first. Abyssal
 * Demons decode as `[85, 18]` - level 85 in stat 18, slayer - so the stock codec would try to
 * resolve stat 85 and read the requirement as level 18.
 */
object SlayerStatReqCodec : DbColumnCodec<Int, StatRequirement> {
    override val types: List<CacheVarLiteral> = listOf(CacheVarLiteral.INT, CacheVarLiteral.STAT)

    override fun decode(
        iterator: DbColumnCodec.Iterator<Int, StatRequirement>,
        types: TypeListMap,
    ): StatRequirement {
        val level = iterator.next()
        val stat = iterator.next()
        return StatRequirement(types.stats.getValue(stat).toHashedType(), level)
    }

    override fun encode(value: StatRequirement): List<Int> = listOf(value.level, value.stat.id)
}
