package org.rsmod.content.skills.fletching.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.fletching.configs.CrossbowRecipe
import org.rsmod.content.skills.fletching.configs.FletchingRecipes
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Fitting metal limbs to a wooden stock.
 *
 * The result is an unstrung crossbow; [BowStringing] finishes it, since a crossbow string is just
 * another stringing row. Stocks are cut from logs by [LogFletching], so the whole chain -- logs to
 * stock, stock plus limbs to unstrung, unstrung plus string to crossbow -- is playable end to end.
 */
class CrossbowFletching
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.crossbows) {
            onOpHeldU(recipe.limb, recipe.stock) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: CrossbowRecipe) {
        val carried = affordable(recipe)
        if (carried == 0) {
            return
        }
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Make,
                title = "How many would you like to make?",
                objs = listOf(recipe.unstrung),
                maxQuantity = carried,
            ) ?: return
        assemble(recipe, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.assemble(recipe: CrossbowRecipe, count: Int) {
        if (player.fletchingLvl < recipe.levelReq) {
            val name = objTypes[recipe.unstrung].name.lowercase()
            mes("You need a Fletching level of ${recipe.levelReq} to make a $name.")
            return
        }

        var made = 0
        while (made < count && affordable(recipe) > 0) {
            anim(recipe.seq)
            delay(ASSEMBLE_TICKS)

            // Re-checked after the delay: either part could have left the inventory.
            if (affordable(recipe) == 0) {
                break
            }
            invDel(inv, recipe.limb, 1)
            invDel(inv, recipe.stock, 1)
            invAdd(inv, recipe.unstrung)
            statAdvance(stats.fletching, recipe.xp * xpMods.get(player, stats.fletching))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the parts to make that.")
        }
    }

    private fun ProtectedAccess.affordable(recipe: CrossbowRecipe): Int =
        minOf(invTotal(inv, recipe.limb), invTotal(inv, recipe.stock))

    private companion object {
        /** Ticks per crossbow. Tuned, not measured against live. */
        const val ASSEMBLE_TICKS = 3
    }
}
