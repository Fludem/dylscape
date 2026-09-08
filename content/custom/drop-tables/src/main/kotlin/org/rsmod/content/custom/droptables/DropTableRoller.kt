package org.rsmod.content.custom.droptables

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom

/**
 * Rolls [DropTable]s into the concrete objs that should be spawned for a kill.
 *
 * All randomness goes through [GameRandom] so that rolls are deterministic under test.
 */
@Singleton
class DropTableRoller @Inject constructor(private val random: GameRandom) {
    fun roll(table: DropTable): List<RolledDrop> {
        val drops = mutableListOf<RolledDrop>()

        for (drop in table.always) {
            drops += drop.resolve()
        }

        for (weighted in table.tables) {
            rollInto(weighted, drops)
        }

        for (drop in table.tertiary) {
            if (random.of(drop.oneIn) == 0) {
                drops += drop.drop.resolve()
            }
        }

        return drops
    }

    private fun rollInto(table: WeightedTable, output: MutableList<RolledDrop>) {
        val roll = random.of(table.denominator)

        var cursor = 0
        for (slot in table.slots) {
            cursor += slot.weight
            if (roll >= cursor) {
                continue
            }
            when (slot) {
                is DropSlot.Item -> output += slot.drop.resolve()
                is DropSlot.Nested -> rollInto(slot.table, output)
                is DropSlot.Empty -> {}
            }
            return
        }
    }

    private fun DropQuantity.resolve(): RolledDrop {
        val low = count.first
        val high = count.last
        val rolled = if (low == high) low else random.of(low, high)
        return RolledDrop(obj, rolled)
    }
}
