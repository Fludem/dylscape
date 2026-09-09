package org.rsmod.content.interfaces.settings.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.config.refs.varps
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.enumVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerInit
import org.rsmod.api.utils.vars.VarEnumDelegate
import org.rsmod.content.interfaces.settings.configs.setting_components
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ControlSettingsScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    private var Player.acceptAid by boolVarBit(varbits.option_acceptaid)
    private var Player.skullPrevention by boolVarBit(varbits.skull_prevent)
    private var Player.priorityPlayer by enumVarp<PlayerPriority>(varps.option_attackpriority)
    private var Player.priorityNpc by enumVarp<NpcPriority>(varps.option_attackpriority_npc)
    private val Player.newAccount by boolVarBit(varbits.new_player_account)

    override fun ScriptContext.startup() {
        onPlayerInit { player.setDefaultAttackPriority() }

        onIfOverlayButton(setting_components.skull_prevention) { player.toggleSkullPrevention() }

        onIfOverlayButton(setting_components.attack_priority_player_buttons) {
            player.selectPlayerPriority(comsub)
        }

        onIfOverlayButton(setting_components.attack_priority_npc_buttons) {
            player.selectNpcPriority(comsub)
        }

        onIfOverlayButton(setting_components.acceptaid) { player.toggleAcceptAid() }
        onIfOverlayButton(setting_components.houseoptions) { player.selectHouseOptions() }
        onIfOverlayButton(setting_components.bondoptions) { player.selectBondPouch() }
    }

    /**
     * Starts a fresh account on `Left-click where available` for both players and npcs, rather than
     * on whatever the varps' zero value happens to mean.
     *
     * Zero is [PlayerPriority.CombatLevel] and [NpcPriority.CombatLevel], which hide the left-click
     * `Attack` on anything whose combat level is far enough from yours — which, on a server where
     * the interesting fights are against things you are not level-matched with, just reads as the
     * option being missing.
     *
     * Gated on [varbits.new_player_account] for the same reason `InitialStatsScript` is: that
     * varbit is rewritten from the login response on every login, so it is only ever true on the
     * very first one. A returning player's own choice is never overwritten — including a deliberate
     * choice of `CombatLevel`, which is indistinguishable from "unset" and would be clobbered by
     * anything less careful than a first-login check.
     */
    private fun Player.setDefaultAttackPriority() {
        if (!newAccount) {
            return
        }
        priorityPlayer = PlayerPriority.LeftClick
        priorityNpc = NpcPriority.LeftClick
    }

    private fun Player.toggleSkullPrevention() {
        skullPrevention = !skullPrevention
    }

    private fun Player.selectPlayerPriority(comsub: Int) {
        val priority =
            when (comsub) {
                1 -> PlayerPriority.CombatLevel
                2 -> PlayerPriority.RightClickAlways
                3 -> PlayerPriority.LeftClick
                4 -> PlayerPriority.Hidden
                5 -> PlayerPriority.RightClickClan
                else -> error("Invalid comsub: $comsub")
            }
        priorityPlayer = priority
    }

    private fun Player.selectNpcPriority(comsub: Int) {
        val priority =
            when (comsub) {
                1 -> NpcPriority.CombatLevel
                2 -> NpcPriority.RightClickAlways
                3 -> NpcPriority.LeftClick
                4 -> NpcPriority.Hidden
                else -> error("Invalid comsub: $comsub")
            }
        priorityNpc = priority
    }

    private fun Player.toggleAcceptAid() {
        acceptAid = !acceptAid
    }

    private fun Player.selectHouseOptions() {
        protectedAccess.launch(this) { ifOpenSide(interfaces.poh_options) }
    }

    private fun Player.selectBondPouch() {
        val opened = protectedAccess.launch(this) { ifOpenMainModal(interfaces.bond_main, -1, -2) }
        if (!opened) {
            mes(constants.dm_busy)
        }
    }
}

private enum class PlayerPriority(override val varValue: Int) : VarEnumDelegate {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
    RightClickClan(4),
}

private enum class NpcPriority(override val varValue: Int) : VarEnumDelegate {
    CombatLevel(0),
    RightClickAlways(1),
    LeftClick(2),
    Hidden(3),
}
