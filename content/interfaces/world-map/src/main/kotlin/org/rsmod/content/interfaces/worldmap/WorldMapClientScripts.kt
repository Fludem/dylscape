package org.rsmod.content.interfaces.worldmap

import org.rsmod.api.player.output.runClientScript
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * `[clientscript,worldmap_transmitdata]` is the only writer of the three varcs that the world map
 * draws its markers from. Its three arguments land in `varc,worldmap_youarehere`,
 * `varc,worldmap_clue` and `varc,worldmap_gravestone`, in that order, and each is a packed coord
 * with `-1` -- which is what [CoordGrid.NULL] packs to -- meaning "no marker".
 * `[clientscript,worldmap_overlay]` re-reads them on a client-side timer, so the markers keep
 * themselves drawn once the values are set.
 *
 * All three varcs are written on every call, so a clue or gravestone marker cannot be transmitted
 * on its own: it has to be threaded through here alongside the position, or the next position
 * update will clear it.
 */
internal fun Player.worldMapTransmitData(
    youAreHere: CoordGrid,
    clue: CoordGrid = CoordGrid.NULL,
    gravestone: CoordGrid = CoordGrid.NULL,
) {
    runClientScript(WORLDMAP_TRANSMITDATA, youAreHere.packed, clue.packed, gravestone.packed)
}

private const val WORLDMAP_TRANSMITDATA = 1749
