package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
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
 * **The buttons need nothing from the server.** Every product row and every `make_*` button already
 * carries `op1` *and* the op1 event bit in the cache, and no clientscript in the whole cache so
 * much as mentions interface 446 -- so a press is a plain `IfButton` on the component itself, with
 * no subcomponent. Nothing is enabled here, and nothing should be: `if_setevents` over a
 * subcomponent range would aim the client's event window at children these components do not have.
 * That is what the first version did, on the guess that 446 re-targets its clicks the way 270 does.
 * It does not; only 270 has the clientscript that does that.
 *
 * The quantity is the one thing the cache does not hand over. `make_1` through `make_all` also run
 * `[clientscript,skillmain_setquantity]` locally and stash the number in a varc the server cannot
 * read. Since they reach the server as well, [pending] mirrors it: whichever one was pressed last
 * is the count the next product button uses.
 *
 * `make_x` is deliberately left unbound: it opens the client's own "Enter amount" box, and binding
 * it would stack a second, server-side one on top. The cost is that a player who types an exact
 * count gets the last *button* they pressed instead -- the varc it writes is not readable here.
 */
class Jewellery
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val perks: Perks,
) : PluginScript() {
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
            onIfModalButton(recipe.component) { cast(recipe) }
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
    }

    private suspend fun ProtectedAccess.cast(recipe: JewelleryRecipe) {
        val count = pending[player] ?: 1
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

        val instant = perks.has(player, Perk.InstantProduction)

        var made = 0
        while (made < count && canAfford(recipe)) {
            anim(CraftingSeqs.furnace)
            if (made == 0 || !instant) {
                delay(CAST_TICKS)
            }

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
        /** Ticks per cast. Tuned, not measured against live. */
        const val CAST_TICKS = 4
    }
}
