package org.rsmod.content.skills.hunter.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.hunter.TrapKind

/**
 * Guards the things the type resolver cannot.
 *
 * A creature can resolve perfectly and still be uncatchable because a param is missing, and — the
 * expensive one — a creature can be fully configured and still never reach a trap because its
 * ranges do not line up. See [every creature can actually reach a trap].
 */
@Execution(ExecutionMode.SAME_THREAD)
class HunterConfigTest {
    @Test
    fun GameTestState.`every creature is fully configured`() = runBasicGameTest {
        val creatures =
            cacheTypes.npcs.values.filter { it.isContentType(HunterContent.hunter_trap_creature) }
        assertEquals(16, creatures.size) {
            "Expected 16 huntable creatures, found ${creatures.map { it.internalName }}."
        }
        for (creature in creatures) {
            val name = creature.internalName
            val kind = creature.param(HunterParams.trap_kind)
            assertTrue(kind in TrapKind.SNARE..TrapKind.DEADFALL) {
                "Creature '$name' has no trap kind."
            }
            assertTrue(creature.param(params.levelrequire) in 1..99) {
                "Creature '$name' has an out-of-range level requirement."
            }
            assertTrue(creature.param(params.skill_xp) > 0) { "Creature '$name' grants no xp." }

            val full = creature.param(HunterParams.loc_full)
            assertNotNull(cacheTypes.locs[full.id]) {
                "Creature '$name' points at an unresolvable full-trap loc."
            }

            // A box trap picks its catching loc by approach direction, everything else has one.
            if (kind == TrapKind.BOX) {
                val dirs = creature.param(HunterParams.loc_trapping_dirs)
                val enum = cacheTypes.enums[dirs.id]
                assertNotNull(enum) { "Creature '$name' has no direction enum." }
                assertEquals(4, enum!!.size) {
                    "Creature '$name' needs exactly four approach directions."
                }
            } else {
                val trapping = creature.param(HunterParams.loc_trapping)
                assertNotNull(cacheTypes.locs[trapping.id]) {
                    "Creature '$name' points at an unresolvable catching loc."
                }
            }

            assertTrue(creature.param(HunterParams.rate_low) in 1..255) {
                "Creature '$name' has an out-of-range low catch weight."
            }
            assertTrue(creature.param(HunterParams.rate_high) in 1..255) {
                "Creature '$name' has an out-of-range high catch weight."
            }
            assertTrue(
                creature.param(HunterParams.rate_high) > creature.param(HunterParams.rate_low)
            ) {
                "Creature '$name' would get harder to catch as Hunter rises."
            }
        }
    }

    /**
     * The invariant that is invisible until you watch a creature walk halfway to a trap and turn
     * around. `NpcInteractionProcessor` cancels an interaction once the npc is further than
     * `maxRange + attackRange` from its **spawn** tile, so a creature that has already wandered
     * `wanderRange` tiles away and then spots a trap `huntRange` tiles further needs a `maxRange`
     * wider than the sum. Nothing at authoring time makes that visible, hence the test.
     */
    @Test
    fun GameTestState.`every creature can actually reach a trap`() = runBasicGameTest {
        val creatures =
            cacheTypes.npcs.values.filter { it.isContentType(HunterContent.hunter_trap_creature) }
        for (creature in creatures) {
            assertTrue(creature.maxRange > creature.wanderRange + creature.huntRange) {
                "Creature '${creature.internalName}' can wander ${creature.wanderRange} and hunt " +
                    "${creature.huntRange} tiles but leashes at ${creature.maxRange}, so it will " +
                    "abandon a trap it has already started walking to."
            }
            assertTrue(creature.huntMode != null) {
                "Creature '${creature.internalName}' has no hunt mode, so it ignores traps."
            }
            // Roaming is the gameplay, but it has to stay on the patch it was spawned on.
            assertTrue(creature.wanderRange in 1..6) {
                "Creature '${creature.internalName}' wanders ${creature.wanderRange} tiles; that " +
                    "is either pinned solid or halfway across the map."
            }
        }
    }

    /**
     * The attractor npcs are the ones that must not move at all. Out of the cache they inherit the
     * same `wanderRange = 5` every npc does, which would walk a trap's own marker off the trap.
     */
    @Test
    fun GameTestState.`trap npcs are pinned and walkable through`() = runBasicGameTest {
        val trapNpcs = HunterNpcs.trapNpcs.map { cacheTypes.npcs.getValue(it.id) }
        assertEquals(8, trapNpcs.size)
        for (npc in trapNpcs) {
            assertEquals(0, npc.wanderRange) {
                "Trap npc '${npc.internalName}' would wander off its own trap."
            }
            assertEquals(0, npc.huntRange) {
                "Trap npc '${npc.internalName}' hunts; the relationship runs the other way."
            }
            // A creature has to be able to step onto the trap tile to reach the attractor.
            assertEquals(org.rsmod.game.movement.BlockWalk.None, npc.blockWalk) {
                "Trap npc '${npc.internalName}' blocks the tile creatures must walk onto."
            }
        }
    }

    @Test
    fun GameTestState.`trap locs are grouped by what a player can do`() = runBasicGameTest {
        val armed =
            cacheTypes.locs.values.filter { it.isContentType(HunterContent.hunter_trap_armed) }
        val sprung =
            cacheTypes.locs.values.filter { it.isContentType(HunterContent.hunter_trap_sprung) }
        assertTrue(armed.isNotEmpty()) { "No locs in the armed group." }
        assertTrue(sprung.isNotEmpty()) { "No locs in the sprung group." }

        // The op slots are what the scripts bind, so assert them rather than trusting the dump.
        for (loc in armed) {
            assertEquals("Dismantle", loc.op[0]) {
                "Armed trap '${loc.internalName}' has no Dismantle on op1."
            }
        }
        for (loc in sprung) {
            assertTrue(loc.op[0] == "Check" || loc.op[0] == "Dismantle") {
                "Sprung trap '${loc.internalName}' has '${loc.op[0]}' on op1."
            }
        }
    }

    @Test
    fun GameTestState.`trap items carry Lay on the first inventory op`() = runBasicGameTest {
        val traps =
            cacheTypes.objs.values.filter { it.isContentType(HunterContent.hunter_trap_obj) }
        assertEquals(2, traps.size)
        for (trap in traps) {
            assertEquals("Lay", trap.iop[0]) {
                "Trap '${trap.internalName}' has '${trap.iop[0]}' on iop1, not Lay."
            }
        }
    }
}
