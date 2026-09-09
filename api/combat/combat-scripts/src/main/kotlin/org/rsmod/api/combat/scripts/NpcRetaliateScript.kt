package org.rsmod.api.combat.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.npc.attackStyle
import org.rsmod.api.combat.commons.npc.combatDefaultRetaliateAp
import org.rsmod.api.combat.commons.npc.combatDefaultRetaliateOp
import org.rsmod.api.config.refs.queues
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.script.onNpcQueue
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Public so integration suites can start it: `runGameTest` only registers the scripts it is handed,
 * and a test source set cannot see `internal`.
 */
public class NpcRetaliateScript
@Inject
constructor(private val interactions: AiPlayerInteractions) : PluginScript() {
    override fun ScriptContext.startup() {
        onNpcQueue(queues.com_retaliate_player) { autoRetaliatePlayer() }
    }

    private fun StandardNpcAccess.autoRetaliatePlayer() {
        // A ranged or magic npc has to retaliate into ap mode. Retaliating into op walks it into
        // melee range, where it fires its ranged attack animation at point-blank as though it were
        // a punch. `combatDefaultRetaliateAp` has existed unused for exactly as long as the ap
        // path went unregistered.
        if (npc.attackStyle().attacksAtRange) {
            npc.combatDefaultRetaliateAp(interactions)
        } else {
            npc.combatDefaultRetaliateOp(interactions)
        }
    }
}
