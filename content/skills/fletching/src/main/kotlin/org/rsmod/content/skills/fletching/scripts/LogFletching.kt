package org.rsmod.content.skills.fletching.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.fletching.configs.CutLogRecipe
import org.rsmod.content.skills.fletching.configs.CutProduct
import org.rsmod.content.skills.fletching.configs.FletchingRecipes
import org.rsmod.content.skills.fletching.configs.FletchingSeqs
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Cutting logs with a knife.
 *
 * Every log type is bound as its own obj pair rather than through a content group, because logs
 * already belong to `firemaking_logs` and an obj carries exactly one group. Enumerating the pairs
 * costs nine bindings and leaves the cache untouched.
 *
 * The menu lists every product the log offers, including ones the player is too low to make. That
 * matches the live game -- the level is checked when a button is pressed, not when the menu is
 * drawn.
 */
class LogFletching
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.cutting) {
            onOpHeldU(objs.knife, recipe.log) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: CutLogRecipe) {
        // A single product still goes through the menu: it is how the player picks a quantity.
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Cut,
                title = "What would you like to make?",
                objs = recipe.products.map { it.product },
            ) ?: return
        val product = recipe.products.getOrNull(pick.slot) ?: return
        cut(recipe, product, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.cut(recipe: CutLogRecipe, product: CutProduct, count: Int) {
        if (player.fletchingLvl < product.levelReq) {
            val name = objTypes[product.product].name.lowercase()
            mes("You need a Fletching level of ${product.levelReq} to make a $name.")
            return
        }

        var made = 0
        while (made < count && invTotal(inv, recipe.log) >= product.logs) {
            anim(FletchingSeqs.cut_logs)
            delay(CUT_TICKS)

            // Re-checked after the delay: the logs could have been banked or dropped mid-animation.
            if (invTotal(inv, recipe.log) < product.logs) {
                break
            }
            invDel(inv, recipe.log, product.logs)
            invAdd(inv, product.product, product.count)
            statAdvance(stats.fletching, product.xp * xpMods.get(player, stats.fletching))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have enough logs to make that.")
        }
    }

    private companion object {
        /** Ticks per cut. Tuned, not measured against live. */
        const val CUT_TICKS = 3
    }
}
