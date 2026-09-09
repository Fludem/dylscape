"""Read-only queries against the game's SQLite save file.

Everything the website knows about players comes from here. The file is the live database the game
server writes to, so every connection is opened read-only through a `file:...?mode=ro` URI and no
statement in this module writes. The server holds one long-lived WAL connection; WAL permits
concurrent readers, but a reader still needs write access to the `-shm` sidecar, which is why the
web service runs as the same OS user that owns `.data/saves/`.

Three facts about the schema drive nearly every query here, and each one silently produces
plausible-but-wrong numbers if forgotten:

- `stats.fine_xp` is experience times ten (`PlayerStatMap.XP_FINE_PRECISION`).
- Stat ids 23 (`sailing`) and 24 (`unreleased`) are written on every save but are not real skills,
  so any total must exclude them.
- `characters.realm_id` separates dev from production. The live realm is 2.
"""

from __future__ import annotations

import math
import os
import sqlite3
import time
from typing import Any

# `.data/symbols/stat.sym`. Index is the stat id; ids at or above REAL_STAT_COUNT are excluded from
# totals and never offered as a hiscore table.
STAT_NAMES = [
    "attack", "defence", "strength", "hitpoints", "ranged", "prayer", "magic", "cooking",
    "woodcutting", "fletching", "fishing", "firemaking", "crafting", "smithing", "mining",
    "herblore", "agility", "thieving", "slayer", "farming", "runecrafting", "hunter",
    "construction", "sailing", "unreleased",
]
REAL_STAT_COUNT = 23

STAT_IDS = {name: i for i, name in enumerate(STAT_NAMES)}
SKILLS = STAT_NAMES[:REAL_STAT_COUNT]

DISPLAY_NAMES = {"defence": "Defence", "hitpoints": "Hitpoints", "runecrafting": "Runecraft"}

# `AccountMode` in engine/game/.../player/AccountMode.kt. Ids double as cache varbit 1777 values.
ACCOUNT_MODES = {0: "standard", 1: "ironman", 2: "uim", 3: "hcim"}
ACCOUNT_MODE_LABELS = {
    "standard": "Standard",
    "ironman": "Ironman",
    "uim": "Ultimate Ironman",
    "hcim": "Hardcore Ironman",
}

# `XpRateTier` in engine/game/.../player/XpRateTier.kt. The id is the multiplier itself; 0 means the
# first-login chooser has not run yet.
XP_TIERS = {10: "10x", 16: "16x", 30: "30x"}

XP_PRECISION = 10
MAX_LEVEL_XP = 13_034_431  # xp at level 99


def skill_label(name: str) -> str:
    return DISPLAY_NAMES.get(name, name.capitalize())


def combat_level(levels: dict[str, int]) -> int:
    """Port of `CombatLevel.calculate` in api/utils/utils-skills.

    The halvings there are Kotlin `Int` divisions, so they truncate. Using true division here would
    put the site half a level above the client on odd prayer, ranged or magic levels.
    """
    attack = levels.get("attack", 1)
    defence = levels.get("defence", 1)
    strength = levels.get("strength", 1)
    hitpoints = levels.get("hitpoints", 10)
    ranged = levels.get("ranged", 1)
    magic = levels.get("magic", 1)
    prayer = levels.get("prayer", 1)

    base = 0.25 * (defence + hitpoints + (prayer // 2))
    melee = 0.325 * (attack + strength)
    ranged_c = 0.325 * (ranged + (ranged // 2))
    magic_c = 0.325 * (magic + (magic // 2))
    return int(math.floor(base + max(melee, ranged_c, magic_c)))


def level_progress(fine_xp: int) -> float:
    """Fraction of the way from the current level to the next, for the player-page bars."""
    xp = fine_xp // XP_PRECISION
    if xp >= MAX_LEVEL_XP:
        return 1.0
    level = level_for_xp(xp)
    if level >= 99:
        return 1.0
    start, end = xp_for_level(level), xp_for_level(level + 1)
    return (xp - start) / (end - start) if end > start else 0.0


def xp_for_level(level: int) -> int:
    """The standard RuneScape experience curve. Only used for progress bars; levels come from the
    database, which is authoritative."""
    total = 0.0
    for n in range(1, level):
        total += math.floor(n + 300 * (2 ** (n / 7.0)))
    return int(math.floor(total / 4))


_XP_TABLE = [xp_for_level(lvl) for lvl in range(1, 127)]


def level_for_xp(xp: int) -> int:
    level = 1
    for i, threshold in enumerate(_XP_TABLE, start=1):
        if xp >= threshold:
            level = i
        else:
            break
    return min(level, 99)


class Cache:
    """A tiny TTL cache. The database sits on the game server's disk, so it is worth not hammering
    it once a page with several panels is open in several browsers."""

    def __init__(self, ttl: float = 10.0) -> None:
        self.ttl = ttl
        self._entries: dict[Any, tuple[float, Any]] = {}

    def get(self, key: Any, produce):
        now = time.monotonic()
        hit = self._entries.get(key)
        if hit is not None and now - hit[0] < self.ttl:
            return hit[1]
        value = produce()
        self._entries[key] = (now, value)
        if len(self._entries) > 512:
            cutoff = now - self.ttl
            self._entries = {k: v for k, v in self._entries.items() if v[0] >= cutoff}
        return value


class GameDatabase:
    def __init__(self, path: str, realm_id: int, ttl: float = 10.0) -> None:
        self.path = path
        self.realm_id = realm_id
        self.cache = Cache(ttl)

    def _connect(self) -> sqlite3.Connection:
        uri = f"file:{os.path.abspath(self.path)}?mode=ro"
        conn = sqlite3.connect(uri, uri=True, timeout=5.0)
        conn.row_factory = sqlite3.Row
        return conn

    def available(self) -> bool:
        try:
            with self._connect() as conn:
                conn.execute("SELECT 1 FROM characters LIMIT 1")
            return True
        except sqlite3.Error:
            return False

    def _rows(self, sql: str, params: tuple = ()) -> list[sqlite3.Row]:
        conn = self._connect()
        try:
            return conn.execute(sql, params).fetchall()
        finally:
            conn.close()

    # -- shared shapes ---------------------------------------------------------------------------

    # `accounts.display_name` is nullable, so every name the site shows falls back to the login
    # name. A character with `last_logout` unset has never completed a logout, so it counts as
    # online too.
    _NAME = "COALESCE(a.display_name, a.login_username)"
    _ONLINE = "(c.last_logout IS NULL OR c.last_login > c.last_logout)"

    def _mode_clause(self, mode: str) -> tuple[str, tuple]:
        if mode in ("all", "", None):
            return "", ()
        wanted = [k for k, v in ACCOUNT_MODES.items() if v == mode]
        if not wanted:
            return "", ()
        return " AND c.account_mode = ?", (wanted[0],)

    # -- endpoints -------------------------------------------------------------------------------

    def status(self) -> dict:
        return self.cache.get(("status",), self._status)

    def _status(self) -> dict:
        row = self._rows(
            f"""
            SELECT COUNT(*) AS characters,
                   COUNT(DISTINCT c.account_id) AS accounts,
                   SUM(CASE WHEN {self._ONLINE} THEN 1 ELSE 0 END) AS online
            FROM characters c
            WHERE c.realm_id = ?
            """,
            (self.realm_id,),
        )[0]
        recent = self._rows(
            f"""
            SELECT {self._NAME} AS name, c.last_login AS last_login
            FROM characters c JOIN accounts a ON a.id = c.account_id
            WHERE c.realm_id = ? AND c.last_login IS NOT NULL
            ORDER BY c.last_login DESC LIMIT 5
            """,
            (self.realm_id,),
        )
        return {
            "characters": row["characters"] or 0,
            "accounts": row["accounts"] or 0,
            # Derived from timestamps, not from a live session list: an unclean server restart
            # leaves `last_login > last_logout` set, so this can over-report. The site says so.
            "online": row["online"] or 0,
            "online_is_estimate": True,
            "recent": [{"name": r["name"], "last_login": r["last_login"]} for r in recent],
        }

    def hiscores(self, skill: str, mode: str, limit: int, offset: int) -> dict:
        key = ("hiscores", skill, mode, limit, offset)
        return self.cache.get(key, lambda: self._hiscores(skill, mode, limit, offset))

    def _hiscores(self, skill: str, mode: str, limit: int, offset: int) -> dict:
        mode_sql, mode_params = self._mode_clause(mode)
        if skill == "overall":
            sql = f"""
                SELECT {self._NAME} AS name, c.account_mode AS account_mode,
                       c.xp_rate_tier AS xp_rate_tier, {self._ONLINE} AS online,
                       SUM(s.base_level) AS level, SUM(s.fine_xp) AS fine_xp,
                       MAX(s.updated_at) AS updated_at
                FROM characters c
                JOIN accounts a ON a.id = c.account_id
                JOIN stats s ON s.character_id = c.id AND s.stat_id < {REAL_STAT_COUNT}
                WHERE c.realm_id = ?{mode_sql}
                GROUP BY c.id
                ORDER BY level DESC, fine_xp DESC, c.id ASC
                LIMIT ? OFFSET ?
            """
            params = (self.realm_id, *mode_params, limit, offset)
        else:
            stat_id = STAT_IDS[skill]
            # `fine_xp > 0` is the standard "ranked" rule: a skill nobody has touched should have an
            # empty table rather than a table of level 1s.
            sql = f"""
                SELECT {self._NAME} AS name, c.account_mode AS account_mode,
                       c.xp_rate_tier AS xp_rate_tier, {self._ONLINE} AS online,
                       s.base_level AS level, s.fine_xp AS fine_xp, s.updated_at AS updated_at
                FROM stats s
                JOIN characters c ON c.id = s.character_id
                JOIN accounts a ON a.id = c.account_id
                WHERE c.realm_id = ? AND s.stat_id = ? AND s.fine_xp > 0{mode_sql}
                ORDER BY level DESC, fine_xp DESC, c.id ASC
                LIMIT ? OFFSET ?
            """
            params = (self.realm_id, stat_id, *mode_params, limit, offset)

        rows = self._rows(sql, params)
        entries = [
            {
                "rank": offset + i + 1,
                "name": r["name"],
                "mode": ACCOUNT_MODES.get(r["account_mode"], "standard"),
                "xp_rate": XP_TIERS.get(r["xp_rate_tier"]),
                "online": bool(r["online"]),
                "level": r["level"],
                "xp": (r["fine_xp"] or 0) // XP_PRECISION,
                "updated_at": r["updated_at"],
            }
            for i, r in enumerate(rows)
        ]
        return {"skill": skill, "mode": mode, "offset": offset, "entries": entries}

    def player(self, name: str) -> dict | None:
        return self.cache.get(("player", name.lower()), lambda: self._player(name))

    def _player(self, name: str) -> dict | None:
        row = self._rows(
            f"""
            SELECT c.id AS cid, {self._NAME} AS name, c.account_mode AS account_mode,
                   c.xp_rate_tier AS xp_rate_tier, c.created_at AS created_at,
                   c.last_login AS last_login, c.last_logout AS last_logout,
                   {self._ONLINE} AS online, a.modlevel AS modlevel
            FROM characters c JOIN accounts a ON a.id = c.account_id
            WHERE c.realm_id = ?
              AND (LOWER(a.display_name) = LOWER(?) OR LOWER(a.login_username) = LOWER(?))
            LIMIT 1
            """,
            (self.realm_id, name, name),
        )
        if not row:
            return None
        row = row[0]

        # ROW_NUMBER rather than RANK, and with the same tiebreak as the leaderboard's ORDER BY, so
        # the rank shown here is the row the player actually occupies on the hiscores table.
        ranked = self._rows(
            f"""
            WITH ranked AS (
                SELECT s.character_id AS cid, s.stat_id AS stat_id, s.base_level AS level,
                       s.fine_xp AS fine_xp, s.updated_at AS updated_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY s.stat_id
                           ORDER BY s.base_level DESC, s.fine_xp DESC, s.character_id ASC
                       ) AS rank
                FROM stats s
                JOIN characters c ON c.id = s.character_id
                WHERE c.realm_id = ? AND s.stat_id < {REAL_STAT_COUNT} AND s.fine_xp > 0
            )
            SELECT * FROM ranked WHERE cid = ?
            """,
            (self.realm_id, row["cid"]),
        )
        ranks = {r["stat_id"]: r for r in ranked}

        raw = self._rows(
            f"SELECT stat_id, base_level, fine_xp, updated_at FROM stats "
            f"WHERE character_id = ? AND stat_id < {REAL_STAT_COUNT} ORDER BY stat_id",
            (row["cid"],),
        )

        skills, levels, total_level, total_xp, updated = [], {}, 0, 0, None
        for r in raw:
            key = STAT_NAMES[r["stat_id"]]
            xp = (r["fine_xp"] or 0) // XP_PRECISION
            levels[key] = r["base_level"]
            total_level += r["base_level"]
            total_xp += xp
            if r["updated_at"] and (updated is None or r["updated_at"] > updated):
                updated = r["updated_at"]
            hit = ranks.get(r["stat_id"])
            skills.append(
                {
                    "skill": key,
                    "label": skill_label(key),
                    "level": r["base_level"],
                    "xp": xp,
                    "rank": hit["rank"] if hit else None,
                    "progress": level_progress(r["fine_xp"] or 0),
                }
            )

        overall = self._rows(
            f"""
            WITH totals AS (
                SELECT c.id AS cid, SUM(s.base_level) AS level, SUM(s.fine_xp) AS fine_xp,
                       ROW_NUMBER() OVER (
                           ORDER BY SUM(s.base_level) DESC, SUM(s.fine_xp) DESC, c.id ASC
                       ) AS rank
                FROM characters c
                JOIN stats s ON s.character_id = c.id AND s.stat_id < {REAL_STAT_COUNT}
                WHERE c.realm_id = ?
                GROUP BY c.id
            )
            SELECT rank FROM totals WHERE cid = ?
            """,
            (self.realm_id, row["cid"]),
        )

        return {
            "name": row["name"],
            "mode": ACCOUNT_MODES.get(row["account_mode"], "standard"),
            "mode_label": ACCOUNT_MODE_LABELS.get(
                ACCOUNT_MODES.get(row["account_mode"], "standard"), "Standard"
            ),
            "xp_rate": XP_TIERS.get(row["xp_rate_tier"]),
            "modlevel": row["modlevel"],
            "online": bool(row["online"]),
            "created_at": row["created_at"],
            "last_login": row["last_login"],
            "last_logout": row["last_logout"],
            "updated_at": updated,
            "total_level": total_level,
            "total_xp": total_xp,
            "overall_rank": overall[0]["rank"] if overall else None,
            "combat_level": combat_level(levels),
            "skills": skills,
        }

    def search(self, term: str, limit: int = 10) -> list[str]:
        rows = self._rows(
            f"""
            SELECT {self._NAME} AS name FROM characters c
            JOIN accounts a ON a.id = c.account_id
            WHERE c.realm_id = ? AND LOWER({self._NAME}) LIKE LOWER(?)
            ORDER BY name LIMIT ?
            """,
            (self.realm_id, f"%{term}%", limit),
        )
        return [r["name"] for r in rows]
