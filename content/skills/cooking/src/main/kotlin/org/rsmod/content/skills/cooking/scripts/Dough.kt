package org.rsmod.content.skills.cooking.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeldU
import org.rsmod.content.skills.cooking.configs.CookingObjs
import org.rsmod.game.type.obj.ObjType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Mixing a pot of flour with water. Not a Cooking action in itself -- no level, no xp -- but the
 * only way to get the dough that bread is baked from, so it lives with the skill that needs it.
 *
 * Any water container works and each hands back its empty. Ordinary flour offers the three doughs
 * the live game does; Tutorial Island's own flour goes straight to bread dough, since bread is the
 * only thing the Master Chef teaches.
 */
class Dough @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (water in Water.all) {
            onOpHeldU(CookingObjs.pot_flour, water.full) { mix(it, water, choose = true) }
            onOpHeldU(CookingObjs.newbie_pot_flour, water.full) { mix(it, water, choose = false) }
        }
    }

    private suspend fun ProtectedAccess.mix(
        event: HeldUEvents.Type,
        water: Water,
        choose: Boolean,
    ) {
        // Two containers come back empty and the dough is new, so one free slot is needed.
        if (inv.isFull()) {
            mes("You don't have enough inventory space to make dough.")
            return
        }

        val kind =
            if (choose) {
                choice3(
                    choice1 = "Bread dough",
                    result1 = DoughKind.Bread,
                    choice2 = "Pastry dough",
                    result2 = DoughKind.Pastry,
                    choice3 = "Pizza base",
                    result3 = DoughKind.Pizza,
                    title = "What do you wish to make?",
                )
            } else {
                DoughKind.Bread
            }

        // Re-checked after the choice: the menu could have been left open a long time.
        if (invTotal(inv, event.first) == 0 || invTotal(inv, water.full) == 0 || inv.isFull()) {
            return
        }
        invReplace(inv, replace = event.first, count = 1, replacement = CookingObjs.pot_empty)
        invReplace(inv, replace = water.full, count = 1, replacement = water.empty)
        invAdd(inv, kind.dough)
        mes("You mix the flour and water to make ${kind.phrase}.")
        publish(MixedDough(player, kind.dough))
    }

    private enum class DoughKind(val dough: ObjType, val phrase: String) {
        Bread(CookingObjs.bread_dough, "some bread dough"),
        Pastry(CookingObjs.pastry_dough, "some pastry dough"),
        Pizza(CookingObjs.pizza_base, "a pizza base"),
    }

    /** A water container and what it becomes once poured. */
    private class Water(val full: ObjType, val empty: ObjType) {
        companion object {
            val all =
                listOf(
                    Water(CookingObjs.bucket_water, CookingObjs.bucket_empty),
                    Water(CookingObjs.jug_water, CookingObjs.jug_empty),
                    Water(CookingObjs.bowl_water, CookingObjs.bowl_empty),
                )
        }
    }
}
