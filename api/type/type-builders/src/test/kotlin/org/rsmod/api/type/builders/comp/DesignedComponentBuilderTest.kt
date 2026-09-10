package org.rsmod.api.type.builders.comp

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.rsmod.api.type.builders.comp.DesignedComponentBuilder.Companion.EVENT_COM
import org.rsmod.game.type.comp.UnpackedComponentType

/**
 * Pins the mapping from a `tools/interface-designer` file onto the component fields the cache
 * stores. `fixture.interface.json` exercises every field the designer can write.
 */
class DesignedComponentBuilderTest {
    private object FixtureBuilder : DesignedComponentBuilder("fixture.interface.json")

    private val components: List<UnpackedComponentType>
        get() = FixtureBuilder.cache

    private fun component(name: String): UnpackedComponentType =
        components[FixtureBuilder.componentNames.indexOf(name)]

    @Test
    fun `components are declared in file order, which is child-index order`() {
        assertEquals("fixture_panel", FixtureBuilder.interfaceName)
        assertEquals(
            listOf("root", "frame", "well", "button", "button_label", "icon", "slot"),
            FixtureBuilder.componentNames,
        )
        assertEquals(
            FixtureBuilder.componentNames.map { "fixture_panel:$it" },
            components.map { it.internalName },
        )
    }

    @Test
    fun `root is a centred fixed-size layer that swallows clicks`() {
        val root = component("root")
        assertEquals(0, root.type)
        assertEquals(-1, root.layer)
        assertEquals(488, root.width)
        assertEquals(320, root.height)
        assertEquals(1, root.xMode)
        assertEquals(1, root.yMode)
        assertEquals(0, root.widthMode)
        assertTrue(root.noClickThrough)
        assertTrue(root.v3)
    }

    @Test
    fun `parents are written as the child index of the named parent`() {
        assertEquals(0, component("frame").layer)
        assertEquals(3, component("button_label").layer)
    }

    @Test
    fun `a frame runs steelborder on itself`() {
        val frame = component("frame")
        assertArrayEquals(arrayOf<Any>(227, EVENT_COM, "Fixture"), frame.onLoad)
        assertEquals(1, frame.widthMode)
        assertEquals(1, frame.heightMode)
    }

    @Test
    fun `a rect keeps colour, fill and trans, and hovers back to its resting trans`() {
        val well = component("well")
        assertEquals(3, well.type)
        assertEquals(0x0E0E0C, well.colour1)
        assertTrue(well.fill)
        assertEquals(190, well.trans1)
        assertArrayEquals(arrayOf<Any>(273, EVENT_COM, 150), well.onMouseOver)
        assertArrayEquals(arrayOf<Any>(273, EVENT_COM, 190), well.onMouseLeave)
    }

    @Test
    fun `ops set both the text and the events mask, skipping blank slots`() {
        val button = component("button")
        assertArrayEquals(arrayOf("Buy", "", "Examine"), button.op)
        // Op1 is bit 1 and Op3 is bit 3; the blank Op2 enables nothing.
        assertEquals((1 shl 1) or (1 shl 3), button.events)
        assertEquals("<col=ff9040>Sword</col>", button.opBase)
        assertEquals(2, button.xMode)
        assertEquals(2, button.yMode)
        assertTrue(button.hide)
    }

    @Test
    fun `text keeps its font, alignment and shadow, and hovers back to its own colour`() {
        val label = component("button_label")
        assertEquals(4, label.type)
        assertEquals("Buy", label.text)
        assertEquals(496, label.textFont)
        assertEquals(1, label.textAlignH)
        assertEquals(1, label.textAlignV)
        assertEquals(12, label.textLineHeight)
        assertTrue(label.textShadow)
        assertArrayEquals(arrayOf<Any>(45, EVENT_COM, 0xFFFFFF), label.onMouseOver)
        assertArrayEquals(arrayOf<Any>(45, EVENT_COM, 0xFF981F), label.onMouseLeave)
    }

    @Test
    fun `a graphic keeps its sprite, tiling and trans, and swaps sprites on hover`() {
        val icon = component("icon")
        assertEquals(5, icon.type)
        assertEquals(812, icon.graphic)
        assertTrue(icon.tiling)
        assertEquals(10, icon.trans1)
        assertArrayEquals(arrayOf<Any>(44, EVENT_COM, 813), icon.onMouseOver)
        assertArrayEquals(arrayOf<Any>(44, EVENT_COM, 812), icon.onMouseLeave)
    }

    @Test
    fun `an item slot is an empty model component for the server to fill`() {
        val slot = component("slot")
        assertEquals(6, slot.type)
        assertEquals(36, slot.width)
        assertEquals(32, slot.height)
        // Nothing is authored into it; ifSetObj supplies the obj at runtime.
        assertEquals(-1, slot.model)
        assertEquals(-1, slot.graphic)
        assertArrayEquals(arrayOf("Value"), slot.op)
        assertEquals(1 shl 1, slot.events)
    }

    @Test
    fun `fields a design leaves out take the builder defaults`() {
        val frame = component("frame")
        assertEquals(-1, frame.textFont)
        assertEquals(-1, frame.graphic)
        assertEquals(0, frame.events)
        assertEquals(0, frame.op.size)
        assertFalse(frame.hide)
        assertFalse(frame.noClickThrough)
        assertNull(frame.onMouseOver)
    }

    @Test
    fun `an unknown field is refused, not dropped`() {
        val error =
            assertThrows<IllegalArgumentException> { parse(root(""", "colur": "ffffff"""")) }
        assertTrue("colur" in error.message!!, error.message)
    }

    @Test
    fun `a parent must be declared above its child`() {
        val json =
            design(
                """{"name": "root", "type": "layer"}""",
                """{"name": "a", "parent": "b", "type": "layer"}""",
                """{"name": "b", "parent": "root", "type": "layer"}""",
            )
        assertThrows<IllegalArgumentException> { parse(json) }
    }

    @Test
    fun `names must be unique and lowercase`() {
        assertThrows<IllegalArgumentException> {
            parse(
                design(
                    """{"name": "root", "type": "layer"}""",
                    """{"name": "root", "parent": "root", "type": "layer"}""",
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            parse(design("""{"name": "Root", "type": "layer"}"""))
        }
    }

    @Test
    fun `hover must suit the component type`() {
        val json =
            design(
                """{"name": "root", "type": "layer"}""",
                """{"name": "r", "parent": "root", "type": "rect", "hover": {"colour": "ffffff"}}""",
            )
        assertThrows<IllegalArgumentException> { parse(json) }
    }

    @Test
    fun `text needs a font and a graphic needs a sprite`() {
        assertThrows<IllegalArgumentException> {
            parse(
                design(
                    """{"name": "root", "type": "layer"}""",
                    """{"name": "t", "parent": "root", "type": "text"}""",
                )
            )
        }
        assertThrows<IllegalArgumentException> {
            parse(
                design(
                    """{"name": "root", "type": "layer"}""",
                    """{"name": "g", "parent": "root", "type": "graphic"}""",
                )
            )
        }
    }

    private fun parse(json: String) = InterfaceDesigns.parse(json, "test")

    private fun root(extra: String): String = design("""{"name": "root", "type": "layer"$extra}""")

    private fun design(vararg components: String): String =
        """{"format": 1, "interface": "test", "components": [${components.joinToString()}]}"""
}
