package org.rsmod.content.generic.locs.banks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.testing.GameTestState
import org.rsmod.map.CoordGrid

/**
 * Runs single-threaded on purpose: the interaction tests place locs in the same shared world, and
 * `integration-test-suite` runs methods concurrently by default.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BankLocsTest {
    @Test
    fun GameTestState.`every booth has Bank on op2 and is tagged bank_booth`() = runBasicGameTest {
        for (type in BankLocs.booths) {
            val loc = cacheTypes.locs[type]
            assertEquals(
                "Bank",
                loc.op.getOrNull(1),
                "Expected Bank on op2 for ${loc.internalName}: ${loc.op.toList()}",
            )
            assertEquals(
                content.bank_booth.id,
                loc.contentGroup,
                "Expected bank_booth content group on ${loc.internalName}",
            )
        }
    }

    @Test
    fun GameTestState.`every chest has Use or Bank on op1 and is tagged bank_chest`() =
        runBasicGameTest {
            for (type in BankLocs.chests) {
                val loc = cacheTypes.locs[type]
                assertTrue(loc.op.getOrNull(0) in setOf("Use", "Bank")) {
                    "Expected Use/Bank on op1 for ${loc.internalName}: ${loc.op.toList()}"
                }
                assertEquals(
                    content.bank_chest.id,
                    loc.contentGroup,
                    "Expected bank_chest content group on ${loc.internalName}",
                )
            }
        }

    @Test
    fun GameTestState.`bank op on a non-Lumbridge booth opens the bank`() =
        runGameTest(BankBooth::class) {
            val booth = placeMapLoc(BOOTH_TILE, boothNamed("bankbooth"))
            player.teleport(booth.coords.translateX(-1))

            player.opLoc2(booth)
            advance(ticks = 2)

            assertTrue(player.ui.containsModal(interfaces.bank_main)) {
                "Bank did not open from op2 on `bankbooth` (Varrock West booth)."
            }
        }

    @Test
    fun GameTestState.`use op on a bank chest opens the bank`() =
        runGameTest(BankBooth::class) {
            val chest = placeMapLoc(CHEST_TILE, chestNamed("champions_bankchest"))
            player.teleport(chest.coords.translateX(-1))

            player.opLoc1(chest)
            advance(ticks = 2)

            assertTrue(player.ui.containsModal(interfaces.bank_main)) {
                "Bank did not open from op1 on `champions_bankchest`."
            }
        }

    private fun boothNamed(name: String) = BankLocs.booths.single { it.internalNameValue == name }

    private fun chestNamed(name: String) = BankLocs.chests.single { it.internalNameValue == name }

    private companion object {
        /* Open ground in Lumbridge, away from the tiles other content tests place locs on. */
        val BOOTH_TILE = CoordGrid(0, 50, 50, 36, 31)
        val CHEST_TILE = CoordGrid(0, 50, 50, 38, 31)
    }
}
