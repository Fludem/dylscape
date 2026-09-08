package org.rsmod.api.net.rsprot.player

import org.rsmod.game.type.comp.UnpackedComponentType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.ui.UserInterfaceMap

internal object InterfaceEvents {
    /**
     * Whether the server is willing to accept [event] on [component].
     *
     * A press on a component with no dynamic child arrives with `comsub == -1`. Such a component
     * usually carries its op in the cache, but not always: an interface whose buttons are built by
     * a clientscript starts out with no events at all and is made clickable by the server calling
     * `if_setevents(component, -1, -1, ...)` at runtime -- which is how the construction build menu
     * works, and there is no other way to enable it, since components have no type editor.
     *
     * So both are consulted. The grant is looked up at slot `0` rather than `-1` because
     * [org.rsmod.game.ui.collection.ComponentEventMap.Event.from] reads a `-1` bound as "unbounded"
     * and stores `-1..-1` as `0..Int.MAX_VALUE`; nothing can ever match a query at `-1`. A grant
     * only exists because the server asked for it, so this stays as strict as reading the cache.
     */
    fun isEnabled(
        ui: UserInterfaceMap,
        component: UnpackedComponentType,
        comsub: Int,
        event: IfEvent,
    ): Boolean {
        if (comsub == -1) {
            return component.hasEvent(event) || ui.hasEvent(component, slot = 0, event)
        }
        return ui.hasEvent(component, comsub, event)
    }
}
