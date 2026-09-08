package org.rsmod.content.interfaces.skillmulti

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.ClientScripts.topLevelChatboxResetBackground
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiComponents
import org.rsmod.content.interfaces.skillmulti.configs.SkillMultiInterfaces
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.game.type.interf.IfSubType
import org.rsmod.game.type.obj.ObjType

/**
 * The vanilla make-menu, interface 270, driven from the server.
 *
 * The menu is a *pause button* dialogue, exactly like `choice2` or the destroy-item confirmation:
 * the caller opens it and suspends, and the press resumes the caller where it left off. It is not
 * an `IfButton` interface, which is what the first version of this class assumed -- and why every
 * press was silently dropped.
 *
 * **How the press gets back here.** The item buttons carry no static events in the cache, and their
 * left-click op is set and handled entirely by clientscripts: `proc,skillmulti_itembutton_init`
 * calls `cc_setop` and `cc_setonop`, so pressing one runs
 * `[clientscript,skillmulti_itembutton_op]` locally and sends nothing. That script ends in
 * `proc,skillmulti_itembutton_triggered`, which re-targets the press with
 * `cc_find(component, varc,skillmulti_quantity)` and then triggers the subcomponent it found. So
 * the button we receive names the item, and its subcomponent is how many to make.
 *
 * `if_setevents` over a subcomponent range is what makes that `cc_find` resolve at all, and the
 * event the client needs on the subcomponent is [IfEvent.PauseButton], not an op: nothing named the
 * subcomponent's op, so there is no op to fire, and a `PauseButton` grant is the only thing that
 * turns the trigger into a packet. `confirmdestroy` is the same shape down to the opcode -- see
 * `[clientscript,confirmdestroy_triggerbutton]` against
 * `Player.ifConfirmDestroy`, which grants `PauseButton` over `0..1` on
 * `confirmdestroy:universe` and then suspends.
 *
 * @see [SkillMultiComponents] for how the interface's arguments were recovered from the cache.
 */
@Singleton
class SkillMulti @Inject constructor() {
    /**
     * Draws [objs] as buttons and suspends until one is pressed, returning what was picked.
     *
     * Slot order is preserved, so the nth entry of [objs] comes back as [SkillMultiPick.slot] `n`.
     *
     * [maxQuantity] is the most the quantity buttons will offer and [quantity] is what the menu
     * starts on; both are clamped by the client into `1..28`. The client keeps its selection in a
     * varc the server cannot read, so opening is the only chance to set it.
     *
     * Returns `null` if the press cannot be resolved to one of [objs] -- a slot the menu was not
     * opened with. Walking away or closing the menu never returns: the coroutine is abandoned, the
     * way an unanswered dialogue is.
     *
     * @throws org.rsmod.api.player.protect.ProtectedAccessLostException if the player could not
     *   retain protected access across the suspension.
     */
    suspend fun open(
        access: ProtectedAccess,
        type: SkillMultiType,
        title: String,
        objs: List<ObjType>,
        maxQuantity: Int = MAX_QUANTITY,
        quantity: Int = maxQuantity,
    ): SkillMultiPick? {
        require(objs.isNotEmpty()) { "`skillMulti` needs at least one obj to show." }
        require(objs.size <= MAX_SLOTS) { "`skillMulti` shows at most $MAX_SLOTS objs: $objs" }

        val max = maxQuantity.coerceIn(1, MAX_QUANTITY)
        val start = quantity.coerceIn(1, max)
        val slots = List(MAX_SLOTS) { objs.getOrNull(it)?.id ?: EMPTY_SLOT }
        val args = listOf(type.id, max) + slots + listOf(start, title)

        with(access) {
            // Mirrors what `Player.ifOpenChat` does for the dialogue interfaces: the menu sizes
            // itself in its own setup script, so the chatbox must not clamp it.
            vars[varbits.chatmodal_unclamp] = constants.modal_infinitewidthandheight
            topLevelChatboxResetBackground(player)
            ifOpenSub(
                SkillMultiInterfaces.skillmulti,
                components.chatbox_chatmodal,
                IfSubType.Modal,
            )
            player.runClientScript(SETUP_SCRIPT, args)

            // Nothing reaches the server without this. The range has to cover every quantity,
            // because the quantity *is* the subcomponent the client re-targets the press at; `0`
            // is what the skillmulti types that draw no quantity buttons re-target at instead.
            for (slot in SkillMultiComponents.slots) {
                ifSetEvents(slot, 0..MAX_QUANTITY, IfEvent.PauseButton)
            }

            val input = pauseButton()
            val slot = SkillMultiComponents.slots.indexOfFirst(input::isComponentType)
            val obj = objs.getOrNull(slot) ?: return null
            // Only the quantity-less skillmulti types send `0`; one is the safe reading of it.
            val picked = if (input.subcomponent in 1..MAX_QUANTITY) input.subcomponent else 1
            return SkillMultiPick(obj, slot, picked)
        }
    }

    companion object {
        /**
         * The client refuses to draw more than ten items, and `proc,skillmulti_quantitybuttons_set`
         * clamps every quantity into `1..28`, so both limits are the client's rather than ours.
         */
        const val MAX_SLOTS: Int = 10

        const val MAX_QUANTITY: Int = 28

        private const val SETUP_SCRIPT: Int = 2046
        private const val EMPTY_SLOT: Int = -1
    }
}

/**
 * What the player chose: the obj in [slot] of the list the menu was opened with, [quantity] times.
 */
data class SkillMultiPick(val obj: ObjType, val slot: Int, val quantity: Int)

/**
 * Which verb the menu's buttons offer, keyed into the client's own enum 1809.
 *
 * The number is not decorative: it also decides whether the quantity buttons are drawn at all
 * (enum 5178) and which of them appear (enum 1810). Only the values this server actually uses are
 * listed; the full set runs 2..34, and `SkillMultiDump` prints all three enums against the
 * installed cache.
 *
 * There is deliberately no `Craft`: enum 1809 has no such verb. Leather crafting and tanning use
 * the generic [Make], which is what the live game shows.
 *
 * `Cook` appears three times in enum 1809 (6, 7 and 8). 8 is in enum 1810's exclusion set and so
 * loses its "X" quantity button; 6 and 7 are indistinguishable, and 6 is the one used here.
 *
 * The types enum 5178 suppresses the quantity row for -- 26 and 29..34, which includes `Choose` --
 * re-target their press at subcomponent `0` rather than at a quantity. [SkillMulti] grants that
 * subcomponent too, so they should work, but none is used here and none has been tried.
 */
enum class SkillMultiType(val id: Int) {
    Cook(6),
    String(10),
    Cut(12),
    Smelt(13),
    Spin(16),
    Make(22),
    Smith(25),
}
