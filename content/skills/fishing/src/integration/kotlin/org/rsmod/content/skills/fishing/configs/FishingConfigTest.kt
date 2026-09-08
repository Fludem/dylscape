package org.rsmod.content.skills.fishing.configs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState

/**
 * Guards the things the type resolver cannot. A spot can resolve perfectly and still do nothing
 * when clicked, because the module never learned the option pair it advertises.
 */
class FishingConfigTest {
    @Test
    fun GameTestState.`every tagged spot advertises a known option pair`() = runBasicGameTest {
        val spots = cacheTypes.npcs.values.filter { it.isContentType(FishingContent.fishing_spot) }
        assertTrue(spots.isNotEmpty()) { "No npcs were tagged into the fishing_spot group." }
        for (spot in spots) {
            assertTrue(FishingSpots.isKnownSpot(spot)) {
                "Spot '${spot.internalName}' advertises an unhandled option pair: " +
                    "${spot.op.toList()}"
            }
        }
    }

    @Test
    fun GameTestState.`every tagged spot resolves at least one method`() = runBasicGameTest {
        val spots = cacheTypes.npcs.values.filter { it.isContentType(FishingContent.fishing_spot) }
        for (spot in spots) {
            val op1 = FishingSpots.methodFor(spot, op = 1)
            assertNotNull(op1) { "Spot '${spot.internalName}' has nothing bound to op1." }

            // op3 is genuinely absent on Tutorial Island's spot, which is net-only. Everywhere
            // else a missing op3 would mean half the spot is dead.
            val op3 = FishingSpots.methodFor(spot, op = 3)
            if (spot.id != FishingSpotNpcs.newbie.id) {
                assertNotNull(op3) { "Spot '${spot.internalName}' has nothing bound to op3." }
            }
        }
    }

    @Test
    fun GameTestState.`the whole cache's standard spots are tagged`() = runBasicGameTest {
        // The module tags by name, so a spot that exists in the cache but was left out of the list
        // would simply never work. This catches that rather than leaving it to be discovered in
        // game, months later, by someone standing at a river.
        val standard =
            cacheTypes.npcs.values.filter {
                val name = it.internalName ?: return@filter false
                name.endsWith("freshfish") ||
                    name.endsWith("saltfish") ||
                    name.endsWith("rarefish") ||
                    name.endsWith("memberfish") ||
                    name.contains("newbiefishing")
            }
        val untagged = standard.filterNot { it.isContentType(FishingContent.fishing_spot) }
        assertTrue(untagged.isEmpty()) {
            "Fishing spots exist in the cache but are untagged: " +
                untagged.mapNotNull { it.internalName }
        }
        assertEquals(FishingSpotNpcs.all.size, standard.size) {
            "The spot list and the cache disagree on how many standard spots there are."
        }
    }

    @Test
    fun GameTestState.`every method is fully configured`() = runBasicGameTest {
        for (method in FishingSpots.allMethods) {
            assertNotNull(cacheTypes.objs[method.tool.id]) {
                "A method needs a tool that does not resolve."
            }
            val bait = method.bait
            if (bait != null) {
                assertNotNull(cacheTypes.objs[bait.id]) {
                    "A method needs bait that does not resolve."
                }
                assertNotNull(method.baitMessage) {
                    "A method consumes bait but has no message for running out."
                }
            }
            assertTrue(method.catches.isNotEmpty()) { "A method can catch nothing at all." }

            for (fish in method.catches) {
                val type =
                    checkNotNull(cacheTypes.objs[fish.fish.id]) {
                        "A catch yields an unresolvable fish."
                    }
                assertTrue(fish.level in 1..99) {
                    "'${type.internalName}' has an out-of-range level: ${fish.level}."
                }
                assertTrue(fish.xp > 0.0) { "'${type.internalName}' awards no experience." }
                // An inverted or zero weight pair would make the fish impossible to land.
                assertTrue(fish.rateLow in 1..fish.rateHigh) {
                    "'${type.internalName}' has a broken success range: " +
                        "${fish.rateLow}..${fish.rateHigh}"
                }
            }

            // Rolled best-first, so a list in the wrong order would make high levels catch the
            // worst fish nearly every time.
            val levels = method.catches.map { it.level }
            assertEquals(levels.sortedDescending(), levels) {
                "A method's catches are not ordered highest level first: $levels"
            }
        }
    }

    @Test
    fun GameTestState.`Lumbridge's spots are fishable from level one`() = runBasicGameTest {
        // These two are the only spots the world actually spawns today, so if either stops
        // resolving, fishing is unreachable in practice no matter what the rest of the cache says.
        val swamp = cacheTypes.npcs.values.single { it.internalName == "0_50_49_saltfish" }
        val river = cacheTypes.npcs.values.single { it.internalName == "0_50_50_freshfish" }

        val net = checkNotNull(FishingSpots.methodFor(swamp, op = 1))
        assertEquals(FishingObjs.net.id, net.tool.id) { "The swamp spot is not a small net spot." }
        assertEquals(1, net.lowestLevel) { "A new account cannot fish at the swamp spot." }

        val lure = checkNotNull(FishingSpots.methodFor(river, op = 1))
        assertEquals(FishingObjs.fly_fishing_rod.id, lure.tool.id) {
            "The river spot is not a lure spot."
        }
    }

    @Test
    fun GameTestState.`Tutorial Island's spot yields the tutorial's own shrimps`() =
        runBasicGameTest {
            val newbie = checkNotNull(cacheTypes.npcs[FishingSpotNpcs.newbie.id])
            val method = checkNotNull(FishingSpots.methodFor(newbie, op = 1))
            assertEquals(1, method.catches.size) { "The tutorial spot should catch one thing." }

            val caught = method.catches.single()
            assertEquals(FishingObjs.newbie_raw_shrimp.id, caught.fish.id) {
                "The tutorial spot yields ordinary shrimps, which can be carried off the island."
            }
            assertEquals(1, caught.level) { "The tutorial spot is not fishable at level 1." }
        }
}
