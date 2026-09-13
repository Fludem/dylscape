package org.rsmod.content.custom.leagues

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.content.custom.leagues.tasks.LeagueTaskCounterCodec

class LeagueTaskPersistenceTest {
    @Test
    fun `counters survive a round trip in id order`() {
        val counters = mapOf(17 to 3, 2 to 99, 305 to 1)
        val encoded = LeagueTaskCounterCodec.encode(counters)
        assertEquals("2:99;17:3;305:1", encoded)
        assertEquals(counters, LeagueTaskCounterCodec.decode(encoded))
    }

    @Test
    fun `empty and malformed entries are dropped rather than failing`() {
        assertEquals(emptyMap<Int, Int>(), LeagueTaskCounterCodec.decode(""))
        assertEquals(mapOf(4 to 2), LeagueTaskCounterCodec.decode("x:1;4:2;5:;7:0;;"))
        assertEquals("", LeagueTaskCounterCodec.encode(mapOf(1 to 0)))
    }
}
