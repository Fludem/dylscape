package org.rsmod.content.other.special.attacks.boost

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.spotanims
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.synths
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.content.other.special.attacks.configs.special_objs
import org.rsmod.content.other.special.attacks.configs.special_seqs

/**
 * Rampage: takes 10% of the wielder's current Attack, Defence, Ranged and Magic, and boosts
 * Strength by 10 plus a quarter of the total taken.
 */
class DragonBattleaxeSpecialAttack @Inject constructor(private val worldRepo: WorldRepository) :
    SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant(special_objs.dragon_battleaxe, ::activateRampage)
    }

    private fun activateRampage(access: ProtectedAccess): Boolean = access.rampage()

    private fun ProtectedAccess.rampage(): Boolean {
        var drained = 0
        for (stat in RAMPAGE_DRAINS) {
            val amount = stat(stat) / 10
            if (amount > 0) {
                statSub(stat, constant = amount, percent = 0)
                drained += amount
            }
        }
        statBoost(stats.strength, constant = 10 + drained / 4, percent = 0)

        say("Raarrrrrgggggghhhhhhh!")
        anim(special_seqs.dragon_battleaxe)
        spotanim(spotanims.sp_attackglow_red, height = 96, slot = constants.spotanim_slot_combat)
        soundArea(worldRepo, coords, synths.rampage, radius = 1)
        return true
    }

    private companion object {
        val RAMPAGE_DRAINS = listOf(stats.attack, stats.defence, stats.ranged, stats.magic)
    }
}
