package org.rsmod.content.custom.zulrah.configs

import org.rsmod.api.repo.region.RegionTemplate
import org.rsmod.content.custom.zulrah.ZulrahSpot
import org.rsmod.map.CoordGrid

/**
 * Zulrah's shrine, in the swamp east of Zul-Andra, and the village the boat leaves from.
 *
 * The shrine ships in the rev-233 map. `ZulrahDump`'s walkability render shows a U of platform over
 * water: a south strip (z 3068-3070) and two lanes running north from it (x 2262-2264 and x
 * 2272-2274, z 3071-3077). Everything else is water, which is where Zulrah sits. An npc may stand
 * on a blocked tile - `NpcRegistry.add` does not check walkability - so the four surfacing spots
 * are all in the water around the platform.
 *
 * Every fight runs in its own copy of it, and the copy has to cover all four 5x5 spots:
 * `StandardNpcAccess.telejump` silently does nothing when its destination zone was not copied.
 */
object ZulrahShrine {
    /**
     * Zones 281-285 x, 382-385 z cover x 2248-2287 by z 3056-3087: the platform, all four spots and
     * a margin of water around them. The shrine straddles mapsquares 35_47 and 35_48, which the
     * zone copy does not care about.
     */
    val template: RegionTemplate =
        RegionTemplate.create {
            copy(SHRINE_ZONE_X, SHRINE_ZONE_Z, SHRINE_LEVEL) {
                zoneWidth = SHRINE_ZONE_WIDTH
                zoneLength = SHRINE_ZONE_LENGTH
            }
        }

    /** The zones [template] copies, so a test can check every spot lands inside them. */
    val copiedZonesX: IntRange = SHRINE_ZONE_X until SHRINE_ZONE_X + SHRINE_ZONE_WIDTH
    val copiedZonesZ: IntRange = SHRINE_ZONE_Z until SHRINE_ZONE_Z + SHRINE_ZONE_LENGTH

    /** Where the player lands, on the middle of the south strip, facing Zulrah. */
    val arrival: CoordGrid = CoordGrid(2268, 3069, SHRINE_LEVEL)

    /**
     * South-west corners of Zulrah's four surfacing spots. Each 5x5 footprint sits entirely in
     * water: the middle one between the two lanes, the south one below the strip, and the east and
     * west ones outside their lanes.
     */
    val spots: Map<ZulrahSpot, CoordGrid> =
        mapOf(
            ZulrahSpot.Middle to CoordGrid(2266, 3072, SHRINE_LEVEL),
            ZulrahSpot.South to CoordGrid(2266, 3062, SHRINE_LEVEL),
            ZulrahSpot.East to CoordGrid(2276, 3072, SHRINE_LEVEL),
            ZulrahSpot.West to CoordGrid(2256, 3072, SHRINE_LEVEL),
        )

    /**
     * The platform tiles, read off the same render. Clouds and snakelings land on these, so a
     * barrage never drops a cloud in the water where it cannot hurt anyone.
     */
    val platform: List<CoordGrid> = buildList {
        for (x in 2266..2270) add(CoordGrid(x, 3068, SHRINE_LEVEL))
        for (x in 2264..2272) add(CoordGrid(x, 3069, SHRINE_LEVEL))
        for (x in 2262..2273) add(CoordGrid(x, 3070, SHRINE_LEVEL))
        for (z in 3071..3076) {
            for (x in 2262..2264) add(CoordGrid(x, z, SHRINE_LEVEL))
            for (x in 2272..2274) add(CoordGrid(x, z, SHRINE_LEVEL))
        }
        // The west lane's last row is a tile narrower than the rest.
        for (x in 2263..2264) add(CoordGrid(x, 3077, SHRINE_LEVEL))
        for (x in 2272..2274) add(CoordGrid(x, 3077, SHRINE_LEVEL))
    }

    /**
     * Zul-Andra, beside the Sacrificial boat at (2214, 3056). The boat is 3x3 and blocks, so this
     * is the open tile just west of its south-west corner.
     */
    val zulAndra: CoordGrid = CoordGrid(2212, 3056, 0)

    const val ZULRAH_SIZE: Int = 5

    private const val SHRINE_LEVEL = 0
    private const val SHRINE_ZONE_X = 281
    private const val SHRINE_ZONE_Z = 382
    private const val SHRINE_ZONE_WIDTH = 5
    private const val SHRINE_ZONE_LENGTH = 4
}
