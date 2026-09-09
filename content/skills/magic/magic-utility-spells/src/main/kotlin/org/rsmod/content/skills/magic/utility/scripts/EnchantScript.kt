package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.EnchantTable
import org.rsmod.content.skills.magic.utility.configs.utility_params
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The seven jewellery enchantments, cast on an item in the inventory.
 *
 * The spells sit in the spellbook's sub-list, which the client opens and closes on its own (its
 * `magic_spellbook_op` script sets varbit `spellbook_sublist` locally from the "View" button's obj
 * param), so the server only ever sees the seven `enchant_N` buttons. Their obj params carry
 * `spell_desc` ("For use on ruby and topaz jewellery"), which is what the wrong-level message is
 * built from, so it cannot drift from what the spellbook tooltip says.
 */
class EnchantScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for ((level, obj) in EnchantTable.spells) {
            val spell = spellbooks.spellFor(obj)
            onSpellOnInvObj(spell, protectedAccess, eventBus) { slot, target ->
                enchant(spell, level, slot, target)
            }
        }
    }

    private fun ProtectedAccess.enchant(
        spell: MagicSpell,
        level: Int,
        slot: Int,
        obj: UnpackedObjType,
    ) {
        if (actionDelay > mapClock || !slotHolds(slot, obj)) {
            return
        }
        val row = EnchantTable.byBase[obj.id]
        val cast =
            casting.attemptUtility(this, spell) {
                when {
                    row == null -> {
                        mes("You can only enchant jewellery with this spell.")
                        false
                    }
                    row.level != level -> {
                        val desc =
                            objTypes[spell.obj].paramMap?.getOrNull(utility_params.spell_desc)
                        val usage = desc?.removePrefix("For use on ") ?: "other jewellery"
                        mes("This spell can only be cast on $usage.")
                        false
                    }
                    else -> true
                }
            }
        if (!cast || row == null) {
            return
        }
        invReplaceSlot(inv, slot, count = 1, replacement = row.product)
        anim(EnchantTable.anim(row))
        spotanim(EnchantTable.spotanim(row), height = 92)
        soundSynth(EnchantTable.sound(row))
        actionDelay = mapClock + ENCHANT_DELAY
    }

    private companion object {
        const val ENCHANT_DELAY = 3
    }
}
