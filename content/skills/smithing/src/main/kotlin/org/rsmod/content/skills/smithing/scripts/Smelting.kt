package org.rsmod.content.skills.smithing.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.content
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.smithing.configs.SmeltRecipe
import org.rsmod.content.skills.smithing.configs.SmeltingRecipes
import org.rsmod.content.skills.smithing.configs.SmithingContent
import org.rsmod.content.skills.smithing.configs.SmithingSeqs
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Smelting ore into bars at a furnace.
 *
 * Clicking the furnace opens the game's own make-menu -- see [SkillMulti], which documents how that
 * interface's arguments were recovered from the cache. Using an ore on the furnace stays supported
 * as a shortcut, and skips straight to that ore's recipe.
 *
 * The menu always lists all eight bars, in table order, rather than only the ones the player has
 * the level for. That matches the live game, which also shows bars you cannot yet make; the level
 * is checked when a bar is picked.
 */
class Smelting
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
    private val perks: Perks,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Furnaces carry `Smelt` on op2, not op1 -- decoded from the cache, and not uniform:
        // Tutorial Island's `newbiefurnace` carries `Use` on op1 instead. Both are bound; a loc
        // simply never fires the op it does not have.
        onOpLoc1(SmithingContent.smithing_furnace) { openMenu() }
        onOpLoc2(SmithingContent.smithing_furnace) { openMenu() }
        onOpLocU(SmithingContent.smithing_furnace, content.ore) { smeltWithOre(it.objType) }
    }

    private suspend fun ProtectedAccess.openMenu() {
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Smelt,
                title = "What would you like to smelt?",
                objs = SmeltingRecipes.all.map { it.bar },
            ) ?: return
        val recipe = SmeltingRecipes.all.getOrNull(pick.slot) ?: return
        smelt(recipe, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.smeltWithOre(ore: UnpackedObjType) {
        val candidates = SmeltingRecipes.byOre[ore.id]
        if (candidates.isNullOrEmpty()) {
            mes("You can't smelt ${ore.name.lowercase()}.")
            return
        }

        val recipe =
            if (candidates.size == 1) {
                candidates.single()
            } else {
                // Iron ore is the only ore feeding two recipes.
                val first = candidates[0]
                val second = candidates[1]
                choice2(
                    choice1 = objTypes[first.bar].name,
                    result1 = first,
                    choice2 = objTypes[second.bar].name,
                    result2 = second,
                    title = "What would you like to smelt?",
                )
            }
        smelt(recipe)
    }

    /**
     * Smelts up to [count] bars, stopping early when the ore runs out.
     *
     * A failed iron smelt still counts as one of the [count]: the ore is spent either way, so
     * counting attempts rather than successes is what stops "make 5" from quietly eating fifteen
     * ores on an unlucky run.
     */
    private suspend fun ProtectedAccess.smelt(recipe: SmeltRecipe, count: Int = Int.MAX_VALUE) {
        if (player.smithingLvl < recipe.levelReq) {
            mes("You need a Smithing level of ${recipe.levelReq} to smelt this.")
            return
        }

        val bar = objTypes[recipe.bar]
        val instant = perks.has(player, Perk.InstantProduction)
        var made = 0
        while (made < count && canAfford(recipe)) {
            anim(SmithingSeqs.smelt)
            if (made == 0 || !instant) {
                delay(SMELT_TICKS)
            }

            // Re-checked after the delay: the ore could have been banked or dropped mid-animation.
            if (!canAfford(recipe)) {
                break
            }
            for (ingredient in recipe.ingredients) {
                invDel(inv, ingredient.obj, ingredient.count)
            }
            made++

            val succeeded =
                recipe.successPercent >= 100 ||
                    random.of(maxExclusive = 100) < recipe.successPercent
            if (!succeeded) {
                // Iron only. The ore is consumed either way, which is the real behaviour.
                mes("The ore is too impure and you fail to refine it.")
                continue
            }

            invAdd(inv, recipe.bar)
            statAdvance(stats.smithing, recipe.xp * xpMods.get(player, stats.smithing))
            mes("You retrieve a bar of ${bar.name.lowercase().removeSuffix(" bar")}.")
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have enough ore to make a ${bar.name.lowercase()}.")
        }
    }

    private fun ProtectedAccess.canAfford(recipe: SmeltRecipe): Boolean =
        recipe.ingredients.all { invTotal(inv, it.obj) >= it.count }

    private companion object {
        /** Ticks per smelt. Tuned, not measured against live. */
        const val SMELT_TICKS = 4
    }
}
