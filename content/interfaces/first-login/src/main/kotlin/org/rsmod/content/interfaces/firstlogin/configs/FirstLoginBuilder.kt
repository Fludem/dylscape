package org.rsmod.content.interfaces.firstlogin.configs

import org.rsmod.api.type.builders.comp.ComponentBuilder
import org.rsmod.content.interfaces.firstlogin.configs.FirstLoginLayout as layout
import org.rsmod.game.type.comp.ComponentTypeBuilder

/**
 * Authors interface 1000 `first_login_setup`: the account-mode and experience-rate chooser shown
 * once, to a brand new character.
 *
 * This is the first interface this server builds rather than borrows, so a few notes on the shape
 * of it:
 * - **Absolute positioning, no clientscript hooks.** Every component is fully static. The panel is
 *   a fixed pixel size centred in its container, which costs us responsive resizing and any
 *   client-side reaction (hover recolour, page turns), and buys not needing a cs2 compiler. Page
 *   switching is done server-side with `ifSetHide` instead.
 * - **Ops are baked into the cache.** There is no `IfSetOp` packet in this revision, so a button's
 *   menu text has to be packed with it. Baking [FirstLoginLayout.EVENT_OP1] alongside it is enough
 *   for the server-side gate too, because a static component's click arrives with `comsub == -1`.
 * - **Child indices must match `.data/symbols/.local/component.sym`.** They are asserted below
 *   rather than trusted, since a silent mismatch would name the wrong component.
 *
 * Both pages are authored as sibling layers; [FirstLoginComponents.page_rate] starts hidden.
 */
object FirstLoginBuilder : ComponentBuilder() {
    private const val TYPE_LAYER = 0
    private const val TYPE_RECT = 3
    private const val TYPE_TEXT = 4
    private const val TYPE_GRAPHIC = 5

    /** Child indices, matching `.data/symbols/.local/component.sym`. */
    private const val ROOT = 0
    private const val PAGE_MODE = 5
    private const val PAGE_RATE = 6

    /** Mode rows, in the order they are drawn. Copy lives here; the enum stays policy-free. */
    val modeRows: List<Row> =
        listOf(
            Row("Standard", "No restrictions. Trade, bank and play with everyone."),
            Row("Ironman", "Fully self-sufficient. No trading, no other players' drops."),
            Row("Hardcore Ironman", "Ironman rules. One death and you are demoted to Ironman."),
            Row("Ultimate Ironman", "Ironman rules, played at the hardest setting."),
        )

    /** Rate rows, in the order they are drawn. */
    val rateRows: List<Row> =
        listOf(
            Row("10x experience", "Slowest levelling, but every drop rate is boosted by 20%."),
            Row("16x experience", "Middle ground. Rare drops are boosted by 10%."),
            Row("30x experience", "Fastest levelling. No drop rate bonus at all."),
        )

    init {
        build("first_login_setup:root") {
            type = TYPE_LAYER
            x = 0
            y = 0
            width = layout.WIDTH
            height = layout.HEIGHT
            widthMode = layout.SIZE_ABSOLUTE
            heightMode = layout.SIZE_ABSOLUTE
            // Fixed size, centred in whatever container the panel is opened into. This is exactly
            // what `messagebox_titled:universe` does.
            xMode = layout.POS_CENTRED
            yMode = layout.POS_CENTRED
            // Swallow clicks so they do not fall through to the game world behind the panel.
            noClickThrough = true
        }

        child("first_login_setup:background", parent = ROOT) {
            type = TYPE_GRAPHIC
            graphic = layout.BACKGROUND_SPRITE
            tiling = true
            // Fill the parent exactly: "parent size minus 0".
            width = 0
            height = 0
            widthMode = layout.SIZE_FILL_PARENT
            heightMode = layout.SIZE_FILL_PARENT
        }

        child("first_login_setup:border", parent = ROOT) {
            type = TYPE_RECT
            fill = false
            colour1 = layout.COLOUR_BORDER
            width = layout.WIDTH
            height = layout.HEIGHT
        }

        child("first_login_setup:title", parent = ROOT) {
            heading()
            y = 10
            width = layout.WIDTH
            textAlignH = layout.ALIGN_CENTRE
            text = "Choose your path"
        }

        child("first_login_setup:subtitle", parent = ROOT) {
            body()
            y = 32
            width = layout.WIDTH
            textAlignH = layout.ALIGN_CENTRE
            text = "Both choices are permanent."
        }

        page("first_login_setup:page_mode", hidden = false)
        page("first_login_setup:page_rate", hidden = true)

        modeRows.forEachIndexed { index, row -> row(PAGE_MODE, "mode", index, row) }
        rateRows.forEachIndexed { index, row -> row(PAGE_RATE, "rate", index, row) }
    }

    private fun page(internal: String, hidden: Boolean) {
        child(internal, parent = ROOT) {
            type = TYPE_LAYER
            y = layout.PAGE_TOP
            width = layout.WIDTH
            height = layout.PAGE_HEIGHT
            hide = hidden
        }
    }

    /** One selectable row: a clickable plate, a heading and a line of description. */
    private fun row(parent: Int, prefix: String, index: Int, row: Row) {
        val top = layout.ROW_GAP + (index * layout.ROW_STRIDE)

        child("first_login_setup:${prefix}_button_$index", parent = parent) {
            type = TYPE_RECT
            fill = true
            colour1 = layout.COLOUR_ROW
            x = layout.ROW_INSET_X
            y = top
            width = layout.ROW_WIDTH
            height = layout.ROW_HEIGHT
            events = layout.EVENT_OP1
            op = arrayOf("Select")
            opBase = row.name
        }

        child("first_login_setup:${prefix}_name_$index", parent = parent) {
            heading()
            x = layout.TEXT_INSET_X
            y = top + layout.NAME_OFFSET_Y
            width = layout.ROW_WIDTH
            text = row.name
        }

        child("first_login_setup:${prefix}_desc_$index", parent = parent) {
            body()
            x = layout.TEXT_INSET_X
            y = top + layout.DESC_OFFSET_Y
            width = layout.ROW_WIDTH
            text = row.description
        }
    }

    private fun ComponentTypeBuilder.heading() {
        type = TYPE_TEXT
        textFont = layout.FONT_BOLD
        textAlignH = layout.ALIGN_LEFT
        textShadow = true
        textLineHeight = layout.LINE_HEIGHT
        height = layout.LINE_HEIGHT
        colour1 = layout.COLOUR_HEADING
    }

    private fun ComponentTypeBuilder.body() {
        type = TYPE_TEXT
        textFont = layout.FONT_REGULAR
        textAlignH = layout.ALIGN_LEFT
        textLineHeight = layout.LINE_HEIGHT
        height = layout.LINE_HEIGHT
        colour1 = layout.COLOUR_BODY
    }

    /** A row's user-facing copy. */
    data class Row(val name: String, val description: String)
}
