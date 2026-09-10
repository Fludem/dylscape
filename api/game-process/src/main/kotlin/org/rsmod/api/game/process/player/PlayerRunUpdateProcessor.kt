package org.rsmod.api.game.process.player

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.inv.weight.InvWeight
import org.rsmod.api.player.output.UpdateRun
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList

public class PlayerRunUpdateProcessor @Inject constructor(private val objTypes: ObjTypeList) {
    public fun process(player: Player) {
        player.updateRunWeight()
        player.updateRunEnergy()
    }

    /*
     * Every player on this server has unlimited run energy, by design: the orb is held at full.
     * That replaces upstream's weight/agility drain and graceful-boosted restore, which would only
     * ever be undone here the next tick. Anything that lowers energy - a new account's default,
     * an old save, a script - is topped back up on the next update.
     */
    private fun Player.updateRunEnergy() {
        if (runEnergy < constants.run_max_energy) {
            runEnergy = constants.run_max_energy
            UpdateRun.energy(this, runEnergy)
        }
    }

    private fun Player.updateRunWeight() {
        if (!pendingRunWeight) {
            return
        }
        val currentGrams = calculateWeightInGrams()
        val previousGrams = runWeight
        runWeight = currentGrams

        val currentKg = currentGrams / 1000
        val previousKg = previousGrams / 1000
        if (previousKg != currentKg) {
            UpdateRun.weight(this, kg = currentKg)
        }
    }

    private fun Player.calculateWeightInGrams(): Int {
        return InvWeight.calculateWeightInGrams(this, objTypes)
    }
}
