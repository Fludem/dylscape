package org.rsmod.content.custom.knightwaves

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseDefenceLvl
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.region.RegionRepository
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.trialComplete
import org.rsmod.content.custom.knightwaves.KnightWavesProgress.wavesCleared
import org.rsmod.content.custom.knightwaves.configs.KnightWavesArena
import org.rsmod.content.custom.knightwaves.configs.knightwaves_npcs
import org.rsmod.game.entity.Npc
import org.rsmod.game.region.Region
import org.rsmod.game.type.npc.NpcTypeList
import org.rsmod.map.CoordGrid

/**
 * The mechanics of the trial: the arena, the waves and the reward.
 *
 * Kept apart from [org.rsmod.content.custom.knightwaves.scripts.KnightWavesScript] because the
 * script is only bindings and conversation, while this is the part with rules worth testing on its
 * own - the wave count, what happens on the sixth kill, what the reward writes.
 */
@Singleton
class KnightWavesTrial
@Inject
constructor(
    private val regionRepo: RegionRepository,
    private val npcRepo: NpcRepository,
    private val npcTypes: NpcTypeList,
) {
    /**
     * Allocates a private copy of the training grounds, moves the player into it and sets the next
     * knight on them.
     *
     * Returns `null` when there is nothing to enter for: the trial is already finished, or the
     * server has run out of region slots. Neither case leaves anything to clean up.
     */
    fun enter(access: ProtectedAccess): Region? {
        // Nothing left to face. Guarded here as well as in the conversation because `wavesCleared`
        // saturates at `TOTAL_WAVES`, so a finished player would otherwise index past the last
        // knight.
        if (access.player.trialComplete) {
            return null
        }
        val region = regionRepo.add(KnightWavesArena.template) ?: return null
        val entrance = region.normal[KnightWavesArena.entrance]
        val arenaTile = region.normal[KnightWavesArena.knightSpawn]

        access.telejump(entrance)
        access.rebuildAppearance()
        spawnKnight(arenaTile, wave = access.player.wavesCleared)
        return region
    }

    /**
     * Called once a knight has fallen. [arenaTile] is where that knight stood, which is where the
     * next one appears.
     */
    suspend fun knightDefeated(access: ProtectedAccess, arenaTile: CoordGrid) {
        val cleared = access.player.wavesCleared + 1
        if (cleared < KnightWavesProgress.TOTAL_WAVES) {
            access.vars[varbits.kr_knightwaves_state] = cleared
            val remaining = KnightWavesProgress.TOTAL_WAVES - cleared
            val plural = if (remaining == 1) "knight still stands" else "knights still stand"
            access.mes("The knight yields. $remaining $plural against you.")
            spawnKnight(arenaTile, wave = cleared)
            return
        }
        complete(access)
    }

    /**
     * [wave] is the number of knights already beaten, which doubles as the index of the one still
     * to face.
     */
    fun spawnKnight(arenaTile: CoordGrid, wave: Int) {
        val type = npcTypes[knightwaves_npcs.waves[wave]]
        npcRepo.add(Npc(type, arenaTile), duration = KNIGHT_LIFETIME)
    }

    suspend fun complete(access: ProtectedAccess) {
        access.vars[varbits.kr_knightwaves_state] = KnightWavesProgress.COMPLETE
        access.vars[varbits.preserve_unlocked] = 1
        access.statAdvance(stats.defence, DEFENCE_XP)
        access.statAdvance(stats.prayer, PRAYER_XP)
        access.telejump(KnightWavesArena.squireCoords)
        access.rebuildAppearance()
        access.mesbox(
            "You have bested all six knights of the hall.<br><br>" +
                "You may now use the <col=000080>Chivalry</col> and " +
                "<col=000080>Piety</col> prayers, and the knights have taught you " +
                "<col=000080>Preserve</col>."
        )
        // The prayers unlock, but they still have their own levels. Saying so here saves the
        // player opening the book and finding them just as grey as before.
        val player = access.player
        if (player.basePrayerLvl < CHIVALRY_PRAYER || player.baseDefenceLvl < CHIVALRY_DEFENCE) {
            access.mes(
                "You will need $CHIVALRY_PRAYER Prayer and $CHIVALRY_DEFENCE Defence " +
                    "before Chivalry will answer you."
            )
        }
    }

    private companion object {
        /**
         * Long enough that no fight outlasts it, short enough that a knight left behind by some
         * mishap cleans itself up. The region normally takes them first, when its last occupant
         * leaves.
         */
        const val KNIGHT_LIFETIME = 3000

        /** What King's Ransom itself pays out. */
        const val DEFENCE_XP = 33_000.0
        const val PRAYER_XP = 10_000.0

        const val CHIVALRY_PRAYER = 60
        const val CHIVALRY_DEFENCE = 65
    }
}
