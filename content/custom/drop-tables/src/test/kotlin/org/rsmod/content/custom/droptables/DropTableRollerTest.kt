package org.rsmod.content.custom.droptables

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.config.refs.objs
import org.rsmod.api.random.GameRandom

class DropTableRollerTest {
    @Test
    fun `always drops are returned on every roll`() {
        val table = dropTable {
            always(objs.bones)
            always(objs.coins, 25)
        }
        val drops = DropTableRoller(ScriptedRolls()).roll(table)

        assertEquals(2, drops.size)
        assertSame(objs.bones, drops[0].obj)
        assertEquals(1, drops[0].count)
        assertSame(objs.coins, drops[1].obj)
        assertEquals(25, drops[1].count)
    }

    @Test
    fun `weighted slots claim the roll range below their cumulative weight`() {
        val table = dropTable {
            table(outOf = 128) {
                drop(64, objs.coins, 1)
                drop(32, objs.coins, 2)
                nothing(32)
            }
        }

        // The first slot owns rolls [0, 64), the second [64, 96), and nothing owns [96, 128).
        assertEquals(1, rollSingle(table, roll = 0)?.count)
        assertEquals(1, rollSingle(table, roll = 63)?.count)
        assertEquals(2, rollSingle(table, roll = 64)?.count)
        assertEquals(2, rollSingle(table, roll = 95)?.count)
        assertEquals(null, rollSingle(table, roll = 96))
        assertEquals(null, rollSingle(table, roll = 127))
    }

    @Test
    fun `quantity ranges are rolled through game random`() {
        val table = dropTable { table(outOf = 8) { drop(8, objs.coins, 2..12) } }
        // First value picks the slot, second resolves the quantity within `2..12`.
        val drops = DropTableRoller(ScriptedRolls(0, 7)).roll(table)

        assertEquals(1, drops.size)
        assertEquals(7, drops.single().count)
    }

    @Test
    fun `tertiary drops roll independently of the main table`() {
        val table = dropTable {
            always(objs.bones)
            tertiary(oneIn = 128, objs.cabbage)
        }

        val hit = DropTableRoller(ScriptedRolls(0)).roll(table)
        assertEquals(2, hit.size)
        assertSame(objs.cabbage, hit[1].obj)

        val miss = DropTableRoller(ScriptedRolls(1)).roll(table)
        assertEquals(1, miss.size)
        assertSame(objs.bones, miss.single().obj)
    }

    @Test
    fun `nested tables are rolled when their slot is picked`() {
        val nested = WeightedTableBuilder(4).apply { drop(4, objs.cabbage, 3) }.build()
        val table = dropTable {
            table(outOf = 128) {
                nested(1, nested)
                nothing(127)
            }
        }
        // First value picks the nested slot, second rolls within the nested table.
        val drops = DropTableRoller(ScriptedRolls(0, 0)).roll(table)

        assertEquals(1, drops.size)
        assertSame(objs.cabbage, drops.single().obj)
        assertEquals(3, drops.single().count)
    }

    @Test
    fun `table weights must sum to their denominator`() {
        val error =
            assertThrows<IllegalArgumentException> {
                dropTable {
                    table(outOf = 128) {
                        drop(64, objs.coins)
                        nothing(32)
                    }
                }
            }
        assertTrue(error.message.orEmpty().contains("summed to 96"))
    }

    private fun rollSingle(table: DropTable, roll: Int): RolledDrop? =
        DropTableRoller(ScriptedRolls(roll)).roll(table).singleOrNull()

    private class ScriptedRolls(private vararg val values: Int) : GameRandom {
        private var index = 0

        override fun of(maxExclusive: Int): Int = next()

        override fun of(minInclusive: Int, maxInclusive: Int): Int = next()

        override fun randomDouble(): Double = 0.0

        private fun next(): Int {
            check(index < values.size) { "ScriptedRolls ran out of values." }
            return values[index++]
        }
    }
}
