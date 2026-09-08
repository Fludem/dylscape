package org.rsmod.content.custom.cityshops

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

class ShopAssignmentsTest {
    @Test
    fun `no npc is assigned to two shops`() {
        val npcs = ShopAssignments.all.flatMap { it.npcs }
        val duplicates = npcs.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue(duplicates.isEmpty()) {
            "Npc assigned to more than one shop, which would double-register its ops: $duplicates"
        }
    }

    /**
     * The op slot is not uniform: most shopkeepers carry `Trade` on op3, Aleck carries it on op5,
     * and Bob (Lumbridge's, not ours) carries `Repair` on op4. Binding the wrong slot produces a
     * shopkeeper a player can walk up to and not trade with, which nothing else here would catch.
     */
    @Test
    fun GameTestState.`every shopkeeper talks on op1 and trades on the slot it declares`() =
        runBasicGameTest {
            for (assignment in ShopAssignments.all) {
                for (type in assignment.npcs) {
                    val npc = cacheTypes.npcs[type]
                    assertEquals(
                        "Talk-to",
                        npc.op.getOrNull(0),
                        "Expected Talk-to on op1 for ${npc.internalName}: ${npc.op.toList()}",
                    )
                    assertEquals(
                        "Trade",
                        npc.op.getOrNull(assignment.tradeOp - 1),
                        "'${assignment.title}' binds op${assignment.tradeOp}, but " +
                            "${npc.internalName} has ${npc.op.toList()}",
                    )
                }
            }
        }

    @Test
    fun GameTestState.`every shop opens with stock`() = runBasicGameTest {
        for (assignment in ShopAssignments.all) {
            val inv = cacheTypes.invs[assignment.inv]
            val stocked = inv.stock?.filterNotNull().orEmpty()
            assertTrue(stocked.isNotEmpty()) {
                "'${assignment.title}' would open empty: ${inv.internalName} has no stock."
            }
            assertTrue(inv.size >= stocked.size) {
                "'${assignment.title}' stocks ${stocked.size} items but only has ${inv.size} slots."
            }
        }
    }

    @Test
    fun GameTestState.`every stocked obj resolves in the cache`() = runBasicGameTest {
        for (assignment in ShopAssignments.all) {
            val inv = cacheTypes.invs[assignment.inv]
            for (stock in inv.stock?.filterNotNull().orEmpty()) {
                val obj = cacheTypes.objs[stock.obj]
                assertTrue(obj != null && obj.name.isNotBlank()) {
                    "'${assignment.title}' stocks an obj with no name: ${stock.obj}"
                }
            }
        }
    }

    /**
     * A shopkeeper who wanders is a shop nobody can find. The cache never writes RSMod's
     * `wanderRange` opcode, so every npc defaults to roaming a 5-tile box around its spawn tile --
     * far enough to walk out of the room. `ShopNpcEditor` pins them; this is what proves it stuck.
     */
    @Test
    fun GameTestState.`every shopkeeper stays on its post`() = runBasicGameTest {
        val roaming = mutableListOf<String>()
        for (assignment in ShopAssignments.all) {
            for (type in assignment.npcs) {
                val npc = cacheTypes.npcs[type]
                if (npc.wanderRange != 0) {
                    roaming +=
                        "${npc.internalName} would wander ${npc.wanderRange} tiles out of " +
                            "'${assignment.title}'"
                }
            }
        }
        assertTrue(roaming.isEmpty()) { roaming.joinToString("\n") }
    }

    /**
     * `ShopRestockProcess` restocks on `mapClock % restockCycles`, so a zero rate divides by zero
     * the first time anybody buys from that shop. Vanilla writes 0 for "never restocks"; anything
     * transcribed from the wiki has to be remapped before it lands here.
     */
    @Test
    fun GameTestState.`no stock line restocks on a zero cycle`() = runBasicGameTest {
        val divideByZero = mutableListOf<String>()
        for (assignment in ShopAssignments.all) {
            val inv = cacheTypes.invs[assignment.inv]
            for (stock in inv.stock?.filterNotNull().orEmpty()) {
                if (stock.restockCycles <= 0) {
                    divideByZero += "'${assignment.title}' obj ${stock.obj}"
                }
            }
        }
        assertTrue(divideByZero.isEmpty()) {
            "These would throw ArithmeticException on the first purchase:\n" +
                divideByZero.joinToString("\n")
        }
    }

    /**
     * Shop invs are [org.rsmod.game.type.inv.InvScope.Shared] -- one inv is one physical pile of
     * stock for the whole world. That is what a keeper and their assistant want; across two
     * different towns it means buying one out empties the other.
     */
    @Test
    fun `no inv is shared by two different shops`() {
        val shared =
            ShopAssignments.all
                .groupBy { it.inv }
                .filter { (_, shops) -> shops.map { it.title }.distinct().size > 1 }
        assertTrue(shared.isEmpty()) {
            "One inv is one shop. These would share a stock pile:\n" +
                shared.entries.joinToString("\n") { (inv, shops) ->
                    "  ${inv.internalNameValue} <- ${shops.map { it.title }.distinct()}"
                }
        }
    }
}
