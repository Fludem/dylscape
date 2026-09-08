-- Player-owned houses.
--
-- The grid itself is a single text column rather than a row per room: a house is only ever read and
-- written whole, and the format stays readable when someone goes looking in the database.
--
--   rooms   := room (';' room)*
--   room    := level ',' gridX ',' gridZ ',' roomType ',' rotation (',' slot ':' furnitureRow)*
CREATE TABLE IF NOT EXISTS player_owned_houses (
    character_id INTEGER PRIMARY KEY,
    style        INTEGER NOT NULL DEFAULT 0,
    location     INTEGER NOT NULL DEFAULT 0,
    locked       INTEGER NOT NULL DEFAULT 0,
    entrance     INTEGER NOT NULL DEFAULT -1,
    rooms        TEXT    NOT NULL DEFAULT '',
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters (id) ON DELETE CASCADE
);
