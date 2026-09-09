package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.utility_content
import org.rsmod.content.skills.magic.utility.configs.utility_objs
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.content.skills.magic.utility.configs.utility_synths
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Bones to Bananas and Bones to Peaches: every buriable bone in the inventory becomes fruit.
 *
 * "Buriable" is the prayer module's `prayer_bones` content group, found by name rather than by
 * depending on that module, so the two spells convert exactly what the altar accepts.
 */
class BonesToFoodScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val bananas = spellbooks.spellFor(objs.spell_bones_to_bananas)
        val peaches = spellbooks.spellFor(objs.spell_bones_to_peaches)
        onSelfSpell(bananas, protectedAccess, eventBus) { convert(bananas, utility_objs.banana) }
        onSelfSpell(peaches, protectedAccess, eventBus) { convert(peaches, utility_objs.peach) }
    }

    private fun ProtectedAccess.convert(spell: MagicSpell, food: ObjType) {
        if (actionDelay > mapClock) {
            return
        }
        val slots = inv.indices.filter { slot -> inv[slot]?.let { isBone(it.id) } == true }
        val cast =
            casting.attemptUtility(this, spell) {
                if (slots.isEmpty()) {
                    mes("You aren't holding any bones!")
                }
                slots.isNotEmpty()
            }
        if (!cast) {
            return
        }
        for (slot in slots) {
            val count = inv[slot]?.count ?: continue
            invReplaceSlot(inv, slot, count, food)
        }
        anim(utility_seqs.bones_to_food)
        spotanim(utility_spotanims.bones_to_food, height = 92)
        soundSynth(utility_synths.bones_to_food)
        actionDelay = mapClock + CAST_DELAY
    }

    private fun isBone(objId: Int): Boolean =
        objTypes[objId]?.isContentType(utility_content.bones) == true

    private companion object {
        const val CAST_DELAY = 2
    }
}
