package org.rsmod.content.interfaces.firstlogin.configs

/**
 * Every pixel and colour constant for interface `first_login_setup`, in one place.
 *
 * Each layout change costs a full `./gradlew packCache` plus a client restart, so keeping the
 * numbers together makes an iteration a one-line edit rather than a hunt through the builder.
 */
internal object FirstLoginLayout {
    const val WIDTH = 500
    const val HEIGHT = 340

    /** Where the two pages start, below the title block. */
    const val PAGE_TOP = 56
    const val PAGE_HEIGHT = HEIGHT - PAGE_TOP

    const val ROW_HEIGHT = 52
    const val ROW_GAP = 8
    const val ROW_STRIDE = ROW_HEIGHT + ROW_GAP
    const val ROW_INSET_X = 24
    const val ROW_WIDTH = WIDTH - (ROW_INSET_X * 2)

    const val TEXT_INSET_X = ROW_INSET_X + 16
    const val NAME_OFFSET_Y = 8
    const val DESC_OFFSET_Y = 28
    const val LINE_HEIGHT = 16

    /**
     * The tiled parchment background `ironman_setup` draws its info pane on. Tiling a 2x2 sprite
     * across a fill-parent component is the vanilla idiom for a panel backdrop.
     */
    const val BACKGROUND_SPRITE = 1040

    /** `b12_full` - bold, for headings. */
    const val FONT_BOLD = 496

    /** `p12_full` - regular body copy. */
    const val FONT_REGULAR = 495

    const val COLOUR_HEADING = 0xFF981F
    const val COLOUR_BODY = 0x000000
    const val COLOUR_ROW = 0x3A3226
    const val COLOUR_BORDER = 0x5A503C

    /** Text alignment, matching the cache's own encoding. */
    const val ALIGN_LEFT = 0
    const val ALIGN_CENTRE = 1

    /** Position mode: 0 absolute, 1 centred in parent. */
    const val POS_ABSOLUTE = 0
    const val POS_CENTRED = 1

    /** Size mode: 0 absolute, 1 fill parent minus the authored value. */
    const val SIZE_ABSOLUTE = 0
    const val SIZE_FILL_PARENT = 1

    /**
     * `Op1` as the v1 mask the cache stores. `IfEvent.Op1` lives at bit 32 at runtime and is folded
     * back down to bit 1 by `UnpackedComponentType.hasEvent`.
     */
    const val EVENT_OP1 = 2
}
