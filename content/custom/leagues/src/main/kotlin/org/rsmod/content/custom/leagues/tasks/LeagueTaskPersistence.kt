package org.rsmod.content.custom.leagues.tasks

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

/** The counters as they come out of the database, before a [Player] exists to hang them on. */
class CharacterLeagueTaskData(val counters: String) : CharacterDataStage.Segment

class CharacterLeagueTaskApplier @Inject constructor(private val progress: LeagueTaskProgress) :
    CharacterDataStage.Applier<CharacterLeagueTaskData> {
    override fun apply(player: Player, data: CharacterLeagueTaskData) {
        progress.restore(player, LeagueTaskCounterCodec.decode(data.counters))
    }
}

/**
 * Loads and saves the unfinished task counters alongside the rest of the character. Completed tasks
 * are not here: they are bits in `Perm` varps and travel with the varp blob.
 *
 * The migration creating the table ships with this module under
 * `resources/plugin/leagues/migration`, one of the classpath locations Flyway scans.
 */
class CharacterLeagueTaskPipeline
@Inject
constructor(
    private val applier: CharacterLeagueTaskApplier,
    private val progress: LeagueTaskProgress,
) : CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val select =
            connection.prepareStatement(
                """
                    SELECT counters
                    FROM player_league_tasks
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
                val data = CharacterLeagueTaskData(resultSet.getString("counters") ?: "")
                metadata.add(applier, data)
            }
        }
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        // A player who has never advanced a counted task has nothing to write, and no row.
        if (!progress.tracked(player)) {
            return
        }
        val encoded = LeagueTaskCounterCodec.encode(progress.snapshot(player))
        val upsert =
            connection.prepareStatement(
                """
                    INSERT INTO player_league_tasks (character_id, counters)
                    VALUES (?, ?)
                    ON CONFLICT(character_id) DO UPDATE SET
                        counters = excluded.counters,
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

/**
 * `id:count;id:count`, ids ascending. Malformed entries are skipped rather than failing a login.
 */
object LeagueTaskCounterCodec {
    fun encode(counters: Map<Int, Int>): String =
        counters.entries
            .filter { it.value > 0 }
            .sortedBy { it.key }
            .joinToString(";") { "${it.key}:${it.value}" }

    fun decode(encoded: String): Map<Int, Int> {
        if (encoded.isBlank()) {
            return emptyMap()
        }
        val counters = LinkedHashMap<Int, Int>()
        for (entry in encoded.split(';')) {
            val id = entry.substringBefore(':').toIntOrNull() ?: continue
            val count = entry.substringAfter(':', "").toIntOrNull() ?: continue
            if (count > 0) {
                counters[id] = count
            }
        }
        return counters
    }
}
