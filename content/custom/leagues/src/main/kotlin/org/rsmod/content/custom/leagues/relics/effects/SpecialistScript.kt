package org.rsmod.content.custom.leagues.relics.effects

import kotlin.math.min
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.varps
import org.rsmod.api.npc.events.NpcDeathEvents
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onEvent
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Specialist's kill refund: every npc kill gives back [KILL_RESTORE] special attack energy.
 *
 * The relic's other effects - the 20% cost cap, doubled spec accuracy and doubled regen - live in
 * `api/specials` behind their perks.
 */
class SpecialistScript : PluginScript() {
    private var Player.specialEnergy by intVarp(varps.sa_energy)

    override fun ScriptContext.startup() {
        onEvent<NpcDeathEvents.Killed> {
            val player = killer ?: return@onEvent
            if (player.hasRelic(Relic.Specialist)) {
                player.specialEnergy =
                    min(constants.sa_max_energy, player.specialEnergy + KILL_RESTORE)
            }
        }
    }

    private companion object {
        /** 15% of the bar. */
        const val KILL_RESTORE = 150
    }
}
