package org.rsmod.content.skills.magic.utility.scripts

import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.config.refs.components
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onIfOverlayButtonT
import org.rsmod.content.skills.magic.commons.launchSpell
import org.rsmod.events.EventBus
import org.rsmod.game.type.obj.UnpackedObjType
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The two ways a utility spell is cast from the spellbook overlay.
 *
 * A self-cast is a plain click on the spell's component (`Op1` in the cache). A cast on an
 * inventory item arrives as `IfOverlayButtonT` with the spell as the selected component and
 * `inventory:items` as the target; the handler has already checked `TgtCom` on the spell and
 * `Target` on the inventory, so all that is left is the slot and the obj in it. Both hand the
 * player over to protected access the way `HeldUOpScript` does.
 */
internal fun ScriptContext.onSelfSpell(
    spell: MagicSpell,
    launcher: ProtectedAccessLauncher,
    eventBus: EventBus,
    action: suspend ProtectedAccess.() -> Unit,
) {
    onIfOverlayButton(spell.component) { launcher.launchSpell(player, eventBus) { action() } }
}

internal fun ScriptContext.onSpellOnInvObj(
    spell: MagicSpell,
    launcher: ProtectedAccessLauncher,
    eventBus: EventBus,
    action: suspend ProtectedAccess.(slot: Int, obj: UnpackedObjType) -> Unit,
) {
    onIfOverlayButtonT(spell.component, components.inv_items) {
        val slot = targetSlot
        val obj = targetObj ?: return@onIfOverlayButtonT
        launcher.launchSpell(player, eventBus) { action(slot, obj) }
    }
}

/** `true` when the obj in [slot] is still [obj]; an inventory can change under a queued click. */
internal fun ProtectedAccess.slotHolds(slot: Int, obj: UnpackedObjType): Boolean =
    inv[slot]?.id == obj.id
