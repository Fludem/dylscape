-- The login message is the only player-facing place the server names itself. It was seeded by
-- V3 as the upstream RS Mod default; this realm is Dylscape. Guarded on the old default so a
-- message set by hand on a live realm is left alone.
UPDATE realms
SET login_message = 'Welcome to Dylscape.'
WHERE login_message = 'Welcome to RS Mod.';
