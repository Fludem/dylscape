-- Per-player league task counters: progress towards the counted tasks (kill 25 goblins, mine 100
-- iron) that are not yet complete. Completed tasks are not stored here - each is one bit in the
-- vanilla `league_task_completed_<n>` varps, which are permanent and travel with the varp blob.
--
--   counters := entry (';' entry)*
--   entry   := taskId ':' count
--
-- One row per character, read and written whole; a player who has never advanced a counter has no
-- row at all.
CREATE TABLE IF NOT EXISTS player_league_tasks (
    character_id INTEGER PRIMARY KEY,
    counters     TEXT    NOT NULL DEFAULT '',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE
);
