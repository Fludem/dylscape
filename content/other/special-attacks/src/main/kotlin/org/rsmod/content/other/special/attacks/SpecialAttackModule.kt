package org.rsmod.content.other.special.attacks

import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.content.other.special.attacks.boost.DragonBattleaxeSpecialAttack
import org.rsmod.content.other.special.attacks.boost.StatBoostSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.AbyssalBludgeonSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DaggerSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.DragonClawsSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonLongswordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.DragonWarhammerSpecialAttack
import org.rsmod.content.other.special.attacks.melee.GodswordSpecialAttacks
import org.rsmod.content.other.special.attacks.melee.GraniteMaulSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SaradominSwordSpecialAttack
import org.rsmod.content.other.special.attacks.melee.SimpleMeleeSpecialAttacks
import org.rsmod.content.other.special.attacks.ranged.DarkBowSpecialAttack
import org.rsmod.content.other.special.attacks.ranged.MagicBowSpecialAttacks
import org.rsmod.plugin.module.PluginModule

class SpecialAttackModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpecialAttackMap>(StatBoostSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DarkBowSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonLongswordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SimpleMeleeSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DaggerSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(DragonWarhammerSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonClawsSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(GodswordSpecialAttacks::class.java)
        addSetBinding<SpecialAttackMap>(AbyssalBludgeonSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(SaradominSwordSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(GraniteMaulSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(DragonBattleaxeSpecialAttack::class.java)
        addSetBinding<SpecialAttackMap>(MagicBowSpecialAttacks::class.java)
    }
}
