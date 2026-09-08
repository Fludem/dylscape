package org.rsmod.content.areas.misc.tutorial

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorial.configs.TutorialLocs
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.type.loc.UnpackedLocType
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Makes Tutorial Island's doors work.
 *
 * The `newbie_door*` locs all carry `Open` on op1 but no base script binds them, so without this a
 * new player is trapped in the Gielinor Guide's room. Each one is a single-model wall door, so
 * "opening" it swings the same loc a quarter-turn onto the neighbouring edge and drops its
 * collision from the doorway — exactly what the generic [org.rsmod.content.generic.locs.doors]
 * script does, only that one keys off a separate opened-door loc id which these tutorial doors do
 * not have. The door reverts on its own after [DURATION] cycles.
 *
 * The doors are intentionally left ungated: progression is driven by the instructors and skill xp,
 * not by locking doors, and gating them on a mis-guessed stage could strand a player.
 */
class TutorialDoors @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (door in TutorialLocs.allDoors) {
            onOpLoc1(door) { openDoor(it.loc, it.type) }
        }
    }

    private suspend fun ProtectedAccess.openDoor(door: BoundLocInfo, type: UnpackedLocType) {
        val openAngle = door.turnAngle(rotations = 1)
        val openCoords = translateOpen(door.coords, door.shape, door.angle)

        // A diagonal door opening onto the tile the player stands on would clip them; step off.
        val stepAway = diagonalStepAway(door.shape, door.angle, openCoords)
        if (coords == openCoords && stepAway != CoordGrid.NULL) {
            teleport(stepAway)
            delay(1)
        }

        locRepo.del(door, DURATION)
        locRepo.add(openCoords, type, DURATION, openAngle, door.shape)
    }

    private fun translateOpen(coords: CoordGrid, shape: LocShape, angle: LocAngle): CoordGrid =
        when {
            shape == LocShape.WallStraight && angle == LocAngle.West -> coords.translateX(-1)
            shape == LocShape.WallStraight && angle == LocAngle.North -> coords.translateZ(1)
            shape == LocShape.WallStraight && angle == LocAngle.East -> coords.translateX(1)
            shape == LocShape.WallStraight && angle == LocAngle.South -> coords.translateZ(-1)
            shape == LocShape.WallDiagonal && angle == LocAngle.West -> coords.translateZ(1)
            shape == LocShape.WallDiagonal && angle == LocAngle.North -> coords.translateX(1)
            shape == LocShape.WallDiagonal && angle == LocAngle.East -> coords.translateZ(-1)
            shape == LocShape.WallDiagonal && angle == LocAngle.South -> coords.translateX(-1)
            else -> coords
        }

    private fun diagonalStepAway(shape: LocShape, angle: LocAngle, opened: CoordGrid): CoordGrid {
        if (shape != LocShape.WallDiagonal) {
            return CoordGrid.NULL
        }
        return when (angle) {
            LocAngle.West -> opened.translateX(-1)
            LocAngle.North -> opened.translateZ(1)
            LocAngle.East -> opened.translateX(1)
            LocAngle.South -> opened.translateZ(-1)
        }
    }

    private companion object {
        /** Cycles a door stays open before reverting; matches the generic door script. */
        private const val DURATION = 500
    }
}
