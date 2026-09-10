package org.rsmod.content.custom.teleports.configs

/**
 * Every pixel, colour, sprite and clientscript id behind interface `teleport_panel`, in one place.
 *
 * Each layout change costs a `./gradlew packCache` with the server stopped, so keeping the numbers
 * together makes an iteration a one-line edit. Every art and script id here was copied off a
 * vanilla interface that already renders it, rather than guessed; the source is named beside each
 * one.
 *
 * Public rather than internal because `TeleportPanelTest`, a separate compilation unit, checks the
 * script ids and slot count against the packed cache.
 */
object TeleportPanelLayout {
    /** A vanilla modal size (`thormac`, `nzone_rewards`), inside the fixed 512x334 modal layer. */
    const val WIDTH = 488
    const val HEIGHT = 320

    /**
     * Where the content area sits inside the steelborder frame. The frame's title bar spans y 6-30
     * and its steel edge is a few pixels thick; `ii_tracker` starts its content at y 35.
     */
    const val CONTENT_TOP = 36
    const val CONTENT_INSET_X = 8
    const val CONTENT_INSET_BOTTOM = 8
    const val CONTENT_WIDTH = WIDTH - (CONTENT_INSET_X * 2)

    /** The tab strip, copied from `ii_tracker` (interface 180): 150-wide tabs 20 high, from x 3. */
    const val TAB_HEIGHT = 20
    const val TAB_WIDTH = 150
    const val TAB_FIRST_X = 3

    /** Three-slice tab art from `ii_tracker`: left cap, tiled middle, right cap. */
    const val TAB_CAP_WIDTH = 20
    val TAB_SELECTED = TabSprites(left = 998, middle = 999, right = 1000)
    val TAB_UNSELECTED = TabSprites(left = 1001, middle = 1002, right = 1003)

    /** The destination grid: a well directly under the tabs holding [SLOT_COUNT] tiles. */
    const val GRID_TOP = TAB_HEIGHT
    const val GRID_COLUMNS = 3
    const val GRID_ROWS = 8
    const val SLOT_COUNT = GRID_COLUMNS * GRID_ROWS
    const val GRID_PAD_X = 7
    const val GRID_PAD_Y = 6
    const val TILE_GAP_X = 4
    const val TILE_GAP_Y = 4
    const val TILE_WIDTH = 150
    const val TILE_HEIGHT = 22
    const val GRID_HEIGHT =
        (GRID_PAD_Y * 2) + (GRID_ROWS * TILE_HEIGHT) + ((GRID_ROWS - 1) * TILE_GAP_Y)

    /** The Home / Previous row under the grid, aligned to the grid's first column. */
    const val FOOTER_TOP = GRID_TOP + GRID_HEIGHT + 8
    const val FOOTER_HEIGHT = 28
    const val HOME_X = GRID_PAD_X
    const val HOME_WIDTH = TILE_WIDTH
    const val PREVIOUS_X = HOME_X + HOME_WIDTH + TILE_GAP_X
    const val PREVIOUS_WIDTH = (TILE_WIDTH * 2) + TILE_GAP_X

    /**
     * The inset-box idiom from `slayer_rewards` (interface 426, `tasks_current_container`): a dark
     * outline, a faint white fill, and a lighter inner outline one pixel in.
     */
    const val COLOUR_OUTLINE = 0x0E0E0C
    const val COLOUR_INNER_OUTLINE = 0x474745
    const val COLOUR_FILL = 0xFFFFFF

    /** Fill transparency at rest and under the mouse. 255 is invisible, 0 opaque. */
    const val TILE_TRANS = 238
    const val TILE_TRANS_HOVER = 205

    /** The grid well is darker than the stone behind it, so the tiles read as raised. */
    const val COLOUR_WELL = 0x000000
    const val WELL_TRANS = 190

    /** Vanilla interface orange, and white for hover - `slayer_rewards:back_button`'s pair. */
    const val COLOUR_TEXT = 0xFF981F
    const val COLOUR_TEXT_HOVER = 0xFFFFFF

    /** Fonts, from `.data/symbols/font.sym`. */
    const val FONT_SMALL = 494 // p11_full: tile labels, as on vanilla buttons
    const val FONT_REGULAR = 495 // p12_full
    const val FONT_BOLD = 496 // b12_full: tab labels, as in `ii_tracker`

    /**
     * Clientscripts the components call, with the ids read off `.data/symbols/clientscript.sym`.
     * `TeleportPanelTest` asserts each id still names the script expected here.
     */
    const val SCRIPT_STEELBORDER = 227 // [clientscript,steelborder](component, title)
    const val SCRIPT_TEXT_COLOUR = 45 // [clientscript,text_colour_swapper](component, colour)
    const val SCRIPT_SETTRANS = 273 // [clientscript,settrans](component, trans)

    /** The `event_com` hook argument: the component the hook is attached to. */
    const val EVENT_COM = -2147483645

    /** Component type ids, matching the cache encoding. */
    const val TYPE_LAYER = 0
    const val TYPE_RECT = 3
    const val TYPE_TEXT = 4
    const val TYPE_GRAPHIC = 5

    /** Position modes: 0 absolute from top/left, 1 centred, 2 absolute from bottom/right. */
    const val POS_ABSOLUTE = 0
    const val POS_CENTRED = 1
    const val POS_FROM_END = 2

    /** Size modes: 0 absolute, 1 parent size minus the authored value. */
    const val SIZE_ABSOLUTE = 0
    const val SIZE_MINUS = 1

    const val ALIGN_CENTRE = 1

    /** `Op1` as the v1 mask the cache stores; see `FirstLoginLayout.EVENT_OP1`. */
    const val EVENT_OP1 = 2

    data class TabSprites(val left: Int, val middle: Int, val right: Int)
}
