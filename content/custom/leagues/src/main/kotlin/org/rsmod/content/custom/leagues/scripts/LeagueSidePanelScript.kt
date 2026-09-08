package org.rsmod.content.custom.leagues.scripts

import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.content.custom.leagues.configs.league_components
import org.rsmod.content.custom.leagues.configs.league_varbits
import org.rsmod.content.custom.leagues.configs.league_varps
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Puts leagues on the game frame, so the panels are reachable without a chat command.
 *
 * `side_journal` reserves five tab slots and the fifth is leagues -- `side_journal:league_list`
 * ships with Op1 already baked into it, and the journal module opens `league_side_panel` into the
 * tab container when it is clicked. That panel draws itself from `[clientscript,3225]` and carries
 * its own Mastery / Tasks / Areas / Relics buttons, which is what this script listens on.
 *
 * The tab is hidden by default. `[proc,side_journal_switchtab]` draws it only when
 * `[proc,league_world]` returns 1 *and* `league_tutorial_completed` reads 3 or higher, so
 * [enableLeagues] sets both on login. Neither is authored content: the world flag lives in a varp
 * no clientscript ever writes, and the tutorial counter is a plain varbit.
 */
class LeagueSidePanelScript
@Inject
constructor(private val protectedAccess: ProtectedAccessLauncher) : PluginScript() {
    private var Player.worldFlags by intVarp(league_varps.map_flags)
    private var Player.leagueTutorialCompleted by intVarBit(league_varbits.tutorial_completed)
    private var Player.leagueAccount by intVarBit(league_varbits.account)
    private var Player.leagueType by intVarBit(league_varbits.type)

    override fun ScriptContext.startup() {
        onPlayerLogin { player.enableLeagues() }

        // These four sit on plain components carrying Op1 in the cache, so they arrive with
        // `comsub == -1` and need no arming from us.
        onIfOverlayButton(league_components.side_relics_button) { player.openRelics() }
        onIfOverlayButton(league_components.side_tasks_button) { player.openTasks() }
        onIfOverlayButton(league_components.side_areas_button) {
            player.mes("League areas are not in yet.")
        }
        onIfOverlayButton(league_components.side_mastery_button) {
            player.mes("Combat mastery is not in yet.")
        }
    }

    /**
     * Marks the account as taking part in a league and flags the world as a league world, which is
     * what makes the journal's fifth tab draw.
     *
     * Only bit 30 of the world flags is set. `[proc,league_world]` wants bit 30 set and bit 29
     * clear, and every other `*_world` predicate reads bits of its own, so this flips exactly one
     * of them from false to true and leaves deadman, tournament, pvp and the rest reading 0 as
     * before.
     *
     * **Write order matters.** The gameframe has already opened the journal by the time this runs,
     * and `[clientscript,journal_list_init]` only listens for changes to varps 2854, 2606 and 3698
     * -- the world flags varp is not among them. What redraws the tab bar is the write to
     * `league_general` (2606), which is where the three varbits below live. So the flags have to be
     * in place before the first of those lands, or the redraw evaluates a world that is not yet a
     * league world and the tab stays hidden until something else nudges it.
     */
    private fun Player.enableLeagues() {
        worldFlags = LEAGUE_WORLD_FLAGS
        leagueTutorialCompleted = TUTORIAL_COMPLETED
        leagueAccount = 1
        if (leagueType == 0) {
            leagueType = DEFAULT_LEAGUE_TYPE
        }
    }

    private fun Player.openRelics() {
        if (!protectedAccess.launch(this) { openRelicsScreen() }) {
            mes("You are busy right now.")
        }
    }

    private fun Player.openTasks() {
        if (!protectedAccess.launch(this) { openTasksScreen() }) {
            mes("You are busy right now.")
        }
    }

    private companion object {
        /** Bit 30 is the league flag; bit 29 must stay clear for `[proc,league_world]` to pass. */
        private const val LEAGUE_WORLD_FLAGS = 1 shl 30

        /** `[proc,side_journal_switchtab]` wants this at 3 or higher before it draws the tab. */
        private const val TUTORIAL_COMPLETED = 3

        /**
         * Which value maps to which league is unknown; 5 is what renders the Trailblazer Reloaded
         * relic set. Overridable in-game with `::leagues <n>`.
         */
        private const val DEFAULT_LEAGUE_TYPE = 5
    }
}
