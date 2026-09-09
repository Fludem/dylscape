package org.rsmod.content.skills.slayer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.combat.commons.slayer.SlayerRequirementRegistry
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.slayer.scripts.SlayerRequirementScript

/**
 * The level and equipment gates combat enforces.
 *
 * The helmet assertion is the one earning its keep. The first cut of the gear sweep read the
 * `slayer_helm` param - the same one the melee formula uses for black masks - and found
 * **nothing**, because no obj in this cache carries it. Everything still compiled, every other test
 * still passed, and the only symptom was a boot line reading "0 slayer helmet variants": a player
 * in a slayer helmet would have been told to go and fetch earmuffs.
 */
class SlayerGatingTest {
    @Test
    fun GameTestState.`a slayer helmet stands in for the head-slot protective gear`() =
        runInjectedGameTest(
            SlayerRequirementRegistry::class,
            null,
            SlayerRequirementScript::class,
        ) { registry ->
            val banshee = cacheTypes.npcs.getValue(BANSHEE_NPC)
            val requirement = registry.get(banshee)
            assertNotNull(requirement, "Banshees are not gated at all.")

            val helmets =
                cacheTypes.objs.values
                    .filter { it.internalName?.startsWith("slayer_helm") == true }
                    .map { it.id }
            assertTrue(helmets.size >= 20) { "Only found ${helmets.size} slayer helmets." }
            assertTrue(requirement!!.gear.containsAll(helmets)) {
                "A slayer helmet does not satisfy the banshee's ear protection."
            }
        }

    @Test
    fun GameTestState.`a shield requirement is not satisfied by a helmet`() =
        runInjectedGameTest(
            SlayerRequirementRegistry::class,
            null,
            SlayerRequirementScript::class,
        ) { registry ->
            val basilisk = cacheTypes.npcs.getValue(BASILISK_NPC)
            val requirement = registry.get(basilisk)
            assertNotNull(requirement, "Basilisks are not gated at all.")
            val helmet = cacheTypes.objs.values.first { it.internalName == "slayer_helm" }
            assertTrue(helmet.id !in requirement!!.gear) {
                "A head-slot helmet is standing in for a mirror shield."
            }
        }

    @Test
    fun GameTestState.`the level gate carries the task's own requirement`() =
        runInjectedGameTest(
            SlayerRequirementRegistry::class,
            null,
            SlayerRequirementScript::class,
        ) { registry ->
            val abyssal = cacheTypes.npcs.getValue(ABYSSAL_DEMON_NPC)
            assertEquals(85, registry.get(abyssal)?.level, "Abyssal demons should need 85 slayer.")
        }

    /**
     * An empty registry gates nothing, which is the right way round - but a registry that stayed
     * empty by accident would silently disable every requirement in the game.
     */
    @Test
    fun GameTestState.`the registry is actually populated`() =
        runInjectedGameTest(
            SlayerRequirementRegistry::class,
            null,
            SlayerRequirementScript::class,
        ) { registry ->
            val gated = cacheTypes.npcs.values.count { registry.get(it) != null }
            assertTrue(gated > 100) { "Only $gated npcs are gated; the sweep is not running." }
        }

    private companion object {
        const val BANSHEE_NPC = 414
        const val BASILISK_NPC = 417
        const val ABYSSAL_DEMON_NPC = 415
    }
}
