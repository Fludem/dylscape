package org.rsmod.content.custom.droptables

import org.rsmod.game.type.obj.ObjType

/**
 * A drop table in the shape the official game uses: a set of guaranteed drops, zero or more
 * weighted tables that are each rolled once per kill, and independently-rolled tertiary drops.
 *
 * Build these with the [dropTable] dsl rather than calling the constructor directly - the builder
 * validates that weights add up to their denominator.
 */
class DropTable(
    val always: List<DropQuantity>,
    val tables: List<WeightedTable>,
    val tertiary: List<TertiaryDrop>,
)

/**
 * A weighted table rolled as `1 in [denominator]`, where every slot claims [DropSlot.weight] of
 * those outcomes. Slot weights are guaranteed to sum to [denominator].
 */
class WeightedTable(val denominator: Int, val slots: List<DropSlot>)

/** A drop that has not been rolled yet; [count] is the inclusive quantity range. */
class DropQuantity(val obj: ObjType, val count: IntRange)

/** A drop rolled independently of the main tables, at a `1 in [oneIn]` chance. */
class TertiaryDrop(val oneIn: Int, val drop: DropQuantity)

/** A drop that has been rolled and is ready to be spawned on the ground. */
data class RolledDrop(val obj: ObjType, val count: Int)

/** One outcome within a [WeightedTable]. */
sealed class DropSlot {
    abstract val weight: Int

    /** Drops [drop] when rolled. */
    class Item(override val weight: Int, val drop: DropQuantity) : DropSlot()

    /** Rolls [table] when rolled, allowing sub-tables such as the shared herb table. */
    class Nested(override val weight: Int, val table: WeightedTable) : DropSlot()

    /** Drops nothing when rolled. Every official table reserves some weight for this. */
    class Empty(override val weight: Int) : DropSlot()
}

@DslMarker private annotation class DropTableDsl

@DropTableDsl
class DropTableBuilder {
    private val always = mutableListOf<DropQuantity>()
    private val tables = mutableListOf<WeightedTable>()
    private val tertiary = mutableListOf<TertiaryDrop>()

    fun always(obj: ObjType, count: Int = 1) {
        always(obj, count..count)
    }

    fun always(obj: ObjType, count: IntRange) {
        always += DropQuantity(obj, count)
    }

    fun table(outOf: Int, init: WeightedTableBuilder.() -> Unit) {
        tables += WeightedTableBuilder(outOf).apply(init).build()
    }

    fun tertiary(oneIn: Int, obj: ObjType, count: Int = 1) {
        tertiary += TertiaryDrop(oneIn, DropQuantity(obj, count..count))
    }

    fun build(): DropTable = DropTable(always.toList(), tables.toList(), tertiary.toList())
}

@DropTableDsl
class WeightedTableBuilder(private val denominator: Int) {
    private val slots = mutableListOf<DropSlot>()

    fun drop(weight: Int, obj: ObjType, count: Int = 1) {
        drop(weight, obj, count..count)
    }

    fun drop(weight: Int, obj: ObjType, count: IntRange) {
        slots += DropSlot.Item(weight, DropQuantity(obj, count))
    }

    fun nested(weight: Int, table: WeightedTable) {
        slots += DropSlot.Nested(weight, table)
    }

    fun nothing(weight: Int) {
        slots += DropSlot.Empty(weight)
    }

    fun build(): WeightedTable {
        val total = slots.sumOf(DropSlot::weight)
        require(total == denominator) {
            "Weighted table slots must sum to their denominator: " +
                "expected $denominator, but slots summed to $total."
        }
        return WeightedTable(denominator, slots.toList())
    }
}

/** Entry point for the drop table dsl. @see [DropTableBuilder] */
fun dropTable(init: DropTableBuilder.() -> Unit): DropTable = DropTableBuilder().apply(init).build()
