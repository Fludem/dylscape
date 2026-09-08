package org.rsmod.content.interfaces.worldmap.configs

import org.rsmod.api.type.refs.timer.TimerReferences

typealias worldmap_timers = WorldMapTimers

object WorldMapTimers : TimerReferences() {
    val transmit = find("worldmap_transmit")
}
