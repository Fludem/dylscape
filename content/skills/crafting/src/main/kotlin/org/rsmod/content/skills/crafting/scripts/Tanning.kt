package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.skills.crafting.configs.CraftingInterfaces
import org.rsmod.content.skills.crafting.configs.CraftingNpcs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.TanRecipe
import org.rsmod.content.skills.crafting.configs.TannerComponents
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Tanning hides into leather, on the game's own panel: interface 324.
 *
 * Unlike the jewellery panels this one is entirely server-driven. It carries no `onLoad` hooks, no
 * clientscript in the cache references it, and its 156 components arrive blank -- so every row's
 * model, name and price is sent from here, and the four unused rows are hidden.
 *
 * That also makes the quantity unambiguous, which the jewellery panels are not: each row has its
 * own `_1`, `_5`, `_x` and `_all` buttons, so the button that was pressed *is* the answer. `_x`
 * asks with the standard count dialog.
 *
 * Tanning is a service, not a Crafting action: it costs coins, pays no experience and has no level
 * requirement. It lives in this module because leather is otherwise unobtainable and every leather
 * recipe depends on it.
 *
 * TODO: the Canifis werewolf charges a premium in the live game; all four tanners share one price
 *   list here.
 */
class Tanning @Inject constructor(private val objTypes: ObjTypeList) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in CraftingNpcs.tanners) {
            // Op3 only. All four tanners carry `Talk-to` on op1 and `Trade` on op3, so binding the
            // whole set would have swallowed the dialogue op as well.
            onOpNpc3(npc) { openPanel() }
        }

        for ((row, recipe) in CraftingRecipes.tanning.withIndex()) {
            onIfModalButton(TannerComponents.buttons1[row]) { tan(recipe, 1) }
            onIfModalButton(TannerComponents.buttons5[row]) { tan(recipe, 5) }
            onIfModalButton(TannerComponents.buttonsAll[row]) { tan(recipe, Int.MAX_VALUE) }
            onIfModalButton(TannerComponents.buttonsX[row]) { tan(recipe, countDialog()) }
        }
    }

    private fun ProtectedAccess.openPanel() {
        ifOpenMainModal(CraftingInterfaces.tanner)

        for (row in 0 until TannerComponents.ROWS) {
            val recipe = CraftingRecipes.tanning.getOrNull(row)
            if (recipe == null) {
                // Rows past the end of the table are blanked rather than left showing the
                // placeholder "Hide"/"Cost" text the interface ships with.
                hideRow(row)
                continue
            }
            ifSetObj(TannerComponents.models[row], recipe.leather, MODEL_ZOOM)
            ifSetText(TannerComponents.names[row], recipe.name)
            ifSetText(TannerComponents.prices[row], "${recipe.cost} coins")
            for (button in rowButtons(row)) {
                ifSetHide(button, false)
                ifSetEvents(button, 0..0, IfEvent.Op1)
            }
        }
    }

    private fun ProtectedAccess.hideRow(row: Int) {
        ifSetText(TannerComponents.names[row], "")
        ifSetText(TannerComponents.prices[row], "")
        for (button in rowButtons(row)) {
            ifSetHide(button, true)
        }
    }

    private suspend fun ProtectedAccess.tan(recipe: TanRecipe, requested: Int) {
        ifClose()
        if (requested <= 0) {
            return
        }

        val carried = invTotal(inv, recipe.hide)
        if (carried == 0) {
            mes("You don't have any ${objTypes[recipe.hide].name.lowercase()} to tan.")
            return
        }

        val affordable =
            if (recipe.cost == 0) carried else minOf(carried, invCoinTotal() / recipe.cost)
        val count = minOf(requested, affordable)
        if (count == 0) {
            mes("You can't afford to tan that.")
            return
        }

        // Charged first, then handed over: a failed exchange must not leave the player short. The
        // fee is refunded if the swap does not take, the way `Firemaking` refunds its logs.
        if (!invTakeFee(recipe.cost * count)) {
            mes("You can't afford to tan that.")
            return
        }
        val replaced =
            invReplace(inv, replace = recipe.hide, count = count, replacement = recipe.leather)
        if (!replaced.success) {
            invAdd(inv, objs.coins, recipe.cost * count)
            mes("You don't have room for that.")
            return
        }

        val name = objTypes[recipe.leather].name.lowercase()
        if (count == 1) {
            mes("The tanner tans your hide into $name.")
        } else {
            mes("The tanner tans $count of your hides into $name.")
        }
    }

    private fun rowButtons(row: Int): List<ComponentType> =
        listOf(
            TannerComponents.buttons1[row],
            TannerComponents.buttons5[row],
            TannerComponents.buttonsX[row],
            TannerComponents.buttonsAll[row],
        )

    private companion object {
        /** Zoom for the hide model in each row. Tuned to sit the model inside its slot. */
        const val MODEL_ZOOM = 175
    }
}
