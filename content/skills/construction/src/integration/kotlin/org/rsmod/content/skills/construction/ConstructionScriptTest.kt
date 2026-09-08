package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.player.events.interact.LocEvents
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.construction.house.HouseRegistry
import org.rsmod.content.skills.construction.scripts.ConstructionScript
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocInfo
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded: every method here allocates a region and moves the shared player into it.
 */
@Execution(ExecutionMode.SAME_THREAD)
class ConstructionScriptTest {
    class Deps @Inject constructor(val registry: HouseRegistry)

    @Test
    fun GameTestState.`entering a portal builds a house to stand in`() =
        runGameTest(ConstructionScript::class) {
            val (portal, type) = placePortal(x = 20)
            fresh()
            player.teleport(portal.coords.translateX(-2))

            enter(portal, type, buildMode = false)
            advance(ticks = 1)

            assertTrue(player.coords.x >= REGION_START_X) {
                "The player should have been moved into a region, not left at ${player.coords}."
            }
            assertNotNull(findExitPortal()) { "A new house has no way back out." }
        }

    @Test
    fun GameTestState.`leaving through the exit portal returns the player`() =
        runGameTest(ConstructionScript::class) {
            val (portal, type) = placePortal(x = 24)
            fresh()
            val outside = portal.coords.translateX(-2)
            player.teleport(outside)

            enter(portal, type, buildMode = false)
            advance(ticks = 1)
            val exit = findExitPortal()
            assertNotNull(exit)

            val bound = BoundLocInfo(exit!!, locTypes[exit.id]!!)
            player.withProtectedAccess {
                eventBus.publish(this, LocEvents.Op1(bound, bound, locTypes[exit.id]!!))
            }
            advance(ticks = 1)

            assertEquals(outside, player.coords) { "The exit portal did not put the player back." }
        }

    /**
     * The account save reads the house straight out of [HouseRegistry], and it is queued *after*
     * the logout event. Dropping the house on logout meant every house was written back empty --
     * rooms and furniture alike were gone by the next login.
     */
    @Test
    fun GameTestState.`a house outlives the logout event that saves it`() =
        runInjectedGameTest(Deps::class, scripts = arrayOf(ConstructionScript::class)) { deps ->
            val (portal, type) = placePortal(x = 28)
            fresh()
            player.teleport(portal.coords.translateX(-2))

            enter(portal, type, buildMode = true)
            advance(ticks = 1)
            assertNotNull(deps.registry[player]) { "Entering a portal should create a house." }

            eventBus.publish(SessionStateEvent.Logout(player))
            assertNotNull(deps.registry[player]) {
                "The house has to survive logout: the save pipeline runs after this event and " +
                    "reads the house out of the registry."
            }

            eventBus.publish(SessionStateEvent.Delete(player))
            assertNull(deps.registry[player]) {
                "The house should be released once the player leaves the player list."
            }
        }

    private fun GameTestScope.placePortal(x: Int): Pair<BoundLocInfo, UnpackedLocType> {
        val type = findLocTypes { it.internalName == "poh_rimmington_portal" }.first()
        return placeMapLoc(CoordGrid(0, 50, 50, x, 40), type) to type
    }

    private fun GameTestScope.enter(
        portal: BoundLocInfo,
        type: UnpackedLocType,
        buildMode: Boolean,
    ) {
        player.withProtectedAccess {
            if (buildMode) {
                eventBus.publish(this, LocEvents.Op3(portal, portal, type))
            } else {
                eventBus.publish(this, LocEvents.Op1(portal, portal, type))
            }
        }
    }

    /** The exit portal, looked for in the zone the player was put down in. */
    private fun GameTestScope.findExitPortal(): LocInfo? =
        nearbyLocs().firstOrNull { locTypes[it.id]?.internalName == "poh_exit_portal" }

    private fun GameTestScope.countBuildHotspots(): Int =
        nearbyLocs().count { loc ->
            val name = locTypes[loc.id]?.internalName ?: return@count false
            name.startsWith("poh_crude_garden") || name.startsWith("poh_garden_")
        }

    private fun GameTestScope.nearbyLocs(): List<LocInfo> {
        val found = ArrayList<LocInfo>()
        for (dx in -8..8) {
            for (dz in -8..8) {
                val coords = player.coords.translate(dx, dz, 0)
                found += findLocs(coords)
            }
        }
        return found
    }

    private fun GameTestScope.fresh() {
        player.ifClose()
        player.clearInv()
    }

    private companion object {
        /** `RegionRegistry.START_COORD_X`: everything from here east is region working area. */
        const val REGION_START_X = 6400 + 32
    }
}
