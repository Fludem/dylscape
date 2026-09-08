package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.crafting.configs.CraftingObjs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.CraftingSeqs
import org.rsmod.content.skills.crafting.configs.LeatherProduct
import org.rsmod.content.skills.crafting.configs.LeatherRecipe
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Stitching leather and dragonhide with a needle and thread.
 *
 * Thread is spent a reel at a time, five items to the reel, and the reel carries over between
 * actions the way it does in the live game -- so a player who makes three items and comes back
 * later still has two left on that reel. The count lives on the player's inventory rather than in
 * server state: what is tracked is only how far into the current reel this run has got.
 */
class LeatherCrafting
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in CraftingRecipes.leather) {
            onOpHeldU(CraftingObjs.needle, recipe.hide) { openMenu(recipe) }
        }
    }

    private suspend fun ProtectedAccess.openMenu(recipe: LeatherRecipe) {
        if (invTotal(inv, CraftingObjs.thread) == 0) {
            mes("You need some thread to make anything out of leather.")
            return
        }
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Make,
                title = "What would you like to make?",
                objs = recipe.products.map { it.product },
            ) ?: return
        val product = recipe.products.getOrNull(pick.slot) ?: return
        stitch(recipe, product, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.stitch(
        recipe: LeatherRecipe,
        product: LeatherProduct,
        count: Int,
    ) {
        if (player.craftingLvl < product.levelReq) {
            val name = objTypes[product.product].name.lowercase()
            mes("You need a Crafting level of ${product.levelReq} to make $name.")
            return
        }

        var made = 0
        var onReel = 0
        while (made < count && canAfford(recipe, product)) {
            anim(CraftingSeqs.leather)
            delay(STITCH_TICKS)

            // Re-checked after the delay: the hide or the thread could have been banked.
            if (!canAfford(recipe, product)) {
                break
            }
            invDel(inv, recipe.hide, product.hides)
            invAdd(inv, product.product)
            statAdvance(stats.crafting, product.xp * xpMods.get(player, stats.crafting))
            made++

            onReel++
            if (onReel == CraftingRecipes.ITEMS_PER_THREAD) {
                onReel = 0
                invDel(inv, CraftingObjs.thread, 1)
                if (invTotal(inv, CraftingObjs.thread) == 0) {
                    mes("You have run out of thread.")
                    break
                }
            }
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have enough leather to make that.")
        }
    }

    private fun ProtectedAccess.canAfford(recipe: LeatherRecipe, product: LeatherProduct): Boolean =
        invTotal(inv, recipe.hide) >= product.hides && invTotal(inv, CraftingObjs.thread) > 0

    private companion object {
        /** Ticks per item. Tuned, not measured against live. */
        const val STITCH_TICKS = 3
    }
}
