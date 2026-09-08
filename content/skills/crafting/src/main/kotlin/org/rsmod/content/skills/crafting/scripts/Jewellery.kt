package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.crafting.configs.CraftingContent
import org.rsmod.content.skills.crafting.configs.CraftingGoldComponents
import org.rsmod.content.skills.crafting.configs.CraftingInterfaces
import org.rsmod.content.skills.crafting.configs.CraftingObjs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.CraftingSeqs
import org.rsmod.content.skills.crafting.configs.JewelleryRecipe
import org.rsmod.content.skills.crafting.configs.SilverCraftingComponents
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.interf.InterfaceType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Casting jewellery at a furnace, on the game's own panels: interface 446 for gold and 6 for
 * silver.
 *
 * **What the cache gave up, and what it did not.** Both panels draw themselves. Every product row
 * carries an `onLoad` hook naming its gem, its product and its mould, and the four section headers
 * swap between "you need a ring mould" and the row list on their own, so the server never sends a
 * single line of layout -- it opens the interface and enables buttons. `CraftingInterfaceDump`
 * prints all of that against the installed cache.
 *
 * The quantity is the one thing the cache does not hand over. `make_1` through `make_all` run
 * `[clientscript,skillmain_setquantity]` locally and stash the result in a varc the server cannot
 * read, and `[proc,skillmain_setup]` sets no server-visible events at all. So the quantity is
 * recovered two ways at once, and whichever the client actually uses wins:
 * 1. The product buttons are enabled over a subcomponent *range*, so if the client re-targets the
 *    click at the chosen quantity -- which is exactly what interface 270 does -- it arrives in the
 *    `comsub`.
 * 2. The quantity buttons are enabled too, and pressing one records the same number server-side.
 *    The client updates its own label from its own hook; both sides see every click.
 *
 * `make_x` is deliberately left alone: it opens the client's own "Enter amount" box, and binding it
 * would stack a second, server-side one on top. A player who wants an exact count can use it and
 * the click still arrives via (1); if it does not, they get the last button they pressed.
 */
class Jewellery
@Inject
constructor(private val objTypes: ObjTypeList, private val xpMods: XpModifiers) : PluginScript() {
    /** The quantity each player last pressed, mirroring the client's own varc. */
    private val pending = HashMap<Player, Int>()

    override fun ScriptContext.startup() {
        onOpLocU(CraftingContent.smithing_furnace, CraftingObjs.gold_bar) {
            open(CraftingInterfaces.crafting_gold)
        }
        onOpLocU(CraftingContent.smithing_furnace, CraftingObjs.silver_bar) {
            open(CraftingInterfaces.silver_crafting)
        }

        for (recipe in CraftingRecipes.jewellery) {
            onIfModalButton(recipe.component) { cast(recipe, it.comsub) }
        }
        for ((button, quantity) in CraftingGoldComponents.quantityButtons) {
            onIfModalButton(button) { pending[player] = quantity }
        }
        for ((button, quantity) in SilverCraftingComponents.quantityButtons) {
            onIfModalButton(button) { pending[player] = quantity }
        }

        onIfClose(CraftingInterfaces.crafting_gold) { pending.remove(player) }
        onIfClose(CraftingInterfaces.silver_crafting) { pending.remove(player) }
        // Closing the panel is the usual way out, but a logout is not: without this the map would
        // hold a `Player` for every character that ever opened a furnace.
        onEvent<SessionStateEvent.Logout> { pending.remove(player) }
    }

    private fun ProtectedAccess.open(interf: InterfaceType) {
        pending[player] = 1
        ifOpenMainModal(interf)

        val components =
            if (interf.id == CraftingInterfaces.crafting_gold.id) {
                CraftingRecipes.goldJewellery.map { it.component } +
                    CraftingGoldComponents.quantityButtons.map { it.first }
            } else {
                CraftingRecipes.silverJewellery.map { it.component } +
                    SilverCraftingComponents.quantityButtons.map { it.first }
            }
        for (component in components) {
            ifSetEvents(component, 0..MAX_QUANTITY, IfEvent.Op1)
        }
    }

    private suspend fun ProtectedAccess.cast(recipe: JewelleryRecipe, comsub: Int) {
        // The subcomponent is the quantity, when the client sends one. It arrives outside the range
        // if the client did not re-target the click, in which case the last quantity button pressed
        // is the best answer available.
        val count = if (comsub in 1..MAX_QUANTITY) comsub else pending[player] ?: 1
        ifClose()

        if (player.craftingLvl < recipe.levelReq) {
            val name = objTypes[recipe.product].name.lowercase()
            mes("You need a Crafting level of ${recipe.levelReq} to make a $name.")
            return
        }
        if (invTotal(inv, recipe.mould) == 0) {
            mes("You need a ${objTypes[recipe.mould].name.lowercase()} to make that.")
            return
        }

        var made = 0
        while (made < count && canAfford(recipe)) {
            anim(CraftingSeqs.furnace)
            delay(CAST_TICKS)

            // Re-checked after the delay: the bar or the gem could have been banked.
            if (!canAfford(recipe)) {
                break
            }
            invDel(inv, recipe.bar, 1)
            recipe.gem?.let { invDel(inv, it, 1) }
            invAdd(inv, recipe.product)
            statAdvance(stats.crafting, recipe.xp * xpMods.get(player, stats.crafting))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the materials to make that.")
        }
    }

    private fun ProtectedAccess.canAfford(recipe: JewelleryRecipe): Boolean {
        if (invTotal(inv, recipe.bar) == 0) {
            return false
        }
        val gem = recipe.gem ?: return true
        return invTotal(inv, gem) > 0
    }

    private companion object {
        /**
         * The client clamps every makex quantity into `1..28`, the same ceiling interface 270 has.
         */
        const val MAX_QUANTITY = 28

        /** Ticks per cast. Tuned, not measured against live. */
        const val CAST_TICKS = 4
    }
}
