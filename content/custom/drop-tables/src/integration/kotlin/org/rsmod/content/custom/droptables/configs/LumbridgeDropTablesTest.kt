package org.rsmod.content.custom.droptables.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.custom.droptables.DropSlot
import org.rsmod.game.type.obj.ObjType

class LumbridgeDropTablesTest {
    @Test
    fun GameTestState.`every npc with a drop table can be attacked and respawns`() =
        runBasicGameTest {
            for ((type, _) in LumbridgeDropTables.assignments) {
                val npc = cacheTypes.npcs[type]
                assertTrue(
                    npc.op.any { it.equals("Attack", ignoreCase = true) },
                    "Npc has a drop table but no attack op: ${npc.internalName}",
                )
                assertTrue(
                    npc.respawnRate > 0,
                    "Npc has a drop table but never respawns: ${npc.internalName}",
                )
            }
        }

    @Test
    fun GameTestState.`every drop resolves to an obj in the cache`() = runBasicGameTest {
        for ((type, table) in LumbridgeDropTables.assignments) {
            val drops = table.always.map { it.obj } + table.tables.flatMap { it.slots.allObjs() }
            for (obj in drops) {
                val resolved = cacheTypes.objs[obj]
                assertTrue(
                    resolved.name.isNotBlank(),
                    "Drop for '${cacheTypes.npcs[type].internalName}' has no name: $obj",
                )
            }
        }
    }

    @Test
    fun `weighted tables are rolled out of 128`() {
        val tables = LumbridgeDropTables.assignments.flatMap { it.second.tables }
        assertTrue(tables.isNotEmpty())
        for (table in tables) {
            assertEquals(128, table.denominator)
            assertEquals(128, table.slots.sumOf(DropSlot::weight))
        }
    }

    private fun List<DropSlot>.allObjs(): List<ObjType> = buildList {
        for (slot in this@allObjs) {
            when (slot) {
                is DropSlot.Item -> add(slot.drop.obj)
                is DropSlot.Nested -> addAll(slot.table.slots.allObjs())
                is DropSlot.Empty -> {}
            }
        }
    }
}
