package org.rsmod.content.skills.magic.commons

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Player

/**
 * Turns a spellbook click into protected access.
 *
 * The spellbook is an overlay, so its buttons arrive as `IfOverlayButton`/`IfOverlayButtonT` events
 * that carry no access of their own. This is the same three-step launch `api/inv-plugin`'s
 * `HeldUOpScript` does for "use item on item": drop whatever the player was about to do, stop
 * facing it, and only then ask for access. A player who is busy (delayed, or behind a modal) is
 * refused by [ProtectedAccessLauncher.launch] and the click is simply lost, which is what the
 * official game does with a spell cast mid-action.
 */
public fun ProtectedAccessLauncher.launchSpell(
    player: Player,
    eventBus: EventBus,
    block: suspend ProtectedAccess.() -> Unit,
) {
    player.clearPendingAction(eventBus)
    player.resetFaceEntity()
    if (player.isAccessProtected) {
        return
    }
    launch(player) { block() }
}
