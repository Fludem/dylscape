package org.rsmod.content.skills.magic.utility.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.utility.configs.utility_seqs
import org.rsmod.content.skills.magic.utility.configs.utility_spotanims
import org.rsmod.content.skills.magic.utility.configs.utility_synths
import org.rsmod.content.skills.smithing.configs.SmeltRecipe
import org.rsmod.content.skills.smithing.configs.SmeltingRecipes
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Superheat Item: smelt one bar from an ore in the inventory, without a furnace and without the
 * furnace's failure chance.
 *
 * The recipes are Smithing's own `SmeltingRecipes`, so the two can never disagree about what an ore
 * makes. Where an ore has more than one recipe (iron ore is iron or, with two coal, steel) the best
 * bar the player can make is chosen, the way the official spell does.
 */
class SuperheatScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val objTypes: ObjTypeList,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = spellbooks.spellFor(objs.spell_superheat)
        onSpellOnInvObj(spell, protectedAccess, eventBus) { slot, obj ->
            superheat(spell, slot, obj)
        }
    }

    private fun ProtectedAccess.superheat(spell: MagicSpell, slot: Int, obj: UnpackedObjType) {
        if (actionDelay > mapClock || !slotHolds(slot, obj)) {
            return
        }
        val recipes = SmeltingRecipes.byOre[obj.id].orEmpty()
        var chosen: SmeltRecipe? = null
        val cast =
            casting.attemptUtility(this, spell) {
                if (recipes.isEmpty()) {
                    mes("You need to cast this spell on an ore.")
                    return@attemptUtility false
                }
                val affordable =
                    recipes
                        .filter { recipe ->
                            recipe.ingredients.all { hasIngredient(it.obj, it.count) }
                        }
                        .sortedByDescending { it.levelReq }
                val recipe = affordable.firstOrNull()
                if (recipe == null) {
                    mes("You don't have the ore needed to make a bar from that.")
                    return@attemptUtility false
                }
                if (player.smithingLvl < recipe.levelReq) {
                    val bar = objTypes[recipe.bar].name.lowercase()
                    mes("You need a Smithing level of ${recipe.levelReq} to make a $bar.")
                    return@attemptUtility false
                }
                chosen = recipe
                true
            }
        val recipe = chosen
        if (!cast || recipe == null) {
            return
        }
        for (ingredient in recipe.ingredients) {
            invDel(inv, ingredient.obj, count = ingredient.count)
        }
        invAdd(inv, recipe.bar)
        statAdvance(stats.smithing, recipe.xp * xpMods.get(player, stats.smithing))
        anim(utility_seqs.superheat)
        spotanim(utility_spotanims.superheat, height = 92)
        soundSynth(utility_synths.superheat)
        actionDelay = mapClock + SUPERHEAT_DELAY
    }

    private fun ProtectedAccess.hasIngredient(obj: org.rsmod.game.type.obj.ObjType, count: Int) =
        inv.count(objTypes[obj]) >= count

    private companion object {
        const val SUPERHEAT_DELAY = 3
    }
}
