package org.rsmod.content.skills.farming.data

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

/**
 * A player's patches as they come out of the database, before a [Player] exists to hang them on.
 */
public class CharacterFarmingData(public val patches: String) : CharacterDataStage.Segment

public class CharacterFarmingApplier @Inject constructor(private val registry: FarmingRegistry) :
    CharacterDataStage.Applier<CharacterFarmingData> {
    override fun apply(player: Player, data: CharacterFarmingData) {
        registry.put(player, PatchCodec.decode(data.patches))
    }
}

/**
 * Loads and saves farming state alongside the rest of the character.
 *
 * The migration that creates the table ships with this module under
 * `resources/plugin/farming/migration`, one of the classpath locations Flyway is pointed at.
 */
public class CharacterFarmingPipeline
@Inject
constructor(private val applier: CharacterFarmingApplier, private val registry: FarmingRegistry) :
    CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val select =
            connection.prepareStatement(
                """
                    SELECT patches
                    FROM player_farming_patches
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
                val data = CharacterFarmingData(resultSet.getString("patches") ?: "")
                metadata.add(applier, data)
            }
        }
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        // A player who has never touched a patch has nothing to write, and no row to create.
        if (!registry.contains(player)) {
            return
        }
        val encoded = PatchCodec.encode(registry[player])
        val upsert =
            connection.prepareStatement(
                """
                    INSERT INTO player_farming_patches (character_id, patches)
                    VALUES (?, ?)
                    ON CONFLICT(character_id) DO UPDATE SET
                        patches = excluded.patches,
                        updated_at = CURRENT_TIMESTAMP
                """
                    .trimIndent()
            )

        upsert.use {
            it.setInt(1, characterId)
            it.setString(2, encoded)
            it.executeUpdate()
        }
    }
}
