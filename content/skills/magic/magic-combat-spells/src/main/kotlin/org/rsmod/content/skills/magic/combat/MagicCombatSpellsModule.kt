package org.rsmod.content.skills.magic.combat

import org.rsmod.api.spells.attack.SpellAttackMap
import org.rsmod.content.skills.magic.combat.spells.AncientSpells
import org.rsmod.content.skills.magic.combat.spells.StandardCombatSpells
import org.rsmod.plugin.module.PluginModule

/**
 * Registers the combat spells with upstream's `SpellAttackRegistry`, exactly as
 * `content/skills/magic/spell-attacks` registers the twenty elemental spells. The registry refuses
 * a duplicate, so the elementals are never touched here.
 */
class MagicCombatSpellsModule : PluginModule() {
    override fun bind() {
        addSetBinding<SpellAttackMap>(StandardCombatSpells::class.java)
        addSetBinding<SpellAttackMap>(AncientSpells::class.java)
    }
}
