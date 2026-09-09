package org.rsmod.content.skills.herblore.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.herblore.configs.GrindRecipe
import org.rsmod.content.skills.herblore.configs.HerbloreObjs
import org.rsmod.content.skills.herblore.configs.HerbloreRecipes
import org.rsmod.content.skills.herblore.configs.HerbloreSeqs
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The pestle and mortar: a horn or a scale in, a dust out.
 *
 * No experience and no level requirement, which is live's behaviour and not an oversight -- the
 * reward for grinding is that the dust is worth more than the horn.
 *
 * Bound as six explicit pairs rather than through `onOpHeldU`'s single-obj "used on anything"
 * overload. That overload exists and would be one line, but a catch-all binding would swallow
 * "pestle on anything" including content nobody has written yet, and the exact pair is tried first
 * anyway. Enumerating is the same call the fletching build made about knives.
 */
class HerbGrinding @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in HerbloreRecipes.grinding) {
            onOpHeldU(HerbloreObjs.pestle_and_mortar, recipe.input) { grind(recipe) }
        }
    }

    private suspend fun ProtectedAccess.grind(recipe: GrindRecipe) {
        if (invTotal(inv, recipe.input) < 1) {
            return
        }
        anim(HerbloreSeqs.grind)
        delay(GRIND_TICKS)

        // Re-checked after the delay: the input could have been banked mid-animation.
        if (invTotal(inv, recipe.input) < 1) {
            resetAnim()
            return
        }
        invReplace(inv, replace = recipe.input, count = 1, replacement = recipe.output)
        resetAnim()
    }

    private companion object {
        /** One grind per click, matching live. Tuned, not measured. */
        const val GRIND_TICKS = 2
    }
}
