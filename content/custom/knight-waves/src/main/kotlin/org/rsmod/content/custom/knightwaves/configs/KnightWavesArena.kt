package org.rsmod.content.custom.knightwaves.configs

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.map.CoordGrid

/**
 * The Knight Waves Training Grounds: the hall on Camelot Castle's top floor, behind the
 * `kr_cam_wave_doubledoor` pair at (2751, 3507). It ships in the rev-233 map complete with its
 * sword dummies, archery targets and blood splatter, so the arena is copied rather than built.
 *
 * Every trial runs in its own copy of it. Regions are reclaimed once nobody is standing inside, and
 * the knights go with them, so leaving is the only cleanup there is.
 */
object KnightWavesArena {
    /**
     * Zones 343-346 x, 436-439 z cover x 2744-2775 by z 3488-3519 - the whole hall plus the walls
     * around it. Level 2 only: nothing below is part of the trial.
     */
    val template: RegionTemplate =
        RegionTemplate.create {
            copy(ARENA_ZONE_X, ARENA_ZONE_Z, ARENA_LEVEL) {
                zoneWidth = ARENA_ZONE_WIDTH
                zoneLength = ARENA_ZONE_LENGTH
            }
        }

    /** Where the player lands, in the open middle of the hall. */
    val entrance: CoordGrid = CoordGrid(2758, 3500, ARENA_LEVEL)

    /**
     * Where every knight appears - six tiles north of [entrance], inside `huntRange` so the wave
     * begins without the player having to go looking for it.
     */
    val knightSpawn: CoordGrid = CoordGrid(2758, 3506, ARENA_LEVEL)

    /** Camelot's courtyard, where the Squire stands and where the trial returns you. */
    val squireCoords: CoordGrid = CoordGrid(2757, 3475, 0)

    private const val ARENA_LEVEL = 2
    private const val ARENA_ZONE_X = 343
    private const val ARENA_ZONE_Z = 436
    private const val ARENA_ZONE_WIDTH = 4
    private const val ARENA_ZONE_LENGTH = 4
}
