package org.rsmod.content.custom.teleports.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.custom.teleports.MAX_WILDERNESS_LEVEL
import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations
import org.rsmod.content.custom.teleports.configs.TeleportPanelLayout
import org.rsmod.content.custom.teleports.configs.teleport_components
import org.rsmod.content.custom.teleports.configs.teleport_interfaces
import org.rsmod.content.custom.teleports.data.LastTeleportRegistry
import org.rsmod.content.custom.teleports.wildernessLevel
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Turns the standard spellbook's Home Teleport button into the `teleport_panel` destination picker:
 * one tab per [TeleportCategory], a grid of destination tiles, and Home / Previous underneath.
 *
 * The panel itself is static cache data (see `TeleportPanelBuilder`); this script only decides what
 * it shows. Switching tabs swaps the tab art and relabels the generic slot tiles, so the
 * destination table stays plain Kotlin and can grow without a repack.
 *
 * The frame's close button is wired client-side by `[clientscript,steelborder]`, so there is no
 * close handler here.
 */
class TeleportPanelScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val lastTeleports: LastTeleportRegistry,
) : PluginScript() {
    /**
     * The tab each player is looking at. A slot click is resolved against this - never against the
     * slot index alone - because the server cannot see which slots the client has hidden, and a
     * replayed click on a hidden slot must not teleport anywhere.
     *
     * It also makes the panel reopen on the tab the player last used this session.
     */
    private val viewing = HashMap<Player, TeleportCategory>()

    override fun ScriptContext.startup() {
        onIfOverlayButton(teleport_components.home_teleport) {
            protectedAccess.launch(player) { openPanel() }
        }
        TeleportCategory.entries.forEachIndexed { index, category ->
            onIfModalButton(teleport_components.tabs[index]) { showCategory(category) }
        }
        teleport_components.slots.forEachIndexed { index, slot ->
            onIfModalButton(slot) { pickSlot(index) }
        }
        onIfModalButton(teleport_components.home) { teleportTo(TeleportDestinations.home) }
        onIfModalButton(teleport_components.previous) {
            lastTeleports[player]?.let { teleportTo(it) }
        }
        // `Delete`, not `Logout`: the latter fires before the account save. Neither map needs to
        // outlive the session; they just must not pin the player forever.
        onEvent<SessionStateEvent.Delete> {
            lastTeleports.remove(player)
            viewing.remove(player)
        }
    }

    private fun ProtectedAccess.openPanel() {
        // Refused up front, the way a spell refuses to cast, rather than after a pick.
        if (tooDeepToTeleport()) {
            return
        }
        ifOpenMainModal(teleport_interfaces.panel)
        showPrevious()
        showCategory(viewing[player] ?: TeleportCategory.entries.first())
    }

    private fun ProtectedAccess.showCategory(category: TeleportCategory) {
        viewing[player] = category
        TeleportCategory.entries.forEachIndexed { index, tab ->
            val selected = tab == category
            ifSetHide(teleport_components.tabs_selected[index], hide = !selected)
            ifSetHide(teleport_components.tabs_unselected[index], hide = selected)
        }
        val destinations = TeleportDestinations[category]
        for (slot in 0 until TeleportPanelLayout.SLOT_COUNT) {
            val destination = destinations.getOrNull(slot)
            if (destination != null) {
                ifSetText(teleport_components.slot_labels[slot], destination.label)
            }
            ifSetHide(teleport_components.slots[slot], hide = destination == null)
        }
    }

    private fun ProtectedAccess.showPrevious() {
        val previous = lastTeleports[player]
        if (previous != null) {
            ifSetText(teleport_components.previous_label, "Previous: ${previous.label}")
        }
        ifSetHide(teleport_components.previous, hide = previous == null)
    }

    private fun ProtectedAccess.pickSlot(slot: Int) {
        val category = viewing[player] ?: return
        val destination = TeleportDestinations[category].getOrNull(slot) ?: return
        teleportTo(destination)
    }

    private fun ProtectedAccess.teleportTo(destination: TeleportDestination) {
        ifClose()
        // Checked again on the pick: the panel may have been opened before the player walked in.
        if (tooDeepToTeleport()) {
            return
        }
        val dest = destination.dest
        telejump(mapFindSquareLineOfWalk(dest, 0, 2) ?: dest)
        mes(destination.arrival)
        // Recorded after the guard, so a refused teleport never becomes the "Previous" button.
        lastTeleports.set(player, destination)
    }

    /**
     * Checked on departure, not arrival: no destination is in the Wilderness, and the rule being
     * modelled is that you cannot teleport *out* of deep Wilderness.
     */
    private fun ProtectedAccess.tooDeepToTeleport(): Boolean {
        if (wildernessLevel(player.coords) <= MAX_WILDERNESS_LEVEL) {
            return false
        }
        mes("You can't teleport above level $MAX_WILDERNESS_LEVEL Wilderness.")
        return true
    }
}
