package org.rsmod.content.interfaces.firstlogin.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias first_login_interfaces = FirstLoginInterfaces

typealias first_login_components = FirstLoginComponents

object FirstLoginInterfaces : InterfaceReferences() {
    val setup = find("first_login_setup")
}

/**
 * Server-side handles for the components [FirstLoginBuilder] authors.
 *
 * Identity hashes are deliberately omitted while the layout is still moving: `find` with no hash
 * takes the auto-resolve path and adopts whatever the cache holds. Pin them once the panel is
 * final, so an accidental layout change becomes a boot failure rather than a silent difference.
 */
object FirstLoginComponents : ComponentReferences() {
    val page_mode = find("first_login_setup:page_mode")
    val page_rate = find("first_login_setup:page_rate")

    val mode_buttons = List(MODE_COUNT) { find("first_login_setup:mode_button_$it") }
    val rate_buttons = List(RATE_COUNT) { find("first_login_setup:rate_button_$it") }

    private const val MODE_COUNT = 4
    private const val RATE_COUNT = 3
}
