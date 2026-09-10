"""Tests for serve.py: canonical form, validation, symbol sync and path confinement.

    python3 -m unittest tools/interface-designer/test_serve.py
"""

import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import serve  # noqa: E402
from serve import DesignError  # noqa: E402


def design(*components, interface="demo"):
    return {"format": 1, "interface": interface, "notes": "", "components": list(components)}


ROOT = {"name": "root", "type": "layer", "w": 488, "h": 320, "xMode": "centre", "yMode": "centre"}


class NormaliseTest(unittest.TestCase):
    def test_defaults_are_dropped_and_keys_ordered(self):
        label = {
            "shadow": True, "font": 496, "text": "Buy", "type": "text", "parent": "root",
            "name": "label", "x": 0, "hidden": False, "colour": "FF981F", "ops": ["Buy", "", ""],
        }
        out = serve.normalise(design(ROOT, label))
        self.assertEqual(
            list(out["components"][1]),
            ["name", "parent", "type", "colour", "text", "font", "shadow", "ops"],
        )
        self.assertEqual(out["components"][1]["colour"], "ff981f")
        self.assertEqual(out["components"][1]["ops"], ["Buy"])

    def test_an_empty_frame_and_hover_mean_none(self):
        frame = {"name": "frame", "parent": "root", "type": "layer", "frame": "", "hover": {}}
        out = serve.normalise(design(ROOT, frame))
        self.assertEqual(out["components"][1], {"name": "frame", "parent": "root", "type": "layer"})

    def test_rejections(self):
        cases = {
            "unknown key": {"name": "a", "parent": "root", "type": "layer", "colur": "ffffff"},
            "parent below": {"name": "a", "parent": "b", "type": "layer"},
            "bad name": {"name": "A", "parent": "root", "type": "layer"},
            "field of another type": {"name": "a", "parent": "root", "type": "layer", "font": 495},
            "text without font": {"name": "a", "parent": "root", "type": "text", "text": "x"},
            "graphic without sprite": {"name": "a", "parent": "root", "type": "graphic"},
            "hover on wrong type": {"name": "a", "parent": "root", "type": "rect", "hover": {"colour": "ffffff"}},
            "two hovers": {"name": "a", "parent": "root", "type": "graphic", "sprite": 1, "hover": {"trans": 1, "sprite": 2}},
            "trans out of range": {"name": "a", "parent": "root", "type": "rect", "trans": 300},
            "bad mode": {"name": "a", "parent": "root", "type": "layer", "xMode": "middle"},
            "bool as int": {"name": "a", "parent": "root", "type": "layer", "x": True},
        }
        for label, component in cases.items():
            with self.subTest(label), self.assertRaises(DesignError):
                serve.normalise(design(ROOT, component))

    def test_duplicate_names_and_second_roots_are_refused(self):
        with self.assertRaises(DesignError):
            serve.normalise(design(ROOT, {"name": "root", "parent": "root", "type": "layer"}))
        with self.assertRaises(DesignError):
            serve.normalise(design(ROOT, {"name": "other", "type": "layer"}))

    def test_format_is_one_component_per_line_and_round_trips(self):
        out = serve.normalise(design(ROOT, {"name": "frame", "parent": "root", "type": "layer", "frame": "Demo"}))
        text = serve.format_design(out)
        self.assertEqual(json.loads(text), out)
        self.assertIn('    {"name": "frame", "parent": "root", "type": "layer", "frame": "Demo"}', text)
        self.assertEqual(serve.format_design(serve.normalise(json.loads(text))), text)


class SyncSymbolsTest(unittest.TestCase):
    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        self.sym = Path(self.dir.name) / "component.sym"

    def tearDown(self):
        self.dir.cleanup()

    def test_rewrites_only_the_interface_block_in_place(self):
        self.sym.write_text(
            "# header\n\n"
            "other:0\tother:root\n"
            "# demo comment\n"
            "demo:0\tdemo:root\n"
            "demo:1\tdemo:old\n"
            "\n"
            "later:0\tlater:root\n"
        )
        d = serve.normalise(design(ROOT, {"name": "a", "parent": "root", "type": "layer"},
                                   {"name": "b", "parent": "a", "type": "layer"}))
        self.assertTrue(serve.sync_symbols(d, self.sym, "x"))
        self.assertEqual(
            self.sym.read_text(),
            "# header\n\n"
            "other:0\tother:root\n"
            "# demo comment\n"
            "demo:0\tdemo:root\n"
            "demo:1\tdemo:a\n"
            "demo:2\tdemo:b\n"
            "\n"
            "later:0\tlater:root\n",
        )
        self.assertFalse(serve.sync_symbols(d, self.sym, "x"))

    def test_a_first_sync_appends_a_labelled_block(self):
        self.sym.write_text("other:0\tother:root")
        serve.sync_symbols(serve.normalise(design(ROOT)), self.sym, "content/x/demo.interface.json")
        self.assertEqual(
            self.sym.read_text(),
            "other:0\tother:root\n\n"
            "# demo: written by tools/interface-designer from content/x/demo.interface.json; do not edit.\n"
            "demo:0\tdemo:root\n",
        )


class PathTest(unittest.TestCase):
    def test_designs_are_confined_to_drafts_and_module_resources(self):
        repo = serve.REPO
        self.assertEqual(
            serve.resolve_design("tools/interface-designer/designs/demo.interface.json"),
            (repo / "tools/interface-designer/designs/demo.interface.json").resolve(),
        )
        serve.resolve_design("content/custom/x/src/main/resources/org/demo.interface.json")
        for bad in [
            "tools/interface-designer/designs/../serve.py",
            "tools/interface-designer/designs/../../../.data/symbols/demo.interface.json",
            "content/custom/x/src/main/kotlin/demo.interface.json",
            ".data/demo.interface.json",
            "tools/interface-designer/designs/demo.json",
        ]:
            with self.subTest(bad), self.assertRaises(DesignError):
                serve.resolve_design(bad)

    def test_only_content_designs_with_an_id_are_implemented(self):
        repo = serve.REPO
        ids = {"demo": 1002}
        content = (repo / "content/custom/x/src/main/resources/demo.interface.json").resolve()
        draft = (repo / "tools/interface-designer/designs/demo.interface.json").resolve()
        self.assertTrue(serve.is_implemented(content, "demo", ids))
        self.assertFalse(serve.is_implemented(draft, "demo", ids))
        self.assertFalse(serve.is_implemented(content, "other", ids))

    def test_referenced_names_are_read_from_kotlin(self):
        with tempfile.TemporaryDirectory() as root:
            source = Path(root) / "custom" / "x" / "Refs.kt"
            source.parent.mkdir(parents=True)
            source.write_text('val a = find("demo:slot_0")\nval b = find("demo:close")\nval c = find("other:x")\n')
            self.assertEqual(serve.referenced_names("demo", Path(root)), ["close", "slot_0"])


if __name__ == "__main__":
    unittest.main()
