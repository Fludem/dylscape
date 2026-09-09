package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.LunarTables
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Bake Pie: cooks every uncooked pie in the inventory, one at a time, paying runes for each.
 *
 * Written as the `Smelting.smelt` loop: re-validate every pass, since the pies can be banked or
 * dropped mid-cast, and stop the moment a cast fails so a rune shortfall ends it cleanly.
 */
class BakePieScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val xpMods: XpModifiers,
) : PluginScript() {
    private val byRaw = LunarTables.bakePie.associateBy { it.raw.id }

    override fun ScriptContext.startup() {
        val spell = spellbooks.spellFor(objs.spell_bake_pie)
        onSelfSpell(spell, protectedAccess, eventBus) { bakeAll(spell) }
    }

    private suspend fun ProtectedAccess.bakeAll(
        spell: org.rsmod.api.combat.commons.magic.MagicSpell
    ) {
        var baked = 0
        while (true) {
            val slot = inv.indices.firstOrNull { inv[it]?.id in byRaw }
            if (slot == null) {
                if (baked == 0) mes("You have no pies to bake.")
                break
            }
            val pie = byRaw.getValue(inv[slot]!!.id)
            val cast =
                casting.attemptUtility(this, spell) {
                    if (player.cookingLvl < pie.level) {
                        mes("You need a Cooking level of ${pie.level} to bake that pie.")
                    }
                    player.cookingLvl >= pie.level
                }
            if (!cast) {
                break
            }
            invReplaceSlot(inv, slot, count = 1, replacement = pie.cooked)
            statAdvance(stats.cooking, pie.xp * xpMods.get(player, stats.cooking))
            anim(utility_seqs.bake_pie)
            spotanim(utility_spotanims.bake_pie, height = 92)
            baked++
            delay(BAKE_DELAY)
        }
        resetAnim()
    }

    private companion object {
        const val BAKE_DELAY = 3
    }
}
