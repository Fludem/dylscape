package org.rsmod.content.areas.misc.tutorial

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Pure checks on the stage encoding — no game world needed. */
class TutorialStageTest {
    @Test
    fun `a new account reads as not started`() {
        assertEquals(TutorialStage.NOT_STARTED, TutorialStage.of(0))
    }

    @Test
    fun `an exact value maps to its stage`() {
        assertEquals(TutorialStage.COOKING, TutorialStage.of(TutorialStage.COOKING.value))
        assertEquals(TutorialStage.COMPLETE, TutorialStage.of(TutorialStage.COMPLETE.value))
    }

    @Test
    fun `an unknown value rounds down to the last stage passed`() {
        // A value between SURVIVAL_FIRE (30) and SURVIVAL_FISH (40) is still the fire stage.
        assertEquals(TutorialStage.SURVIVAL_FIRE, TutorialStage.of(35))
        // Below the first real stage is not-started.
        assertEquals(TutorialStage.NOT_STARTED, TutorialStage.of(5))
    }

    @Test
    fun `ordering helpers read the right way round`() {
        assertTrue(TutorialStage.COOKING.atLeast(TutorialStage.SURVIVAL_FISH))
        assertTrue(TutorialStage.COOKING.atLeast(TutorialStage.COOKING))
        assertFalse(TutorialStage.SURVIVAL_FISH.atLeast(TutorialStage.COOKING))
        assertTrue(TutorialStage.QUEST.isBefore(TutorialStage.MINING_MINE))
        assertFalse(TutorialStage.COMPLETE.isBefore(TutorialStage.MAGIC))
    }

    @Test
    fun `every stage value is distinct and ascending by declaration`() {
        val values = TutorialStage.entries.map { it.value }
        assertEquals(values.sorted(), values) {
            "Stages are not declared in ascending value order."
        }
        assertEquals(values.toSet().size, values.size) { "Two stages share a value." }
    }
}
