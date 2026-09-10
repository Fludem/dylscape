#!/usr/bin/env python3
"""Local server for the interface designer. Standard library only, like web/api.

Serves the editor (app/) and the exported cache art (assets/), and reads and writes design files:
drafts in tools/interface-designer/designs/, and implemented designs in any content module's
src/main/resources/. Nothing else on disk is reachable through it.

Saving an implemented design also rewrites that interface's block in
.data/symbols/.local/component.sym. A design's component order is its child-index order, so
moving one layer renumbers every component after it; doing that by hand is how panels end up
packed with parents pointing at the wrong components.

    serve.py [--port 8098]           run the server
    serve.py --import FILE [--force] normalise DesignImport output into designs/
    serve.py --normalise FILE        print FILE in canonical form
    serve.py --sync-symbols FILE     rewrite FILE's component.sym block now
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from http import HTTPStatus
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
DRAFTS = HERE / "designs"
CONTENT = REPO / "content"
LOCAL_SYMBOLS = REPO / ".data" / "symbols" / ".local"
SUFFIX = ".interface.json"

FORMAT = 1
NAME = re.compile(r"[a-z0-9_]+")
HEX = re.compile(r"[0-9a-f]{6}")

# Key order of a component on disk. Stable ordering keeps git diffs to the fields that changed.
KEY_ORDER = [
    "name", "parent", "type", "x", "y", "w", "h", "xMode", "yMode", "wMode", "hMode",
    "hidden", "clickThrough", "frame", "colour", "filled", "trans", "sprite", "tiling",
    "text", "font", "alignH", "alignV", "lineHeight", "shadow", "hover", "ops", "opBase", "notes",
]

# Must match the defaults of DesignComponent in DesignedComponentBuilder.kt. Defaults are left out
# of the file, so a component only lists what makes it different.
DEFAULTS = {
    "x": 0, "y": 0, "w": 0, "h": 0,
    "xMode": "start", "yMode": "start", "wMode": "fixed", "hMode": "fixed",
    "hidden": False, "clickThrough": True, "frame": "", "colour": "000000", "filled": False,
    "trans": 0, "sprite": -1, "tiling": False, "text": "", "font": -1, "alignH": "left",
    "alignV": "top", "lineHeight": 0, "shadow": False, "hover": None, "ops": [], "opBase": "",
    "notes": "",
}

COMMON = {"name", "parent", "type", "x", "y", "w", "h", "xMode", "yMode", "wMode", "hMode",
          "hidden", "clickThrough", "hover", "ops", "opBase", "notes"}
TYPE_FIELDS = {
    "layer": COMMON | {"frame"},
    "rect": COMMON | {"colour", "filled", "trans"},
    "text": COMMON | {"colour", "text", "font", "alignH", "alignV", "lineHeight", "shadow"},
    "graphic": COMMON | {"sprite", "tiling", "trans"},
}
HOVER_BY_TYPE = {"text": {"colour"}, "rect": {"trans"}, "graphic": {"trans", "sprite"}, "layer": set()}
WORDS = {
    "xMode": {"start", "centre", "end"}, "yMode": {"start", "centre", "end"},
    "wMode": {"fixed", "minus"}, "hMode": {"fixed", "minus"},
    "alignH": {"left", "centre", "right"}, "alignV": {"top", "centre", "bottom"},
}
INTS = {"x", "y", "w", "h", "trans", "sprite", "font", "lineHeight"}
BOOLS = {"hidden", "clickThrough", "filled", "tiling", "shadow"}
STRINGS = {"name", "parent", "type", "frame", "text", "opBase", "notes"}
MAX_OPS = 10


class DesignError(ValueError):
    pass


def normalise(design: dict) -> dict:
    """Validates a design and returns it in canonical form: ordered keys, defaults removed."""
    if not isinstance(design, dict):
        raise DesignError("a design is a JSON object")
    unknown = set(design) - {"format", "interface", "notes", "components"}
    if unknown:
        raise DesignError(f"unknown top-level keys: {sorted(unknown)}")
    if design.get("format") != FORMAT:
        raise DesignError(f"format must be {FORMAT}")
    interface = design.get("interface")
    if not isinstance(interface, str) or not NAME.fullmatch(interface):
        raise DesignError("interface must be a [a-z0-9_] name")
    components = design.get("components")
    if not isinstance(components, list) or not components:
        raise DesignError("components must be a non-empty list")
    notes = design.get("notes", "")
    if not isinstance(notes, str):
        raise DesignError("notes must be a string")

    seen: set[str] = set()
    out = []
    for index, component in enumerate(components):
        out.append(_component(component, index, seen))
    return {"format": FORMAT, "interface": interface, "notes": notes, "components": out}


def _component(c: object, index: int, seen: set[str]) -> dict:
    if not isinstance(c, dict):
        raise DesignError(f"component {index} is not an object")
    name = c.get("name")
    where = f"component `{name}` (index {index})"
    if not isinstance(name, str) or not NAME.fullmatch(name):
        raise DesignError(f"{where}: name must be [a-z0-9_]")
    if name in seen:
        raise DesignError(f"{where}: name is used twice")
    unknown = set(c) - set(KEY_ORDER)
    if unknown:
        raise DesignError(f"{where}: unknown keys {sorted(unknown)}")
    kind = c.get("type")
    if kind not in TYPE_FIELDS:
        raise DesignError(f"{where}: unknown type `{kind}`")

    parent = c.get("parent")
    if index == 0 and parent is not None:
        raise DesignError(f"{where}: the root must have no parent")
    if index > 0 and parent not in seen:
        raise DesignError(f"{where}: parent `{parent}` must be declared above it")
    seen.add(name)

    for key, value in c.items():
        if key in INTS and (not isinstance(value, int) or isinstance(value, bool)):
            raise DesignError(f"{where}: {key} must be an integer")
        if key in BOOLS and not isinstance(value, bool):
            raise DesignError(f"{where}: {key} must be true or false")
        if key in STRINGS and not isinstance(value, str):
            raise DesignError(f"{where}: {key} must be a string")
        if key in WORDS and value not in WORDS[key]:
            raise DesignError(f"{where}: {key} must be one of {sorted(WORDS[key])}")

    result = {}
    for key in KEY_ORDER:
        if key not in c:
            continue
        value = c[key]
        if key == "colour":
            value = value.lower() if isinstance(value, str) else value
            if not isinstance(value, str) or not HEX.fullmatch(value):
                raise DesignError(f"{where}: colour must be six hex digits")
        if key == "ops":
            value = _ops(value, where)
        if key == "hover":
            value = _hover(value, kind, where)
        if key in DEFAULTS and value == DEFAULTS[key]:
            continue
        if key not in TYPE_FIELDS[kind]:
            raise DesignError(f"{where}: a {kind} has no `{key}`")
        result[key] = value

    if "trans" in result and not 0 <= result["trans"] <= 255:
        raise DesignError(f"{where}: trans must be 0..255")
    if kind == "text" and result.get("font", -1) < 0:
        raise DesignError(f"{where}: text needs a font")
    if kind == "graphic" and result.get("sprite", -1) < 0:
        raise DesignError(f"{where}: a graphic needs a sprite")
    return result


def _ops(value: object, where: str) -> list[str]:
    if not isinstance(value, list) or not all(isinstance(op, str) for op in value):
        raise DesignError(f"{where}: ops must be a list of strings")
    ops = list(value)
    while ops and not ops[-1]:
        ops.pop()  # the editor always shows five boxes; trailing blanks enable nothing
    if len(ops) > MAX_OPS:
        raise DesignError(f"{where}: at most {MAX_OPS} ops")
    return ops


def _hover(value: object, kind: str, where: str) -> dict | None:
    if value is None or value == {}:
        return None
    if not isinstance(value, dict) or len(value) != 1:
        raise DesignError(f"{where}: hover sets exactly one of colour, trans or sprite")
    (key, amount), = value.items()
    if key not in HOVER_BY_TYPE[kind]:
        raise DesignError(f"{where}: a {kind} cannot hover its {key}")
    if key == "colour":
        if not isinstance(amount, str) or not HEX.fullmatch(amount.lower()):
            raise DesignError(f"{where}: hover colour must be six hex digits")
        return {"colour": amount.lower()}
    if not isinstance(amount, int) or isinstance(amount, bool):
        raise DesignError(f"{where}: hover {key} must be an integer")
    if key == "trans" and not 0 <= amount <= 255:
        raise DesignError(f"{where}: hover trans must be 0..255")
    return {key: amount}


def format_design(design: dict) -> str:
    """One component per line: a moved button is a one-line diff."""
    head = [
        f'  "format": {design["format"]}',
        f'  "interface": {json.dumps(design["interface"])}',
        f'  "notes": {json.dumps(design["notes"], ensure_ascii=False)}',
    ]
    body = ",\n".join(
        "    " + json.dumps(c, ensure_ascii=False) for c in design["components"]
    )
    return "{\n" + ",\n".join(head) + ',\n  "components": [\n' + body + "\n  ]\n}\n"


def interface_ids(symbols: Path = LOCAL_SYMBOLS) -> dict[str, int]:
    ids = {}
    path = symbols / "interface.sym"
    if path.exists():
        for line in path.read_text().splitlines():
            parts = line.split("\t")
            if len(parts) == 2 and parts[0].strip().isdigit():
                ids[parts[1].strip()] = int(parts[0])
    return ids


def sync_symbols(design: dict, component_sym: Path, source: str) -> bool:
    """Rewrites the interface's block of `component_sym` from the design's order.

    Only lines starting `<interface>:` change. The block keeps its place in the file, so any
    comment above it survives. Returns whether the file changed.
    """
    interface = design["interface"]
    prefix = f"{interface}:"
    block = [
        f"{prefix}{index}\t{prefix}{c['name']}\n" for index, c in enumerate(design["components"])
    ]
    lines = component_sym.read_text().splitlines(keepends=True) if component_sym.exists() else []
    if lines and not lines[-1].endswith("\n"):
        lines[-1] += "\n"
    positions = [i for i, line in enumerate(lines) if line.startswith(prefix)]
    if positions:
        kept = [line for line in lines if not line.startswith(prefix)]
        first = positions[0]
        updated = kept[:first] + block + kept[first:]
    else:
        header = f"# {interface}: written by tools/interface-designer from {source}; do not edit.\n"
        updated = lines + ["\n", header] + block
    if updated == lines:
        return False
    component_sym.write_text("".join(updated))
    return True


def resolve_design(relative: str, repo: Path = REPO) -> Path:
    """Maps a repo-relative path to a design file, refusing anything outside the two roots."""
    path = (repo / relative).resolve()
    if not path.name.endswith(SUFFIX):
        raise DesignError(f"not a design file: {relative}")
    drafts = (repo / "tools" / "interface-designer" / "designs").resolve()
    content = (repo / "content").resolve()
    if path.parent == drafts:
        return path
    if path.is_relative_to(content):
        parts = path.relative_to(content).parts
        for i in range(len(parts) - 2):
            if parts[i:i + 3] == ("src", "main", "resources"):
                return path
    raise DesignError(f"designs live in designs/ or a content module's resources: {relative}")


def is_implemented(path: Path, interface: str, ids: dict[str, int], repo: Path = REPO) -> bool:
    """A design is implemented once it is packed from a content module under an allocated id."""
    return path.is_relative_to((repo / "content").resolve()) and interface in ids


def referenced_names(interface: str, content: Path = CONTENT) -> list[str]:
    """Component names Kotlin code refers to, so the editor can warn before a rename breaks boot."""
    pattern = re.compile(re.escape(f'"{interface}:') + r"([a-z0-9_]+)\"")
    names: set[str] = set()
    for source in content.rglob("*.kt"):
        if "build" in source.parts:
            continue
        text = source.read_text(errors="ignore")
        if f'"{interface}:' in text:
            names.update(pattern.findall(text))
    return sorted(names)


def list_designs() -> list[dict]:
    ids = interface_ids()
    found = sorted(DRAFTS.glob(f"*{SUFFIX}")) + sorted(
        p for p in CONTENT.rglob(f"*{SUFFIX}") if "build" not in p.relative_to(CONTENT).parts
    )
    designs = []
    for path in found:
        interface = path.name[: -len(SUFFIX)]
        designs.append({
            "path": str(path.relative_to(REPO)),
            "interface": interface,
            "implemented": is_implemented(path.resolve(), interface, ids),
        })
    return designs


def save(relative: str, design: dict) -> dict:
    path = resolve_design(relative)
    if not path.exists() and path.parent != DRAFTS.resolve():
        raise DesignError("new designs start as drafts in designs/")
    canonical = normalise(design)
    if path.name != canonical["interface"] + SUFFIX:
        raise DesignError(f"file name must be {canonical['interface']}{SUFFIX}")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(format_design(canonical))
    synced = False
    if is_implemented(path, canonical["interface"], interface_ids()):
        synced = sync_symbols(canonical, LOCAL_SYMBOLS / "component.sym", relative)
    return {"ok": True, "symbolsSynced": synced}


class Handler(SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=str(HERE), **kwargs)

    def log_message(self, fmt, *args):
        if "/assets/" not in self.path:
            super().log_message(fmt, *args)

    def end_headers(self):
        if not self.path.startswith("/assets/"):
            self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def do_GET(self):
        url = urlparse(self.path)
        if url.path == "/":
            self.send_response(HTTPStatus.FOUND)
            self.send_header("Location", "/app/index.html")
            self.end_headers()
        elif url.path.startswith(("/app/", "/assets/")) and ".." not in url.path:
            super().do_GET()
        elif url.path == "/api/designs":
            self._json({"designs": list_designs(), "interfaces": interface_ids()})
        elif url.path == "/api/design":
            self._guard(lambda: self._read(parse_qs(url.query)["path"][0]))
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_PUT(self):
        url = urlparse(self.path)
        if url.path != "/api/design":
            self.send_error(HTTPStatus.NOT_FOUND)
            return
        length = int(self.headers.get("Content-Length", 0))
        body = self.rfile.read(length)
        self._guard(lambda: self._json(save(parse_qs(url.query)["path"][0], json.loads(body))))

    def _read(self, relative: str) -> None:
        path = resolve_design(relative)
        design = json.loads(path.read_text())
        interface = design.get("interface", "")
        self._json({
            "path": relative,
            "design": design,
            "implemented": is_implemented(path, interface, interface_ids()),
            "referenced": referenced_names(interface) if interface in interface_ids() else [],
        })

    def _guard(self, action) -> None:
        try:
            action()
        except (DesignError, KeyError, json.JSONDecodeError, FileNotFoundError) as e:
            self._json({"ok": False, "error": str(e)}, HTTPStatus.BAD_REQUEST)

    def _json(self, payload: dict, status: HTTPStatus = HTTPStatus.OK) -> None:
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawTextHelpFormatter)
    parser.add_argument("--port", type=int, default=8098)
    parser.add_argument("--import", dest="import_file", metavar="FILE")
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--normalise", metavar="FILE")
    parser.add_argument("--sync-symbols", metavar="FILE")
    args = parser.parse_args()

    try:
        if args.import_file:
            design = normalise(json.loads(Path(args.import_file).read_text()))
            target = DRAFTS / f"{design['interface']}{SUFFIX}"
            if target.exists() and not args.force:
                print(f"{target.relative_to(REPO)} exists; pass --force to overwrite it", file=sys.stderr)
                return 1
            DRAFTS.mkdir(exist_ok=True)
            target.write_text(format_design(design))
            print(f"wrote {target.relative_to(REPO)} ({len(design['components'])} components)")
            return 0
        if args.normalise:
            print(format_design(normalise(json.loads(Path(args.normalise).read_text()))), end="")
            return 0
        if args.sync_symbols:
            path = Path(args.sync_symbols).resolve()
            design = normalise(json.loads(path.read_text()))
            if design["interface"] not in interface_ids():
                print(f"`{design['interface']}` has no id in .local/interface.sym yet", file=sys.stderr)
                return 1
            changed = sync_symbols(design, LOCAL_SYMBOLS / "component.sym", str(path.relative_to(REPO)))
            print("component.sym updated" if changed else "component.sym already in step")
            return 0
    except DesignError as e:
        print(f"invalid design: {e}", file=sys.stderr)
        return 1

    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print(f"Interface designer: http://127.0.0.1:{args.port}/")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    return 0


if __name__ == "__main__":
    sys.exit(main())
