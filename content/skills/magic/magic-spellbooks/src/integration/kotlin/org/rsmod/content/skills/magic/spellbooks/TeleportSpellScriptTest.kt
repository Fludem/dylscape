package org.rsmod.content.skills.magic.spellbooks

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.commons.magic.MagicSpell
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.objs
import org.rsmod.api.config.refs.stats
import org.rsmod.api.config.refs.varbits
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.skills.magic.commons.MagicRuneTestSupport
import org.rsmod.content.skills.magic.commons.MagicSpellbooks
import org.rsmod.content.skills.magic.spellbooks.scripts.TeleportSpellScript
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class TeleportTestDeps @Inject constructor(val spellbooks: MagicSpellbooks)

/**
 * The spellbook is an overlay, so the click is a plain `ifButton` on the spell's component; the
 * three-tick cast means the arrival is asserted four ticks after the click lands.
 */
@Execution(ExecutionMode.SAME_THREAD)
class TeleportSpellScriptTest {
    @Test
    fun GameTestState.`every book has teleports and none of them is arceuus or a home teleport`() =
        runInjectedGameTest(
            TeleportTestDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            TeleportSpellScript::class,
        ) { deps ->
            val teleports = deps.spellbooks.teleports()
            assertTrue(teleports.any { it.spellbook == Spellbook.Standard })
            assertTrue(teleports.any { it.spellbook == Spellbook.Ancients })
            assertTrue(teleports.any { it.spellbook == Spellbook.Lunars })
            assertTrue(teleports.none { it.obj == objs.spell_hometeleport_lumbridge })
            assertTrue(teleports.none { it.obj == objs.spell_moonclan_telegroup })
            for (spell in teleports) {
                val dest = checkNotNull(deps.spellbooks.telecoord(spell))
                assertTrue(dest.level == 0) { "${spell.name} lands on level ${dest.level}" }
            }
        }

    @Test
    fun GameTestState.`varrock teleport consumes the runes and lands at the spell coordinate`() =
        runInjectedGameTest(
            TeleportTestDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            TeleportSpellScript::class,
        ) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_varrock_teleport)
            prepare(Spellbook.Standard, spell)
            player.placeAt(START)
            val magicXp = player.statMap.getXP(stats.magic)

            player.ifButton(spell.component)
            advance(1)
            assertEquals(START, player.coords) // still casting
            advance(3)
            assertArrivedAt(checkNotNull(deps.spellbooks.telecoord(spell)))
            assertEquals(0, player.count(objs.law_rune))
            assertTrue(player.statMap.getXP(stats.magic) > magicXp)
        }

    @Test
    fun GameTestState.`an ancient teleport needs the ancient book`() =
        runInjectedGameTest(
            TeleportTestDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            TeleportSpellScript::class,
        ) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_paddewwa_teleport)
            prepare(Spellbook.Standard, spell)
            player.placeAt(START)
            player.ifButton(spell.component)
            advance(4)
            assertEquals(START, player.coords)

            prepare(Spellbook.Ancients, spell)
            player.ifButton(spell.component)
            advance(4)
            assertArrivedAt(checkNotNull(deps.spellbooks.telecoord(spell)))
        }

    @Test
    fun GameTestState.`missing runes are reported before anything happens`() =
        runInjectedGameTest(
            TeleportTestDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            TeleportSpellScript::class,
        ) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_lumbridge_teleport)
            prepare(Spellbook.Standard, spell)
            player.clearInv()
            player.placeAt(START)
            player.ifButton(spell.component)
            advance(1)
            assertMessageSent("You do not have enough Law Runes to cast this spell.")
            assertEquals(START, player.coords)
        }

    @Test
    fun GameTestState.`deep wilderness refuses the teleport and keeps the runes`() =
        runInjectedGameTest(
            TeleportTestDeps::class,
            MagicRuneTestSupport.Module(),
            *MagicRuneTestSupport.scripts,
            TeleportSpellScript::class,
        ) { deps ->
            val spell = deps.spellbooks.spellFor(objs.spell_lumbridge_teleport)
            prepare(Spellbook.Standard, spell)
            player.placeAt(DEEP_WILDERNESS)
            player.ifButton(spell.component)
            advance(1)
            assertMessageSent("You can't teleport above level 20 Wilderness.")
            assertEquals(DEEP_WILDERNESS, player.coords)
            assertEquals(1, player.count(objs.law_rune))
        }

    private fun GameTestScope.prepare(book: Spellbook, spell: MagicSpell) {
        player.ifOpenOverlay(interfaces.magic_spellbook, components.toplevel_target_side6)
        VarPlayerIntMapSetter.set(player, varbits.spellbook, book.varValue)
        player.stats[stats.magic] = 99
        player.clearInv()
        for ((slot, req) in spell.objReqs.withIndex()) {
            player.inv[slot] = InvObj(req.obj, req.count)
        }
    }

    private fun GameTestScope.assertArrivedAt(dest: CoordGrid) {
        val distance = player.coords.chebyshevDistance(dest)
        assertTrue(distance <= 2) { "Expected to land near $dest but at ${player.coords}" }
    }

    private companion object {
        val START = CoordGrid(3096, 3504, 0)
        val DEEP_WILDERNESS = CoordGrid(3100, 3690, 0)
    }
}
