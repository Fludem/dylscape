package org.rsmod.content.custom.zulrah

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ZulrahRotationTest {
    @Test
    fun `the rotation is ten phases and starts green in the middle`() {
        assertEquals(10, ZulrahRotation.phases.size)
        val first = ZulrahRotation.phases.first()
        assertEquals(ZulrahForm.Serpentine, first.form)
        assertEquals(ZulrahSpot.Middle, first.spot)
    }

    /**
     * The Jad phase is the one where a green or blue Zulrah alternates ranged and magic ten times.
     * With it removed, no phase has more than eleven actions and every magma phase is two whips.
     */
    @Test
    fun `there is no jad phase`() {
        for (phase in ZulrahRotation.phases) {
            assertTrue(phase.actions.isNotEmpty()) { "Empty phase: $phase" }
            assertTrue(phase.actions.size <= 11) { "Phase too long to be anything but Jad: $phase" }
            if (phase.form == ZulrahForm.Magma) {
                assertEquals(listOf(ZulrahAction.Attack, ZulrahAction.Attack), phase.actions)
                assertEquals(ZulrahSpot.Middle, phase.spot) { "Magma only ever surfaces middle." }
            }
        }
    }

    @Test
    fun `the rotation loops back to the start`() {
        val last = ZulrahRotation.phases.lastIndex
        assertEquals(0, ZulrahRotation.next(last))
        assertEquals(1, ZulrahRotation.next(0))
    }

    /**
     * Within one pass, every dive changes the form or the spot. The loop back from the last phase
     * to the first is the exception, as on live: rotation 1 ends and begins green in the middle.
     */
    @Test
    fun `consecutive phases always change something`() {
        for ((a, b) in ZulrahRotation.phases.zipWithNext()) {
            assertTrue(a.form != b.form || a.spot != b.spot) {
                "Zulrah would dive and resurface unchanged: $a -> $b"
            }
        }
    }
}
