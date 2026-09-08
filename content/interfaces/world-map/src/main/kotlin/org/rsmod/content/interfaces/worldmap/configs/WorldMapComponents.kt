package org.rsmod.content.interfaces.worldmap.configs

import org.rsmod.api.type.refs.comp.ComponentReferences

typealias worldmap_components = WorldMapComponents

object WorldMapComponents : ComponentReferences() {
    val orb = find("orbs:worldmap")

    val close = find("worldmap:close")
    val esckey = find("worldmap:esckey")
}
