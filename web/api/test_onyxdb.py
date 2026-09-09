"""Tests for the website's read-only view of the game database.

The queries are exercised against a real SQLite file built from the same DDL the server ships, so a
schema change upstream breaks these rather than silently changing what the site reports.
"""

from __future__ import annotations

import os
import sqlite3
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from onyxdb import (
    REAL_STAT_COUNT, STAT_IDS, GameDatabase, combat_level, host_local_iso, skill_label, utc_iso,
)

SCHEMA = """
CREATE TABLE accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    login_username TEXT NOT NULL UNIQUE,
    display_name TEXT UNIQUE,
    password_hash TEXT NOT NULL,
    modlevel TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE characters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    realm_id INTEGER NOT NULL,
    account_mode INTEGER NOT NULL DEFAULT 0,
    xp_rate_tier INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    last_login TIMESTAMP,
    last_logout TIMESTAMP
);
CREATE TABLE stats (
    character_id INTEGER NOT NULL,
    stat_id INTEGER NOT NULL,
    vis_level INTEGER NOT NULL,
    base_level INTEGER NOT NULL,
    fine_xp INTEGER NOT NULL,
    updated_at TIMESTAMP,
    UNIQUE (character_id, stat_id)
);
"""


class Fixture:
    """Builds a throwaway database in the shape of `V1__init.sql` plus the V6/V7 columns."""

    def __init__(self, path: str) -> None:
        self.conn = sqlite3.connect(path)
        self.conn.executescript(SCHEMA)
        self.next_id = 0

    def add(self, name, levels, realm=2, mode=0, tier=16, display=None, logged_in=False):
        self.next_id += 1
        cid = self.next_id
        self.conn.execute(
            "INSERT INTO accounts (id, login_username, display_name, password_hash, modlevel) "
            "VALUES (?, ?, ?, 'x', 'player')",
            (cid, name.lower(), display),
        )
        self.conn.execute(
            "INSERT INTO characters (id, account_id, realm_id, account_mode, xp_rate_tier, "
            "created_at, last_login, last_logout) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            (
                cid, cid, realm, mode, tier,
                "2026-01-01 00:00:00",
                "2026-01-02 00:00:00",
                None if logged_in else "2026-01-03 00:00:00",
            ),
        )
        # Every save writes all 25 stat rows, including the two that are not real skills.
        for stat_id in range(25):
            key = [k for k, v in STAT_IDS.items() if v == stat_id][0]
            level, xp = levels.get(key, (1, 0))
            self.conn.execute(
                "INSERT INTO stats (character_id, stat_id, vis_level, base_level, fine_xp, "
                "updated_at) VALUES (?, ?, ?, ?, ?, '2026-01-03 00:00:00')",
                (cid, stat_id, level, level, xp * 10),
            )
        self.conn.commit()
        return cid


class CombatLevelTest(unittest.TestCase):
    """`CombatLevel.calculate` halves prayer, ranged and magic with Kotlin Int division. If these
    ever use true division the site drifts half a level above the client."""

    def test_fresh_account(self):
        self.assertEqual(combat_level({"hitpoints": 10}), 3)

    def test_maxed_account(self):
        maxed = dict.fromkeys(
            ["attack", "defence", "strength", "hitpoints", "ranged", "magic", "prayer"], 99
        )
        self.assertEqual(combat_level(maxed), 126)

    def test_odd_prayer_truncates(self):
        base = {"attack": 40, "strength": 40, "defence": 40, "hitpoints": 40}
        self.assertEqual(combat_level({**base, "prayer": 43}), combat_level({**base, "prayer": 42}))

    def test_odd_ranged_truncates(self):
        base = {"defence": 40, "hitpoints": 40, "prayer": 1}
        self.assertEqual(combat_level({**base, "ranged": 61}), combat_level({**base, "ranged": 60}))

    def test_live_character_matches_client(self):
        # The real dev-realm character: attack 10, defence 1, strength 8, hitpoints 13, prayer 22.
        levels = {"attack": 10, "defence": 1, "strength": 8, "hitpoints": 13, "prayer": 22,
                  "ranged": 1, "magic": 1}
        self.assertEqual(combat_level(levels), 12)

    def test_ranged_beats_melee_when_higher(self):
        # 0.25 * (1 + 10 + 0) + 0.325 * (70 + 35) = 36.875, floored.
        levels = {"defence": 1, "hitpoints": 10, "prayer": 1, "attack": 1, "strength": 1,
                  "ranged": 70, "magic": 1}
        self.assertEqual(combat_level(levels), 36)


class DatabaseTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.path = os.path.join(self.dir.name, "game.db")
        self.fx = Fixture(self.path)
        # ttl=0 so each assertion sees the fixture as written rather than a cached earlier answer.
        self.db = GameDatabase(self.path, realm_id=2, ttl=0)

    def tearDown(self):
        self.fx.conn.close()
        self.dir.cleanup()

    def test_total_level_excludes_non_skills(self):
        # stat_id 23 (sailing) and 24 (unreleased) are written on every save but are not skills.
        self.fx.add("alpha", {"attack": (50, 101_333), "sailing": (99, 13_034_431)})
        entry = self.db.hiscores("overall", "all", 50, 0)["entries"][0]
        # 50 attack + 22 other skills at level 1 = 72. Including sailing would read 170.
        self.assertEqual(entry["level"], 50 + (REAL_STAT_COUNT - 1))

    def test_xp_is_fine_xp_divided_by_ten(self):
        self.fx.add("alpha", {"attack": (50, 101_333)})
        entry = self.db.hiscores("attack", "all", 50, 0)["entries"][0]
        self.assertEqual(entry["xp"], 101_333)

    def test_other_realms_are_invisible(self):
        self.fx.add("devguy", {"attack": (99, 13_034_431)}, realm=1)
        self.fx.add("mainguy", {"attack": (50, 101_333)}, realm=2)
        names = [e["name"] for e in self.db.hiscores("attack", "all", 50, 0)["entries"]]
        self.assertEqual(names, ["mainguy"])

    def test_display_name_falls_back_to_login_name(self):
        self.fx.add("nodisplay", {"attack": (50, 101_333)}, display=None)
        self.fx.add("hasdisplay", {"attack": (40, 37_224)}, display="Zezima")
        names = [e["name"] for e in self.db.hiscores("attack", "all", 50, 0)["entries"]]
        self.assertEqual(names, ["nodisplay", "Zezima"])

    def test_ranking_orders_by_level_then_xp(self):
        self.fx.add("low", {"attack": (40, 37_224)})
        self.fx.add("high", {"attack": (50, 101_333)})
        self.fx.add("tied", {"attack": (50, 111_333)})
        entries = self.db.hiscores("attack", "all", 50, 0)["entries"]
        self.assertEqual([e["name"] for e in entries], ["tied", "high", "low"])
        self.assertEqual([e["rank"] for e in entries], [1, 2, 3])

    def test_unranked_skills_are_excluded(self):
        # A skill nobody has trained should be an empty table, not a table of level 1s.
        self.fx.add("alpha", {"attack": (50, 101_333)})
        self.assertEqual(self.db.hiscores("mining", "all", 50, 0)["entries"], [])

    def test_mode_filter(self):
        self.fx.add("normal", {"attack": (50, 101_333)}, mode=0)
        self.fx.add("iron", {"attack": (45, 61_512)}, mode=1)
        self.fx.add("hardcore", {"attack": (40, 37_224)}, mode=3)
        self.assertEqual(
            [e["name"] for e in self.db.hiscores("attack", "ironman", 50, 0)["entries"]], ["iron"]
        )
        self.assertEqual(
            [e["name"] for e in self.db.hiscores("attack", "hcim", 50, 0)["entries"]], ["hardcore"]
        )
        self.assertEqual(len(self.db.hiscores("attack", "all", 50, 0)["entries"]), 3)

    def test_offset_continues_the_ranking(self):
        for i in range(5):
            self.fx.add(f"p{i}", {"attack": (50 - i, 101_333 - i)})
        page = self.db.hiscores("attack", "all", 2, 2)
        self.assertEqual([e["rank"] for e in page["entries"]], [3, 4])
        self.assertEqual([e["name"] for e in page["entries"]], ["p2", "p3"])

    def test_player_lookup_is_case_insensitive(self):
        self.fx.add("zezima", {"attack": (50, 101_333)}, display="Zezima")
        for probe in ("zezima", "ZEZIMA", "ZeZiMa"):
            self.assertIsNotNone(self.db.player(probe), probe)
        self.assertIsNone(self.db.player("nobody"))

    def test_player_reports_ranks_and_totals(self):
        self.fx.add("best", {"attack": (60, 273_742), "mining": (50, 101_333)})
        self.fx.add("second", {"attack": (50, 101_333)})
        found = self.db.player("second")
        skills = {s["skill"]: s for s in found["skills"]}
        self.assertEqual(skills["attack"]["rank"], 2)
        self.assertEqual(skills["attack"]["level"], 50)
        # Untrained skills are unranked rather than being given a rank among all level 1s.
        self.assertIsNone(skills["mining"]["rank"])
        self.assertEqual(found["overall_rank"], 2)
        self.assertEqual(found["total_level"], 50 + (REAL_STAT_COUNT - 1))
        self.assertEqual(found["mode_label"], "Standard")
        self.assertEqual(found["xp_rate"], "16x")

    def test_player_skill_list_covers_every_real_skill(self):
        self.fx.add("alpha", {"attack": (50, 101_333)})
        self.assertEqual(len(self.db.player("alpha")["skills"]), REAL_STAT_COUNT)

    def test_online_is_derived_from_logout_timestamp(self):
        self.fx.add("on", {"attack": (50, 101_333)}, logged_in=True)
        self.fx.add("off", {"attack": (40, 37_224)}, logged_in=False)
        status = self.db.status()
        self.assertEqual(status["online"], 1)
        self.assertEqual(status["characters"], 2)
        self.assertTrue(status["online_is_estimate"])
        self.assertTrue(self.db.player("on")["online"])
        self.assertFalse(self.db.player("off")["online"])

    def test_search_matches_substrings(self):
        self.fx.add("zezima", {"attack": (50, 101_333)}, display="Zezima")
        self.fx.add("other", {"attack": (40, 37_224)}, display="Woox")
        self.assertEqual(self.db.search("ezi"), ["Zezima"])
        self.assertEqual(self.db.search("zzz"), [])

    def test_missing_database_is_reported_not_raised(self):
        self.assertFalse(GameDatabase(self.path + ".nope", realm_id=2).available())


class TimestampTest(unittest.TestCase):
    """The schema stores no offsets and mixes zones: CURRENT_TIMESTAMP columns are UTC, but
    `characters.last_login` is bound from LocalDateTime.now() on the game host. Reading both the
    same way puts last-login in the future wherever the host is not on UTC."""

    def test_current_timestamp_columns_are_utc(self):
        self.assertEqual(utc_iso("2026-09-09 08:46:30"), "2026-09-09T08:46:30+00:00")

    def test_last_login_is_read_in_the_host_zone(self):
        iso = host_local_iso("2026-09-09 08:46:30")
        # Whatever the host zone is, the wall-clock reading must be preserved and an offset added.
        self.assertTrue(iso.startswith("2026-09-09T08:46:30"), iso)
        self.assertRegex(iso, r"(\+|-)\d{2}:\d{2}$")

    def test_null_timestamps_stay_null(self):
        self.assertIsNone(utc_iso(None))
        self.assertIsNone(host_local_iso(""))

    def test_unparseable_timestamp_is_not_fatal(self):
        self.assertIsNone(utc_iso("not a date"))

    def test_fractional_seconds_are_accepted(self):
        self.assertEqual(utc_iso("2026-09-09 08:46:30.123"), "2026-09-09T08:46:30.123000+00:00")

    def test_player_timestamps_carry_offsets(self):
        with tempfile.TemporaryDirectory() as d:
            path = os.path.join(d, "game.db")
            fx = Fixture(path)
            fx.add("alpha", {"attack": (50, 101_333)})
            fx.conn.close()
            found = GameDatabase(path, realm_id=2, ttl=0).player("alpha")
            for field in ("created_at", "last_login", "last_logout", "updated_at"):
                self.assertRegex(found[field], r"(\+|-)\d{2}:\d{2}$", field)


class LabelTest(unittest.TestCase):
    def test_runecrafting_uses_its_in_game_name(self):
        self.assertEqual(skill_label("runecrafting"), "Runecraft")
        self.assertEqual(skill_label("woodcutting"), "Woodcutting")


if __name__ == "__main__":
    unittest.main()
