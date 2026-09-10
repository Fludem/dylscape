package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.crafting.configs.CraftingObjs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.CraftingSeqs
import org.rsmod.content.skills.crafting.configs.GemRecipe
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Cutting gems with a chisel.
 *
 * Opal, jade and red topaz can shatter, which is modelled rather than smoothed away: the gem is
 * spent either way and "You accidentally crush the gem" is a line players recognise. The odds taper
 * with level -- see `CraftingRecipes.crushChance` -- because the live rates do, even though the
 * exact curve is not published.
 *
 * A failed cut still counts against the requested quantity, for the same reason a failed iron smelt
 * does in `Smelting`: counting attempts rather than successes is what stops "cut 5" from quietly
 * eating fifteen gems on an unlucky run.
 */
class GemCutting
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
    private val perks: Perks,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in CraftingRecipes.gems) {
            onOpHeldU(objs.chisel, recipe.uncut) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: GemRecipe) {
        val carried = invTotal(inv, recipe.uncut)
        if (carried == 0) {
            return
        }
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Make,
                title = "How many would you like to cut?",
                objs = listOf(recipe.cut),
                maxQuantity = carried,
            ) ?: return
        cut(recipe, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.cut(recipe: GemRecipe, count: Int) {
        if (player.craftingLvl < recipe.levelReq) {
            val name = objTypes[recipe.cut].name.lowercase()
            mes("You need a Crafting level of ${recipe.levelReq} to cut a $name.")
            return
        }

        val crushChance = CraftingRecipes.crushChance(recipe, player.craftingLvl)
        val instant = perks.has(player, Perk.InstantProduction)
        var made = 0
        while (made < count && invTotal(inv, recipe.uncut) > 0) {
            anim(CraftingSeqs.chisel)
            if (made == 0 || !instant) {
                delay(CUT_TICKS)
            }

            // Re-checked after the delay: the gems could have been banked mid-animation.
            if (invTotal(inv, recipe.uncut) == 0) {
                break
            }
            invDel(inv, recipe.uncut, 1)
            made++

            if (crushChance > 0 && random.of(maxExclusive = 100) < crushChance) {
                invAdd(inv, CraftingObjs.crushed_gemstone)
                mes("You accidentally crush the gem.")
                continue
            }

            invAdd(inv, recipe.cut)
            statAdvance(stats.crafting, recipe.xp * xpMods.get(player, stats.crafting))
            mes("You cut the ${objTypes[recipe.cut].name.lowercase()}.")
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have any gems to cut.")
        }
    }

    private companion object {
        /** Ticks per gem. Tuned, not measured against live. */
        const val CUT_TICKS = 3
    }
}
