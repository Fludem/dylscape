package org.rsmod.content.skills.hunter.scripts

import jakarta.inject.Inject
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.stats
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.hunter.HunterTraps
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapKind
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapOwner
import org.rsmod.content.skills.hunter.HunterTraps.Companion.trapState
import org.rsmod.content.skills.hunter.TrapKind
import org.rsmod.content.skills.hunter.TrapState
import org.rsmod.content.skills.hunter.configs.HunterLocs
import org.rsmod.content.skills.hunter.configs.HunterNpcs
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid

/**
 * The trap lifecycle, end to end.
 *
 * Laying is driven through [HunterTraps] rather than through the "Lay" inventory op, because the
 * test harness wires up loc and npc op handlers but no held-op handler. That the op itself is in
 * the right slot is asserted separately, in `HunterConfigTest`, straight off the cache type — which
 * is the part that could silently be wrong. Everything after the item leaves the inventory is
 * exercised for real here.
 *
 * Runs single-threaded on purpose: `integration-test-suite` sets
 * `junit.jupiter.execution.parallel.mode.default=concurrent`, so methods in one class share a
 * single game world. Tests that place locs, spawn npcs or move items race each other otherwise.
 */
@Execution(ExecutionMode.SAME_THREAD)
class HunterTrapTest {
    class Deps @Inject constructor(val traps: HunterTraps, val locRepo: LocRepository)

    @Test
    fun GameTestState.`laying a box trap registers a controller and an attractor`() =
        runInjectedGameTest(Deps::class, null, HunterTrapScript::class) { deps ->
            val tile = CoordGrid(0, 50, 50, 20, 20)
            player.teleport(tile)
            player.stats[stats.hunter] = 60

            val loc = deps.traps.tryPlace(player, tile, TrapKind.BOX, HunterLocs.box_armed)
            assertNotNull(loc) { "The tile refused a box trap." }
            deps.traps.register(player, tile, TrapKind.BOX, loc!!)

            val trap = conRepo.findExact(tile)
            assertNotNull(trap) { "Laying a box trap did not create a controller." }
            assertEquals(TrapKind.BOX, trap!!.trapKind)
            assertEquals(TrapState.ARMED, trap.trapState)
            assertEquals(player.uid.packed, trap.trapOwner)

            // The invisible attractor is what makes creatures walk in; without it the trap is
            // inert.
            val attractor =
                npcRepo.findAll(tile).firstOrNull { it.id == HunterNpcs.box_trap_npc.id }
            assertNotNull(attractor) { "No attractor npc was spawned on the trap." }
        }

    /**
     * The riskiest part of the whole design, and the reason it is tested first.
     *
     * Nothing else in this codebase consumes an AI npc-on-npc op, so this is the proof that the
     * hunt mode finds the attractor, that the hunt processor routes the creature onto the trap
     * tile, and that arriving there actually reaches the trap script's handler.
     */
    @Test
    fun GameTestState.`a chinchompa walks into a laid box trap`() =
        runInjectedGameTest(Deps::class, null, HunterTrapScript::class) { deps ->
            val tile = CoordGrid(0, 50, 50, 30, 30)
            // `spawnNpc` allocates no collision and `teleport` only allocates its own zone, so the
            // ground the chinchompa has to walk over has to be opened up by hand.
            for (dx in -2..6) {
                for (dz in -2..2) {
                    allocZoneCollision(tile.translate(dx, dz))
                }
            }
            player.teleport(tile)
            player.stats[stats.hunter] = 99

            val loc = deps.traps.tryPlace(player, tile, TrapKind.BOX, HunterLocs.box_armed)!!
            deps.traps.register(player, tile, TrapKind.BOX, loc)

            // Force the catch roll to succeed so this asserts routing, not luck.
            random.next = 0

            val chinType = npcTypes.getValue(HunterNpcs.chinchompa.id)
            spawnNpc(tile.translateX(3), chinType)

            advance(ticks = 40)

            val trap = conRepo.findExact(tile)
            assertNotNull(trap) { "The trap vanished before the chinchompa reached it." }
            assertTrue(trap!!.trapState == TrapState.CATCHING || trap.trapState == TrapState.FULL) {
                "The chinchompa never sprang the trap; it is still in state ${trap.trapState}."
            }
        }

    @Test
    fun GameTestState.`the trap allowance rises with level`() =
        runInjectedGameTest(Deps::class, null, HunterTrapScript::class) { deps ->
            assertEquals(1, deps.traps.maxTraps(1))
            assertEquals(1, deps.traps.maxTraps(19))
            assertEquals(2, deps.traps.maxTraps(20))
            assertEquals(3, deps.traps.maxTraps(40))
            assertEquals(4, deps.traps.maxTraps(60))
            assertEquals(5, deps.traps.maxTraps(80))
            assertEquals(5, deps.traps.maxTraps(99))

            val tile = CoordGrid(0, 50, 50, 10, 10)
            player.teleport(tile)
            player.stats[stats.hunter] = 1

            val first = deps.traps.tryPlace(player, tile, TrapKind.BOX, HunterLocs.box_armed)
            assertNotNull(first) { "A level-1 hunter could not lay their first trap." }
            deps.traps.register(player, tile, TrapKind.BOX, first!!)

            // One trap is the whole level-1 allowance, so the second must be refused.
            val second =
                deps.traps.tryPlace(player, tile.translateX(2), TrapKind.BOX, HunterLocs.box_armed)
            assertNull(second) { "A level-1 hunter was allowed a second trap." }
        }

    @Test
    fun GameTestState.`only the owner can check a trap`() =
        runInjectedGameTest(Deps::class, null, HunterTrapScript::class) { deps ->
            val tile = CoordGrid(0, 50, 50, 40, 40)
            for (dx in -2..2) {
                for (dz in -2..2) {
                    allocZoneCollision(tile.translate(dx, dz))
                }
            }
            player.teleport(tile)
            player.stats[stats.hunter] = 60

            val loc = deps.traps.tryPlace(player, tile, TrapKind.BOX, HunterLocs.box_armed)!!
            deps.traps.register(player, tile, TrapKind.BOX, loc)

            val trap = conRepo.findExact(tile)!!
            // Somebody else's trap: an owner uid this player will never match.
            trap.trapOwner = trap.trapOwner + 1

            val placed = deps.locRepo.findExact(tile, HunterLocs.box_armed)
            assertNotNull(placed) { "The armed box trap loc was not placed." }

            player.teleport(tile.translateX(-1))
            player.opLoc1(BoundLocInfo(placed!!, width = 1, length = 1, forceApproachFlags = 0))

            // Captured messages are cleared at the start of every tick, so advance one at a time
            // and check as we go rather than asserting after the fact.
            var refused = false
            repeat(5) {
                advance(ticks = 1)
                if (client.mapOf(MessageGame::message).contains("This isn't your trap.")) {
                    refused = true
                }
            }
            assertTrue(refused) { "Checking someone else's trap was not refused." }
        }

    @Test
    fun GameTestState.`a trap collapses once its owner leaves`() =
        runInjectedGameTest(Deps::class, null, HunterTrapScript::class) { deps ->
            val tile = CoordGrid(0, 50, 50, 15, 15)
            player.teleport(tile)
            player.stats[stats.hunter] = 60

            val loc = deps.traps.tryPlace(player, tile, TrapKind.BOX, HunterLocs.box_armed)!!
            deps.traps.register(player, tile, TrapKind.BOX, loc)
            assertNotNull(conRepo.findExact(tile))

            // Well beyond the abandon radius, and long enough for the countdown to run out.
            player.teleport(CoordGrid(0, 50, 52, 15, 15))
            advance(ticks = HunterTraps.ABANDON_TICKS + 10)

            assertNull(conRepo.findExact(tile)) { "An abandoned trap was never cleaned up." }
            assertNull(deps.locRepo.findExact(tile, HunterLocs.box_armed)) {
                "An abandoned trap left its loc behind."
            }
        }
}
