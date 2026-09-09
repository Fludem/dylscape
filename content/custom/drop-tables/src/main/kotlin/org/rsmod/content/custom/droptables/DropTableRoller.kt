package org.rsmod.content.custom.droptables

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.random.GameRandom

/**
 * Rolls [DropTable]s into the concrete objs that should be spawned for a kill.
 *
 * All randomness goes through [GameRandom] so that rolls are deterministic under test. A
 * [DropBoost] never changes how many draws a roll consumes, so a scripted-roll test keeps asserting
 * the same thing whether or not a boost is applied.
 */
@Singleton
class DropTableRoller @Inject constructor(private val random: GameRandom) {
    fun roll(table: DropTable, boost: DropBoost = DropBoost.NONE): List<RolledDrop> {
        val drops = mutableListOf<RolledDrop>()

        for (drop in table.always) {
            drops += drop.resolve()
        }

        for (weighted in table.tables) {
            rollInto(weighted, drops, boost, inheritedRare = false)
        }

        for (drop in table.tertiary) {
            val ratio = boost.forSlot(drop.rare)
            // With no boost this is `random.of(oneIn) < 1`, i.e. exactly the unboosted `== 0`, and
            // it is still a single draw.
            if (random.of(drop.oneIn * ratio.den) < ratio.num) {
                drops += drop.drop.resolve()
            }
        }

        return drops
    }

    /**
     * Rolls one weighted table, scaling every non-[DropSlot.Empty] slot by whatever [boost] applies
     * to it and letting the empty slots absorb what is left of the roll space.
     *
     * Weights are scaled up into a common integer space rather than divided, so nothing is lost to
     * rounding and an unboosted roll is bit-for-bit identical to what it was before boosts existed:
     * the scale is 1, every empty slot gets back exactly its authored weight, and slots are still
     * visited in declaration order against the same single draw.
     *
     * [inheritedRare] propagates a rare [DropSlot.Nested] down into its sub-table, which is what
     * makes a boost to the shared rare drop table apply both to reaching it and to its contents.
     */
    private fun rollInto(
        table: WeightedTable,
        output: MutableList<RolledDrop>,
        boost: DropBoost,
        inheritedRare: Boolean,
    ) {
        val slots = table.slots
        val scale = boost.all.den * boost.rare.den
        val weights = IntArray(slots.size)

        var scaledNonEmpty = 0
        var emptyWeightTotal = 0
        for ((index, slot) in slots.withIndex()) {
            if (slot is DropSlot.Empty) {
                emptyWeightTotal += slot.weight
                continue
            }
            val ratio = boost.forSlot(inheritedRare || slot.rare)
            weights[index] = slot.weight * ratio.num * (scale / ratio.den)
            scaledNonEmpty += weights[index]
        }

        val fullSpace = table.denominator * scale
        val rollSpace: Int
        if (scaledNonEmpty >= fullSpace) {
            // The boost pushed this table past a guaranteed drop. Roll among the real slots only,
            // at their boosted weights: clamping would penalise whichever slot happened to be
            // declared last, and rescaling them against each other would change which drop won.
            // Leaving the empty slots at zero preserves every drop's odds relative to the others,
            // which is the limit this boost tends towards.
            rollSpace = scaledNonEmpty
        } else {
            rollSpace = fullSpace
            distributeRemainder(
                slots,
                weights,
                remainder = fullSpace - scaledNonEmpty,
                emptyWeightTotal,
            )
        }

        val roll = random.of(rollSpace)

        var cursor = 0
        for ((index, slot) in slots.withIndex()) {
            cursor += weights[index]
            if (roll >= cursor) {
                continue
            }
            when (slot) {
                is DropSlot.Item -> output += slot.drop.resolve()
                is DropSlot.Nested ->
                    rollInto(slot.table, output, boost, inheritedRare || slot.rare)
                is DropSlot.Empty -> {}
            }
            return
        }
    }

    /**
     * Shares [remainder] out across the empty slots in proportion to their authored weights, so an
     * unboosted table hands each one back exactly what it declared. Any rounding shortfall goes to
     * the last empty slot, keeping the weights summing to the roll space exactly.
     */
    private fun distributeRemainder(
        slots: List<DropSlot>,
        weights: IntArray,
        remainder: Int,
        emptyWeightTotal: Int,
    ) {
        if (emptyWeightTotal <= 0) {
            return
        }
        var assigned = 0
        var lastEmpty = -1
        for ((index, slot) in slots.withIndex()) {
            if (slot !is DropSlot.Empty) {
                continue
            }
            val share = remainder * slot.weight / emptyWeightTotal
            weights[index] = share
            assigned += share
            lastEmpty = index
        }
        weights[lastEmpty] += remainder - assigned
    }

    private fun DropQuantity.resolve(): RolledDrop {
        val low = count.first
        val high = count.last
        val rolled = if (low == high) low else random.of(low, high)
        return RolledDrop(obj, rolled)
    }
}
