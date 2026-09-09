package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.righthand
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.enumVarBit
import org.rsmod.api.spells.autocast.AutocastWeapons
import org.rsmod.content.skills.magic.commons.configs.magic_timers
import org.rsmod.game.entity.Player
import org.rsmod.game.type.obj.ObjTypeList

/**
 * Changes a player's spellbook and drops any autocast that no longer makes sense.
 *
 * The book itself is cache varbit 4070 `spellbook` (two bits of varp 439); the client redraws
 * interface 218 from it on its own. Autocast is reset the same way `CombatTabScript` does on a
 * weapon switch: the three live varbits, plus the per-weapon saved choice via [AutocastWeapons].
 */
@Singleton
public class SpellbookSwitcher
@Inject
constructor(
    private val objTypes: ObjTypeList,
    private val autocast: AutocastWeapons,
    private val buffs: MagicBuffRegistry,
) {
    private val Player.spellbook by enumVarBit<Spellbook>(varbits.spellbook)

    public fun current(player: Player): Spellbook = player.spellbook

    public fun switch(player: Player, book: Spellbook, announce: Boolean = true) {
        VarPlayerIntMapSetter.set(player, varbits.spellbook, book.varValue)
        resetAutocast(player)
        if (announce) {
            player.mes(switchMessage(book))
        }
    }

    /**
     * Lunar Spellbook Swap: use [book] until the next cast from it, or until the timer runs out.
     * The caller starts the timer; this only records where to come back to.
     */
    public fun beginSpellbookSwap(player: Player, book: Spellbook) {
        buffs[player].spellbookSwapReturn = player.spellbook
        switch(player, book)
        player.softTimer(magic_timers.spellbook_swap, SPELLBOOK_SWAP_TICKS)
    }

    public fun endSpellbookSwap(player: Player) {
        val back = buffs[player].spellbookSwapReturn ?: return
        buffs[player].spellbookSwapReturn = null
        player.clearSoftTimer(magic_timers.spellbook_swap)
        switch(player, back, announce = false)
        player.mes("Your spellbook swap has ended and you return to the Lunar spellbook.")
    }

    private fun resetAutocast(player: Player) {
        VarPlayerIntMapSetter.set(player, varbits.autocast_set, 0)
        VarPlayerIntMapSetter.set(player, varbits.autocast_spell, 0)
        VarPlayerIntMapSetter.set(player, varbits.autocast_defmode, 0)
        val weapon = objTypes.getOrNull(player.righthand) ?: return
        autocast.reset(player, weapon)
    }

    public companion object {
        /** Two minutes, the official Spellbook Swap limit. */
        public const val SPELLBOOK_SWAP_TICKS: Int = 200

        public fun switchMessage(book: Spellbook): String =
            when (book) {
                Spellbook.Standard ->
                    "Your mind clears and you switch back to the standard spellbook."
                Spellbook.Ancients ->
                    "You feel a surge of power as the Ancient Magicks fill your mind."
                Spellbook.Lunars -> "You feel a surge of power as your spellbook changes to Lunar."
                Spellbook.Arceuus ->
                    "You feel a surge of power as the Arceuus magic fills your mind."
            }
    }
}
