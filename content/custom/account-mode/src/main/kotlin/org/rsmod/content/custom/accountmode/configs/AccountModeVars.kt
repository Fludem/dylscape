package org.rsmod.content.custom.accountmode.configs

import org.rsmod.api.type.refs.varbit.VarBitReferences

typealias account_mode_varbits = AccountModeVarBits

/**
 * Vanilla varbits this server writes to describe an account's mode to the client.
 *
 * These are mirrors, never the source of truth: the authoritative values live in
 * `characters.account_mode` and `characters.xp_rate_tier`. Writing them is what makes the cache's
 * own ironman clientscripts behave, at the cost of one assignment on login and on change.
 */
object AccountModeVarBits : VarBitReferences() {
    /**
     * The account-type varbit. Its value domain is fixed by the button order of the cache's
     * `ironman_setup` interface: none, ironman, ultimate, hardcore, then the three group modes we
     * do not support. [org.rsmod.game.entity.player.AccountMode.id] matches it deliberately.
     */
    val ironman = find("ironman")

    /** Set once a hardcore ironman has died, so the client can render a downgraded account. */
    val hardcore_dead = find("ironman_hardcore_dead")
}
