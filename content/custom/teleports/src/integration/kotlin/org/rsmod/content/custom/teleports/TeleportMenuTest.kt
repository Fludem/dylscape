package org.rsmod.content.custom.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestinations

/**
 * Row-layout tests. These need no game harness -- they are about the shape of the lists the script
 * draws, which is exactly what the script test then derives its indices from.
 */
class TeleportMenuTest {
    @Test
    fun `top level is home, then the categories, then cancel`() {
        val rows = TeleportMenu.topLevel(previous = null)
        val expected = listOf("Home") + TeleportCategory.entries.map { it.label } + listOf("Cancel")
        assertEquals(expected, rows.map { it.label })
    }

    @Test
    fun `previous is offered only once there is one, and never displaces a fixed row`() {
        val varrock = requireNotNull(TeleportDestinations["varrock"])
        val without = TeleportMenu.topLevel(previous = null)
        val with = TeleportMenu.topLevel(previous = varrock)

        assertTrue(without.none { it is TeleportRow.Destination && it.destination == varrock })
        assertEquals(without.size + 1, with.size)

        // `hotkeys = true` binds number keys to row indices, so everything above the new row has
        // to keep the index it had.
        val fixed = 1 + TeleportCategory.entries.size
        assertEquals(without.take(fixed).map { it.label }, with.take(fixed).map { it.label })
        assertEquals("Previous: Varrock", with[fixed].label)
        assertEquals(TeleportRow.Cancel, with.last())
    }

    @Test
    fun `each category lists its destinations in order and ends in back`() {
        for (category in TeleportCategory.entries) {
            val rows = TeleportMenu.destinations(category)
            assertEquals(TeleportRow.Back, rows.last()) { "$category does not end in Back." }
            assertEquals(
                TeleportDestinations[category],
                rows.dropLast(1).map { (it as TeleportRow.Destination).destination },
                "$category rows do not match its destinations.",
            )
        }
    }

    @Test
    fun `no menu exceeds the choice cap`() {
        // `ProtectedAccess.menu` throws above 127 choices, which would take the button down
        // entirely rather than degrade.
        val previous = TeleportDestinations["varrock"]
        assertTrue(TeleportMenu.topLevel(previous).size < 128)
        for (category in TeleportCategory.entries) {
            assertTrue(TeleportMenu.destinations(category).size < 128) { "$category is too long." }
        }
    }
}
