package org.rsmod.content.custom.teleports.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias teleport_components = TeleportComponents

typealias teleport_interfaces = TeleportInterfaces

object TeleportInterfaces : InterfaceReferences() {
    /** Interface 1001, authored by [TeleportPanelBuilder]. */
    val panel = find("teleport_panel")
}

/**
 * The standard spellbook's Home Teleport button, and the server-side handles for the components
 * [TeleportPanelBuilder] authors.
 *
 * `magic_spellbook` is registered as a gameframe overlay in
 * `content/interfaces/gameframe/.../StandardOverlays.kt`, so clicks on [home_teleport] arrive as
 * `IfOverlayButton`; the panel is a modal, so its clicks arrive as `IfModalButton`.
 *
 * Components 5 and 6 of `magic_spellbook` are the league home teleports; 7 is the standard one.
 *
 * Panel handles carry no identity hash while the layout may still move - see `FirstLoginComponents`
 * for the same choice.
 */
object TeleportComponents : ComponentReferences() {
    val home_teleport = find("magic_spellbook:teleport_home_standard")

    val tabs = List(TAB_COUNT) { find("teleport_panel:tab_$it") }
    val tabs_selected = List(TAB_COUNT) { find("teleport_panel:tab_${it}_on") }
    val tabs_unselected = List(TAB_COUNT) { find("teleport_panel:tab_${it}_off") }

    val slots = List(TeleportPanelLayout.SLOT_COUNT) { find("teleport_panel:slot_$it") }
    val slot_labels =
        List(TeleportPanelLayout.SLOT_COUNT) { find("teleport_panel:slot_${it}_label") }

    val home = find("teleport_panel:home")
    val previous = find("teleport_panel:previous")
    val previous_label = find("teleport_panel:previous_label")

    private val TAB_COUNT: Int
        get() = TeleportCategory.entries.size
}
