package org.rsmod.content.skills.fletching.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.fletchingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.fletching.configs.FletchingRecipes
import org.rsmod.content.skills.fletching.configs.FletchingSeqs
import org.rsmod.content.skills.fletching.configs.StringRecipe
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Fitting a string to an unstrung bow, and the crossbow equivalent.
 *
 * Bows take a bowstring and crossbows take a crossbow string, which is why one table covers both:
 * only the string obj differs. The pair is registered unstrung-first so the recipe lookup can key
 * on `event.first` no matter which way round the player clicked -- `onOpHeldU` normalises the
 * arguments to the registration order.
 */
class BowStringing
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
    private val perks: Perks,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.stringing) {
            onOpHeldU(recipe.unstrung, recipe.string) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: StringRecipe) {
        val carried = invTotal(inv, recipe.unstrung)
        if (carried == 0) {
            return
        }
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.String,
                title = "How many would you like to make?",
                objs = listOf(recipe.strung),
                maxQuantity = carried,
            ) ?: return
        attachString(recipe, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.attachString(recipe: StringRecipe, count: Int) {
        if (player.fletchingLvl < recipe.levelReq) {
            val name = objTypes[recipe.strung].name.lowercase()
            mes("You need a Fletching level of ${recipe.levelReq} to make a $name.")
            return
        }

        val instant = perks.has(player, Perk.InstantProduction)

        var made = 0
        while (made < count && canAfford(recipe)) {
            anim(FletchingSeqs.string_bow)
            if (made == 0 || !instant) {
                delay(STRING_TICKS)
            }

            // Re-checked after the delay: either half could have left the inventory.
            if (!canAfford(recipe)) {
                break
            }
            invDel(inv, recipe.unstrung, 1)
            invDel(inv, recipe.string, 1)
            invAdd(inv, recipe.strung)
            statAdvance(stats.fletching, recipe.xp * xpMods.get(player, stats.fletching))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the materials to string that.")
        }
    }

    private fun ProtectedAccess.canAfford(recipe: StringRecipe): Boolean =
        invTotal(inv, recipe.unstrung) >= 1 && invTotal(inv, recipe.string) >= 1

    private companion object {
        /** Ticks per bow. Tuned, not measured against live. */
        const val STRING_TICKS = 3
    }
}
