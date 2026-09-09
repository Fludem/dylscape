package org.rsmod.api.cache.types.comp

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.rsmod.game.type.comp.ComponentTypeBuilder
import org.rsmod.game.type.comp.UnpackedComponentType

/**
 * Guards the encode/decode symmetry that hand-authored interfaces depend on.
 *
 * [ComponentBuilderResolver][org.rsmod.api.type.builders.comp.ComponentBuilderResolver] decides
 * whether a component needs packing by comparing the authored type against the one decoded back out
 * of the cache, using [UnpackedComponentType]'s deep `equals`. If any field fails to survive a
 * round trip the two can never match: the verifier reports `CacheUpdateRequired` on every boot,
 * packs, re-verifies, still mismatches, and `packCache`'s second pass throws.
 *
 * `layer` is the field that makes this a real hazard rather than a theoretical one. It holds the
 * *packed* id of the parent component, but the wire format only has room for the low 16 bits:
 * [ComponentTypeEncoder.encodeV3] writes `writeShort(layer)` and [ComponentTypeDecoder.decodeV3]
 * adds `combinedId and -65536` back on. It round-trips by truncation, so it only survives if the
 * authored value already carries the interface id in its high bits.
 *
 * A byte-length mismatch is the other failure this catches. `encodeV3` writes several literal
 * values the decoder discards, so an edit to one side and not the other silently misparses the
 * *next* file in the group rather than throwing. Asserting the buffer is fully drained pins the
 * length down per component type.
 */
class ComponentTypeCodecTest {
    @Test
    fun `root layer round trips`() {
        assertRoundTrip(
            component(index = 0) {
                type = 0
                width = 500
                height = 324
            }
        )
    }

    @Test
    fun `child layer keeps its packed parent id`() {
        val child =
            component(index = 5) {
                type = 0
                layer = packed(parentIndex = 0)
            }
        val decoded = assertRoundTrip(child)
        assertEquals(packed(parentIndex = 0), decoded.layer)
    }

    @Test
    fun `a layer authored without the interface id would not survive`() {
        // Documents precisely what ComponentBuilderResolver normalises away: an author writing the
        // bare child index gets a value back that can never equal what they wrote.
        val bare =
            component(index = 5) {
                type = 0
                layer = 3
            }
        assertEquals(packed(parentIndex = 3), roundTrip(bare).layer)
    }

    @Test
    fun `filled rect round trips`() {
        assertRoundTrip(
            component(index = 1) {
                type = 3
                colour1 = 0x1E1710
                fill = true
                trans1 = 0
            }
        )
    }

    @Test
    fun `text round trips`() {
        assertRoundTrip(
            component(index = 3) {
                type = 4
                textFont = 496
                text = "Choose your path"
                textLineHeight = 16
                textAlignH = 1
                textAlignV = 0
                textShadow = true
                colour1 = 0xFF981F
            }
        )
    }

    @Test
    fun `sprite round trips`() {
        assertRoundTrip(
            component(index = 2) {
                type = 5
                graphic = 1234
                angle2d = 0
                tiling = false
                trans1 = 0
                outline = 0
                graphicShadow = 0
                vFlip = false
                hFlip = false
            }
        )
    }

    @Test
    fun `line round trips`() {
        assertRoundTrip(
            component(index = 7) {
                type = 9
                lineWid = 1
                colour1 = 0x5A503C
                lineDirection = false
            }
        )
    }

    @Test
    fun `clickable button round trips with its op text`() {
        val button =
            component(index = 8) {
                type = 3
                colour1 = 0x3A3226
                fill = true
                events = 2 // Op1, as the v1 mask the cache stores.
                op = arrayOf("Select")
                opBase = "Ironman"
            }
        val decoded = assertRoundTrip(button)
        assertEquals(2, decoded.events)
        assertEquals("Select", decoded.op[0])
    }

    @Test
    fun `static component carries no clientscript hooks`() {
        val decoded = roundTrip(component(index = 0) { type = 0 })
        assertNoHooks(decoded)
    }

    private fun assertRoundTrip(authored: UnpackedComponentType): UnpackedComponentType {
        val decoded = roundTrip(authored)
        assertEquals(authored, decoded)
        return decoded
    }

    private fun roundTrip(authored: UnpackedComponentType): UnpackedComponentType {
        val buffer = Unpooled.buffer()
        ComponentTypeEncoder.encode(authored, buffer)
        val combinedId = authored.packed
        val decoded = ComponentTypeDecoder.decode(combinedId, buffer).build(combinedId)
        assertFalse(buffer.isReadable, "Decoder left ${buffer.readableBytes()} unread byte(s).")
        return decoded
    }

    private fun assertNoHooks(type: UnpackedComponentType) {
        val hooks =
            mapOf(
                "onLoad" to type.onLoad,
                "onMouseOver" to type.onMouseOver,
                "onMouseLeave" to type.onMouseLeave,
                "onVarTransmit" to type.onVarTransmit,
                "onInvTransmit" to type.onInvTransmit,
                "onStatTransmit" to type.onStatTransmit,
                "onTimer" to type.onTimer,
                "onOp" to type.onOp,
                "onClick" to type.onClick,
                "onScrollWheel" to type.onScrollWheel,
            )
        val set = hooks.filterValues { it != null }.keys
        assertEquals(emptySet<String>(), set, "Expected a fully static component.")
    }

    private fun component(
        index: Int,
        init: ComponentTypeBuilder.() -> Unit,
    ): UnpackedComponentType {
        val builder = ComponentTypeBuilder("test_interface:com_$index")
        builder.v3 = true
        builder.apply(init)
        return builder.build(packed(index))
    }

    private fun packed(parentIndex: Int): Int = (INTERFACE_ID shl 16) or parentIndex

    private companion object {
        /** Matches the `.local` id band; upstream `interface.sym` tops out at 924. */
        private const val INTERFACE_ID = 1000
    }
}
