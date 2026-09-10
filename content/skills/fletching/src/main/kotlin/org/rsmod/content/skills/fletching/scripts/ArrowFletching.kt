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
import org.rsmod.content.skills.fletching.configs.AttachRecipe
import org.rsmod.content.skills.fletching.configs.FletchingRecipes
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Everything that is finished by attaching one stack to another: feathers onto shafts, heads onto
 * headless arrows, feathers onto dart tips and unfeathered bolts, heads onto javelin shafts.
 *
 * They share a script because they share a shape. What differs is the batch -- fifteen for arrows,
 * bolts and javelins, ten for darts -- and the animation, both of which ride on the recipe row.
 *
 * The quantity the player picks is a number of *batches*, which is what the live game's menu
 * counts. A short final batch is still made and paid for: running out with four shafts left should
 * hand back four arrows, not nothing.
 */
class ArrowFletching
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
    private val perks: Perks,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in FletchingRecipes.attaching) {
            onOpHeldU(recipe.base, recipe.tip) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: AttachRecipe) {
        val batches =
            affordable(recipe).let { if (it == 0) 0 else (it + recipe.batch - 1) / recipe.batch }
        if (batches == 0) {
            return
        }
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Make,
                title = "How many would you like to make?",
                objs = listOf(recipe.product),
                maxQuantity = batches,
            ) ?: return
        attach(recipe, batches = pick.quantity)
    }

    private suspend fun ProtectedAccess.attach(recipe: AttachRecipe, batches: Int) {
        if (player.fletchingLvl < recipe.levelReq) {
            val name = objTypes[recipe.product].name.lowercase()
            mes("You need a Fletching level of ${recipe.levelReq} to make $name.")
            return
        }

        val instant = perks.has(player, Perk.InstantProduction)

        var made = 0
        while (made < batches) {
            val size = affordable(recipe).coerceAtMost(recipe.batch)
            if (size == 0) {
                break
            }
            anim(recipe.seq)
            if (made == 0 || !instant) {
                delay(ATTACH_TICKS)
            }

            // Re-measured rather than re-checked: the batch is only as big as what is still held
            // once the animation has played.
            val settled = affordable(recipe).coerceAtMost(recipe.batch)
            if (settled == 0) {
                break
            }
            invDel(inv, recipe.base, settled)
            invDel(inv, recipe.tip, settled)
            invAdd(inv, recipe.product, settled)
            statAdvance(stats.fletching, recipe.xp * settled * xpMods.get(player, stats.fletching))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the materials to make that.")
        }
    }

    /** How many individual items the held materials could finish, ignoring the batch ceiling. */
    private fun ProtectedAccess.affordable(recipe: AttachRecipe): Int =
        minOf(invTotal(inv, recipe.base), invTotal(inv, recipe.tip))

    private companion object {
        /** Ticks per batch. Tuned, not measured against live. */
        const val ATTACH_TICKS = 3
    }
}
