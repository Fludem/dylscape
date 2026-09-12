package org.rsmod.content.custom.autosave.configs

import org.rsmod.api.type.refs.timer.TimerReferences

internal typealias autosave_timers = AutosaveTimers

internal object AutosaveTimers : TimerReferences() {
    /** Set on login, so every player's saves are staggered by whenever they logged in. */
    val autosave = find("autosave")
}
