package org.rsmod.api.type.builders.comp

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.rsmod.game.type.comp.ComponentTypeBuilder

/**
 * A [ComponentBuilder] whose components come from a design file drawn in
 * `tools/interface-designer`, instead of from Kotlin.
 *
 * The design file (`<interface>.interface.json`) is the single source of truth for the layout, so
 * what was drawn is exactly what gets packed. A concrete builder is one line, with the file in the
 * same package's resources:
 * ```
 * object BankTabsBuilder : DesignedComponentBuilder("bank_tabs.interface.json")
 * ```
 *
 * The file speaks in a deliberately small vocabulary: five component types, words for position and
 * size modes, and *semantic* hooks (`frame`, `hover`) instead of raw clientscript arrays.
 * Everything it can say is known to render in rev 233, and anything it cannot say fails here, at
 * boot, rather than packing something the client draws wrongly:
 * - `frame` becomes `onLoad = [steelborder, event_com, title]`, the frame 127 vanilla panels use.
 * - `hover` becomes an `onMouseOver`/`onMouseLeave` pair on one of three vanilla scripts, with the
 *   leave hook restoring the component's resting value.
 * - `ops` becomes both the op text and the `events` mask, so the two can never disagree.
 *
 * Array order in the file is child-index order. `tools/interface-designer/serve.py` rewrites the
 * interface's `.data/symbols/.local/component.sym` block on every save, and
 * [ComponentBuilderResolver] refuses to pack a component whose declared position disagrees with its
 * symbol, so a reorder cannot silently pack parents onto the wrong components.
 */
public abstract class DesignedComponentBuilder(resource: String) : ComponentBuilder() {
    /** The interface's symbol name, e.g. `teleport_panel`. */
    public val interfaceName: String

    /**
     * Every component's name, at the index of its child id. `tools/interface-mockup` reflects on
     * this, exactly as it does on `TeleportPanelBuilder.componentNames`.
     */
    public val componentNames: List<String>

    init {
        val stream =
            checkNotNull(javaClass.getResourceAsStream(resource)) {
                "Design `$resource` not found beside ${javaClass.name} in its resources."
            }
        val design =
            stream.use { InterfaceDesigns.parse(it.readBytes().decodeToString(), resource) }
        interfaceName = design.interfaceName
        componentNames = design.components.map(DesignComponent::name)
        author(design)
    }

    private fun author(design: InterfaceDesign) {
        val indices = componentNames.withIndex().associate { it.value to it.index }
        for ((index, component) in design.components.withIndex()) {
            val internal = "${design.interfaceName}:${component.name}"
            if (index == 0) {
                build(internal) { apply(component) }
            } else {
                val parent = indices.getValue(checkNotNull(component.parent))
                child(internal, parent) { apply(component) }
            }
        }
    }

    private fun ComponentTypeBuilder.apply(c: DesignComponent) {
        type = InterfaceDesigns.TYPES.getValue(c.type)
        x = c.x
        y = c.y
        width = c.w
        height = c.h
        xMode = InterfaceDesigns.POS_MODES.getValue(c.xMode)
        yMode = InterfaceDesigns.POS_MODES.getValue(c.yMode)
        widthMode = InterfaceDesigns.SIZE_MODES.getValue(c.wMode)
        heightMode = InterfaceDesigns.SIZE_MODES.getValue(c.hMode)
        hide = c.hidden
        noClickThrough = !c.clickThrough

        if (c.frame != null) {
            onLoad = arrayOf(SCRIPT_STEELBORDER, EVENT_COM, c.frame)
        }

        when (c.type) {
            "rect" -> {
                colour1 = InterfaceDesigns.colour(c.colour)
                fill = c.filled
                trans1 = c.trans
            }
            "text" -> {
                text = c.text
                textFont = c.font
                colour1 = InterfaceDesigns.colour(c.colour)
                textShadow = c.shadow
                textAlignH = InterfaceDesigns.ALIGN_H.getValue(c.alignH)
                textAlignV = InterfaceDesigns.ALIGN_V.getValue(c.alignV)
                textLineHeight = c.lineHeight
            }
            "graphic" -> {
                graphic = c.sprite
                tiling = c.tiling
                trans1 = c.trans
            }
        }

        val hover = c.hover
        if (hover != null) {
            val (script, over, rest) =
                when {
                    hover.colour != null ->
                        Triple(
                            SCRIPT_TEXT_COLOUR,
                            InterfaceDesigns.colour(hover.colour),
                            InterfaceDesigns.colour(c.colour),
                        )
                    hover.trans != null -> Triple(SCRIPT_SETTRANS, hover.trans, c.trans)
                    else -> Triple(SCRIPT_GRAPHIC_SWAPPER, checkNotNull(hover.sprite), c.sprite)
                }
            onMouseOver = arrayOf(script, EVENT_COM, over)
            onMouseLeave = arrayOf(script, EVENT_COM, rest)
        }

        op = c.ops.toTypedArray()
        events = InterfaceDesigns.eventsFor(c.ops)
        opBase = c.opBase
    }

    public companion object {
        /**
         * `[clientscript,steelborder](component, title)`: stone panel, steel edges, close button.
         */
        public const val SCRIPT_STEELBORDER: Int = 227

        /** `[clientscript,text_colour_swapper](component, colour)`. */
        public const val SCRIPT_TEXT_COLOUR: Int = 45

        /** `[clientscript,settrans](component, trans)`. */
        public const val SCRIPT_SETTRANS: Int = 273

        /** `[clientscript,graphic_swapper](component, graphic)`. */
        public const val SCRIPT_GRAPHIC_SWAPPER: Int = 44

        /** The `event_com` hook argument: the component the hook is attached to. */
        public const val EVENT_COM: Int = -2147483645
    }
}

/**
 * Parsing and validation of design files, kept apart from [DesignedComponentBuilder] so they can be
 * tested on strings.
 */
internal object InterfaceDesigns {
    const val FORMAT: Int = 1

    /**
     * `item` is an empty model component (type 6). Nothing is authored into it: the server fills it
     * at runtime with `ifSetObj`, the way the vanilla guide-prices panel shows its items.
     */
    val TYPES: Map<String, Int> =
        mapOf("layer" to 0, "rect" to 3, "text" to 4, "graphic" to 5, "item" to 6)
    val POS_MODES: Map<String, Int> = mapOf("start" to 0, "centre" to 1, "end" to 2)
    val SIZE_MODES: Map<String, Int> = mapOf("fixed" to 0, "minus" to 1)
    val ALIGN_H: Map<String, Int> = mapOf("left" to 0, "centre" to 1, "right" to 2)
    val ALIGN_V: Map<String, Int> = mapOf("top" to 0, "centre" to 1, "bottom" to 2)

    /** Ten op bits, `Op1`..`Op10`; see [ComponentBuilderResolver]'s `validateOps`. */
    private const val MAX_OPS: Int = 10

    private val NAME = Regex("[a-z0-9_]+")
    private val HEX = Regex("[0-9a-f]{6}")

    // A typo'd field must fail rather than be dropped, so unknown properties stay fatal.
    private val mapper: ObjectMapper =
        jacksonObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun parse(json: String, source: String): InterfaceDesign {
        val design =
            try {
                mapper.readValue<InterfaceDesign>(json)
            } catch (e: Exception) {
                throw IllegalArgumentException("Design `$source` is not valid: ${e.message}", e)
            }
        validate(design, source)
        return design
    }

    fun colour(hex: String): Int = hex.toInt(16)

    /** The v1 `events` mask the cache stores: bit `n + 1` enables `Op(n + 1)`. */
    fun eventsFor(ops: List<String>): Int =
        ops.withIndex()
            .filter { it.value.isNotEmpty() }
            .fold(0) { mask, op -> mask or (1 shl (op.index + 1)) }

    private fun validate(design: InterfaceDesign, source: String) {
        fun fail(message: String): Nothing =
            throw IllegalArgumentException("Design `$source`: $message")

        if (design.format != FORMAT) fail("format ${design.format} is not $FORMAT.")
        if (!NAME.matches(design.interfaceName))
            fail("interface `${design.interfaceName}` is not [a-z0-9_].")
        if (design.components.isEmpty()) fail("has no components.")

        val seen = mutableSetOf<String>()
        for ((index, c) in design.components.withIndex()) {
            val where = "component `${c.name}` (index $index)"
            if (!NAME.matches(c.name)) fail("$where: name is not [a-z0-9_].")
            if (!seen.add(c.name)) fail("$where: name is used twice.")
            if (index == 0 && c.parent != null) fail("$where: the root must have no parent.")
            if (index > 0 && c.parent == null)
                fail("$where: only the first component may be a root.")
            // Parents first keeps the file readable top-down and matches every hand-written
            // builder.
            if (c.parent != null && c.parent !in seen) {
                fail("$where: parent `${c.parent}` must be declared above it.")
            }
            if (c.type !in TYPES) fail("$where: unknown type `${c.type}`.")
            if (c.xMode !in POS_MODES || c.yMode !in POS_MODES)
                fail("$where: unknown position mode.")
            if (c.wMode !in SIZE_MODES || c.hMode !in SIZE_MODES) fail("$where: unknown size mode.")
            if (c.alignH !in ALIGN_H || c.alignV !in ALIGN_V)
                fail("$where: unknown text alignment.")
            if (!HEX.matches(c.colour)) fail("$where: colour `${c.colour}` is not six hex digits.")
            if (c.trans !in 0..255) fail("$where: trans ${c.trans} is not 0..255.")
            if (c.frame != null && c.type != "layer")
                fail("$where: only a layer can carry a frame.")
            if (c.type == "text" && c.font < 0) fail("$where: text needs a font.")
            if (c.type == "graphic" && c.sprite < 0) fail("$where: a graphic needs a sprite.")
            if (c.ops.size > MAX_OPS) fail("$where: at most $MAX_OPS ops.")
            c.hover?.let { validateHover(it, c, where, ::fail) }
        }
    }

    private fun validateHover(
        hover: DesignHover,
        c: DesignComponent,
        where: String,
        fail: (String) -> Nothing,
    ) {
        val set = listOfNotNull(hover.colour, hover.trans, hover.sprite)
        if (set.size != 1) fail("$where: hover must set exactly one of colour, trans or sprite.")
        // Each maps to one vanilla script; the others would run on a component they cannot change.
        when {
            hover.colour != null && c.type != "text" -> fail("$where: only text hovers a colour.")
            hover.trans != null && c.type != "rect" && c.type != "graphic" ->
                fail("$where: only a rect or graphic hovers a trans.")
            hover.sprite != null && c.type != "graphic" ->
                fail("$where: only a graphic hovers a sprite.")
        }
        if (hover.colour != null && !HEX.matches(hover.colour))
            fail("$where: hover colour is not hex.")
        if (hover.trans != null && hover.trans !in 0..255)
            fail("$where: hover trans is not 0..255.")
    }
}

/**
 * One design file. Field defaults here must match `DEFAULTS` in
 * `tools/interface-designer/serve.py`, which strips default values on save.
 */
internal data class InterfaceDesign(
    val format: Int,
    @JsonProperty("interface") val interfaceName: String,
    val notes: String = "",
    val components: List<DesignComponent>,
)

internal data class DesignComponent(
    val name: String,
    val parent: String? = null,
    val type: String,
    val x: Int = 0,
    val y: Int = 0,
    val w: Int = 0,
    val h: Int = 0,
    // Explicit names: a one-letter lowercase prefix trips Jackson's getter-based naming.
    @JsonProperty("xMode") val xMode: String = "start",
    @JsonProperty("yMode") val yMode: String = "start",
    @JsonProperty("wMode") val wMode: String = "fixed",
    @JsonProperty("hMode") val hMode: String = "fixed",
    val hidden: Boolean = false,
    val clickThrough: Boolean = true,
    val frame: String? = null,
    val colour: String = "000000",
    val filled: Boolean = false,
    val trans: Int = 0,
    val sprite: Int = -1,
    val tiling: Boolean = false,
    val text: String = "",
    val font: Int = -1,
    val alignH: String = "left",
    val alignV: String = "top",
    val lineHeight: Int = 0,
    val shadow: Boolean = false,
    val hover: DesignHover? = null,
    val ops: List<String> = emptyList(),
    val opBase: String = "",
    /** Behaviour notes for whoever implements the design; never packed. */
    val notes: String = "",
)

internal data class DesignHover(
    val colour: String? = null,
    val trans: Int? = null,
    val sprite: Int? = null,
)
