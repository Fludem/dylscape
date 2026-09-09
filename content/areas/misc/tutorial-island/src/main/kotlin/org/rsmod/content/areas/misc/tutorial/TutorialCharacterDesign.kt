package org.rsmod.content.areas.misc.tutorial

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.ui.ifOpenMainModal
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignComponents
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignData.ColourSlot
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignData.StyleSlot
import org.rsmod.content.areas.misc.tutorial.configs.TutorialDesignInterfaces
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The character-design screen, as Tutorial Island's first step.
 *
 * The whole screen is driven server-side. Every button on interface 679 carries `Op1`, so a press
 * arrives here as an ordinary modal-button event; this script edits the player's [Appearance] and
 * the new look goes back out on the next cycle as appearance extended-info. That matters because
 * this revision's protocol has **no incoming "design chosen" packet** -- the client cannot tell us
 * what it is showing, so the server has to be the thing that decides, and the screen is really just
 * a set of buttons pointed at [Appearance.setIdentKit] and [Appearance.setColour].
 *
 * Each row is a `_left`/`_right` pair, which step backwards and forwards through that slot's option
 * list, wrapping at both ends. The lists are per gender, so switching gender re-seats every style
 * at the same position in the new gender's list -- a player who had picked the third hair keeps the
 * third hair rather than being reset.
 *
 * The screen is opened by the Gielinor Guide (and once automatically, when a new account is placed
 * on the island), and `confirm` simply closes it: there is nothing to commit, because every press
 * has already been applied.
 */
class TutorialCharacterDesign @Inject constructor(private val eventBus: EventBus) : PluginScript() {
    override fun ScriptContext.startup() {
        for (slot in StyleSlot.entries) {
            onIfModalButton(slot.leftButton()) { cycleStyle(slot, step = -1) }
            onIfModalButton(slot.rightButton()) { cycleStyle(slot, step = 1) }
        }
        for (slot in ColourSlot.entries) {
            val buttons = slot.buttons() ?: continue
            onIfModalButton(buttons.first) { cycleColour(slot, step = -1) }
            onIfModalButton(buttons.second) { cycleColour(slot, step = 1) }
        }
        onIfModalButton(TutorialDesignComponents.gender_male) { setGender(female = false) }
        onIfModalButton(TutorialDesignComponents.gender_female) { setGender(female = true) }
        onIfModalButton(TutorialDesignComponents.confirm) { confirm() }
    }

    private fun ProtectedAccess.cycleStyle(slot: StyleSlot, step: Int) {
        val options = slot.options(player.isFemale)
        val current = player.appearance.identKitSnapshot()[slot.index].toInt()
        val position = options.indexOf(current)
        // An unknown current kit (a transmog, or a look saved before this screen existed) has no
        // position in the list; stepping from the start is better than refusing to move.
        val next = if (position == -1) 0 else Math.floorMod(position + step, options.size)
        player.appearance.setIdentKit(slot.index, options[next])
    }

    private fun ProtectedAccess.cycleColour(slot: ColourSlot, step: Int) {
        val current = player.appearance.coloursSnapshot()[slot.index].toInt()
        val next = Math.floorMod(current + step, slot.count)
        player.appearance.setColour(slot.index, next)
    }

    /**
     * Switches gender, carrying every style across by its position in the list rather than by its
     * id -- the two genders' kits are entirely separate id ranges, so an id never means the same
     * thing on the other side.
     */
    private fun ProtectedAccess.setGender(female: Boolean) {
        if (player.isFemale == female) {
            return
        }
        val positions =
            StyleSlot.entries.map { slot ->
                val was = slot.options(player.isFemale)
                slot to was.indexOf(player.appearance.identKitSnapshot()[slot.index].toInt())
            }
        player.appearance.bodyType = if (female) 1 else 0
        player.appearance.pronoun = if (female) 1 else 0
        for ((slot, position) in positions) {
            val options = slot.options(female)
            val index = if (position == -1) 0 else position.coerceAtMost(options.size - 1)
            player.appearance.setIdentKit(slot.index, options[index])
        }
    }

    private fun ProtectedAccess.confirm() {
        player.ifClose(eventBus)
    }

    private val Player.isFemale: Boolean
        get() = appearance.bodyType == 1

    private fun StyleSlot.leftButton(): ComponentType =
        when (this) {
            StyleSlot.HAIR -> TutorialDesignComponents.head_left
            StyleSlot.JAW -> TutorialDesignComponents.jaw_left
            StyleSlot.TORSO -> TutorialDesignComponents.torso_left
            StyleSlot.ARMS -> TutorialDesignComponents.arms_left
            StyleSlot.HANDS -> TutorialDesignComponents.hands_left
            StyleSlot.LEGS -> TutorialDesignComponents.legs_left
            StyleSlot.FEET -> TutorialDesignComponents.feet_left
        }

    private fun StyleSlot.rightButton(): ComponentType =
        when (this) {
            StyleSlot.HAIR -> TutorialDesignComponents.head_right
            StyleSlot.JAW -> TutorialDesignComponents.jaw_right
            StyleSlot.TORSO -> TutorialDesignComponents.torso_right
            StyleSlot.ARMS -> TutorialDesignComponents.arms_right
            StyleSlot.HANDS -> TutorialDesignComponents.hands_right
            StyleSlot.LEGS -> TutorialDesignComponents.legs_right
            StyleSlot.FEET -> TutorialDesignComponents.feet_right
        }

    /**
     * The interface lays out exactly seven style rows (head, jaw, torso, arms, hands, legs, feet --
     * one per identkit slot) and then five colour rows (hair, torso, legs, feet, skin -- one per
     * colour slot). So `head_*` is the head *style* and `hair_*` is the hair *colour*, despite the
     * names reading the other way around.
     */
    private fun ColourSlot.buttons(): Pair<ComponentType, ComponentType>? =
        when (this) {
            ColourSlot.HAIR ->
                TutorialDesignComponents.hair_left to TutorialDesignComponents.hair_right
            ColourSlot.TORSO ->
                TutorialDesignComponents.torso_col_left to TutorialDesignComponents.torso_col_right
            ColourSlot.LEGS ->
                TutorialDesignComponents.legs_col_left to TutorialDesignComponents.legs_col_right
            ColourSlot.FEET ->
                TutorialDesignComponents.feet_col_left to TutorialDesignComponents.feet_col_right
            ColourSlot.SKIN ->
                TutorialDesignComponents.skin_left to TutorialDesignComponents.skin_right
        }

    companion object {
        /** Opens the design screen. Shared with the Gielinor Guide's "change my look" line. */
        fun openDesign(player: Player, eventBus: EventBus) {
            player.ifOpenMainModal(TutorialDesignInterfaces.player_design, eventBus)
        }
    }
}
