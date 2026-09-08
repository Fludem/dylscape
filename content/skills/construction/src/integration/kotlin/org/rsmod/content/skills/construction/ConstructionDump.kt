package org.rsmod.content.skills.construction

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.skills.construction.data.ConstructionTables
import org.rsmod.content.skills.construction.data.FurnitureLocs
import org.rsmod.content.skills.construction.house.PohTemplate
import org.rsmod.content.skills.construction.house.TemplateIndex

/**
 * Prints the construction data the skill is built on, for anyone re-deriving it after a cache
 * update. Nothing asserts; run it and read the output.
 *
 * The three things worth knowing, all recovered this way:
 * - **The db tables carry the whole skill.** `poh_room`, `poh_hotspot` and `furniture` hold every
 *   room, hotspot and piece of furniture, including costs, levels and materials. Only experience
 *   and the loc a piece of furniture becomes are missing.
 * - **`poh_room:source_offset` is a tile offset into the template map**, whose south-west corner is
 *   `PohTemplate.BASE_X, BASE_Z` -- 1856, 5696. A parlour's `(0, 56)` is the zone at 1856, 5752,
 *   and dumping that zone's locs shows `poh_parlour_1` through `_7`: the seven hotspots the room's
 *   `hotspot` column lists, in order.
 * - **House styles are stacked two ways.** Every 64 tiles east is a new group of four styles, and
 *   the four map levels within a group are four different themes. The door hotspot each template
 *   places names them: level 0 of the first column is Rimmington, level 1 Lumbridge, and so on
 *   through Canifis at level 3 of the third column.
 */
class ConstructionDump {
    @Test
    fun GameTestState.`dump rooms hotspots and furniture`() = runAdvancedGameTest { advanced ->
        val tables = ConstructionTables(cacheTypes.dbTables, cacheTypes.dbRows)
        val index =
            TemplateIndex(advanced.readOnly.locRegistry, cacheTypes.locs, tables).apply { index() }
        val furnitureLocs = FurnitureLocs(cacheTypes.locs, cacheTypes.objs, tables)

        for (room in tables.rooms.values.sortedBy { it.roomType }) {
            val zone = PohTemplate.templateZone(0, room.templateOffsetX, room.templateOffsetZ)
            val template = index.room(room.roomType)
            println(
                "ROOM ${room.roomType} ${room.internalName} '${room.displayName}' " +
                    "level=${room.levelRequirement} cost=${room.cost} " +
                    "offset=(${room.templateOffsetX},${room.templateOffsetZ}) " +
                    "zone=(${zone.x * 8},${zone.z * 8}) floor=${room.floorRestriction} " +
                    "doors=${template?.doors}"
            )
            for (slot in template?.slots.orEmpty()) {
                val hotspot = room.hotspots.getOrNull(slot)?.let(tables::hotspot)
                val pieces = template?.pieces(slot).orEmpty()
                println(
                    "  slot $slot ${hotspot?.internalName} " +
                        "locs=${pieces.map { cacheTypes.locs[it.locId]?.internalName }} " +
                        "variants=${pieces.map { it.variant }.distinct()}"
                )
                for (rowId in hotspot?.options.orEmpty()) {
                    val furniture = tables.furniture(rowId) ?: continue
                    val built = furnitureLocs.resolve(rowId, variant = null)
                    println(
                        "     ${furniture.internalName} '${furniture.name}' " +
                            "level=${furniture.levelRequirement} " +
                            "materials=${furniture.materials.map { "${it.obj}x${it.count}" }} " +
                            "-> ${built?.let { cacheTypes.locs[it]?.internalName } ?: "UNRESOLVED"}"
                    )
                }
            }
        }
    }

    @Test
    fun GameTestState.`dump house styles`() = runAdvancedGameTest {
        for (style in PohTemplate.styles) {
            val zone = PohTemplate.templateZone(style.id, offsetX = 0, offsetZ = 56)
            println(
                "STYLE ${style.id} '${style.displayName}' doors=poh_hotspot_doorl_" +
                    "${style.doorSuffix} parlourZone=(${zone.x * 8},${zone.z * 8},${zone.level})"
            )
        }
    }
}
