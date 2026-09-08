package org.rsmod.content.interfaces.worldmap.configs

import org.rsmod.api.type.refs.interf.InterfaceReferences

typealias worldmap_interfaces = WorldMapInterfaces

object WorldMapInterfaces : InterfaceReferences() {
    val worldmap = find("worldmap")
}
