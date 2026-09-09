package org.rsmod.content.skills.magic.spellbooks

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.spellbooks.configs.EdgevilleAltars
import org.rsmod.content.skills.magic.spellbooks.scripts.SpellbookAltarScript
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap

class AltarDeps @Inject constructor(val collision: CollisionFlagMap)

/**
 * The altar coordinates were read off `MagicDump`'s Edgeville render; this keeps them honest
 * against the real collision map and drives both altars end to end.
 */
@Execution(ExecutionMode.SAME_THREAD)
class EdgevilleAltarsTest {
    @Test
    fun GameTestState.`both altar footprints are open ground with a clear tile to stand on`() =
        runInjectedGameTest(AltarDeps::class) { deps ->
            for (altar in EdgevilleAltars.all) {
                for (tile in altar.footprint) {
                    assertEquals(0, deps.collision[tile]) {
                        "${altar.loc} footprint blocked at $tile"
                    }
                }
                val south = altar.coords.translateZ(-1)
                assertEquals(0, deps.collision[south]) { "${altar.loc} has no clear tile south" }
            }
            // And they do not overlap each other.
            val ancient = EdgevilleAltars.ancient.footprint.toSet()
            val astral = EdgevilleAltars.astral.footprint.toSet()
            assertEquals(emptySet<Any>(), ancient.intersect(astral))
        }

    @Test
    fun GameTestState.`praying at the ancient altar toggles between ancient and standard`() =
        runGameTest(SpellbookAltarScript::class) {
            // Placed away from the real footprint so the placement test above stays honest.
            val altar = EdgevilleAltars.ancient
            val loc = placeMapLoc(OP_TEST_TILE, locTypes[altar.loc], altar.shape, altar.angle)
            player.placeAt(OP_TEST_TILE.translateZ(-1))

            player.opLoc1(loc)
            advance(2)
            assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])

            player.opLoc1(loc)
            advance(2)
            assertEquals(Spellbook.Standard.varValue, player.vars[varbits.spellbook])
        }

    @Test
    fun GameTestState.`praying at the astral altar switches to lunar`() =
        runGameTest(SpellbookAltarScript::class) {
            val altar = EdgevilleAltars.astral
            val loc = placeMapLoc(OP_TEST_TILE, locTypes[altar.loc], altar.shape, altar.angle)
            player.placeAt(OP_TEST_TILE.translateZ(-1))

            player.opLoc2(loc)
            advance(2)
            assertEquals(Spellbook.Lunars.varValue, player.vars[varbits.spellbook])
        }

    private companion object {
        /** Open ground west of the altars in the same square; the render shows it clear. */
        val OP_TEST_TILE = CoordGrid(3086, 3505, 0)
    }
}
