package org.rsmod.game.entity.player

import org.rsmod.game.entity.Player
import org.rsmod.game.obj.Obj

/**
 * The single definition of what an [AccountMode] may and may not do.
 *
 * These live in the engine because their call sites are spread across modules that cannot depend on
 * one another - the ground-obj rule is enforced from `api/obj-plugin`, which has no access to
 * `content/` - and because every type involved is already an engine type.
 *
 * Two of the three rules have no call site yet, which is deliberate rather than an oversight: the
 * features they restrict do not exist in this codebase. Defining them here means the rule is
 * greppable from the day the feature lands instead of being rediscovered then.
 */
public object AccountModeRules {
    /**
     * Ironmen may only take objs that were originally theirs, or that nobody owns.
     *
     * An obj with no owner is a server spawn - `Obj.fromServer` leaves `ownerId` unset - so world
     * spawns, respawning ground items and map-decoded objs all stay takeable. Ownership
     * deliberately outlives visibility: `Obj.reveal` only widens who can *see* an obj, so one that
     * has gone public still remembers who dropped it, which is exactly what this rule needs.
     */
    public fun canTakeObj(player: Player, obj: Obj): Boolean =
        !player.accountMode.isIronman || obj.nullableOwnerId == null || obj.isOriginalOwner(player)

    /**
     * Ironmen cannot trade other players.
     *
     * Player trading is not implemented in this codebase, so nothing calls this yet. It is claimed
     * here, and by the refusal handler registered on `OpPlayer4`, so that trading cannot be built
     * without confronting the rule.
     */
    public fun canTradePlayers(player: Player): Boolean = !player.accountMode.isIronman

    /**
     * Ironmen cannot use storage another player can also reach.
     *
     * No shared or group storage exists yet. Personal banks are unaffected - the Ultimate Ironman
     * bank restriction is a separate rule and is deliberately not implemented.
     */
    public fun canUseSharedStorage(player: Player): Boolean = !player.accountMode.isIronman
}
