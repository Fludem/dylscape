package org.rsmod.content.custom.teleports.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.teleports.MAX_WILDERNESS_LEVEL
import org.rsmod.content.custom.teleports.TeleportMenu
import org.rsmod.content.custom.teleports.TeleportRow
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.data.LastTeleportRegistry
import org.rsmod.content.custom.teleports.wildernessLevel
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Turns the standard spellbook's Home Teleport button into a two-level destination picker.
 *
 * The list is the engine's own scrolling choice menu (`ProtectedAccess.menu`), not a bespoke panel.
 * The portal nexus interface (17) would look the part but cannot be driven: its 35 destinations are
 * baked into clientscript, which this repo cannot compile, and the server has no primitive for
 * creating the row subcomponents.
 */
class TeleportMenuScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val lastTeleports: LastTeleportRegistry,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(teleport_components.home_teleport) {
            protectedAccess.launch(player) { teleportMenu() }
        }
        // `Delete`, not `Logout`: the latter fires before the account save, and the entry has to
        // outlive nothing in particular -- it just must not pin the player forever.
        onEvent<SessionStateEvent.Delete> { lastTeleports.remove(player) }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        while (true) {
            val rows = TeleportMenu.topLevel(lastTeleports[player])
            val picked = menu(TeleportMenu.TITLE, hotkeys = true, choices = rows.map { it.label })
            // Anything that is not a row is the trailing "Cancel", or an index past the list;
            // closing the menu instead never reaches here, since the coroutine stays suspended.
            when (val row = rows.getOrNull(picked)) {
                is TeleportRow.Destination -> {
                    teleportTo(row.destination)
                    return
                }
                is TeleportRow.Category -> if (pickDestination(row.category)) return
                else -> return
            }
        }
    }

    /**
     * Returns `true` once the destination menu has been answered, `false` to redraw the categories.
     */
    private suspend fun ProtectedAccess.pickDestination(category: TeleportCategory): Boolean {
        // Built once and indexed by the same list the rows were drawn from, so the labels on
        // screen and the coordinate we act on cannot drift apart.
        val rows = TeleportMenu.destinations(category)
        val picked = menu(TeleportMenu.TITLE, hotkeys = true, choices = rows.map { it.label })
        val row = rows.getOrNull(picked) as? TeleportRow.Destination ?: return false
        teleportTo(row.destination)
        return true
    }

    private fun ProtectedAccess.teleportTo(destination: TeleportDestination) {
        // Checked on departure, not arrival: no destination is in the Wilderness, and the rule
        // being modelled is that you cannot teleport *out* of deep Wilderness.
        if (wildernessLevel(player.coords) > MAX_WILDERNESS_LEVEL) {
            mes("You can't teleport above level $MAX_WILDERNESS_LEVEL Wilderness.")
            return
        }
        val dest = destination.dest
        telejump(mapFindSquareLineOfWalk(dest, 0, 2) ?: dest)
        mes(destination.arrival)
        // Recorded after the guard, so a refused teleport never becomes the "Previous" row.
        lastTeleports.set(player, destination)
    }
}
