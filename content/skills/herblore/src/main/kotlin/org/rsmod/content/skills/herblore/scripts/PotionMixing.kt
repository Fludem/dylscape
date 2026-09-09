package org.rsmod.content.skills.herblore.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.herbloreLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.herblore.configs.HerbloreObjs
import org.rsmod.content.skills.herblore.configs.HerbloreRecipes
import org.rsmod.content.skills.herblore.configs.HerbloreSeqs
import org.rsmod.content.skills.herblore.configs.PotionRecipe
import org.rsmod.content.skills.herblore.configs.UnfRecipe
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Both mixing steps, and super combat.
 *
 * Both steps go through the make-menu, which is how the live game asks for a quantity and how every
 * other repeat action in this repo behaves. The verb is `Make`: enum 1809 -- the client's own verb
 * table, printed by `HerbloreDump` -- has no "Mix" entry, so there was no Herblore verb to add to
 * `SkillMultiType`.
 *
 * Interface 270 is a pause-button dialogue rather than an `IfButton` one. `SkillMulti.open` grants
 * `IfEvent.PauseButton` over a subcomponent range and suspends; nothing here needs to know that
 * beyond calling it and handling a null.
 */
class PotionMixing
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (recipe in HerbloreRecipes.unfinished) {
            onOpHeldU(recipe.base, recipe.herb) { openUnfMenu(recipe) }
        }
        for (recipe in HerbloreRecipes.mixing) {
            onOpHeldU(recipe.primary, recipe.secondary) { openPotionMenu(recipe) }
        }
        // Super combat takes four objs at once, so the torstol is used on any one of the three
        // supers and the other two are checked in the inventory. See `HerbloreRecipes`.
        for (input in HerbloreRecipes.superCombatInputs) {
            onOpHeldU(HerbloreObjs.torstol, input) { openSuperCombatMenu() }
        }
    }

    private suspend fun ProtectedAccess.openUnfMenu(recipe: UnfRecipe) {
        val carried = minOf(invTotal(inv, recipe.base), invTotal(inv, recipe.herb))
        if (carried < 1) {
            return
        }
        val pick = pickQuantity(recipe.unf, carried) ?: return
        mixUnf(recipe, count = pick)
    }

    private suspend fun ProtectedAccess.openPotionMenu(recipe: PotionRecipe) {
        val carried = minOf(invTotal(inv, recipe.primary), invTotal(inv, recipe.secondary))
        if (carried < 1) {
            return
        }
        val pick = pickQuantity(recipe.product, carried) ?: return
        mix(recipe, count = pick)
    }

    private suspend fun ProtectedAccess.openSuperCombatMenu() {
        val inputs = HerbloreRecipes.superCombatInputs
        val carried = (inputs.map { invTotal(inv, it) } + invTotal(inv, HerbloreObjs.torstol)).min()
        if (carried < 1) {
            mes("You need a torstol and all three super potions to make that.")
            return
        }
        val product = HerbloreRecipes.superCombatProduct
        val pick = pickQuantity(product, carried) ?: return
        mixSuperCombat(count = pick)
    }

    private suspend fun ProtectedAccess.pickQuantity(product: ObjType, carried: Int): Int? =
        skillMulti
            .open(
                access = this,
                type = SkillMultiType.Make,
                title = "How many do you wish to make?",
                objs = listOf(product),
                maxQuantity = carried,
            )
            ?.quantity

    private suspend fun ProtectedAccess.mixUnf(recipe: UnfRecipe, count: Int) {
        if (player.herbloreLvl < recipe.levelReq) {
            val name = objTypes[recipe.unf].name.lowercase()
            mes("You need a Herblore level of ${recipe.levelReq} to make a $name.")
            return
        }

        var made = 0
        while (made < count && affords(recipe.base, recipe.herb)) {
            anim(HerbloreSeqs.mix)
            delay(MIX_TICKS)

            // Re-checked after the delay: either half could have been banked mid-animation.
            if (!affords(recipe.base, recipe.herb)) {
                break
            }
            // One atomic delete rather than two calls. A two-call sequence can spend the vial and
            // then fail on the herb, and there is no rollback at this level.
            invDel(inv, recipe.base, 1, recipe.herb, 1)
            invAdd(inv, recipe.unf)
            // No `statAdvance`, deliberately: an unfinished potion pays nothing in the live game
            // and the whole reward lands on the mixing step below.
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the ingredients to make that.")
        }
    }

    private suspend fun ProtectedAccess.mix(recipe: PotionRecipe, count: Int) {
        if (player.herbloreLvl < recipe.levelReq) {
            val name = objTypes[recipe.product].name.lowercase()
            mes("You need a Herblore level of ${recipe.levelReq} to make a $name.")
            return
        }

        var made = 0
        while (made < count && affords(recipe.primary, recipe.secondary)) {
            anim(HerbloreSeqs.mix)
            delay(MIX_TICKS)

            if (!affords(recipe.primary, recipe.secondary)) {
                break
            }
            invDel(inv, recipe.primary, 1, recipe.secondary, 1)
            invAdd(inv, recipe.product)
            statAdvance(stats.herblore, recipe.xp * xpMods.get(player, stats.herblore))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have the ingredients to make that potion.")
        }
    }

    private suspend fun ProtectedAccess.mixSuperCombat(count: Int) {
        if (player.herbloreLvl < HerbloreRecipes.SUPER_COMBAT_LEVEL) {
            mes(
                "You need a Herblore level of ${HerbloreRecipes.SUPER_COMBAT_LEVEL} " +
                    "to make a super combat potion."
            )
            return
        }

        val inputs = HerbloreRecipes.superCombatInputs
        var made = 0
        while (made < count && affordsSuperCombat()) {
            anim(HerbloreSeqs.mix)
            delay(MIX_TICKS)

            if (!affordsSuperCombat()) {
                break
            }
            // Four objs, so this cannot use the two- or three-type atomic overload. The `affords`
            // check immediately above is what stands in for atomicity, and it is re-run on the
            // same tick as the deletes.
            invDel(inv, HerbloreObjs.torstol, 1)
            for (input in inputs) {
                invDel(inv, input, 1)
            }
            invAdd(inv, HerbloreRecipes.superCombatProduct)
            statAdvance(
                stats.herblore,
                HerbloreRecipes.SUPER_COMBAT_XP * xpMods.get(player, stats.herblore),
            )
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You need a torstol and all three super potions to make that.")
        }
    }

    private fun ProtectedAccess.affords(first: ObjType, second: ObjType): Boolean =
        invTotal(inv, first) >= 1 && invTotal(inv, second) >= 1

    private fun ProtectedAccess.affordsSuperCombat(): Boolean =
        invTotal(inv, HerbloreObjs.torstol) >= 1 &&
            HerbloreRecipes.superCombatInputs.all { invTotal(inv, it) >= 1 }

    private companion object {
        /** Ticks per potion. Tuned, not measured against live. */
        const val MIX_TICKS = 3
    }
}
