package org.rsmod.content.skills.construction

import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.construction.configs.ConstructionComponents
import org.rsmod.content.skills.construction.configs.ConstructionInterfaces
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.FurnitureData
import org.rsmod.content.skills.construction.scripts.BuildMenu
import org.rsmod.content.skills.construction.scripts.BuildMenuScript
import org.rsmod.content.skills.construction.scripts.BuildOption
import org.rsmod.game.type.interf.IfButtonOp

/**
 * Covers the build menu's server half: that it opens, that a press comes back as the furniture that
 * was drawn in that slot, and that a closed menu cannot fire late.
 *
 * The press goes through the real `If3ButtonHandler`, so it also pins the thing that made this
 * interface usable at all: the slots hold no dynamic child and so arrive with `comsub == -1`, which
 * the handler used to answer from the cache's static events alone -- and these components have
 * none.
 */
@Execution(ExecutionMode.SAME_THREAD)
class BuildMenuTest {
    class Deps @Inject constructor(val buildMenu: BuildMenu, val tables: ConstructionTables)

    @Test
    fun GameTestState.`a press returns the furniture drawn in that slot`() =
        runInjectedGameTest(Deps::class, scripts = arrayOf(BuildMenuScript::class)) { deps ->
            val options = deps.chairOptions()
            var picked: FurnitureData? = null

            player.withProtectedAccess {
                deps.buildMenu.open(this, options) { furniture -> picked = furniture }
            }

            assertTrue(player.ui.containsModal(ConstructionInterfaces.furniture_creation)) {
                "The build menu did not open."
            }
            assertTrue(deps.buildMenu.isOpen(player))

            press(slot = 2)
            advance(ticks = 1)

            assertNotNull(picked) { "Pressing a slot produced no callback." }
            assertEquals(options[2].furniture.rowId, picked!!.rowId) {
                "Slot 2 returned '${picked!!.name}' instead of '${options[2].furniture.name}'."
            }
            assertFalse(deps.buildMenu.isOpen(player)) { "The session outlived the press." }
        }

    @Test
    fun GameTestState.`a menu the player closed does not fire later`() =
        runInjectedGameTest(Deps::class, scripts = arrayOf(BuildMenuScript::class)) { deps ->
            var picked: FurnitureData? = null
            player.withProtectedAccess {
                deps.buildMenu.open(this, deps.chairOptions()) { furniture -> picked = furniture }
            }
            player.ifClose()
            advance(ticks = 1)

            press(slot = 0)
            advance(ticks = 1)

            assertFalse(deps.buildMenu.isOpen(player))
            assertEquals(null, picked) { "A closed menu still built something." }
        }

    /** The seven parlour chairs, which is the list the menu was designed around. */
    private fun Deps.chairOptions(): List<BuildOption> {
        val parlour = tables.rooms.values.first { it.internalName == "poh_dummy_parlour" }
        val hotspot = tables.hotspot(parlour.hotspots[0])!!
        return hotspot.options.mapNotNull(tables::furniture).map {
            BuildOption(it, buildable = true, materials = "2 x Plank")
        }
    }

    /**
     * Presses a slot the way the client does -- op 1 with no subcomponent -- through the real
     * button handler, so the server's own decision about whether that press is allowed is part of
     * what this asserts.
     */
    private fun org.rsmod.api.testing.scope.GameTestScope.press(slot: Int) {
        player.ifButton(ConstructionComponents.slots[slot], comsub = null, op = IfButtonOp.Op1)
    }
}
