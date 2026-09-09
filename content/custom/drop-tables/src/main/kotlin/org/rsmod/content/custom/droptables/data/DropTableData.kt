package org.rsmod.content.custom.droptables.data

/**
 * The wire format of the generated drop tables under `resources/.../droptables/data`.
 *
 * Every field is defaulted so a malformed row reaches [DropTableResourceLoader] as data it can
 * reject with a useful message, rather than blowing up inside Jackson. Names are snake_case in the
 * toml and camelCase here; `ObjectMapperProvider` does that translation.
 *
 * See `tools/drop-tables/generate.py`, which writes these files.
 */
internal data class DropTableIndex(val shard: List<String> = emptyList())

internal data class DropTableShard(val table: List<TomlDropTable> = emptyList())

internal data class TomlDropTable(
    val npc: List<String> = emptyList(),
    /** The wiki page these drops came from. Only used to make errors traceable. */
    val source: String = "",
    val always: List<TomlDrop> = emptyList(),
    val outOf: Int = 0,
    val drop: List<TomlSlot> = emptyList(),
    val tertiary: List<TomlTertiary> = emptyList(),
)

/**
 * One slot of a weighted table. A slot with no [obj] is an empty slot: the generator emits a single
 * one to pad a table out to its denominator, which is what `WeightedTableBuilder` requires.
 */
internal data class TomlSlot(
    val weight: Int = 0,
    val obj: String? = null,
    val count: Int = 1,
    val countMax: Int? = null,
    val noted: Boolean = false,
)

internal data class TomlDrop(
    val obj: String? = null,
    val count: Int = 1,
    val countMax: Int? = null,
    val noted: Boolean = false,
)

internal data class TomlTertiary(
    val oneIn: Int = 0,
    val obj: String? = null,
    val count: Int = 1,
    val countMax: Int? = null,
    val noted: Boolean = false,
)
