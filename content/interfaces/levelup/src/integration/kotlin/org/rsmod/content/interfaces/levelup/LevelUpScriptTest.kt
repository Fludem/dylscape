package org.rsmod.content.interfaces.levelup

import net.rsprot.protocol.game.outgoing.interfaces.IfSetHide
import net.rsprot.protocol.game.outgoing.interfaces.IfSetText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.config.refs.components
import org.rsmod.api.config.refs.interfaces
import org.rsmod.api.config.refs.stats
import org.rsmod.api.player.stat.statAdvance
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.type.comp.ComponentType

@Execution(ExecutionMode.SAME_THREAD)
class LevelUpScriptTest {
    @Test
    fun GameTestState.`advancing a stat opens the levelup dialogue`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance(stats.woodcutting, xp = 100.0)
            advance()

            val congrats = "Congratulations, you just advanced a Woodcutting level."
            assertTrue(player.ui.containsOverlay(interfaces.levelup_display))
            assertMessageSent(congrats)
            assertTrue(setText(components.levelup_text1, congrats) in client) {
                "Level-up dialogue did not show the congratulations line."
            }
            assertTrue(
                setText(components.levelup_text2, "Your Woodcutting level is now 2.") in client
            ) {
                "Level-up dialogue did not show the new level."
            }
            assertTrue(setText(components.levelup_pbutton, "Click here to continue") in client) {
                "Level-up dialogue is missing its continue button."
            }
        }

    @Test
    fun GameTestState.`skill names starting with a vowel are given an article`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance(stats.attack, xp = 100.0)
            advance()

            assertMessageSent("Congratulations, you just advanced an Attack level.")
        }

    @Test
    fun GameTestState.`only the levelled skill icon is revealed`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance(stats.magic, xp = 100.0)
            advance()

            val revealed = client.filterIsInstance<IfSetHide>().filterNot(IfSetHide::hidden)
            assertEquals(listOf(setHide(components.levelup_magic, hidden = false)), revealed)
        }

    @Test
    fun GameTestState.`xp that does not advance a level shows no dialogue`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance(stats.woodcutting, xp = 10.0)
            advance()

            assertFalse(player.ui.containsOverlay(interfaces.levelup_display))
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`every released stat but hunter reveals an icon`() =
        runGameTest(LevelUpScript::class) {
            val released = cacheTypes.stats.values.filterNot { it.unreleased }
            for (stat in released) {
                player.statAdvance(stat, xp = 2000.0)
                advance()

                val revealed = client.filterIsInstance<IfSetHide>().filterNot(IfSetHide::hidden)
                // `levelup_display` never gained a Hunter layer, so its dialogue has no icon.
                val expected = if (stat.isType(stats.hunter)) 0 else 1
                assertTrue(expected == revealed.size) {
                    "stat=${stat.internalNameValue} expected=$expected revealed=$revealed"
                }
            }
        }

    @Test
    fun GameTestState.`levelup dialogue does not lock the player out of protected access`() =
        runGameTest(LevelUpScript::class) {
            player.statAdvance(stats.woodcutting, xp = 100.0)
            advance()

            assertTrue(player.ui.containsOverlay(interfaces.levelup_display))
            assertFalse(player.isAccessProtected)
        }

    private fun setText(component: ComponentType, text: String): IfSetText =
        IfSetText(component.interfaceId, component.component, text)

    private fun setHide(component: ComponentType, hidden: Boolean): IfSetHide =
        IfSetHide(component.interfaceId, component.component, hidden)
}
