package org.rsmod.content.skills.magic.commons

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.magic.commons.scripts.MagicCommonsScript

class SwitcherDeps @Inject constructor(val switcher: SpellbookSwitcher)

@Execution(ExecutionMode.SAME_THREAD)
class SpellbookSwitcherTest {
    @Test
    fun GameTestState.`switching writes the spellbook varbit and clears autocast`() =
        runInjectedGameTest(SwitcherDeps::class, null, MagicCommonsScript::class) { deps ->
            VarPlayerIntMapSetter.set(player, varbits.autocast_set, 1)
            VarPlayerIntMapSetter.set(player, varbits.autocast_spell, 5)
            deps.switcher.switch(player, Spellbook.Ancients)
            assertEquals(Spellbook.Ancients.varValue, player.vars[varbits.spellbook])
            assertEquals(Spellbook.Ancients, deps.switcher.current(player))
            assertEquals(0, player.vars[varbits.autocast_set])
            assertEquals(0, player.vars[varbits.autocast_spell])
            assertMessageSent(SpellbookSwitcher.switchMessage(Spellbook.Ancients))
        }

    @Test
    fun GameTestState.`a spellbook swap returns to the lunar book when the timer ends`() =
        runInjectedGameTest(SwitcherDeps::class, null, MagicCommonsScript::class) { deps ->
            deps.switcher.switch(player, Spellbook.Lunars)
            deps.switcher.beginSpellbookSwap(player, Spellbook.Ancients)
            assertEquals(Spellbook.Ancients, deps.switcher.current(player))
            advance(SpellbookSwitcher.SPELLBOOK_SWAP_TICKS)
            assertEquals(Spellbook.Lunars, deps.switcher.current(player))
        }
}
