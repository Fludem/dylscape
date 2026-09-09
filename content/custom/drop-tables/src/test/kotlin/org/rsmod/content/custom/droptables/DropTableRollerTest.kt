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

    @Test
    fun `an all-rolls boost widens every non-empty slot`() {
        val table = dropTable {
            table(outOf = 128) {
                drop(64, objs.coins)
                nothing(64)
            }
        }

        // Scaled into fifths: the slot owns [0, 384) of 640 rather than [0, 64) of 128, which is
        // 64 * 1.2 / 128. The boundary moving is the whole point, so assert it exactly.
        assertEquals(1, rollSingle(table, roll = 383, DropBoost.PLUS_20_ALL)?.count)
        assertEquals(null, rollSingle(table, roll = 384, DropBoost.PLUS_20_ALL))
    }

    @Test
    fun `a rare boost leaves ordinary slots alone`() {
        val table = dropTable {
            table(outOf = 128) {
                drop(10, objs.coins)
                rare(10, objs.cabbage)
                nothing(108)
            }
        }

        // Scaled into tenths. The ordinary slot keeps its 10/128 as 100/1280; the rare slot is
        // boosted to 110, so it owns [100, 210).
        assertSame(objs.coins, rollSingle(table, roll = 99, DropBoost.PLUS_10_RARE)?.obj)
        assertSame(objs.cabbage, rollSingle(table, roll = 100, DropBoost.PLUS_10_RARE)?.obj)
        assertSame(objs.cabbage, rollSingle(table, roll = 209, DropBoost.PLUS_10_RARE)?.obj)
        assertEquals(null, rollSingle(table, roll = 210, DropBoost.PLUS_10_RARE))
    }

    @Test
    fun `a rare nested slot boosts the table it leads to`() {
        val nested = WeightedTableBuilder(10).apply { drop(10, objs.cabbage) }.build()
        val table = dropTable {
            table(outOf = 128) {
                rareNested(10, nested)
                nothing(118)
            }
        }

        // The access weight is boosted to 110/1280, and the boost is inherited, so the sub-table's
        // ordinary slot is boosted too: 10 * 1.1 = 110 of its own 100 space, which guarantees it.
        val drops = DropTableRoller(ScriptedRolls(109, 99)).roll(table, DropBoost.PLUS_10_RARE)
        assertEquals(1, drops.size)
        assertSame(objs.cabbage, drops.single().obj)

        assertEquals(null, rollSingle(table, roll = 110, DropBoost.PLUS_10_RARE))
    }

    @Test
    fun `a boost that would exceed the denominator always drops something`() {
        val table = dropTable {
            table(outOf = 128) {
                drop(127, objs.coins)
                nothing(1)
            }
        }

        // 127 * 1.2 = 152.4 out of 128, so the empty slot is squeezed out entirely and the roll
        // space collapses onto the real slot. Every roll must produce a drop.
        val rollSpace = 127 * 6
        assertSame(objs.coins, rollSingle(table, roll = 0, DropBoost.PLUS_20_ALL)?.obj)
        assertSame(objs.coins, rollSingle(table, roll = rollSpace - 1, DropBoost.PLUS_20_ALL)?.obj)
    }

    @Test
    fun `tertiary drops scale with the boost`() {
        val table = dropTable { rareTertiary(oneIn = 128, objs.cabbage) }

        // 1 in 128 becomes 1.2 in 128, expressed as 6 in 640.
        assertEquals(1, DropTableRoller(ScriptedRolls(5)).roll(table, DropBoost.PLUS_20_ALL).size)
        assertEquals(0, DropTableRoller(ScriptedRolls(6)).roll(table, DropBoost.PLUS_20_ALL).size)
    }

    @Test
    fun `an unboosted roll is unchanged by the boost machinery`() {
        val table = dropTable {
            table(outOf = 128) {
                drop(64, objs.coins, 1)
                drop(32, objs.coins, 2)
                nothing(32)
            }
        }

        // The boundaries asserted in `weighted slots claim the roll range` must hold identically
        // when NONE is passed explicitly, since that is what every un-tiered player rolls with.
        assertEquals(1, rollSingle(table, roll = 63, DropBoost.NONE)?.count)
        assertEquals(2, rollSingle(table, roll = 64, DropBoost.NONE)?.count)
        assertEquals(null, rollSingle(table, roll = 96, DropBoost.NONE))
    }

    private fun rollSingle(
        table: DropTable,
        roll: Int,
        boost: DropBoost = DropBoost.NONE,
    ): RolledDrop? = DropTableRoller(ScriptedRolls(roll)).roll(table, boost).singleOrNull()

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
