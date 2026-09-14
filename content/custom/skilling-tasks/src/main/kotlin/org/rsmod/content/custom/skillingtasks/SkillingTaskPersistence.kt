package org.rsmod.content.custom.skillingtasks

import jakarta.inject.Inject
import org.rsmod.api.account.character.CharacterDataStage
import org.rsmod.api.account.character.CharacterMetadataList
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.game.entity.Player

/** A row of `player_skilling_tasks`, before a [Player] exists to hang it on. */
class CharacterSkillingTaskData(val tasks: PlayerTasks) : CharacterDataStage.Segment

class CharacterSkillingTaskApplier @Inject constructor(private val state: SkillingTaskState) :
    CharacterDataStage.Applier<CharacterSkillingTaskData> {
    override fun apply(player: Player, data: CharacterSkillingTaskData) {
        state.restore(player, data.tasks)
    }
}

/**
 * Loads and saves each player's task, progress, streak and points alongside the rest of the
 * character. The migration creating the table ships with this module under
 * `resources/plugin/skilling-tasks/migration`, one of the classpath locations Flyway scans.
 *
 * A task key the table no longer knows (a retired row) loads as no task; the streak and points are
 * kept.
 */
class CharacterSkillingTaskPipeline
@Inject
constructor(
    private val applier: CharacterSkillingTaskApplier,
    private val state: SkillingTaskState,
) : CharacterDataStage.Pipeline {
    override fun append(connection: DatabaseConnection, metadata: CharacterMetadataList) {
        val select =
            connection.prepareStatement(
                """
                    SELECT task_key, target, progress, streak, points, completed, last_task_key
                    FROM player_skilling_tasks
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
                val tasks = PlayerTasks()
                val key = resultSet.getString("task_key").orEmpty()
                if (SkillingTasks.find(key) != null) {
                    tasks.taskKey = key
                    tasks.target = resultSet.getInt("target")
                    tasks.progress = resultSet.getInt("progress")
                }
                tasks.streak = resultSet.getInt("streak")
                tasks.points = resultSet.getInt("points")
                tasks.completed = resultSet.getInt("completed")
                tasks.lastTaskKey =
                    resultSet.getString("last_task_key")?.takeIf { s -> s.isNotEmpty() }
                metadata.add(applier, CharacterSkillingTaskData(tasks))
            }
        }
    }

    override fun save(connection: DatabaseConnection, player: Player, characterId: Int) {
        // A player who has never spoken to the Taskmaster has nothing to write, and no row.
        val tasks = state.peek(player) ?: return
        val upsert =
            connection.prepareStatement(
                """
                    INSERT INTO player_skilling_tasks
                        (character_id, task_key, target, progress, streak, points, completed, last_task_key)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(character_id) DO UPDATE SET
                        task_key = excluded.task_key,
                        target = excluded.target,
                        progress = excluded.progress,
                        streak = excluded.streak,
                        points = excluded.points,
                        completed = excluded.completed,
                        last_task_key = excluded.last_task_key,
                        updated_at = CURRENT_TIMESTAMP
                """
                    .trimIndent()
            )

        upsert.use {
            it.setInt(1, characterId)
            it.setString(2, tasks.taskKey.orEmpty())
            it.setInt(3, tasks.target)
            it.setInt(4, tasks.progress)
            it.setInt(5, tasks.streak)
            it.setInt(6, tasks.points)
            it.setInt(7, tasks.completed)
            it.setString(8, tasks.lastTaskKey.orEmpty())
            it.executeUpdate()
        }
    }
}
