-- First-login setup: the account type and experience-rate tier a character picked.
--
-- Both are permanent once chosen, so they live in named columns rather than in the `varps` JSON
-- blob. That blob is keyed by varp *id*, and server-authored ids are resolved by name from the
-- symbol files at boot: an id we later shared with an upstream varp would silently rewrite every
-- character's irreversible choice. A column cannot alias, and needs no cache pack.
--
-- `xp_rate_tier` doubles as the "has chosen" flag: 0 means the chooser has not run yet. The tier
-- is authoritative and regenerates `xp_rate_in_hundreds` on load, so the two cannot drift.

ALTER TABLE characters
ADD COLUMN account_mode INTEGER NOT NULL DEFAULT 0;

ALTER TABLE characters
ADD COLUMN xp_rate_tier INTEGER NOT NULL DEFAULT 0;
