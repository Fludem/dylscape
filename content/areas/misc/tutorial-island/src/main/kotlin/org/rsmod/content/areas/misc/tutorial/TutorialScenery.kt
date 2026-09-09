package org.rsmod.content.areas.misc.tutorial

import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.seqs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.content.areas.misc.tutorial.configs.TutorialConstants
import org.rsmod.content.areas.misc.tutorial.configs.TutorialLocs
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The island's ladders and its bank booth.
 *
 * Both are unbound in the base game for the same reason its doors were: the `newbie*` locs carry
 * their ops but sit in no content group, so none of the generic `generic-locs` scripts can see
 * them. The ladders are the more serious of the two -- the mining cave holds four of the eight
 * instructors' worth of progression (mine, smelt, smith, fight) and the only way in or out of it is
 * `newbieladdertop1`, so an unbound ladder stops the tutorial dead at the Quest Guide.
 *
 * The ladders are the ordinary dungeon pair: down is `+[CAVE_OFFSET]` on z, up is `-[CAVE_OFFSET]`,
 * which puts 3088,3119 onto 3088,9519 and 3111,9526 back onto 3111,3126. That is done here rather
 * than by tagging them into `content.dungeonladder_down`/`_up` because a loc's content group is a
 * single field -- one owner per type -- and binding by type keeps this module the only thing that
 * touches Tutorial Island's locs.
 */
class TutorialScenery : PluginScript() {
    override fun ScriptContext.startup() {
        for (ladder in TutorialLocs.laddersDown) {
            onOpLoc1(ladder) { climb(CAVE_OFFSET) }
        }
        for (ladder in TutorialLocs.laddersUp) {
            onOpLoc1(ladder) { climb(-CAVE_OFFSET) }
        }
        onOpLoc1(TutorialLocs.bank_booth) { openBank() }
    }

    private suspend fun ProtectedAccess.climb(translateZ: Int) {
        arriveDelay()
        val dest: CoordGrid = player.coords.translateZ(translateZ)
        anim(seqs.human_pickupfloor)
        delay(1)
        telejump(dest)
        handOverHintArrow()
    }

    /**
     * Moves the tutorial's hint arrow on once the player has actually used the ladder.
     *
     * The instructor who sends a player to a ladder points the arrow at the ladder, so arriving at
     * the other end would otherwise leave the arrow behind on a ladder they have already climbed.
     * Re-pointing it here is the only moment we know they have made the trip.
     */
    private fun ProtectedAccess.handOverHintArrow() {
        val next =
            when (player.tutorialStage) {
                TutorialStage.MINING_MINE -> TutorialConstants.HINT_TILES["mining"]
                TutorialStage.BANK -> TutorialConstants.HINT_TILES["account"]
                else -> null
            }
        if (next != null) hintArrow(next)
    }

    private fun ProtectedAccess.openBank() {
        ifOpenMainSidePair(main = interfaces.bank_main, side = interfaces.bank_side)
    }

    private companion object {
        /**
         * The z distance between the island and the cave under it. The `newbie*` ladders carry no
         * `climb_anim` param, so unlike the generic dungeon ladders the offset and the animation
         * are both named here rather than read off the type.
         */
        private const val CAVE_OFFSET = 6400
    }
}
