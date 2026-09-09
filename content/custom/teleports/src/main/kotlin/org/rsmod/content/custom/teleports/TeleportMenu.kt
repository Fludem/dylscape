package org.rsmod.content.custom.teleports

import org.rsmod.content.custom.teleports.configs.TeleportCategory
import org.rsmod.content.custom.teleports.configs.TeleportDestination
import org.rsmod.content.custom.teleports.configs.TeleportDestinations

/**
 * One row of a teleport menu.
 *
 * The rows are a type rather than a bare list of strings so that the labels drawn on screen and the
 * thing acted on when a row is picked come from the same list and cannot drift apart.
 */
sealed interface TeleportRow {
    val label: String

    /**
     * A row that teleports. [label] is separate from the destination's own so that the "Previous"
     * row can name itself differently.
     */
    data class Destination(val destination: TeleportDestination, override val label: String) :
        TeleportRow

    data class Category(val category: TeleportCategory) : TeleportRow {
        override val label: String
            get() = category.label
    }

    data object Back : TeleportRow {
        override val label: String
            get() = "Back"
    }

    data object Cancel : TeleportRow {
        override val label: String
            get() = "Cancel"
    }
}

/** Builds the rows for both levels of the Home Teleport picker. */
object TeleportMenu {
    const val TITLE: String = "Where would you like to teleport to?"

    /**
     * The category list, with "Home" always first and [previous] -- the last place this player
     * teleported to, if any -- offered as a one-click repeat.
     *
     * The conditional row deliberately sits *after* the fixed rows. `hotkeys = true` binds number
     * keys to row indices, so a row that appears part-way through a session would otherwise
     * reshuffle the hotkey of everything below it; this way "1 = Home, 2 = Cities" never moves, and
     * only "Cancel" shifts down by one.
     */
    fun topLevel(previous: TeleportDestination?): List<TeleportRow> = buildList {
        add(TeleportRow.Destination(TeleportDestinations.home, TeleportDestinations.home.label))
        TeleportCategory.entries.mapTo(this, TeleportRow::Category)
        if (previous != null) {
            add(TeleportRow.Destination(previous, "Previous: ${previous.label}"))
        }
        add(TeleportRow.Cancel)
    }

    /** One category's destinations, with a trailing "Back" to the category list. */
    fun destinations(category: TeleportCategory): List<TeleportRow> =
        TeleportDestinations[category].map { TeleportRow.Destination(it, it.label) } +
            TeleportRow.Back
}
