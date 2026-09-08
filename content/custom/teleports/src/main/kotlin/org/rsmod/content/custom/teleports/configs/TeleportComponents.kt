package org.rsmod.content.custom.teleports.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias teleport_components = TeleportComponents

/**
 * The standard spellbook's Home Teleport button (`magic_spellbook:7`).
 *
 * `magic_spellbook` is registered as a gameframe overlay in
 * `content/interfaces/gameframe/.../StandardOverlays.kt`, so clicks on this button arrive as
 * `IfOverlayButton` rather than `IfModalButton`.
 *
 * Components 5 and 6 are the league home teleports; 7 is the standard-spellbook one.
 */
object TeleportComponents : ComponentReferences() {
    val home_teleport = find("magic_spellbook:teleport_home_standard")
}
