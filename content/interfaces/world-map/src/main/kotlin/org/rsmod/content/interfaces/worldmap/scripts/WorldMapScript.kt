package org.rsmod.content.interfaces.worldmap.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.ui.ifCloseOverlay
import org.rsmod.api.player.ui.ifOpenOverlay
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfOpen
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.interfaces.worldmap.configs.worldmap_components
import org.rsmod.content.interfaces.worldmap.configs.worldmap_interfaces
import org.rsmod.content.interfaces.worldmap.configs.worldmap_timers
import org.rsmod.content.interfaces.worldmap.worldMapTransmitData
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The world map orb sets its ops client-side in `[proc,orbs_worldmap_setup]`, but the ops
 * themselves are part of the static event mask on `orbs:worldmap`, so they are sent to us as
 * regular button clicks. The client-side hook only plays the click sound; opening `worldmap` is
 * ours to do.
 *
 * Op2 is `Open World Map` on the basic client, and `Floating World Map` on enhanced desktop, where
 * op3 adds `Fullscreen World Map`. Both open the same interface: the window size and placement
 * within it are handled by `[proc,worldmap_window_set]` client-side.
 *
 * Op4 (`Minimise`/`Restore Minimap`) is not handled here.
 */
class WorldMapScript @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(worldmap_components.orb) { player.selectWorldMapOrb(op) }
        onIfOverlayButton(worldmap_components.close) { player.closeWorldMap() }
        onIfOverlayButton(worldmap_components.esckey) { player.closeWorldMap() }

        onIfOpen(worldmap_interfaces.worldmap) { player.startTrackingPosition() }
        onIfClose(worldmap_interfaces.worldmap) { player.stopTrackingPosition() }
        onPlayerSoftTimer(worldmap_timers.transmit) { player.transmitPosition() }
    }

    private fun Player.selectWorldMapOrb(op: IfButtonOp) {
        when (op) {
            IfButtonOp.Op2,
            IfButtonOp.Op3 -> openWorldMap()
            else -> return
        }
    }

    /**
     * `toplevel_osrs_stretch:floater` spans the whole client, which is what `worldmap:safezone`
     * expects to be sized against.
     */
    private fun Player.openWorldMap() {
        ifOpenOverlay(worldmap_interfaces.worldmap, eventBus)
    }

    private fun Player.closeWorldMap() {
        ifCloseOverlay(worldmap_interfaces.worldmap, eventBus)
    }

    /**
     * The "you are here" marker is a plain varc that only the server writes, so it has to be kept
     * in step with the player for as long as the map is up: the map is an overlay, and the player
     * can walk, teleport and change level underneath it. Transmitting on a one-cycle timer is
     * simpler -- and no less correct -- than watching for a coord change, and the packet is three
     * ints.
     */
    private fun Player.startTrackingPosition() {
        transmitPosition()
        softTimer(worldmap_timers.transmit, 1)
    }

    private fun Player.stopTrackingPosition() {
        clearSoftTimer(worldmap_timers.transmit)
        // Leave no marker behind for the next time the map is opened before the first transmit.
        worldMapTransmitData(CoordGrid.NULL)
    }

    private fun Player.transmitPosition() {
        worldMapTransmitData(coords)
    }
}
