-- Per-player farming patches.
--
-- One row per character rather than one per patch: a player's patches are only ever read and
-- written whole, and eighty-odd rows per account would buy nothing. Untouched patches are not
-- stored at all.
--
--   patches := patch (';' patch)*
--   patch   := locId ',' seedId ',' plantedAt ',' clearedAt ',' livesUsed ',' picked ','
--              regrewAt ',' flags ',' compost
--
-- The timestamps are wall-clock epoch milliseconds, which is what lets crops carry on growing
-- while their owner is logged out.
CREATE TABLE IF NOT EXISTS player_farming_patches (
    character_id INTEGER PRIMARY KEY,
    patches      TEXT    NOT NULL DEFAULT '',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE
);
