package org.rsmod.content.skills.woodcutting.configs

import org.rsmod.api.type.refs.obj.ObjReferences

internal object WoodcuttingObjs : ObjReferences() {
    /**
     * The Lumberjack league relic's "Echo axe". It only works as an axe for a player holding
     * `Perk.EchoAxe`, and then as a crystal axe with no level requirement.
     */
    val echo_axe = find("league_trailblazer_axe")
}
