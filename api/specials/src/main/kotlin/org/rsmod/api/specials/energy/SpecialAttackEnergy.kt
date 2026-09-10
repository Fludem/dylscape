package org.rsmod.api.specials.energy

import jakarta.inject.Inject
import org.rsmod.api.config.refs.varps
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

public class SpecialAttackEnergy @Inject constructor(private val perks: Perks) {
    private var Player.specialEnergy by intVarp(varps.sa_energy)

    public fun hasSpecialEnergy(player: Player, energyInHundreds: Int): Boolean {
        return player.specialEnergy >= cost(player, energyInHundreds)
    }

    public fun takeSpecialEnergy(player: Player, energyInHundreds: Int) {
        val cost = cost(player, energyInHundreds)
        require(player.specialEnergy >= cost) {
            "Not enough special energy to take. Use `hasSpecialEnergy` first for validation."
        }
        player.specialEnergy -= cost
    }

    /** What a special costing [energyInHundreds] actually takes from [player]. */
    private fun cost(player: Player, energyInHundreds: Int): Int =
        if (perks.has(player, Perk.CheapSpecials)) {
            minOf(energyInHundreds, CHEAP_SPECIAL_COST)
        } else {
            energyInHundreds
        }

    public fun isSpecializedRequirement(energyInHundreds: Int): Boolean {
        return energyInHundreds < 10
    }

    public companion object {
        public const val MAX_ENERGY: Int = 1000

        /** The most a special can cost under [Perk.CheapSpecials]: 20% of the bar. */
        public const val CHEAP_SPECIAL_COST: Int = 200
    }
}
