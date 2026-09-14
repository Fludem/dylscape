-- Per-player standing with the Edgeville Taskmaster: the current skilling task (a key into the
-- `SkillingTasks` table, empty for none) with its target and progress, the completion streak,
-- unspent skilling points, the lifetime completion count, and the previous task's key so the
-- same job is never handed out twice running.
--
-- One row per character, read and written whole; a player who has never spoken to the Taskmaster
-- has no row at all.
CREATE TABLE IF NOT EXISTS player_skilling_tasks (
    character_id  INTEGER PRIMARY KEY,
    task_key      TEXT    NOT NULL DEFAULT '',
    target        INTEGER NOT NULL DEFAULT 0,
    progress      INTEGER NOT NULL DEFAULT 0,
    streak        INTEGER NOT NULL DEFAULT 0,
    points        INTEGER NOT NULL DEFAULT 0,
    completed     INTEGER NOT NULL DEFAULT 0,
    last_task_key TEXT    NOT NULL DEFAULT '',
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE
);
