package org.rsmod.content.skills.herblore.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.herblore.configs.CleanRecipe
import org.rsmod.content.skills.herblore.configs.HerbloreRecipes
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Cleaning the dirt off a grimy herb.
 *
 * Bound per obj rather than through a content group: fifteen bindings costs nothing, and a group
 * edit is additive once packed, so a skill that does not need one should not create one. Every
 * grimy herb carries `Clean` on op1 in the cache -- `HerbloreDump` prints the op arrays, and the
 * config test asserts it -- which is what makes an op1 binding deliverable at all.
 *
 * Nothing here suspends. Cleaning is instant in the live game: no animation, no delay, one herb per
 * click, and a player can empty a full inventory as fast as they can click. That means none of the
 * "re-validate after the delay" discipline the mixing loop needs applies here.
 */
class HerbCleaning @Inject constructor(private val xpMods: XpModifiers) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in HerbloreRecipes.cleaning) {
            onOpHeld1(recipe.grimy) { clean(recipe, it.slot) }
        }
    }

    private fun ProtectedAccess.clean(recipe: CleanRecipe, slot: Int) {
        if (player.herbloreLvl < recipe.levelReq) {
            mes("You need a Herblore level of ${recipe.levelReq} to clean this herb.")
            return
        }

        // In-slot, not `invReplace`. Herbs do not stack, so a first-free-slot swap would shuffle
        // the inventory under a player cleaning a full backpack -- the same trap `Consume`
        // documents for a half-eaten cake.
        val replaced = invReplaceSlot(inv, slot, count = 1, replacement = recipe.clean)
        if (!replaced.success) {
            return
        }
        statAdvance(stats.herblore, recipe.xp * xpMods.get(player, stats.herblore))
    }
}
