#!/usr/bin/env python3
"""Generate RSMod `npcs.toml` spawn files from GregHib/void's world spawn data.

Void is a 2011-era RuneScape server whose `data/**/*.npc-spawns.toml` files carry ~20k
world spawns as `{ id = "<void name>", x = .., y = .., level = .. }`. Void's names are its
own, so they are bridged to rev-233 npc ids by *display name*: the void name is shortened a
token at a time until it matches a name in the cache, then the best id is picked and turned
back into an internal name via `.data/symbols/npc.sym` (which is what the toml wants).

Spawns are dropped when the name does not bridge (almost always RS-only content such as
Dungeoneering), when the mapsquare is absent from our cache, when the mapsquare is already
covered by an existing spawn builder, or when an existing builder already places the same
npc within `DEDUPE_RADIUS` tiles.

Inputs are produced by `DumpNpcTypes.java` / `DumpMapSquares.java` in this directory; see
README.md for the full run.
"""

import argparse
import collections
import pathlib
import re
import sys

SPAWN_RE = re.compile(r'\{\s*id\s*=\s*"([^"]+)"([^}]*)\}')
FIELD_RE = re.compile(r'(\w+)\s*=\s*("?)([^,"}]+)\2')
DEDUPE_RADIUS = 3

# A mapsquare this densely authored already (upstream's Lumbridge set) is left alone: void
# would place its own crowd of men, goblins and guards a tile or two off ours and the town
# would read as doubled. Sparse builders (thieving's ladder, the city shops, the toll gate)
# are merged into instead, with `DEDUPE_RADIUS` keeping the same npc from landing twice.
DENSE_SQUARE = 30

# Tutorial Island and the empty sea around it. Void has a 2011 tutorial with a different
# script, and the island here is authored to match live OSRS beat for beat.
RESERVED_SQUARES = {(x, z) for x in (47, 48, 49) for z in (47, 48)}

# Void groups that describe content this server has no business standing up permanently:
# seasonal holiday events and random events that should be summoned, not parked in a field,
# and three minigames RuneScape has and OSRS does not.
EXCLUDED_GROUPS = {
    "activity_event",
    "minigame_fist_of_guthix",
    "minigame_soul_wars",
    "minigame_vinesweeper",
    "skill_dungeoneering",
}


def read_npc_types(path):
    """id -> display name, for every npc in the rev-233 cache."""
    types = {}
    for line in path.read_text().splitlines():
        parts = line.split("\t")
        if len(parts) < 2:
            continue
        name = parts[1]
        if name in ("null", ""):
            continue
        types[int(parts[0])] = name
    return types


def read_symbols(path):
    """id -> internal name. Only these ids can be named in a spawn toml."""
    symbols = {}
    for line in path.read_text().splitlines():
        if "\t" not in line:
            continue
        num, name = line.split("\t", 1)
        symbols[int(num)] = name
    return symbols


def normalise(display):
    return re.sub(r"[^a-z0-9 ]", "", display.lower()).strip()


def strip_digits(token):
    return re.sub(r"\d+$", "", token)


class Bridge:
    """Resolves a void npc name to a rev-233 internal name."""

    def __init__(self, npc_types, symbols):
        self.symbols = symbols
        self.by_display = collections.defaultdict(list)
        for npc_id, display in npc_types.items():
            if npc_id in symbols:
                self.by_display[normalise(display)].append(npc_id)
        for ids in self.by_display.values():
            ids.sort()
        self.cache = {}

    def resolve(self, void_name):
        if void_name in self.cache:
            return self.cache[void_name]
        result = self._resolve(void_name)
        self.cache[void_name] = result
        return result

    def _resolve(self, void_name):
        tokens = void_name.split("_")
        for count in range(len(tokens), 0, -1):
            candidates = self.by_display.get(" ".join(tokens[:count]))
            if candidates:
                return self.symbols[self._pick(void_name, candidates)]
        return None

    def _pick(self, void_name, candidates):
        """Prefer the id whose internal name shares the most words with the void name.

        Void disambiguates with suffixes the cache also uses (`banker_falador`,
        `crow_4`), so shared words separate `master_farmer_1` from an unrelated id
        with the same display name. Words the candidate has and the void name does not
        are subtracted, which is what keeps the 97 npcs called "Banker" from resolving
        to `misc_banker` or `werewolfbanker` instead of plain `banker1`. Ties go to the
        lowest id: reliably the classic-world variant rather than a later reskin.
        """
        wanted = {strip_digits(t) for t in void_name.split("_")} - {""}
        best, best_score = None, None
        for npc_id in candidates:
            words = {strip_digits(t) for t in self.symbols[npc_id].split("_")} - {""}
            score = len(wanted & words) - len(words - wanted)
            if best_score is None or score > best_score:
                best, best_score = npc_id, score
        return best


def parse_spawn_file(path):
    for match in SPAWN_RE.finditer(path.read_text()):
        fields = {
            m.group(1): m.group(3).strip() for m in FIELD_RE.finditer(match.group(2))
        }
        try:
            x, y = int(fields["x"]), int(fields["y"])
        except (KeyError, ValueError):
            continue
        yield match.group(1), x, y, int(fields.get("level", 0))


def coord_string(x, y, level):
    return f"{level}_{x // 64}_{y // 64}_{x % 64}_{y % 64}"


def read_existing(repo, symbols):
    """Spawns already authored in the repo, as (name, level, x, y)."""
    names = {name: num for num, name in symbols.items()}
    existing = []
    for toml in (repo / "content").rglob("npcs.toml"):
        if "/build/" in str(toml):
            continue
        name = None
        for line in toml.read_text().splitlines():
            line = line.strip()
            if line.startswith("npc"):
                name = line.split("=", 1)[1].strip().strip("'\"")
            elif line.startswith("coords") and name:
                level, msx, msz, lx, lz = (
                    int(part) for part in line.split("=", 1)[1].strip().strip("'\"").split("_")
                )
                existing.append((names.get(name), level, msx * 64 + lx, msz * 64 + lz))
                name = None
    return existing


BUILDER_TEMPLATE = """package org.rsmod.content.custom.worldspawns.map

import org.rsmod.api.type.builders.map.npc.MapNpcSpawnBuilder

/**
 * Every npc spawn outside the areas this server authors by hand, bridged from GregHib/void's
 * world data by `tools/npc-spawns/generate.py`.
 *
 * **This does not run at boot.** `onPackMapTask` is invoked only by the Gradle `packCache`
 * task, and only with the server stopped, so the tomls beside this file are inert until the
 * packer runs.
 *
 * The {count} tomls are generated. Fix a bad spawn in the generator, not here.
 */
object WorldNpcSpawns : MapNpcSpawnBuilder() {{
    override fun onPackMapTask() {{
{entries}
    }}
}}
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--void", required=True, type=pathlib.Path, help="void checkout")
    parser.add_argument("--npc-types", required=True, type=pathlib.Path)
    parser.add_argument("--mapsquares", required=True, type=pathlib.Path)
    parser.add_argument("--repo", required=True, type=pathlib.Path)
    parser.add_argument("--out", required=True, type=pathlib.Path)
    parser.add_argument("--builder", type=pathlib.Path, help="WorldNpcSpawns.kt to rewrite")
    args = parser.parse_args()

    symbols = read_symbols(args.repo / ".data/symbols/npc.sym")
    bridge = Bridge(read_npc_types(args.npc_types), symbols)
    in_cache = {
        tuple(int(part) for part in line.split("\t"))
        for line in args.mapsquares.read_text().splitlines()
        if line.strip()
    }

    existing = read_existing(args.repo, symbols)
    density = collections.Counter((x // 64, y // 64) for _, _, x, y in existing)
    covered = {square for square, count in density.items() if count >= DENSE_SQUARE}
    covered |= RESERVED_SQUARES
    ids = {name: num for num, name in symbols.items()}

    groups = collections.defaultdict(list)
    stats = collections.Counter()
    unresolved = collections.Counter()
    seen = set()

    root = args.void / "data"
    for path in sorted(root.rglob("*.npc-spawns.toml")):
        relative = path.relative_to(root)
        group = "_".join(relative.parts[:2]).replace("'", "")
        if group in EXCLUDED_GROUPS:
            stats["excluded-group-files"] += 1
            continue
        for void_name, x, y, level in parse_spawn_file(path):
            stats["read"] += 1
            name = bridge.resolve(void_name)
            if name is None:
                stats["unresolved"] += 1
                unresolved[void_name] += 1
                continue
            square = (x // 64, y // 64)
            if square not in in_cache:
                stats["off-map"] += 1
                continue
            if square in covered:
                stats["reserved-square"] += 1
                continue
            key = (name, level, x, y)
            if key in seen:
                stats["duplicate"] += 1
                continue
            seen.add(key)
            npc_id = ids[name]
            if any(
                other_id == npc_id
                and other_level == level
                and abs(other_x - x) <= DEDUPE_RADIUS
                and abs(other_y - y) <= DEDUPE_RADIUS
                for other_id, other_level, other_x, other_y in existing
            ):
                stats["near-existing"] += 1
                continue
            groups[group].append((name, coord_string(x, y, level), void_name))
            stats["kept"] += 1

    args.out.mkdir(parents=True, exist_ok=True)
    for group, spawns in sorted(groups.items()):
        lines = [
            f"# Generated by tools/npc-spawns/generate.py from GregHib/void `{group}`.",
            "# Do not hand-edit: rerun the generator instead.",
            "",
        ]
        for name, coords, void_name in spawns:
            lines.append("[[spawn]]")
            comment = "" if void_name == name else f"  # void: {void_name}"
            lines.append(f"npc = '{name}'{comment}")
            lines.append(f"coords = '{coords}'")
            lines.append("")
        (args.out / f"{group}.toml").write_text("\n".join(lines))

    if args.builder:
        entries = "\n".join(
            f'        resourceFile<WorldNpcSpawns>("{group}.toml")' for group in sorted(groups)
        )
        args.builder.write_text(BUILDER_TEMPLATE.format(count=len(groups), entries=entries))

    for key, value in sorted(stats.items()):
        print(f"{key:24} {value}", file=sys.stderr)
    print(f"{'files':24} {len(groups)}", file=sys.stderr)
    print("\ntop unresolved names:", file=sys.stderr)
    for name, count in unresolved.most_common(15):
        print(f"  {count:5} {name}", file=sys.stderr)


if __name__ == "__main__":
    main()
