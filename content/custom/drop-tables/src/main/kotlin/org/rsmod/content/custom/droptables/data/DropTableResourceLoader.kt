package org.rsmod.content.custom.droptables.data

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.parsers.toml.Toml
import org.rsmod.api.utils.io.InputStreams
import org.rsmod.content.custom.droptables.DropQuantity
import org.rsmod.content.custom.droptables.DropSlot
import org.rsmod.content.custom.droptables.DropTable
import org.rsmod.content.custom.droptables.TertiaryDrop
import org.rsmod.content.custom.droptables.WeightedTable
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.game.type.npc.UnpackedNpcType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * Builds the generated drop tables from the toml resources beside this class.
 *
 * Names are resolved through [NpcTypeList] and [ObjTypeList] rather than through `NameMapping`.
 * Both carry the same `internalName` the sym files do - `TypeListMapDecoder` stamps it on during
 * cache decode - and both are bound in the test injector, whereas `SymbolModule` is not: an
 * injected `NameMapping` would default-construct to an empty one under test and silently resolve
 * nothing.
 *
 * A row the data gets wrong is collected into [Result.errors] and skips that npc rather than
 * aborting the boot, so one stale name out of fifteen thousand cannot take the server down. The
 * integration test asserts the list is empty, which is where a bad import is meant to be caught.
 */
@Singleton
class DropTableResourceLoader
@Inject
constructor(
    @Toml private val mapper: ObjectMapper,
    private val npcTypes: NpcTypeList,
    private val objTypes: ObjTypeList,
) {
    // Not every cache type has a symbol, so the null names are dropped rather than asserted on.
    private val npcsByName: Map<String, UnpackedNpcType> by lazy {
        npcTypes.values.mapNotNull { type -> type.internalName?.let { it to type } }.toMap()
    }

    private val objsByName: Map<String, UnpackedObjType> by lazy {
        objTypes.values.mapNotNull { type -> type.internalName?.let { it to type } }.toMap()
    }

    fun load(): Result {
        val errors = mutableListOf<String>()
        val assignments = mutableListOf<Pair<UnpackedNpcType, DropTable>>()
        val unattackable = mutableListOf<String>()
        val claimed = mutableMapOf<Int, String>()

        val index = read<DropTableIndex>(INDEX_FILE)
        for (shardName in index.shard) {
            val shard = read<DropTableShard>(shardName)
            for (entry in shard.table) {
                val table = entry.toDropTable(errors)
                if (table == null) {
                    continue
                }
                for (name in entry.npc) {
                    val npc = npcsByName[name]
                    if (npc == null) {
                        errors += "${entry.source}: unknown npc '$name'."
                        continue
                    }
                    val previous = claimed.put(npc.id, entry.source)
                    if (previous != null) {
                        errors += "${entry.source}: npc '$name' already claimed by '$previous'."
                        continue
                    }
                    // A wiki page lists every id a monster goes by, which includes its cutscene,
                    // reset and sub-entity forms. Those never die to a player, so a table on them
                    // is dead weight rather than an error in the data.
                    if (npc.op.none { it.equals(ATTACK_OP, ignoreCase = true) }) {
                        unattackable += name
                        continue
                    }
                    assignments += npc to table
                }
            }
        }

        return Result(assignments, errors, unattackable)
    }

    /**
     * Loads a `[[table]]` file that is keyed by `source` rather than by npc, such as the clue
     * chest's reward caskets. [anchor] is the class whose package the resource sits in, so another
     * module can keep its own tables beside its own code.
     *
     * Same leniency as [load]: a bad table is left out and explained in [SourceResult.errors].
     */
    fun loadSources(anchor: Class<*>, fileName: String): SourceResult {
        val bytes =
            anchor.getResourceAsStream(fileName)?.use { it.readAllBytes() }
                ?: return SourceResult(emptyMap(), listOf("No '$fileName' beside ${anchor.name}."))
        val errors = mutableListOf<String>()
        val tables = mutableMapOf<String, DropTable>()
        for (entry in mapper.readValue(bytes, DropTableShard::class.java).table) {
            val table = entry.toDropTable(errors) ?: continue
            if (tables.put(entry.source, table) != null) {
                errors += "${entry.source}: more than one table has this source."
            }
        }
        return SourceResult(tables, errors)
    }

    private inline fun <reified T> read(fileName: String): T {
        val bytes = InputStreams.readAllBytes<DropTableResourceLoader>(fileName)
        return mapper.readValue(bytes, T::class.java)
    }

    /** Null when the table is unusable; every reason is appended to [errors] first. */
    private fun TomlDropTable.toDropTable(errors: MutableList<String>): DropTable? {
        val always = mutableListOf<DropQuantity>()
        for (drop in this.always) {
            always +=
                resolveQuantity(drop.obj, drop.count, drop.countMax, drop.noted, errors)
                    ?: return null
        }

        val tables = mutableListOf<WeightedTable>()
        if (drop.isNotEmpty()) {
            val slots = mutableListOf<DropSlot>()
            var total = 0
            for (slot in drop) {
                if (slot.weight <= 0) {
                    errors += "$source: slot weight must be positive, was ${slot.weight}."
                    return null
                }
                total += slot.weight
                if (slot.obj == null) {
                    slots += DropSlot.Empty(slot.weight)
                    continue
                }
                val quantity =
                    resolveQuantity(slot.obj, slot.count, slot.countMax, slot.noted, errors)
                        ?: return null
                slots += DropSlot.Item(slot.weight, quantity)
            }
            if (total != outOf) {
                errors += "$source: slots sum to $total but the table rolls out of $outOf."
                return null
            }
            if (outOf > MAX_DENOMINATOR) {
                // `DropTableRoller` scales weights into `denominator * boost.all.den *
                // boost.rare.den`, so anything larger than this would overflow `Int` on a boosted
                // roll.
                errors += "$source: denominator $outOf exceeds the safe maximum $MAX_DENOMINATOR."
                return null
            }
            tables += WeightedTable(outOf, slots)
        }

        val tertiary = mutableListOf<TertiaryDrop>()
        for (drop in this.tertiary) {
            if (drop.oneIn <= 0) {
                errors += "$source: tertiary `one_in` must be positive, was ${drop.oneIn}."
                return null
            }
            val quantity =
                resolveQuantity(drop.obj, drop.count, drop.countMax, drop.noted, errors)
                    ?: return null
            tertiary += TertiaryDrop(drop.oneIn, quantity)
        }

        if (always.isEmpty() && tables.isEmpty() && tertiary.isEmpty()) {
            errors += "$source: table has no drops."
            return null
        }
        return DropTable(always, tables, tertiary)
    }

    private fun resolveQuantity(
        name: String?,
        count: Int,
        countMax: Int?,
        noted: Boolean,
        errors: MutableList<String>,
    ): DropQuantity? {
        if (name == null) {
            errors += "Drop is missing an `obj`."
            return null
        }
        val base = objsByName[name]
        if (base == null) {
            errors += "Unknown obj '$name'."
            return null
        }
        val high = countMax ?: count
        if (count <= 0 || high < count) {
            errors += "Obj '$name' has an invalid count range $count..$high."
            return null
        }
        // Noted drops name the unnoted obj; `certlink` is the cache's own pointer to its note.
        val obj =
            if (!noted) {
                base
            } else {
                objTypes[base.certlink]
                    ?: run {
                        errors += "Obj '$name' is dropped noted but has no `certlink`."
                        return null
                    }
            }
        return DropQuantity(obj, count..high)
    }

    class Result(
        val assignments: List<Pair<UnpackedNpcType, DropTable>>,
        val errors: List<String>,
        /** Npcs the wiki gave a table but that cannot be attacked in this cache. */
        val unattackable: List<String>,
    )

    class SourceResult(val tables: Map<String, DropTable>, val errors: List<String>)

    private companion object {
        private const val INDEX_FILE = "index.toml"

        private const val ATTACK_OP = "Attack"

        /**
         * `Int.MAX_VALUE / (boost.all.den * boost.rare.den)`, rounded down to the largest
         * denominator the generator can emit.
         */
        private const val MAX_DENOMINATOR = 10_000_000
    }
}
