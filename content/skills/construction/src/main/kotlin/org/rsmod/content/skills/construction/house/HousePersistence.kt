package org.rsmod.content.skills.construction.house

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

/**
 * Every house currently in memory, keyed by its owner.
 *
 * A house outlives the region it is built into -- the region is thrown away every time a room is
 * added -- so the stored grid lives here and is written back on save.
 */
@Singleton
public class HouseRegistry @Inject constructor() {
    private val houses = HashMap<Player, PlayerOwnedHouse>()

    public operator fun get(player: Player): PlayerOwnedHouse? = houses[player]

    public operator fun set(player: Player, house: PlayerOwnedHouse) {
        houses[player] = house
    }

    public fun remove(player: Player) {
        houses.remove(player)
    }

    /** The player's house, created with its starter garden the first time it is asked for. */
    public fun getOrCreate(player: Player, gardenRoomType: Int): PlayerOwnedHouse {
        houses[player]?.let {
            return it
        }
        val house = PlayerOwnedHouse().apply { placeStarterGarden(gardenRoomType) }
        houses[player] = house
        return house
    }
}

/** The house as it comes out of the database, before a [Player] exists to hang it on. */
public class CharacterHouseData(
    public val style: Int,
    public val location: Int,
    public val locked: Boolean,
    public val entrance: Int,
    public val rooms: String,
) : CharacterDataStage.Segment

public class CharacterHouseApplier @Inject constructor(private val registry: HouseRegistry) :
    CharacterDataStage.Applier<CharacterHouseData> {
    override fun apply(player: Player, data: CharacterHouseData) {
        registry[player] = HouseCodec.decode(data)
    }
}

public class CharacterHousePipeline
@Inject
constructor(private val applier: CharacterHouseApplier, private val registry: HouseRegistry) :
    CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val select =
            connection.prepareStatement(
                """
                    SELECT style, location, locked, entrance, rooms
                    FROM player_owned_houses
                    WHERE character_id = ?
                """
                    .trimIndent()
            )

        select.use {
            it.setInt(1, metadata.characterId)
            it.executeQuery().use { resultSet ->
                if (!resultSet.next()) {
                    return
                }
                val data =
                    CharacterHouseData(
                        style = resultSet.getInt("style"),
                        location = resultSet.getInt("location"),
                        locked = resultSet.getInt("locked") != 0,
                        entrance = resultSet.getInt("entrance"),
                        rooms = resultSet.getString("rooms") ?: "",
                    )
                metadata.add(applier, data)
            }
        }
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        val house = registry[player] ?: return
        val upsert =
            connection.prepareStatement(
                """
                    INSERT INTO player_owned_houses
                        (character_id, style, location, locked, entrance, rooms)
                    VALUES (?, ?, ?, ?, ?, ?)
                    ON CONFLICT(character_id) DO UPDATE SET
                        style = excluded.style,
                        location = excluded.location,
                        locked = excluded.locked,
                        entrance = excluded.entrance,
                        rooms = excluded.rooms,
                        updated_at = CURRENT_TIMESTAMP
                """
                    .trimIndent()
            )

        upsert.use {
            it.setInt(1, characterId)
            it.setInt(2, house.styleId)
            it.setInt(3, house.locationId)
            it.setInt(4, if (house.locked) 1 else 0)
            it.setInt(5, house.entrance?.packed ?: -1)
            it.setString(6, HouseCodec.encodeRooms(house))
            it.executeUpdate()
        }
    }
}

/**
 * Reads and writes the house grid as text.
 *
 * ```
 * 1,6,6,2,0;1,6,7,1,0,0:5517,3:5524
 * ```
 *
 * is a garden in the middle of the ground floor and a parlour north of it with a crude wooden chair
 * in its first hotspot and a brown rug in its fourth. Malformed entries are skipped rather than
 * thrown on: a house that has lost a room is recoverable, a player who cannot log in is not.
 */
public object HouseCodec {
    public fun decode(data: CharacterHouseData): PlayerOwnedHouse {
        val house =
            PlayerOwnedHouse(styleId = data.style, locationId = data.location, locked = data.locked)
        for (entry in data.rooms.split(ROOM_SEPARATOR)) {
            if (entry.isBlank()) continue
            val fields = entry.split(FIELD_SEPARATOR)
            if (fields.size < 5) continue
            val level = fields[0].toIntOrNull() ?: continue
            val gridX = fields[1].toIntOrNull() ?: continue
            val gridZ = fields[2].toIntOrNull() ?: continue
            val roomType = fields[3].toIntOrNull() ?: continue
            val rotation = fields[4].toIntOrNull() ?: continue
            val room = PlacedRoom(roomType, rotation and 0x3)
            for (index in 5 until fields.size) {
                val (slot, furniture) =
                    fields[index].split(SLOT_SEPARATOR).let { parts ->
                        if (parts.size != 2) return@let null
                        val slot = parts[0].toIntOrNull() ?: return@let null
                        val furniture = parts[1].toIntOrNull() ?: return@let null
                        slot to furniture
                    } ?: continue
                room.build(slot, furniture)
            }
            house.put(RoomKey(level, gridX, gridZ), room)
        }
        if (data.entrance >= 0) {
            house.restoreEntrance(RoomKey.unpack(data.entrance))
        }
        return house
    }

    public fun encodeRooms(house: PlayerOwnedHouse): String =
        house.placed().entries.joinToString(ROOM_SEPARATOR.toString()) { (key, room) ->
            buildString {
                append(key.level)
                append(FIELD_SEPARATOR)
                append(key.gridX)
                append(FIELD_SEPARATOR)
                append(key.gridZ)
                append(FIELD_SEPARATOR)
                append(room.roomType)
                append(FIELD_SEPARATOR)
                append(room.rotation)
                for ((slot, furniture) in room.allBuilt()) {
                    append(FIELD_SEPARATOR)
                    append(slot)
                    append(SLOT_SEPARATOR)
                    append(furniture)
                }
            }
        }

    private const val ROOM_SEPARATOR = ';'
    private const val FIELD_SEPARATOR = ','
    private const val SLOT_SEPARATOR = ':'
}
