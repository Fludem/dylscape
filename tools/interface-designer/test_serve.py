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


PANEL = {
    "format": 1, "kind": "panel", "interface": "demo", "title": "Demo", "w": 300, "h": 200,
    "notes": "", "widgets": [{"id": "button_1", "type": "button", "x": 8, "y": 40, "w": 120, "h": 24}],
}


class PanelTest(unittest.TestCase):
    def test_a_panel_is_formatted_one_widget_per_line_and_round_trips(self):
        text = serve.format_panel(serve.check_panel(json.loads(json.dumps(PANEL))))
        self.assertEqual(json.loads(text), PANEL)
        self.assertIn('    {"id": "button_1", "type": "button", "x": 8', text)
        empty = dict(PANEL, widgets=[])
        self.assertEqual(json.loads(serve.format_panel(empty)), empty)

    def test_panel_rejections(self):
        cases = {
            "wrong kind": dict(PANEL, kind="design"),
            "bad interface": dict(PANEL, interface="Demo"),
            "tiny panel": dict(PANEL, w=10),
            "unknown key": dict(PANEL, colour="red"),
            "unknown widget": dict(PANEL, widgets=[{"id": "a", "type": "slider", "x": 0, "y": 0, "w": 1, "h": 1}]),
            "duplicate id": dict(PANEL, widgets=PANEL["widgets"] * 2),
            "float position": dict(PANEL, widgets=[{"id": "a", "type": "box", "x": 1.5, "y": 0, "w": 1, "h": 1}]),
        }
        for label, panel in cases.items():
            with self.subTest(label), self.assertRaises(DesignError):
                serve.check_panel(panel)


class SavePanelTest(unittest.TestCase):
    """save_panel against a throwaway repo, so real designs and symbols are never touched."""

    def setUp(self):
        self.dir = tempfile.TemporaryDirectory()
        root = Path(self.dir.name).resolve()
        (root / "tools/interface-designer/designs").mkdir(parents=True)
        (root / "content/custom/x/src/main/resources").mkdir(parents=True)
        (root / ".data/symbols/.local").mkdir(parents=True)
        (root / ".data/symbols/.local/interface.sym").write_text("1002\tdemo\n")
        (root / ".data/symbols/.local/component.sym").write_text("")
        self.saved = {k: getattr(serve, k) for k in ("REPO", "DRAFTS", "CONTENT", "LOCAL_SYMBOLS")}
        serve.REPO = root
        serve.DRAFTS = root / "tools/interface-designer/designs"
        serve.CONTENT = root / "content"
        serve.LOCAL_SYMBOLS = root / ".data/symbols/.local"
        self.root = root

    def tearDown(self):
        for key, value in self.saved.items():
            setattr(serve, key, value)
        self.dir.cleanup()

    def body(self, interface="demo"):
        return {"panel": dict(PANEL), "design": design(ROOT, interface=interface)}

    def test_a_draft_writes_the_panel_and_its_design_but_no_symbols(self):
        result = serve.save_panel("tools/interface-designer/designs/demo.panel.json", self.body())
        self.assertFalse(result["symbolsSynced"])
        drafts = self.root / "tools/interface-designer/designs"
        self.assertEqual(json.loads((drafts / "demo.panel.json").read_text()), PANEL)
        self.assertEqual(json.loads((drafts / "demo.interface.json").read_text())["interface"], "demo")
        self.assertEqual((self.root / ".data/symbols/.local/component.sym").read_text(), "")

    def test_an_implemented_panel_syncs_the_symbols_of_its_design(self):
        panel = self.root / "content/custom/x/src/main/resources/demo.panel.json"
        panel.write_text("{}")  # implemented panels already exist; new ones start as drafts
        result = serve.save_panel(str(panel.relative_to(self.root)), self.body())
        self.assertTrue(result["symbolsSynced"])
        self.assertIn("demo:0\tdemo:root", (self.root / ".data/symbols/.local/component.sym").read_text())

    def test_mismatches_and_new_panels_outside_drafts_are_refused(self):
        with self.assertRaises(DesignError):
            serve.save_panel("tools/interface-designer/designs/demo.panel.json", self.body("other"))
        with self.assertRaises(DesignError):
            serve.save_panel("tools/interface-designer/designs/wrong.panel.json", self.body())
        with self.assertRaises(DesignError):
            serve.save_panel("content/custom/x/src/main/resources/demo.panel.json", self.body())


if __name__ == "__main__":
    unittest.main()
