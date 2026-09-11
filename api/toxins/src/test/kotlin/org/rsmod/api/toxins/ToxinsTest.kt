package org.rsmod.api.toxins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ToxinsTest {
    @Test
    fun `venom starts at 6 and climbs by 2 to a cap of 20`() {
        val hits = (0 until 12).map { Toxins.venomDamage(Toxins.VENOM_THRESHOLD + it) }
        assertEquals(listOf(6, 8, 10, 12, 14, 16, 18, 20, 20, 20, 20, 20), hits)
    }

    @Test
    fun `poison hits for a fifth of its severity, rounded up`() {
        // The wiki's reanimated kalphite: severity 30 hits 6 five times, then 5.
        val hits = (30 downTo 25).map(Toxins::poisonDamage)
        assertEquals(listOf(6, 6, 6, 6, 6, 5), hits)
        assertEquals(1, Toxins.poisonDamage(1))
    }

    @Test
    fun `only the anti-venoms and antidote++ reach past the venom immunity floor`() {
        val venomProof =
            ToxinCure.entries.filter { -it.immunityIntervals < Toxins.VENOM_IMMUNITY_FLOOR }
        assertEquals(
            setOf(
                ToxinCure.AntidotePlusPlus,
                ToxinCure.Antivenom,
                ToxinCure.AntivenomPlus,
                ToxinCure.ExtendedAntivenomPlus,
            ),
            venomProof.toSet(),
        )
        assertTrue(ToxinCure.entries.filter { it.curesVenom }.all { it in venomProof })
    }
}
