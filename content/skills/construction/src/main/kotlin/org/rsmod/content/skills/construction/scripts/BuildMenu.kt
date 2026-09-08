package org.rsmod.content.skills.construction.scripts

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.output.runClientScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onIfClose
import org.rsmod.api.script.onIfModalButton
import org.rsmod.content.skills.construction.configs.ConstructionComponents
import org.rsmod.content.skills.construction.configs.ConstructionInterfaces
import org.rsmod.content.skills.construction.data.FurnitureData
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.game.type.interf.IfEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The vanilla build menu, driven from the server.
 *
 * Opening it is three steps, in this order: draw every entry with
 * `clientscript,poh_furniture_creation_entry`, blank the slots that are left over, then run the
 * layout script. The layout reads each slot to decide whether it holds anything, so drawing has to
 * come first -- see [ConstructionComponents] for how both signatures were recovered.
 *
 * The slots press with no subcomponent, so [ProtectedAccess.ifSetEvents] grants op 1 on `-1..-1`.
 * That only reaches the server because [org.rsmod.api.net.rsprot.player.InterfaceEvents] consults
 * runtime grants for a `-1` press; the cache leaves these components with no events of their own.
 *
 * A session is remembered per player the way the make-menu does it, because the callback runs in
 * the *click's* protected-access scope rather than the opener's.
 */
@Singleton
public class BuildMenu @Inject constructor() {
    private val sessions = HashMap<Player, Session>()

    /** Draws [options] and calls [onPick] once one is pressed. */
    public fun open(
        access: ProtectedAccess,
        options: List<BuildOption>,
        onPick: suspend ProtectedAccess.(FurnitureData) -> Unit,
    ) {
        require(options.isNotEmpty()) { "`buildMenu` needs at least one option to show." }
        val shown = options.take(ConstructionComponents.slots.size)

        with(access) {
            ifOpenMainModal(ConstructionInterfaces.furniture_creation)

            for ((index, component) in ConstructionComponents.slots.withIndex()) {
                val slot = index + 1
                val option = shown.getOrNull(index)
                if (option == null) {
                    // A negative level is the script's own "this slot is empty" signal.
                    runClientScript(ENTRY_SCRIPT, slot, 0, EMPTY_SLOT_LEVEL, 0, "")
                    continue
                }
                runClientScript(
                    ENTRY_SCRIPT,
                    slot,
                    option.furniture.rowId,
                    option.furniture.levelRequirement,
                    if (option.buildable) 1 else 0,
                    option.materials,
                )
                ifSetEvents(component, STATIC_PRESS, IfEvent.Op1)
            }

            runClientScript(LAYOUT_SCRIPT, shown.size, 0)
        }
        sessions[access.player] = Session(shown, onPick)
    }

    public fun isOpen(player: Player): Boolean = sessions.containsKey(player)

    internal suspend fun dispatch(access: ProtectedAccess, slot: Int) {
        val session = sessions.remove(access.player) ?: return
        access.ifClose()
        val option = session.options.getOrNull(slot) ?: return
        session.onPick(access, option.furniture)
    }

    internal fun clear(player: Player) {
        sessions.remove(player)
    }

    private class Session(
        val options: List<BuildOption>,
        val onPick: suspend ProtectedAccess.(FurnitureData) -> Unit,
    )

    private companion object {
        const val ENTRY_SCRIPT: Int = 1404
        const val LAYOUT_SCRIPT: Int = 1406
        const val EMPTY_SLOT_LEVEL: Int = -1

        /** A slot has no dynamic child, so its press carries no subcomponent. */
        val STATIC_PRESS: IntRange = -1..-1
    }
}

/**
 * One row of the build menu: the furniture, whether the player can actually build it right now, and
 * the cost line the client splits over two rows.
 */
public data class BuildOption(
    public val furniture: FurnitureData,
    public val buildable: Boolean,
    public val materials: String,
)

/** The single owner of the build menu's slots, routing every press back to [BuildMenu]. */
public class BuildMenuScript @Inject constructor(private val buildMenu: BuildMenu) :
    PluginScript() {
    override fun ScriptContext.startup() {
        for ((slot, component) in ConstructionComponents.slots.withIndex()) {
            onIfModalButton(component) { buildMenu.dispatch(this, slot) }
        }
        onIfClose(ConstructionInterfaces.furniture_creation) { buildMenu.clear(player) }
        onEvent<SessionStateEvent.Logout> { buildMenu.clear(player) }
    }
}
