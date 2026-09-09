package org.rsmod.content.skills.farming.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The growth clock, with no game around it.
 *
 * Every one of these is arithmetic on wall-clock timestamps, which is the whole point of the design
 * -- a crop planted before a restart, or before its owner logged out, comes back at the stage it
 * should be at because nothing was ever ticked.
 */
class PatchStateTest {
    private val potato = FarmingCrops.all.first { it.cropName == "potato" }
    private val apple = FarmingCrops.all.first { it.cropName == "apple_tree" }
    private val oak = FarmingCrops.all.first { it.cropName == "oak_tree" }

    /**
     * A stand-in obj id. A `find()` reference has no id until the server binds it at boot, and
     * nothing here boots one -- [PatchState] only ever stores the number. Named so that it does not
     * shadow `PatchState.seedId` inside an `apply` block.
     */
    private val plantedSeed = 5318

    private fun stage(crop: FarmingCrop) = FarmingRates.stageMillis(crop.cycleMinutes)

    @Test
    fun `a crop advances one stage per cycle`() {
        val state = PatchState()
        val planted = 1_000_000L
        state.plant(plantedSeed, planted)

        assertEquals(0, state.stagesGrown(potato, planted))
        assertEquals(1, state.stagesGrown(potato, planted + stage(potato)))
        assertEquals(3, state.stagesGrown(potato, planted + stage(potato) * 3))
        assertTrue(state.fullyGrown(potato, planted + stage(potato) * potato.growthStages))
    }

    @Test
    fun `growth is capped once the crop is ready`() {
        val state = PatchState()
        state.plant(plantedSeed, 0L)
        val fortnight = 14L * 24 * 60 * 60 * 1000
        assertEquals(potato.growthStages, state.stagesGrown(potato, fortnight))
        assertEquals(
            potato.harvestStates.first(),
            state.displayState(PatchKind.Allotment, potato, fortnight),
        )
    }

    @Test
    fun `the speedup is the only thing separating this from the real game`() {
        // Potatoes are four ten-minute stages in OSRS; here that is forty minutes over eight.
        val vanillaMillis = potato.growthStages * potato.cycleMinutes * 60_000L
        val here = potato.growthStages * stage(potato)
        assertEquals(vanillaMillis / FarmingRates.GROWTH_SPEEDUP, here)
        assertEquals(5L * 60_000, here) { "Potatoes should take five real minutes." }
    }

    @Test
    fun `growing crops show the stage the cache draws them at`() {
        val state = PatchState()
        state.plant(plantedSeed, 0L)
        for (grown in 0 until potato.growthStages) {
            val now = stage(potato) * grown
            assertEquals(
                potato.seedState + grown,
                state.displayState(PatchKind.Allotment, potato, now),
            )
        }
    }

    @Test
    fun `a tree waits to be checked before it counts as finished`() {
        val state = PatchState()
        state.plant(plantedSeed, 0L)
        val ready = stage(oak) * oak.growthStages

        assertEquals(oak.checkState, state.displayState(PatchKind.Tree, oak, ready))
        state.checked = true
        assertEquals(oak.grownState, state.displayState(PatchKind.Tree, oak, ready))
        state.felled = true
        assertEquals(oak.stumpState, state.displayState(PatchKind.Tree, oak, ready))
    }

    @Test
    fun `fruit comes back on its own while nobody is watching`() {
        val state = PatchState()
        state.plant(plantedSeed, 0L)
        val ready = stage(apple) * apple.growthStages
        state.checked = true
        state.regrewAt = ready

        assertEquals(apple.maxProduce, state.produceLeft(apple, ready))

        // Pick the tree bare.
        repeat(apple.maxProduce) { state.takeProduce(apple, ready) }
        assertEquals(0, state.produceLeft(apple, ready))

        val regrow = FarmingRates.stageMillis(apple.regrowMinutes)
        assertEquals(1, state.produceLeft(apple, ready + regrow))
        assertEquals(3, state.produceLeft(apple, ready + regrow * 3))
        assertEquals(apple.maxProduce, state.produceLeft(apple, ready + regrow * 20)) {
            "Regrowth must stop at the tree's capacity."
        }
    }

    @Test
    fun `bare soil goes back to weeds one stage at a time`() {
        val state = PatchState()
        state.clear(0L)
        val weed = FarmingRates.weedMillis()

        assertEquals(PatchKind.Allotment.cleared, state.displayState(PatchKind.Allotment, null, 0L))
        assertEquals(2, state.displayState(PatchKind.Allotment, null, weed))
        assertEquals(1, state.displayState(PatchKind.Allotment, null, weed * 2))
        assertEquals(0, state.displayState(PatchKind.Allotment, null, weed * 3))
        assertEquals(0, state.displayState(PatchKind.Allotment, null, weed * 30)) {
            "There is nothing worse than fully overgrown."
        }
    }

    @Test
    fun `raking takes one stage off and reports when it is done`() {
        val state = PatchState()
        val now = FarmingRates.weedMillis() * 5
        state.clear(0L)
        assertEquals(0, state.displayState(PatchKind.Allotment, null, now))

        assertFalse(state.rake(PatchKind.Allotment, now))
        assertEquals(1, state.displayState(PatchKind.Allotment, null, now))
        assertFalse(state.rake(PatchKind.Allotment, now))
        assertEquals(2, state.displayState(PatchKind.Allotment, null, now))
        assertTrue(state.rake(PatchKind.Allotment, now)) { "The third rake clears the patch." }
        assertEquals(
            PatchKind.Allotment.cleared,
            state.displayState(PatchKind.Allotment, null, now),
        )
    }

    @Test
    fun `a vine has no weeds to rake`() {
        val state = PatchState()
        state.clear(0L)
        val later = FarmingRates.weedMillis() * 10
        assertEquals(PatchKind.Grape.cleared, state.displayState(PatchKind.Grape, null, later))
        assertTrue(state.rake(PatchKind.Grape, later))
    }

    @Test
    fun `compost buys extra harvests`() {
        val state = PatchState()
        state.plant(plantedSeed, 0L)
        assertEquals(3, state.livesLeft(potato))
        state.compost = CompostTier.Ultracompost
        assertEquals(6, state.livesLeft(potato))
        state.livesUsed = 6
        assertEquals(0, state.livesLeft(potato))
    }

    @Test
    fun `patches survive a round trip through the save format`() {
        val patches = PlayerPatches()
        val allotment =
            PatchState().apply {
                plant(plantedSeed, 1_757_000_000_000L)
                livesUsed = 2
                compost = CompostTier.Supercompost
            }
        val tree =
            PatchState().apply {
                plant(plantedSeed, 1_757_000_100_000L)
                checked = true
                felled = true
            }
        patches.set(8550, allotment)
        patches.set(8388, tree)

        val decoded = PatchCodec.decode(PatchCodec.encode(patches))

        val savedAllotment = decoded.peek(8550)!!
        assertEquals(plantedSeed, savedAllotment.seedId)
        assertEquals(1_757_000_000_000L, savedAllotment.plantedAt)
        assertEquals(2, savedAllotment.livesUsed)
        assertEquals(CompostTier.Supercompost, savedAllotment.compost)

        val savedTree = decoded.peek(8388)!!
        assertTrue(savedTree.checked)
        assertTrue(savedTree.felled)
    }

    @Test
    fun `an untouched patch is not written out`() {
        val patches = PlayerPatches()
        patches.set(8550, PatchState())
        assertEquals("", PatchCodec.encode(patches))
    }

    @Test
    fun `a corrupt entry is skipped rather than thrown on`() {
        val decoded = PatchCodec.decode("nonsense;8550,5318,1,0,0,0,0,0,0")
        assertEquals(1, decoded.touched.size)
        assertEquals(5318, decoded.peek(8550)?.seedId)
    }
}
