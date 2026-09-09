package org.rsmod.content.skills.magic.spellbooks.scripts

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.objs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessLauncher
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.commons.SpellCasting
import org.rsmod.content.skills.magic.commons.SpellbookSwitcher
import org.rsmod.content.skills.magic.commons.launchSpell
import org.rsmod.content.skills.magic.spellbooks.configs.spellbook_seqs
import org.rsmod.events.EventBus
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Lunar Spellbook Swap: borrow the standard or Ancient book for one cast, or two minutes.
 *
 * The loan is tracked by [SpellbookSwitcher]: the return book is remembered, and the first
 * successful cast from the borrowed book (any spell paid for through [SpellCasting]) or the
 * `magic_spellbook_swap` timer ends it. Arceuus is not offered because it is not on this server.
 */
class SpellbookSwapScript
@Inject
constructor(
    private val protectedAccess: ProtectedAccessLauncher,
    private val eventBus: EventBus,
    private val spellbooks: MagicSpellbooks,
    private val casting: SpellCasting,
    private val switcher: SpellbookSwitcher,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val spell = spellbooks.spellFor(objs.spell_spellbook_swap)
        onIfOverlayButton(spell.component) {
            protectedAccess.launchSpell(player, eventBus) { swap() }
        }
    }

    private suspend fun ProtectedAccess.swap() {
        val spell = spellbooks.spellFor(objs.spell_spellbook_swap)
        if (!casting.onCorrectBook(this, spell)) {
            return
        }
        val book =
            choice2(
                "Standard spellbook",
                Spellbook.Standard,
                "Ancient Magicks",
                Spellbook.Ancients,
                title = "Which spellbook would you like to swap to?",
            )
        if (!casting.attemptUtility(this, spell)) {
            return
        }
        anim(spellbook_seqs.spellbook_swap)
        switcher.beginSpellbookSwap(player, book)
        mes("You may cast one spell from the borrowed spellbook, within the next two minutes.")
    }
}
