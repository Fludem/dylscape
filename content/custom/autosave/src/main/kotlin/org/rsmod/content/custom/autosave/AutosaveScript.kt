package org.rsmod.content.custom.autosave

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import org.rsmod.api.account.AccountManager
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.content.custom.autosave.configs.autosave_timers
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Saves every player once a minute instead of only when they log out.
 *
 * Upstream persists a character exactly once, in `PlayerLogoutProcessor`. Anything that stops that
 * logout from running - a crash, a `::reboot`, a kill - loses the whole session, and a character
 * whose *first* logout never ran has no saved state at all. On 2026-09-11 that combination locked a
 * player out of the live server entirely.
 *
 * [SAVE_INTERVAL] is per player and starts at login, so logins spread the saves out by themselves
 * rather than every account writing on the same cycle.
 *
 * **A save reads the player from the saver's own thread.** That is safe at logout, where the player
 * has already left the world, but here the game thread keeps mutating them while the save runs, so
 * a save can catch a half-finished action. Players who are mid-action are skipped to narrow that
 * window, and the next logout or autosave overwrites anything caught mid-change. Making this
 * airtight means snapshotting the player on the game thread, which every save pipeline - invs,
 * stats, farming, houses - would have to be rewritten for.
 */
class AutosaveScript @Inject constructor(private val accounts: AccountManager) : PluginScript() {
    private val logger = InlineLogger()

    override fun ScriptContext.startup() {
        onPlayerLogin { player.softTimer(autosave_timers.autosave, SAVE_INTERVAL) }
        onPlayerSoftTimer(autosave_timers.autosave) { autosave(player) }
    }

    private fun autosave(player: Player) {
        // The logout path is already saving them; a second request would race it.
        if (player.pendingLogout || player.loggingOut) {
            return
        }
        accounts.save(player, ::onSaved)
    }

    /** Called on the saver's thread, so this only logs. */
    private fun onSaved(response: AccountSaveResponse) {
        if (response is AccountSaveResponse.Failure) {
            logger.warn { "Autosave failed for: ${response.player}" }
        }
    }

    private companion object {
        /** 100 ticks: 60 seconds. */
        const val SAVE_INTERVAL = 100
    }
}
