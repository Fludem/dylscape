package org.rsmod.content.skills.cooking.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.interfaces.skillmulti.SkillMulti
import org.rsmod.content.interfaces.skillmulti.SkillMultiType
import org.rsmod.content.skills.cooking.configs.CookingContent
import org.rsmod.content.skills.cooking.configs.CookingSeqs
import org.rsmod.content.skills.cooking.configs.burntProduct
import org.rsmod.content.skills.cooking.configs.cookedProduct
import org.rsmod.content.skills.cooking.configs.cookingLevel
import org.rsmod.content.skills.cooking.configs.cookingName
import org.rsmod.content.skills.cooking.configs.cookingXp
import org.rsmod.content.skills.cooking.configs.rangeOnly
import org.rsmod.content.skills.cooking.configs.stopBurnFire
import org.rsmod.content.skills.cooking.configs.stopBurnRange
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.type.loc.LocTypeList
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.game.type.seq.SeqType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Cooking on ranges and fires.
 *
 * Two ways in, as in the live game. Using a raw food on a range or fire cooks that food: straight
 * away if you carry only one, otherwise through the make-menu asking how many. Clicking a range's
 * `Cook` option instead lists everything cookable in your inventory and lets you pick.
 *
 * Either way ends in [cook], which holds the range or fire in a closure across the menu click.
 * Cooking is the reason the make-menu takes a callback rather than a fixed table: unlike smelting,
 * what is on the menu depends on the inventory, and what happens afterwards depends on the loc.
 *
 * Fires are only reachable by using food on them -- the one firemaking lights has no ops at all --
 * and burn out on their own, so the loop re-checks that the loc is still standing before each item.
 *
 * TODO: cooking gauntlets, the Hosidius and Lumbridge range bonuses, and the "cook all" spam-click
 *   cadence, which is faithful enough at four ticks an item to leave alone for now.
 */
class Cooking
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val locTypes: LocTypeList,
    private val locRepo: LocRepository,
    private val xpMods: XpModifiers,
    private val skillMulti: SkillMulti,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(CookingContent.cooking_range) { openMenu(it.loc, CookingHeat.Range) }
        onOpLoc1(CookingContent.cooking_fire) { openMenu(it.loc, CookingHeat.Fire) }
        onOpLocU(CookingContent.cooking_range, CookingContent.cooking_raw) {
            useFood(it.loc, it.objType, CookingHeat.Range)
        }
        onOpLocU(CookingContent.cooking_fire, CookingContent.cooking_raw) {
            useFood(it.loc, it.objType, CookingHeat.Fire)
        }
    }

    private suspend fun ProtectedAccess.openMenu(loc: BoundLocInfo, heat: CookingHeat) {
        val cookable = cookableFoods(heat)
        if (cookable.isEmpty()) {
            mes("You have nothing to cook.")
            return
        }
        faceLoc(loc)
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Cook,
                title = "What would you like to cook?",
                objs = cookable.take(SkillMulti.MAX_SLOTS),
            ) ?: return
        cook(loc, heat, objTypes[pick.obj], pick.quantity)
    }

    private suspend fun ProtectedAccess.useFood(
        loc: BoundLocInfo,
        food: UnpackedObjType,
        heat: CookingHeat,
    ) {
        if (heat == CookingHeat.Fire && food.rangeOnly) {
            mes("This needs to be cooked on a range.")
            return
        }
        val carried = invTotal(inv, food)
        if (carried <= 1) {
            cook(loc, heat, food, count = 1)
            return
        }
        faceLoc(loc)
        val pick =
            skillMulti.open(
                access = this,
                type = SkillMultiType.Cook,
                title = "How many would you like to cook?",
                objs = listOf(food),
                maxQuantity = carried,
            ) ?: return
        cook(loc, heat, food, pick.quantity)
    }

    /**
     * Cooks up to [count] of [food] at [loc], stopping early if the food runs out or the fire dies.
     */
    private suspend fun ProtectedAccess.cook(
        loc: BoundLocInfo,
        heat: CookingHeat,
        food: UnpackedObjType,
        count: Int,
    ) {
        if (player.cookingLvl < food.cookingLevel) {
            mes("You need a Cooking level of ${food.cookingLevel} to cook ${food.cookingName}.")
            return
        }

        val locType = locTypes[loc]
        var made = 0
        while (made < count && invTotal(inv, food) > 0) {
            if (locRepo.findExact(loc.coords, locType) == null) {
                if (heat == CookingHeat.Fire) {
                    mes("The fire has burnt out.")
                }
                break
            }

            faceLoc(loc)
            anim(heat.seq)
            delay(COOK_TICKS)

            val burnt =
                CookingRolls.burns(
                    level = player.cookingLvl,
                    levelReq = food.cookingLevel,
                    stopBurn = heat.stopBurn(food),
                    random = random,
                )
            val product = if (burnt) food.burntProduct else food.cookedProduct
            val replaced = invReplace(inv, replace = food, count = 1, replacement = product)
            if (!replaced.success) {
                break
            }
            made++

            if (burnt) {
                mes("You accidentally burn ${food.cookingName}.")
            } else {
                mes("You successfully cook ${food.cookingName}.")
                statAdvance(stats.cooking, food.cookingXp * xpMods.get(player, stats.cooking))
            }
            publish(CookedFood(player, food, product, burnt))
        }
        resetAnim()
    }

    /**
     * Every distinct raw food in the inventory that can go on [heat], in inventory order. Distinct
     * by type: five raw shrimps are one button, not five.
     */
    private fun ProtectedAccess.cookableFoods(heat: CookingHeat): List<ObjType> {
        val seen = HashSet<Int>()
        val foods = ArrayList<ObjType>()
        for (obj in inv.objs) {
            if (obj == null) {
                continue
            }
            val type = objTypes[obj]
            if (!type.isContentType(CookingContent.cooking_raw)) {
                continue
            }
            if (heat == CookingHeat.Fire && type.rangeOnly) {
                continue
            }
            if (seen.add(type.id)) {
                foods += type
            }
        }
        return foods
    }

    private companion object {
        /** Ticks per item, matching the live game's four-tick cooking cadence. */
        const val COOK_TICKS = 4
    }
}

/** What the food is being cooked over. Decides the animation and which stop-burn level applies. */
enum class CookingHeat {
    Range,
    Fire;

    val seq: SeqType
        get() =
            when (this) {
                Range -> CookingSeqs.range
                Fire -> CookingSeqs.fire
            }

    fun stopBurn(food: UnpackedObjType): Int =
        when (this) {
            Range -> food.stopBurnRange
            Fire -> food.stopBurnFire
        }
}
