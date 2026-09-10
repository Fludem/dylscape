package org.rsmod.content.skills.magic.spellbooks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.events.interact.HeldObjEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.spellbooks.configs.Teletab
import org.rsmod.content.skills.magic.spellbooks.configs.Teletabs
import org.rsmod.content.skills.magic.spellbooks.configs.teletab_objs
import org.rsmod.content.skills.magic.spellbooks.scripts.TeletabScript
import org.rsmod.game.inv.InvObj
import org.rsmod.game.type.obj.ObjType
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

/**
 * `Break` on a teleport tablet. The op1 is published straight onto the event bus, the way the
 * client's arrives; the jump lands on the third tick after that, so arrival is asserted after four.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TeletabScriptTest {
    @Test
    fun GameTestState.`every tablet breaks on op1 and borrows a spell destination`() =
        runBasicGameTest {
            val tabs = Teletabs.all.map { it.tab }
            assertEquals(tabs.size, tabs.distinct().size, "A tablet is listed twice.")
            for (teletab in Teletabs.all) {
                val tab = cacheTypes.objs[teletab.tab]
                assertEquals("Break", tab.iop.getOrNull(0)) {
                    "'${tab.name}' has ${tab.iop.toList()}, so op1 is not its Break."
                }
                assertTrue(teletab.destination(cacheTypes.objs) != null) {
                    "'${tab.name}' borrows a spell with no spell_telecoord and would go nowhere."
                }
            }
        }

    /**
     * The standard and Ancient coordinates are already exercised by `TeleportSpellScriptTest`; the
     * Arceuus ones were never used by anything until tablets borrowed them.
     */
    @Test
    fun GameTestState.`every tablet lands within reach of a standable tile`() = runBasicGameTest {
        val stranded =
            Teletabs.all.filter { teletab ->
                val dest = checkNotNull(teletab.destination(cacheTypes.objs))
                val landing =
                    (-LANDING_RADIUS..LANDING_RADIUS).flatMap { dx ->
                        (-LANDING_RADIUS..LANDING_RADIUS).map { dz ->
                            collision[dest.x + dx, dest.z + dz, dest.level]
                        }
                    }
                landing.none { it and BLOCKS_STANDING == 0 }
            }
        assertTrue(stranded.isEmpty()) {
            "These tablets would drop a player into a wall:\n" +
                stranded.joinToString("\n") {
                    "  ${cacheTypes.objs[it.tab].name} -> ${it.destination(cacheTypes.objs)}"
                }
        }
    }

    @Test
    fun GameTestState.`breaking a tablet uses one and lands at its spell coordinate`() =
        runGameTest(TeletabScript::class) {
            val varrock = teletab(teletab_objs.poh_tablet_varrockteleport)
            player.placeAt(START)
            player.clearInv()
            player.inv[0] = InvObj(varrock.tab, 2)

            breakTablet(slot = 0)
            assertEquals(1, player.count(varrock.tab))
            advance(1)
            assertEquals(START, player.coords) // still breaking
            advance(3)
            assertArrivedAt(checkNotNull(varrock.destination(objTypes)))
            assertEquals(1, player.count(varrock.tab))
        }

    @Test
    fun GameTestState.`an ancient tablet needs neither the ancient book nor any runes`() =
        runGameTest(TeletabScript::class) {
            val paddewwa = teletab(teletab_objs.tablet_paddewa)
            player.placeAt(START)
            player.clearInv()
            player.inv[0] = InvObj(paddewwa.tab)

            breakTablet(slot = 0)
            advance(4)
            assertArrivedAt(checkNotNull(paddewwa.destination(objTypes)))
            assertEquals(0, player.count(paddewwa.tab))
        }

    @Test
    fun GameTestState.`deep wilderness refuses the tablet and keeps it`() =
        runGameTest(TeletabScript::class) {
            val lumbridge = teletab(teletab_objs.poh_tablet_lumbridgeteleport)
            player.placeAt(DEEP_WILDERNESS)
            player.clearInv()
            player.inv[0] = InvObj(lumbridge.tab)

            breakTablet(slot = 0)
            assertMessageSent("You can't teleport above level 20 Wilderness.")
            advance(4)
            assertEquals(DEEP_WILDERNESS, player.coords)
            assertEquals(1, player.count(lumbridge.tab))
        }

    private fun teletab(tab: ObjType): Teletab = Teletabs.all.first { it.tab == tab }

    /** Publishes the op1 the client sends for a tablet's `Break`. */
    private fun GameTestScope.breakTablet(slot: Int) {
        val obj = checkNotNull(player.inv[slot])
        player.withProtectedAccess {
            eventBus.publish(
                this,
                HeldObjEvents.Op1(
                    slot = slot,
                    obj = obj,
                    type = objTypes[obj],
                    inventory = player.inv,
                ),
            )
        }
    }

    private fun GameTestScope.assertArrivedAt(dest: CoordGrid) {
        val distance = player.coords.chebyshevDistance(dest)
        assertTrue(distance <= LANDING_RADIUS) {
            "Expected to land near $dest but at ${player.coords}"
        }
    }

    private companion object {
        val START = CoordGrid(3096, 3504, 0)
        val DEEP_WILDERNESS = CoordGrid(3100, 3690, 0)

        /** Matches the `mapFindSquareLineOfWalk(dest, 0, 2)` search in [TeletabScript]. */
        const val LANDING_RADIUS = 2

        const val BLOCKS_STANDING =
            CollisionFlag.BLOCK_WALK or CollisionFlag.LOC or CollisionFlag.GROUND_DECOR
    }
}
