package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.manager.MagicRuneManager
import org.rsmod.api.combat.manager.MagicRuneManager.Companion.isFailure
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.spells.attack.SpellAttackManager
import org.rsmod.api.stats.xpmod.XpModifiers

/**
 * The one place a spell's requirements are checked and paid for.
 *
 * Both entry points route the cast experience through [XpModifiers], which is the house rule for
 * every skill here. Upstream's `SpellAttackManager.attemptCast` awards the raw `castXp` instead, so
 * the elemental spells in `spell-attacks` pay slightly differently from everything in these
 * modules; that is upstream's call and is left alone.
 */
@Singleton
public class SpellCasting
@Inject
constructor(
    private val runes: MagicRuneManager,
    private val attacks: SpellAttackManager,
    private val xpMods: XpModifiers,
    private val switcher: SpellbookSwitcher,
) {
    /**
     * Consumes the runes for a combat spell and pays its cast xp. On failure the messages have
     * already been sent by [MagicRuneManager] and combat is broken off, so callers just return.
     */
    public fun attemptCombat(
        access: ProtectedAccess,
        attack: CombatAttack.Spell,
    ): MagicRuneManager.CastResult {
        val result = runes.attemptCast(access.player, attack.spell)
        if (result.isFailure()) {
            attacks.stopCombat(access)
            return result
        }
        giveCastXp(access, attack.spell)
        return result
    }

    /**
     * Casts a non-combat spell: level and spellbook, then runes (both with their own messages),
     * then [validate] for the spell's own target checks, and only then are the runes consumed and
     * the xp paid. Official order: a rune shortfall is reported before a bad target is.
     *
     * Animations and sounds are the caller's business, since every utility spell has its own.
     *
     * @return `true` if the spell was paid for and its effect should now happen.
     */
    public fun attemptUtility(
        access: ProtectedAccess,
        spell: MagicSpell,
        validate: () -> Boolean = { true },
    ): Boolean {
        if (!runes.canCastSpell(access.player, spell)) {
            return false
        }
        if (!validate()) {
            return false
        }
        val result = runes.delReqs(access.player, spell)
        if (result.isFailure()) {
            return false
        }
        giveCastXp(access, spell)
        return true
    }

    /** Whether the player's current book is the one [spell] belongs to. */
    public fun onCorrectBook(access: ProtectedAccess, spell: MagicSpell): Boolean =
        spell.spellbook == null || switcher.current(access.player) == spell.spellbook

    private fun giveCastXp(access: ProtectedAccess, spell: MagicSpell) {
        val player = access.player
        access.statAdvance(stats.magic, spell.castXp * xpMods.get(player, stats.magic))
        // Lunar Spellbook Swap lends one cast from another book; that cast ends the loan.
        if (spell.spellbook != null && spell.spellbook != Spellbook.Lunars) {
            switcher.endSpellbookSwap(player)
        }
    }
}
