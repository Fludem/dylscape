package org.rsmod.content.custom.accountmode

import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.content.custom.accountmode.configs.account_mode_varbits
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.AccountMode

private var Player.ironmanVarBit by intVarBit(account_mode_varbits.ironman)

private var Player.hardcoreDeadVarBit by boolVarBit(account_mode_varbits.hardcore_dead)

/**
 * Pushes [Player.accountMode] into the vanilla varbits the client understands.
 *
 * Call this on login and whenever the mode changes. It is one-way on purpose: the varbits are a
 * projection of the database column, never a second source of truth, so nothing ever reads them
 * back to decide what mode an account is.
 *
 * Note this does *not* give the ironman chat badge - `MessagePublicHandler` derives that from the
 * player's mod level rather than from any varbit.
 */
public fun Player.syncAccountModeVarBits() {
    ironmanVarBit = accountMode.id
}

/** Records that a hardcore ironman has died, which the cache's own downgrade UI reads. */
public fun Player.markHardcoreDead() {
    hardcoreDeadVarBit = true
}

/** True when this account has been through first-login setup. */
public val Player.hasChosenAccountSetup: Boolean
    get() = xpRateTier != null

/** True for any of the iron modes. */
public val Player.isIronman: Boolean
    get() = accountMode.isIronman

/** Human-readable mode name, for chat and broadcasts. */
public val AccountMode.displayName: String
    get() =
        when (this) {
            AccountMode.Standard -> "Standard"
            AccountMode.Ironman -> "Ironman"
            AccountMode.UltimateIronman -> "Ultimate Ironman"
            AccountMode.HardcoreIronman -> "Hardcore Ironman"
        }
