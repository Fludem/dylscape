package org.rsmod.content.custom.teleports.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.teleports.MAX_WILDERNESS_LEVEL
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.wildernessLevel
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
class TeleportMenuScript @Inject constructor(private val protectedAccess: ProtectedAccessLauncher) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfOverlayButton(teleport_components.home_teleport) {
            protectedAccess.launch(player) { teleportMenu() }
        }
    }

    private suspend fun ProtectedAccess.teleportMenu() {
        while (true) {
            val categories = TeleportCategory.entries
            val picked = menu(TITLE, hotkeys = true, choices = categories.map { it.label } + CANCEL)
            // Anything past the categories is the trailing "Cancel"; closing the menu instead
            // never reaches here, since the coroutine simply stays suspended.
            val category = categories.getOrNull(picked) ?: return
            if (pickDestination(category)) return
        }
    }

    /** Returns `true` once the player has teleported, `false` to fall back to the category list. */
    private suspend fun ProtectedAccess.pickDestination(category: TeleportCategory): Boolean {
        // Built once and indexed by the same list the rows were drawn from, so the labels on
        // screen and the coordinate we act on cannot drift apart.
        val destinations = TeleportDestinations[category]
        val picked = menu(TITLE, hotkeys = true, choices = destinations.map { it.label } + BACK)
        val destination = destinations.getOrNull(picked) ?: return false
        teleportTo(destination)
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
    }

    private companion object {
        const val TITLE = "Where would you like to teleport to?"
        const val CANCEL = "Cancel"
        const val BACK = "Back"
    }
}
