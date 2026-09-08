package org.rsmod.content.custom.teleports.configs

import org.rsmod.map.CoordGrid

enum class TeleportCategory(val label: String) {
    Cities("Cities"),
    Skilling("Skilling"),
}

data class TeleportDestination(
    val label: String,
    val category: TeleportCategory,
    val dest: CoordGrid,
)

/**
 * The destination table behind the Home Teleport menu.
 *
 * City coordinates are the ones already authored for the matching teleport spells in
 * `api/spells/.../configs/SpellObjs.kt` (via `params.spell_telecoord`), copied here rather than
 * invented. The skilling coordinates have no spell equivalent and are authored here.
 *
 * The 5-argument [CoordGrid] form is `(level, mapSquareX, mapSquareZ, localX, localZ)`, so the
 * absolute tile is `mapSquare * 64 + local`.
 */
object TeleportDestinations {
    val all: List<TeleportDestination> =
        listOf(
            // Coordinates match SpellObjs.kt.
            city("Lumbridge", CoordGrid(0, 50, 50, 21, 18)),
            city("Varrock", CoordGrid(0, 50, 53, 13, 32)),
            city("Falador", CoordGrid(0, 46, 52, 21, 50)),
            city("Camelot", CoordGrid(0, 43, 54, 5, 22)),
            city("Ardougne", CoordGrid(0, 41, 51, 37, 38)),
            // Surface spots only: an underground destination would also need its level checked
            // against the wilderness mirror band.
            skilling("Al Kharid Mine", CoordGrid(0, 51, 51, 36, 48)),
            skilling("Fishing Guild", CoordGrid(0, 40, 53, 39, 27)),
            skilling("Draynor Willows", CoordGrid(0, 48, 50, 15, 34)),
            skilling("Catherby Patch", CoordGrid(0, 43, 54, 57, 7)),
        )

    private val byCategory: Map<TeleportCategory, List<TeleportDestination>> =
        all.groupBy(TeleportDestination::category)

    init {
        require(all.none { it.label.isBlank() }) { "Destination labels must not be blank." }
        for ((category, destinations) in byCategory) {
            val labels = destinations.map(TeleportDestination::label)
            require(labels.size == labels.toSet().size) {
                "Duplicate destination label in $category: $labels"
            }
        }
        for (category in TeleportCategory.entries) {
            require(!byCategory[category].isNullOrEmpty()) { "Category $category has no entries." }
        }
    }

    operator fun get(category: TeleportCategory): List<TeleportDestination> =
        byCategory[category] ?: emptyList()

    private fun city(label: String, dest: CoordGrid) =
        TeleportDestination(label, TeleportCategory.Cities, dest)

    private fun skilling(label: String, dest: CoordGrid) =
        TeleportDestination(label, TeleportCategory.Skilling, dest)
}
