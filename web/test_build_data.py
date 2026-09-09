"""Tests for the guide-data generator.

These cover the transforms that turn the game's internal data into something a player can read.
They fail quietly rather than loudly when wrong - a drop rate rendered against the wrong
denominator still looks like a drop rate - so they are worth pinning down.
"""

from __future__ import annotations

import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from build_data import (
    ENTRY_RE, pretty_chance, pretty_obj, pretty_source, render_drop, trainable_skills,
)


class ObjNameTest(unittest.TestCase):
    def test_underscores_become_words(self):
        self.assertEqual(pretty_obj("wolf_bones"), "Wolf bones")

    def test_runes_are_split(self):
        self.assertEqual(pretty_obj("firerune"), "Fire rune")
        self.assertEqual(pretty_obj("naturerune"), "Nature rune")

    def test_a_word_merely_ending_in_rune_is_left_alone(self):
        # Only the listed prefixes are split; "rune_scimitar" is an ordinary underscored name.
        self.assertEqual(pretty_obj("rune_scimitar"), "Rune scimitar")

    def test_poison_suffixes_are_kept(self):
        self.assertEqual(pretty_obj("steel_arrow_p"), "Steel arrow (p)")
        self.assertEqual(pretty_obj("steel_arrow_p_plus_plus"), "Steel arrow (p++)")

    def test_known_awkward_names_are_corrected(self):
        self.assertEqual(pretty_obj("vial_water"), "Vial of water")
        self.assertEqual(pretty_obj("bucket_empty"), "Bucket")

    def test_empty_obj_is_a_nothing_drop(self):
        self.assertEqual(pretty_obj(""), "Nothing")


class ChanceTest(unittest.TestCase):
    """The tables mix denominators wildly - 512 for one monster, 10,000,000 for another - so rates
    are always rendered as 1/N, which is how the game's players read them."""

    def test_certain_drops(self):
        self.assertEqual(pretty_chance(1.0), "Always")

    def test_rare_drops_round_to_whole_numbers(self):
        self.assertEqual(pretty_chance(1 / 1024), "1/1,024")
        self.assertEqual(pretty_chance(1 / 256), "1/256")

    def test_common_drops_keep_a_decimal(self):
        self.assertEqual(pretty_chance(100 / 512), "1/5.1")

    def test_a_huge_denominator_is_normalised(self):
        # 2187500/10000000 must not reach the page in that form.
        self.assertEqual(pretty_chance(2187500 / 10_000_000), "1/4.6")

    def test_zero_is_not_a_division_error(self):
        self.assertEqual(pretty_chance(0), "Never")


class RenderDropTest(unittest.TestCase):
    def test_weight_uses_the_tables_denominator(self):
        row = render_drop({"weight": 100, "obj": "grey_wolf_fur"}, out_of=512)
        self.assertEqual(row["name"], "Grey wolf fur")
        self.assertEqual(row["chance"], "1/5.1")
        self.assertEqual(row["quantity"], "1")

    def test_one_in_ignores_the_denominator(self):
        row = render_drop({"one_in": 1024, "obj": "unidentified_guam"})
        self.assertEqual(row["chance"], "1/1,024")

    def test_quantity_ranges_and_notes(self):
        row = render_drop({"weight": 1, "obj": "gold_ore", "count": 10, "count_max": 20,
                           "noted": True}, out_of=100)
        self.assertEqual(row["quantity"], "10–20 (noted)")

    def test_a_drop_with_no_weight_is_guaranteed(self):
        self.assertEqual(render_drop({"obj": "bones"})["chance"], "Always")


class SourceNameTest(unittest.TestCase):
    def test_wiki_anchors_become_variant_labels(self):
        self.assertEqual(
            pretty_source("Abyssal demon#Catacombs of Kourend"),
            "Abyssal demon (Catacombs of Kourend)",
        )

    def test_plain_names_are_untouched(self):
        self.assertEqual(pretty_source("Gargoyle"), "Gargoyle")


class TeleportParseTest(unittest.TestCase):
    """The teleport list is a regex over Kotlin source. If TeleportTable.kt is reformatted this is
    what notices."""

    def test_matches_the_tables_entry_shape(self):
        line = 'entry("grand_exchange", "Grand Exchange", CoordGrid(3164, 3486, 0)),'
        match = ENTRY_RE.search(line)
        self.assertIsNotNone(match)
        self.assertEqual(match.group("key"), "grand_exchange")
        self.assertEqual(match.group("label"), "Grand Exchange")
        self.assertEqual(match.group("x"), "3164")
        self.assertEqual(match.group("z"), "3486")

    def test_labels_with_apostrophes_survive(self):
        line = '''entry("seers_village", "Seers' Village", CoordGrid(2725, 3491, 0)),'''
        self.assertEqual(ENTRY_RE.search(line).group("label"), "Seers' Village")


class TrainableSkillsTest(unittest.TestCase):
    """The site once listed skills by reading `content/skills/`, which has no directory for the
    five combat skills - their experience comes from api/combat's PlayerAttackManager. The page
    therefore told players Attack and Strength were unimplemented. This pins the real answer."""

    def setUp(self):
        self.trainable, self.missing = trainable_skills()

    def test_every_skill_is_classified_exactly_once(self):
        self.assertEqual(len(self.trainable) + len(self.missing), 23)
        self.assertFalse(set(self.trainable) & set(self.missing))

    def test_combat_skills_count_as_trainable(self):
        # These have no content/skills/ module and must still be listed.
        for skill in ("attack", "defence", "strength", "hitpoints", "ranged"):
            self.assertIn(skill, self.trainable, skill)

    def test_the_three_known_gaps(self):
        self.assertEqual(sorted(self.missing), ["herblore", "runecrafting", "slayer"])

    def test_a_skill_with_a_module_is_trainable(self):
        for skill in ("mining", "thieving", "construction", "farming"):
            self.assertIn(skill, self.trainable, skill)


class RealDataTest(unittest.TestCase):
    """A smoke test over the actual repo data, so a content change that empties a table is caught
    here rather than on the deployed page."""

    def test_generated_files_are_present_and_sane(self):
        import json
        import pathlib

        root = pathlib.Path(__file__).resolve().parent / "site/data"
        if not (root / "drops-index.json").exists():
            self.skipTest("run build_data.py first")

        index = json.loads((root / "drops-index.json").read_text())
        self.assertGreater(index["count"], 500)
        self.assertFalse([s for s in index["sources"] if "#" in s["source"]])

        teleports = json.loads((root / "teleports.json").read_text())
        self.assertGreater(teleports["count"], 20)

        features = json.loads((root / "features.json").read_text())
        self.assertEqual(len(features["skills"]) + len(features["missing_skills"]), 23)
        self.assertIn("Attack", [s["label"] for s in features["skills"]])
        self.assertIn("Runecraft", [s["label"] for s in features["missing_skills"]])
        self.assertEqual(len(features["xp_tiers"]), 3)
        self.assertEqual(len(features["account_modes"]), 4)


if __name__ == "__main__":
    unittest.main()
