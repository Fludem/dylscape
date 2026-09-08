package org.rsmod.content.skills.smithing.scripts

import jakarta.inject.Inject
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.smithingLvl
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLocU
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.content.skills.smithing.configs.SmithingContent
import org.rsmod.content.skills.smithing.configs.SmithingEnums
import org.rsmod.content.skills.smithing.configs.SmithingInterfaces
import org.rsmod.content.skills.smithing.configs.SmithingProducts
import org.rsmod.content.skills.smithing.configs.SmithingSeqs
import org.rsmod.content.skills.smithing.configs.SmithingVarps
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.enums.EnumType
import org.rsmod.game.type.enums.EnumTypeList
import org.rsmod.game.type.interf.IfButtonOp
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.obj.ObjType
import org.rsmod.game.type.obj.ObjTypeList
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Smithing bars into items at an anvil, on the game's own interface.
 *
 * The interface needs almost nothing from us. Component 0 of interface 312 carries
 * `onLoad=[clientscript,smithing_init]`, so it initialises itself the moment it opens, and
 * `proc,smithing_setup` switches on the `smithbars` varp to decide which tier to draw. That switch
 * table's keys are bar **object ids**, which is how we know what to store there -- it was read out
 * of the cached bytecode, not inferred from how other servers do it.
 *
 * So the whole server side is: put the bar's id in the varp, open the interface, enable ops on the
 * product buttons, and wait for a click. Levels, bar costs and output quantities are then read back
 * from the same three enums the client used to draw the menu, so the two cannot disagree.
 *
 * TODO:
 * - The `other_1`/`other_2`/`other_3` slots (crossbow grapple tips, lanterns, spits, blurite,
 *   Shayzien gear) are quest and minigame content and are left unbound.
 * - Smithing sets ("make all" of a matched armour set) and the Giants' Foundry are not modelled.
 */
class AnvilSmithing
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val enumTypes: EnumTypeList,
    private val xpMods: XpModifiers,
) : PluginScript() {
    override fun ScriptContext.startup() {
        // Anvils carry `Smith` on op1; verified against the cache in SmithingConfigTest.
        onOpLoc1(SmithingContent.smithing_anvil) { openBestBar() }
        onOpLocU(SmithingContent.smithing_anvil, SmithingContent.smithing_bar) {
            open(it.objType.id)
        }
        for (component in SmithingProducts.components) {
            onIfModalButton(component) { smith(component, it.op) }
        }
    }

    /** Clicking the anvil with nothing selected opens the best tier the player is carrying. */
    private fun ProtectedAccess.openBestBar() {
        val tier = SmithingProducts.tiers.lastOrNull { invTotal(inv, it.bar) > 0 }
        if (tier == null) {
            mes("You need some bars to work with.")
            return
        }
        open(tier.bar.id)
    }

    private fun ProtectedAccess.open(barId: Int) {
        if (invTotal(inv, objs.hammer) <= 0) {
            mes("You need a hammer to work the metal with.")
            return
        }

        vars[SmithingVarps.smithbars] = barId
        ifOpenMainModal(SmithingInterfaces.smithing)
        for (component in SmithingProducts.components) {
            ifSetEvents(
                component,
                0..0,
                IfEvent.Op1,
                IfEvent.Op2,
                IfEvent.Op3,
                IfEvent.Op4,
                IfEvent.Op5,
            )
        }
    }

    private suspend fun ProtectedAccess.smith(component: ComponentType, op: IfButtonOp) {
        val barId = vars[SmithingVarps.smithbars]
        val product = SmithingProducts.find(barId, component)
        if (product == null) {
            // The client drew something in this slot that this module does not implement.
            ifClose()
            return
        }

        val levelReq = enumInt(SmithingEnums.product_to_requirement, product)
        val barsRequired = enumInt(SmithingEnums.product_to_bars_required, product)
        val madePerBatch = enumInt(SmithingEnums.product_to_quantity, product)
        if (levelReq == null || barsRequired == null || madePerBatch == null) {
            ifClose()
            return
        }

        val productType = objTypes[product]
        if (player.smithingLvl < levelReq) {
            ifClose()
            mes("You need a Smithing level of $levelReq to make a ${productType.name.lowercase()}.")
            return
        }

        val bar = objTypes.getValue(barId)
        val requested = requestedCount(op)
        ifClose()
        if (requested <= 0) {
            return
        }

        val tier = SmithingProducts.tiers.first { it.bar.id == barId }
        var made = 0
        while (made < requested && invTotal(inv, bar) >= barsRequired) {
            anim(SmithingSeqs.smith)
            delay(SMITH_TICKS)

            // Re-checked after the delay; the bars may be gone by now.
            if (invTotal(inv, bar) < barsRequired) {
                break
            }
            invDel(inv, bar, barsRequired)
            invAdd(inv, product, madePerBatch)
            statAdvance(
                stats.smithing,
                barsRequired * tier.xpPerBar * xpMods.get(player, stats.smithing),
            )
            made++
        }
        resetAnim()

        if (made == 0) {
            mes("You don't have enough ${bar.name.lowercase()}s.")
        }
    }

    /**
     * The makex quantity buttons sit at components 3-8 in the order `1, 5, 10, X, All`, and the
     * client relabels each product button's ops to match, so the op index carries the quantity.
     */
    private suspend fun ProtectedAccess.requestedCount(op: IfButtonOp): Int =
        when (op) {
            IfButtonOp.Op1 -> 1
            IfButtonOp.Op2 -> 5
            IfButtonOp.Op3 -> 10
            IfButtonOp.Op4 -> countDialog()
            IfButtonOp.Op5 -> Int.MAX_VALUE
            else -> 0
        }

    /**
     * Reads a value out of one of the vanilla smithing enums by raw obj id.
     *
     * Deliberately going through `primitiveMap` rather than the typed map: the typed keys are
     * whatever the decoder produced, while [product] here is a resolved reference, and the two are
     * different classes that never compare equal.
     */
    private fun enumInt(enum: EnumType<ObjType, Int>, product: ObjType): Int? =
        enumTypes[enum].primitiveMap[product.id] as? Int

    private companion object {
        /** Ticks per item smithed. Tuned, not measured against live. */
        const val SMITH_TICKS = 3
    }
}
