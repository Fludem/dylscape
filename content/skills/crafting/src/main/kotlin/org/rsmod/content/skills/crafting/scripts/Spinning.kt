package org.rsmod.content.skills.crafting.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.perks.Perk
import org.rsmod.api.perks.Perks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.craftingLvl
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.crafting.configs.CraftingLocs
import org.rsmod.content.skills.crafting.configs.CraftingObjs
import org.rsmod.content.skills.crafting.configs.CraftingRecipes
import org.rsmod.content.skills.crafting.configs.CraftingSeqs
import org.rsmod.content.skills.crafting.configs.SpinRecipe
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The spinning wheel: wool into balls of wool, flax into bow strings.
 *
 * Both op1 and op2 are bound on every wheel. Spinning wheels are not uniform across the map -- some
 * carry `Spin` on op1 and others on op2 -- and a loc simply never fires the op it does not have,
 * which is the same reason `Smelting` binds both ops on furnaces.
 *
 * Stringing an unstrung amulet with a ball of wool lives here too. It pays no experience and has no
 * requirement, but it is the step that finishes everything the jewellery panels start, so it
 * belongs with the wool.
 */
class Spinning
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
    private val perks: Perks,
) : PluginScript() {
    override fun ScriptContext.startup() {
        for (wheel in WHEELS) {
            onOpLoc1(wheel) { openMenu() }
            onOpLoc2(wheel) { openMenu() }
        }
        for ((unstrung, strung) in CraftingRecipes.amuletStringing) {
            onOpHeldU(CraftingObjs.ball_of_wool, unstrung) { stringAmulet(unstrung, strung) }
        }
    }

    private suspend fun ProtectedAccess.openMenu() {
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Spin,
                title = "What would you like to spin?",
                objs = CraftingRecipes.spinning.map { it.product },
            ) ?: return
        val recipe = CraftingRecipes.spinning.getOrNull(pick.slot) ?: return
        spin(recipe, count = pick.quantity)
    }

    private suspend fun ProtectedAccess.spin(recipe: SpinRecipe, count: Int) {
        if (player.craftingLvl < recipe.levelReq) {
            val name = objTypes[recipe.product].name.lowercase()
            mes("You need a Crafting level of ${recipe.levelReq} to spin $name.")
            return
        }

        val instant = perks.has(player, Perk.InstantProduction)

        var made = 0
        while (made < count && invTotal(inv, recipe.material) > 0) {
            anim(CraftingSeqs.spin)
            if (made == 0 || !instant) {
                delay(SPIN_TICKS)
            }

            // Re-checked after the delay: the material could have been banked mid-animation.
            if (invTotal(inv, recipe.material) == 0) {
                break
            }
            invReplace(inv, replace = recipe.material, count = 1, replacement = recipe.product)
            statAdvance(stats.crafting, recipe.xp * xpMods.get(player, stats.crafting))
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have anything to spin.")
        }
    }

    private fun ProtectedAccess.stringAmulet(unstrung: ObjType, strung: ObjType) {
        if (invTotal(inv, unstrung) == 0 || invTotal(inv, CraftingObjs.ball_of_wool) == 0) {
            return
        }
        invDel(inv, CraftingObjs.ball_of_wool, 1)
        invReplace(inv, replace = unstrung, count = 1, replacement = strung)
        mes("You put some string on your ${objTypes[unstrung].name.lowercase()}.")
    }

    private companion object {
        val WHEELS =
            listOf(
                CraftingLocs.spinningwheel,
                CraftingLocs.spinningwheel_2,
                CraftingLocs.spinningwheel_quetzacali,
            )

        /** Ticks per spin. Tuned, not measured against live. */
        const val SPIN_TICKS = 3
    }
}
