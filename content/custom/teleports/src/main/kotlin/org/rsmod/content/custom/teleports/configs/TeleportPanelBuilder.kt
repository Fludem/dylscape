package org.rsmod.content.custom.teleports.configs

import org.rsmod.api.type.builders.comp.ComponentBuilder
import org.rsmod.content.custom.teleports.configs.TeleportPanelLayout as layout
import org.rsmod.game.type.comp.ComponentTypeBuilder

/**
 * Authors interface 1001 `teleport_panel`: the Home Teleport destination picker.
 *
 * Unlike `first_login_setup`, which draws its own flat panel, this one is dressed entirely in
 * vanilla parts, each copied off an interface the client already renders:
 * - **The frame** is `[clientscript,steelborder]` run from an `onLoad` hook, exactly as 127 vanilla
 *   panels do. It draws the stone background, the steel edges, the title and a working close
 *   button, so none of that is authored here and closing needs no server code.
 * - **The tabs** are the three-slice tab art of `ii_tracker` (interface 180). Each tab carries both
 *   its selected and unselected art; the server swaps them with `ifSetHide`.
 * - **The tiles** are the inset box of `slayer_rewards`, with the hover of `slayer_rewards`'
 *   buttons: the label turns white and the fill brightens, both run client-side by the existing
 *   `text_colour_swapper` and `settrans` scripts, so hovering costs no packets.
 *
 * The destination tiles are generic slots. Their labels are pushed with `ifSetText` when a tab is
 * shown, and unused slots are hidden, so adding a destination to [TeleportTable] needs no repack as
 * long as its category still fits in [TeleportPanelLayout.SLOT_COUNT].
 *
 * Child indices are not written out by hand: each component's index is its position in
 * [componentNames], parents are referred to by name, and `.data/symbols/.local/component.sym` lists
 * the same names in the same order. `TeleportPanelTest` checks the two agree.
 */
object TeleportPanelBuilder : ComponentBuilder() {
    const val INTERFACE: String = "teleport_panel"
    const val TITLE: String = "Teleports"

    private val names = mutableListOf<String>()

    /** Every component's name, at the index of its child id. */
    val componentNames: List<String>
        get() = names

    init {
        val categories = TeleportCategory.entries
        check(layout.TAB_FIRST_X + (categories.size * layout.TAB_WIDTH) <= layout.CONTENT_WIDTH) {
            "${categories.size} tabs do not fit across the panel."
        }

        root()
        frame()

        add("contents", parent = "root") {
            type = layout.TYPE_LAYER
            y = layout.CONTENT_TOP
            xMode = layout.POS_CENTRED
            width = layout.CONTENT_INSET_X * 2
            widthMode = layout.SIZE_MINUS
            height = layout.CONTENT_TOP + layout.CONTENT_INSET_BOTTOM
            heightMode = layout.SIZE_MINUS
        }

        add("tabs", parent = "contents") {
            type = layout.TYPE_LAYER
            fillWidth()
            height = layout.TAB_HEIGHT
        }
        categories.forEachIndexed { index, category -> tab(index, category.label) }

        grid()
        repeat(layout.SLOT_COUNT) { slot(it) }

        footerButton(
            name = "home",
            x = layout.HOME_X,
            width = layout.HOME_WIDTH,
            label = "Home",
            target = "Home",
            hidden = false,
        )
        // Shown only once the player has teleported this session; the label is pushed at runtime.
        footerButton(
            name = "previous",
            x = layout.PREVIOUS_X,
            width = layout.PREVIOUS_WIDTH,
            label = "",
            target = "",
            hidden = true,
        )
    }

    private fun root() {
        names += "root"
        build("$INTERFACE:root") {
            type = layout.TYPE_LAYER
            width = layout.WIDTH
            height = layout.HEIGHT
            widthMode = layout.SIZE_ABSOLUTE
            heightMode = layout.SIZE_ABSOLUTE
            // Fixed size, centred in the modal layer - how `slayer_rewards:universe` sits.
            xMode = layout.POS_CENTRED
            yMode = layout.POS_CENTRED
            noClickThrough = true
        }
    }

    private fun frame() {
        add("frame", parent = "root") {
            type = layout.TYPE_LAYER
            fillParent()
            onLoad = arrayOf(layout.SCRIPT_STEELBORDER, layout.EVENT_COM, TITLE)
        }
    }

    /**
     * One tab: a clickable layer holding both art states and a label. The first tab is authored
     * selected, so the panel is right even before the server has said anything.
     */
    private fun tab(index: Int, label: String) {
        val tab = "tab_$index"
        val selected = index == 0

        add(tab, parent = "tabs") {
            type = layout.TYPE_LAYER
            x = layout.TAB_FIRST_X + (index * layout.TAB_WIDTH)
            width = layout.TAB_WIDTH
            height = 0
            heightMode = layout.SIZE_MINUS
            events = layout.EVENT_OP1
            op = arrayOf("View")
            opBase = target(label)
        }
        tabArt("${tab}_on", parent = tab, layout.TAB_SELECTED, hidden = !selected)
        tabArt("${tab}_off", parent = tab, layout.TAB_UNSELECTED, hidden = selected)
        add("${tab}_label", parent = tab) {
            label(layout.FONT_BOLD, label)
            fillParent()
        }
    }

    private fun tabArt(name: String, parent: String, sprites: layout.TabSprites, hidden: Boolean) {
        add(name, parent = parent) {
            type = layout.TYPE_LAYER
            fillParent()
            hide = hidden
        }
        add("${name}_left", parent = name) {
            type = layout.TYPE_GRAPHIC
            graphic = sprites.left
            width = layout.TAB_CAP_WIDTH
            height = layout.TAB_HEIGHT
        }
        add("${name}_middle", parent = name) {
            type = layout.TYPE_GRAPHIC
            graphic = sprites.middle
            tiling = true
            xMode = layout.POS_CENTRED
            width = layout.TAB_CAP_WIDTH * 2
            widthMode = layout.SIZE_MINUS
            height = layout.TAB_HEIGHT
        }
        add("${name}_right", parent = name) {
            type = layout.TYPE_GRAPHIC
            graphic = sprites.right
            xMode = layout.POS_FROM_END
            width = layout.TAB_CAP_WIDTH
            height = layout.TAB_HEIGHT
        }
    }

    /** The well the destination tiles sit in, directly under the tab strip. */
    private fun grid() {
        add("grid", parent = "contents") {
            type = layout.TYPE_LAYER
            y = layout.GRID_TOP
            fillWidth()
            height = layout.GRID_HEIGHT
        }
        add("grid_well", parent = "grid") {
            rect(layout.COLOUR_WELL, filled = true)
            trans1 = layout.WELL_TRANS
            fillParent()
        }
        add("grid_outline", parent = "grid") {
            rect(layout.COLOUR_OUTLINE, filled = false)
            fillParent()
        }
        add("grid_inner", parent = "grid") {
            rect(layout.COLOUR_INNER_OUTLINE, filled = false)
            insetOne()
        }
    }

    /** One destination tile, laid out row-major so a part-filled page leaves its last row short. */
    private fun slot(index: Int) {
        val column = index % layout.GRID_COLUMNS
        val row = index / layout.GRID_COLUMNS
        val slot = "slot_$index"
        add(slot, parent = "grid") {
            type = layout.TYPE_LAYER
            x = layout.GRID_PAD_X + (column * (layout.TILE_WIDTH + layout.TILE_GAP_X))
            y = layout.GRID_PAD_Y + (row * (layout.TILE_HEIGHT + layout.TILE_GAP_Y))
            width = layout.TILE_WIDTH
            height = layout.TILE_HEIGHT
            events = layout.EVENT_OP1
            op = arrayOf("Teleport")
            // Every slot starts hidden; the server reveals as many as the shown tab needs.
            hide = true
        }
        insetBox(slot, layout.FONT_SMALL, label = "")
    }

    private fun footerButton(
        name: String,
        x: Int,
        width: Int,
        label: String,
        target: String,
        hidden: Boolean,
    ) {
        add(name, parent = "contents") {
            type = layout.TYPE_LAYER
            this.x = x
            y = layout.FOOTER_TOP
            this.width = width
            height = layout.FOOTER_HEIGHT
            events = layout.EVENT_OP1
            op = arrayOf("Teleport")
            opBase = if (target.isEmpty()) "" else target(target)
            hide = hidden
        }
        insetBox(name, layout.FONT_BOLD, label)
    }

    /**
     * The `slayer_rewards` inset box inside [parent]: outline, faint fill, inner outline, label.
     *
     * The fill and the label each carry their own hover hook aimed at themselves (`event_com`), so
     * no hook argument needs a packed component id that only the symbol table knows.
     */
    private fun insetBox(parent: String, font: Int, label: String) {
        add("${parent}_outline", parent = parent) {
            rect(layout.COLOUR_OUTLINE, filled = false)
            fillParent()
        }
        add("${parent}_fill", parent = parent) {
            rect(layout.COLOUR_FILL, filled = true)
            trans1 = layout.TILE_TRANS
            insetOne()
            onMouseOver = arrayOf(layout.SCRIPT_SETTRANS, layout.EVENT_COM, layout.TILE_TRANS_HOVER)
            onMouseLeave = arrayOf(layout.SCRIPT_SETTRANS, layout.EVENT_COM, layout.TILE_TRANS)
        }
        add("${parent}_inner", parent = parent) {
            rect(layout.COLOUR_INNER_OUTLINE, filled = false)
            insetOne()
        }
        add("${parent}_label", parent = parent) {
            label(font, label)
            fillParent()
        }
    }

    private fun add(name: String, parent: String, init: ComponentTypeBuilder.() -> Unit) {
        val parentIndex = names.indexOf(parent)
        check(parentIndex >= 0) { "Parent `$parent` must be declared before `$name`." }
        check(name !in names) { "Component `$name` declared twice." }
        names += name
        child("$INTERFACE:$name", parent = parentIndex, init)
    }

    private fun ComponentTypeBuilder.label(font: Int, label: String) {
        type = layout.TYPE_TEXT
        textFont = font
        textAlignH = layout.ALIGN_CENTRE
        textAlignV = layout.ALIGN_CENTRE
        textShadow = true
        colour1 = layout.COLOUR_TEXT
        text = label
        onMouseOver = arrayOf(layout.SCRIPT_TEXT_COLOUR, layout.EVENT_COM, layout.COLOUR_TEXT_HOVER)
        onMouseLeave = arrayOf(layout.SCRIPT_TEXT_COLOUR, layout.EVENT_COM, layout.COLOUR_TEXT)
    }

    private fun ComponentTypeBuilder.rect(colour: Int, filled: Boolean) {
        type = layout.TYPE_RECT
        colour1 = colour
        fill = filled
    }

    private fun ComponentTypeBuilder.fillParent() {
        width = 0
        height = 0
        widthMode = layout.SIZE_MINUS
        heightMode = layout.SIZE_MINUS
    }

    private fun ComponentTypeBuilder.fillWidth() {
        width = 0
        widthMode = layout.SIZE_MINUS
    }

    /** One pixel in from every edge of the parent. */
    private fun ComponentTypeBuilder.insetOne() {
        x = 1
        y = 1
        width = 2
        height = 2
        widthMode = layout.SIZE_MINUS
        heightMode = layout.SIZE_MINUS
    }

    /** Menu target text in vanilla interface orange, e.g. "View <col=ff9040>Cities</col>". */
    private fun target(text: String): String = "<col=ff9040>$text</col>"
}
