package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.crafting.configs.CraftingNpcs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.TanRecipe
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Tanning hides into leather, through the make-menu.
 *
 * **Interface 324 cannot be driven, and this used to try.** The panel is in the cache and its
 * components are named -- eight rows of `tanning_X_model` / `_text` / `_price` with their own `_1`,
 * `_5`, `_x` and `_all` buttons -- which is what made it look server-driven. It is not: those
 * buttons carry no op text, no op event, and not one clientscript in the cache references the
 * interface. No amount of `if_setevents` fixes that, because the client builds a click from the
 * component's op *name* and the protocol has no way to send one. The proof is a census of the whole
 * cache: 2,997 components have an op1 event and every single one of them also has op1 text; none
 * has the event without it. So 324 is a layout with nothing wired to it, and the rows would have
 * sat there unclickable.
 *
 * The make-menu is the interface that does work, and it already carries the quantity buttons this
 * needs. What is lost is the price column -- the menu shows names only, so the cost is quoted in
 * the chatbox when the tanning happens.
 *
 * Tanning is a service, not a Crafting action: it costs coins, pays no experience and has no level
 * requirement. It lives in this module because leather is otherwise unobtainable and every leather
 * recipe depends on it.
 *
 * TODO: the Canifis werewolf charges a premium in the live game; all four tanners share one price
 *   list here.
 */
class Tanning
@Inject
constructor(private val objTypes: ObjTypeList, private val skillMulti: SkillMulti) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in CraftingNpcs.tanners) {
            // Op3 only. All four tanners carry `Talk-to` on op1 and `Trade` on op3, so binding the
            // whole set would have swallowed the dialogue op as well.
            onOpNpc3(npc) { openMenu() }
        }
    }

    private suspend fun ProtectedAccess.openMenu() {
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Make,
                title = "What would you like to tan?",
                objs = CraftingRecipes.tanning.map { it.leather },
            ) ?: return
        val recipe = CraftingRecipes.tanning.getOrNull(pick.slot) ?: return
        tan(recipe, pick.quantity)
    }

    private suspend fun ProtectedAccess.tan(recipe: TanRecipe, requested: Int) {
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
            mes("You can't afford to tan that. It costs ${recipe.cost} coins a hide.")
            return
        }

        // Charged first, then handed over: a failed exchange must not leave the player short. The
        // fee is refunded if the swap does not take, the way `Firemaking` refunds its logs.
        if (!invTakeFee(recipe.cost * count)) {
            mes("You can't afford to tan that. It costs ${recipe.cost} coins a hide.")
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
        val cost = recipe.cost * count
        if (count == 1) {
            mes("The tanner tans your hide into $name for $cost coins.")
        } else {
            mes("The tanner tans $count of your hides into $name for $cost coins.")
        }
    }
}
