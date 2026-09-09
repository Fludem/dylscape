package org.rsmod.content.custom.teleports.configs

import org.rsmod.map.CoordGrid

/** The groups the destination picker offers, in the order their rows are drawn. */
enum class TeleportCategory(val label: String) {
    Cities("Cities"),
    Skilling("Skilling"),
    Dungeons("Dungeons & bosses"),
}

/**
 * One row of the destination picker.
 *
 * [key] is the stable identity: it is never displayed, and it is what [LastTeleportRegistry]
 * stores, so reordering or relabelling the table cannot silently repoint a remembered destination.
 * Labels are display text and are expected to be reworded; list indices are expected to move.
 *
 * [arrival] is a field rather than a string built at the call site so that entries which do not
 * read well in the default phrasing -- "Home" above all -- can carry their own wording without the
 * script special-casing them.
 */
data class TeleportDestination(
    val key: String,
    val label: String,
    val dest: CoordGrid,
    val arrival: String = "You teleport to $label.",
)

/**
 * The destination table behind the Home Teleport menu.
 *
 * Every coordinate here is taken from data rather than from memory of the live game. In order of
 * preference: the vetted `params.spell_telecoord` values in `api/spells/.../configs/SpellObjs.kt`,
 * the cache-derived patch coordinates in `content/skills/farming/.../data/FarmingPatches.kt`, and
 * otherwise a real loc placement read out of `.data/cache/game` (bank booths, furnaces, anvils,
 * ladders) with the destination set to a tile beside it. `TeleportDestinationsTest` then checks
 * every one of them against the game's own collision map.
 *
 * The 5-argument [CoordGrid] form is `(level, mapSquareX, mapSquareZ, localX, localZ)`; the
 * 3-argument form used here is `(x, z, level)`, which is what the loc dump reports directly.
 */
object TeleportDestinations {
    /** A category's rows plus the trailing "Back", against `menu`'s hard cap of 127. */
    private const val MAX_CATEGORY_SIZE = 126

    private val KEY_PATTERN = Regex("^[a-z0-9_]+$")

    /**
     * The tile the "Home" row lands on.
     *
     * Edgeville bank, standing west of the booths at (3095, 3489) and (3095, 3491). This is a
     * placeholder for a purpose-built home area -- repointing Home is a one-line edit here, and
     * nothing else in the module refers to the coordinate.
     */
    val HOME: CoordGrid = CoordGrid(3094, 3491, 0)

    /** The top-level "Home" row. Deliberately in no category. */
    val home: TeleportDestination = TeleportDestination("home", "Home", HOME, "You teleport home.")

    private val byCategory: Map<TeleportCategory, List<TeleportDestination>> =
        mapOf(
            TeleportCategory.Cities to TeleportTable.cities,
            TeleportCategory.Skilling to TeleportTable.skilling,
            TeleportCategory.Dungeons to TeleportTable.dungeons,
        )

    val all: List<TeleportDestination> = byCategory.values.flatten() + home

    private val byKey: Map<String, TeleportDestination> = all.associateBy(TeleportDestination::key)

    init {
        for (destination in all) {
            require(destination.label.isNotBlank()) { "Destination labels must not be blank." }
            require(destination.key.isNotBlank()) { "Destination keys must not be blank." }
            require(KEY_PATTERN.matches(destination.key)) {
                "Destination key is not lowercase snake case: ${destination.key}"
            }
            require(destination.arrival.isNotBlank()) { "Arrival messages must not be blank." }
        }
        // Global, not per category: the key is what a remembered destination is looked up by.
        require(byKey.size == all.size) {
            "Duplicate destination key: ${all.map(TeleportDestination::key).duplicates()}"
        }
        val coords = all.map(TeleportDestination::dest)
        require(coords.size == coords.toSet().size) {
            "Two destinations share a coordinate: ${coords.duplicates()}"
        }
        for (category in TeleportCategory.entries) {
            val destinations = byCategory[category]
            require(!destinations.isNullOrEmpty()) { "Category $category has no entries." }
            val labels = destinations.map(TeleportDestination::label)
            require(labels.size == labels.toSet().size) {
                "Duplicate destination label in $category: ${labels.duplicates()}"
            }
            // The category menu is its destinations plus a trailing "Back", and `menu` refuses
            // more than 127 choices.
            require(destinations.size <= MAX_CATEGORY_SIZE) {
                "Category $category has ${destinations.size} entries, over the menu cap."
            }
            require(home !in destinations) { "Home must not sit inside category $category." }
        }
    }

    operator fun get(category: TeleportCategory): List<TeleportDestination> =
        byCategory[category] ?: emptyList()

    operator fun get(key: String): TeleportDestination? = byKey[key]

    private fun <T> List<T>.duplicates(): List<T> =
        groupBy { it }.filterValues { it.size > 1 }.keys.toList()
}
