package org.rsmod.content.other.consumables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.other.consumables.configs.Consumables
import org.rsmod.game.type.obj.UnpackedObjType

/**
 * The test that makes "everything edible" enforceable rather than aspirational.
 *
 * It re-derives the cache's own edible set - the same filter `EdibleObjDump` used to author the
 * table - and fails if anything has been left out. Without this the table rots silently the moment
 * the cache revision moves.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ConsumableCoverageTest {
    @Test
    fun GameTestState.`every edible obj in the cache has a row`() = runBasicGameTest {
        val missing =
            cacheTypes.objs.values
                .filter { isConsumable(it) }
                .filterNot { it.id in Consumables.byObjId }
                .sortedBy { it.id }
        assertTrue(missing.isEmpty()) {
            "Objs the cache says are consumable but the table does not cover:\n" +
                missing.joinToString("\n") { "  ${it.id} ${it.internalName} '${it.name}'" }
        }
    }

    @Test
    fun GameTestState.`no row covers an obj the cache cannot consume`() = runBasicGameTest {
        val stray =
            Consumables.rows
                .mapNotNull { cacheTypes.objs[it.obj.id] }
                .filterNot { isConsumable(it) }
                .sortedBy { it.id }
        assertTrue(stray.isEmpty()) {
            "Rows for objs carrying no Eat or Drink op:\n" +
                stray.joinToString("\n") { "  ${it.id} ${it.internalName} iops=${it.iop.toList()}" }
        }
    }

    @Test
    fun GameTestState.`no obj is listed twice`() = runBasicGameTest {
        val duplicates =
            Consumables.rows.groupBy { it.obj.id }.filterValues { it.size > 1 }.keys.sorted()
        assertTrue(duplicates.isEmpty()) {
            val names = duplicates.mapNotNull { cacheTypes.objs[it]?.internalName }
            "Objs with more than one row: $names"
        }
        assertEquals(Consumables.rows.size, Consumables.byObjId.size)
    }

    @Test
    fun GameTestState.`the editor tags every row into the food group`() = runBasicGameTest {
        val untagged =
            Consumables.rows
                .mapNotNull { cacheTypes.objs[it.obj.id] }
                .filterNot { it.isContentType(content.food) }
        assertTrue(untagged.isEmpty()) {
            "Rows the ConsumableEditor did not tag: ${untagged.map { it.internalName }}"
        }
    }

    /**
     * A multi-bite chain has to end somewhere reachable.
     *
     * Every stage that is itself consumable must have a row, or a half-eaten cake becomes an obj
     * the script refuses to touch. A `next` that is *not* consumable is fine and expected - that is
     * the empty jug a drink hands back.
     */
    @Test
    fun GameTestState.`every consumable next stage has a row`() = runBasicGameTest {
        val orphans =
            Consumables.rows
                .mapNotNull { it.next }
                .mapNotNull { cacheTypes.objs[it.id] }
                .filter { isConsumable(it) }
                .filterNot { it.id in Consumables.byObjId }
        assertTrue(orphans.isEmpty()) {
            "Chain stages with no row of their own: ${orphans.map { it.internalName }}"
        }
    }

    /**
     * A stackable food must never be a chain stage.
     *
     * `invReplaceSlot` swaps the whole slot, so giving a stack of purple sweets a `next` would turn
     * every sweet in the stack into one of whatever came next.
     */
    @Test
    fun GameTestState.`no stackable consumable takes bites`() = runBasicGameTest {
        val biting =
            Consumables.rows
                .filter { it.next != null }
                .mapNotNull { cacheTypes.objs[it.obj.id] }
                .filter { it.isStackable }
        assertTrue(biting.isEmpty()) {
            "Stackable objs wired as multi-bite: ${biting.map { it.internalName }}"
        }
    }

    @Test
    fun GameTestState.`every row resolved against the cache`() = runBasicGameTest {
        val unresolved = Consumables.rows.filter { it.obj.id < 0 }
        assertTrue(unresolved.isEmpty()) {
            "Rows whose obj never resolved: ${unresolved.map { it.obj.internalName }}"
        }
    }

    private companion object {
        private val CONSUME_OPS = setOf("Eat", "Drink")

        /**
         * The cache's own definition of edible, and the same filter the table was authored from.
         *
         * The Herblore dose ladder is excluded, and the test for it is deliberately narrow: a dose
         * suffix *and* a `Drink` op, never the suffix alone. The Chambers of Xeric food is named
         * "Pysk fish (0)" through "Kyren fish (6)" and is eaten, not drunk - filtering on the
         * suffix by itself quietly dropped fourteen real foods.
         */
        private fun isConsumable(type: UnpackedObjType): Boolean {
            if (type.iop.none { it in CONSUME_OPS }) {
                return false
            }
            val dosed = type.iop.any { it == "Drink" } && DOSE_SUFFIX.containsMatchIn(type.name)
            return !dosed
        }

        private val DOSE_SUFFIX = Regex("""\([1-4]\)$""")
    }
}
