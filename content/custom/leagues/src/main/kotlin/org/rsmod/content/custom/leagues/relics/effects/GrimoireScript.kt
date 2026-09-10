package org.rsmod.content.custom.leagues.relics.effects

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.content.custom.leagues.configs.league_objs
import org.rsmod.content.custom.leagues.relics.Relic
import org.rsmod.content.custom.leagues.relics.hasRelic
import org.rsmod.content.skills.magic.commons.SpellbookSwitcher
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.SessionStateEvent
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The Grimoire relic's Arcane grimoire: op1 Change Spellbook, op3 Previous Spellbook, op4 Check
 * Previous. Op2 is Wield and keeps its default handling.
 */
class GrimoireScript @Inject constructor(private val switcher: SpellbookSwitcher) : PluginScript() {
    /** The book each player swapped away from last. Session-only, like vanilla's own. */
    private val previous = HashMap<Player, Spellbook>()

    private val Spellbook.displayName: String
        get() =
            when (this) {
                Spellbook.Standard -> "standard"
                Spellbook.Ancients -> "Ancient"
                Spellbook.Lunars -> "Lunar"
                Spellbook.Arceuus -> "Arceuus"
            }

    override fun ScriptContext.startup() {
        onOpHeld1(league_objs.arcane_grimoire) { changeSpellbook() }
        onOpHeld3(league_objs.arcane_grimoire) { previousSpellbook() }
        onOpHeld4(league_objs.arcane_grimoire) { checkPrevious() }
        onEvent<SessionStateEvent.Delete> { previous.remove(player) }
    }

    private suspend fun ProtectedAccess.changeSpellbook() {
        if (!player.hasRelic(Relic.Grimoire)) {
            mes(NOT_YOURS)
            return
        }
        // Arceuus is left out: the spellbooks module does not offer it, because its spells are not
        // on this server.
        val book =
            choice3(
                "Standard",
                Spellbook.Standard,
                "Ancient Magicks",
                Spellbook.Ancients,
                "Lunar",
                Spellbook.Lunars,
                title = "Choose a spellbook",
            )
        swapTo(book)
    }

    private fun ProtectedAccess.previousSpellbook() {
        if (!player.hasRelic(Relic.Grimoire)) {
            mes(NOT_YOURS)
            return
        }
        val book = previous[player]
        if (book == null) {
            mes("You have not changed spellbook with the grimoire yet.")
            return
        }
        swapTo(book)
    }

    private fun ProtectedAccess.checkPrevious() {
        val book = previous[player]
        if (book == null) {
            mes("You have not changed spellbook with the grimoire yet.")
            return
        }
        mes("Your previous spellbook was the ${book.displayName} spellbook.")
    }

    private fun ProtectedAccess.swapTo(book: Spellbook) {
        val current = switcher.current(player)
        if (current == book) {
            mes("You are already using the ${book.displayName} spellbook.")
            return
        }
        previous[player] = current
        switcher.switch(player, book)
    }

    private companion object {
        const val NOT_YOURS = "The grimoire's pages are blank to you."
    }
}
