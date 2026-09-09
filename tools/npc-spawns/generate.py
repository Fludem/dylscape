#!/usr/bin/env python3
"""Generate RSMod `npcs.toml` spawn files from GregHib/void's world spawn data.

Void is a 2011-era RuneScape server whose `data/**/*.npc-spawns.toml` files carry ~20k
world spawns as `{ id = "<void name>", x = .., y = .., level = .. }`. Void's names are its
own, so they are bridged to rev-233 npc ids by *display name*: the void name is shortened a
token at a time until it matches a name in the cache, then the best id is picked and turned
back into an internal name via `.data/symbols/npc.sym` (which is what the toml wants).

Spawns are dropped when the name does not bridge (almost always RS-only content such as
Dungeoneering), when the mapsquare is absent from our cache, when the mapsquare is already
covered by an existing spawn builder, or when an existing builder already places the same npc
nearby -- "the same npc" meaning the same id within `DEDUPE_RADIUS` tiles, or a member of the
same `DEDUPE_FAMILIES` role within `DEDUPE_FAMILY_RADIUS`.

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

# Npcs that fill the same role under different ids, and the radius the dedupe below uses for them.
#
# Void's general-store staff bridge onto `fortis_shop_general_1` and `generalassistant1`, but
# `city-shops` staffs every store with the numbered `generalshopkeeperN` / `generalassistantN` pair
# its shop invs are keyed to. Those are *different ids*, so the `near-existing` check never fired
# and six towns ended up with two shopkeepers and two assistants -- one pair that trades and one
# pair that is scenery. Al Kharid's void assistant landed on the working one's exact tile.
#
# Radius 6 rather than `DEDUPE_RADIUS`: shop staff are spread across a room, not stacked. Varrock's
# void assistant stands four tiles from the working keeper and survives at 3. Nothing legitimate is
# caught -- the general stores `city-shops` does not staff (Port Khazard, the combat training camp,
# Zanaris' fairy shop) have no existing spawn within any radius.
DEDUPE_FAMILY_RADIUS = 6
DEDUPE_FAMILIES = {
    "general_store_staff": re.compile(
        r"^(fortis_shop_general_\d+|generalshopkeeper\d*|generalassistant\d*)$"
    )
}


def dedupe_key(name, npc_id):
    """What the `near-existing` check matches on: a family token, or the raw id."""
    for family, pattern in DEDUPE_FAMILIES.items():
        if name is not None and pattern.match(name):
            return family
    return npc_id


def dedupe_radius(key):
    return DEDUPE_FAMILY_RADIUS if key in DEDUPE_FAMILIES else DEDUPE_RADIUS

# Ops that mean "stands at a post and serves whoever walks up". Everything in this revision's
# cache wanders five tiles by default, which is how bankers end up strolling around the lobby
# instead of standing behind the counter, so anything wearing one of these is pinned.
COUNTER_OPS = {
    "assignment",
    "bank",
    "claim-tokens",
    "collect",
    "exchange",
    "glider",
    "heal",
    "pay-fare",
    "quick-travel",
    "repairs",
    "rewards",
    "trade",
    "travel",
}

# A mapsquare this densely authored already (upstream's Lumbridge set) is left alone: void
# would place its own crowd of men, goblins and guards a tile or two off ours and the town
# would read as doubled. Sparse builders (thieving's ladder, the city shops, the toll gate)
# are merged into instead, with `DEDUPE_RADIUS` keeping the same npc from landing twice.
DENSE_SQUARE = 30

# Tutorial Island and the empty sea around it. Void has a 2011 tutorial with a different
# script, and the island here is authored to match live OSRS beat for beat.
RESERVED_SQUARES = {(x, z) for x in (47, 48, 49) for z in (47, 48)}

# Void's world is a 2011 RuneScape map, and a few buildings were rebuilt between that and this
# revision's cache. Varrock West Bank is the one you can see: RS lines its counters along the
# side walls of the room, OSRS runs a single counter down the middle, so void's twelve bankers
# land in the public lobby and out on the street rather than behind a booth. Every void spawn of
# `npc` inside `box` (inclusive, absolute coords) is dropped and `place` is spawned instead.
REBUILT_AREAS = [
    {
        "group": "area_misthalin",
        "why": "varrock_west_bank",
        "npc": "banker1",
        "level": 0,
        "box": (3179, 3432, 3192, 3448),
        # One tile east of each `fai_varrock_bankbooth` at x=3186, which is the only column of
        # the staff side left free: x=3187 carries a `bankwall_corner_end` on every odd row.
        "place": [(3187, 3436), (3187, 3438), (3187, 3440), (3187, 3442), (3187, 3444)],
    }
]

# Void groups that describe content this server has no business standing up permanently:
# seasonal holiday events and random events that should be summoned, not parked in a field,
# and three minigames RuneScape has and OSRS does not.
EXCLUDED_GROUPS = {
    "activity_event",
    # Barrows is owned by `content/custom/barrows`, which spawns its own. Void's data is unusable
    # here: this cache puts the crypts on level 3 of mapsquare 55_151 and leaves level 0 - where
    # void's 2011 map has the tunnels, and where all 34 of its crypt monsters sit - without a
    # single walkable tile. It also parks the Strange Old Man on Ahrim's dig spot.
    "minigame_barrows_brothers",
    "minigame_fist_of_guthix",
    "minigame_soul_wars",
    "minigame_vinesweeper",
    "skill_dungeoneering",
}


def read_npc_types(path):
    """id -> (display name, ops), from `DumpNpcTypes.java`."""
    types = {}
    for line in path.read_text().splitlines():
        parts = line.split("\t")
        if len(parts) < 6:
            continue
        display = parts[1]
        if display in ("null", ""):
            continue
        types[int(parts[0])] = (display, [op for op in parts[5].split("|") if op])
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
        for npc_id, (display, _) in npc_types.items():
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
    """Spawns already authored in the repo, as (name, level, x, y).

    Every `[[spawn]]` toml under `content/` counts, not just the ones called `npcs.toml`.
    Matching on that one filename used to be the rule, and it missed `city-shops`, whose spawn
    files are named after their city (`alkharid.toml`, `falador.toml`, ...) -- so all 31 of its
    shopkeepers fell through the dedupe below and got a second copy generated on top of them.
    The gem trader is the one you can see, because he is the only shop npc standing outdoors.

    The generator's own output is skipped, or a rerun would dedupe against the previous run.
    """
    names = {name: num for num, name in symbols.items()}
    existing = []
    for toml in (repo / "content").rglob("*.toml"):
        if "/build/" in str(toml) or "/worldspawns/" in str(toml):
            continue
        if "[[spawn]]" not in toml.read_text():
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
                key = dedupe_key(name, names.get(name))
                existing.append((key, level, msx * 64 + lx, msz * 64 + lz))
                name = None
    return existing


WANDER_TEMPLATE = """package org.rsmod.content.custom.worldspawns.configs

import org.rsmod.api.type.editors.npc.NpcEditor
import org.rsmod.api.type.refs.npc.NpcReferences
import org.rsmod.game.type.npc.NpcType

/**
 * The npcs void keeps on their spawn tile, generated by `tools/npc-spawns/generate.py`.
 *
 * Out of the cache every npc wanders five tiles, so bankers drift out from behind the counter
 * and shopkeepers leave their shops -- the same problem the toll gate border guards had. The
 * {count} listed here are the npcs this server spawns that void pins at 0, or that carry a
 * counter-service op (`Bank`, `Trade`, `Collect` and the like) in this revision's cache.
 *
 * Npcs belonging to a module that already runs its own [NpcEditor] are left out, so no type is
 * edited from two places.
 */
internal object WorldSpawnNpcs : NpcReferences() {{
    val stationary: List<NpcType> =
        listOf(
{names}
            )
            .map {{ find(it) }}
}}

/** Applies [WorldSpawnNpcs.stationary]. A type edit, so the boot config sync picks it up. */
internal object WorldSpawnNpcEditor : NpcEditor() {{
    init {{
        WorldSpawnNpcs.stationary.forEach {{ edit(it) {{ wanderRange = 0 }} }}
    }}
}}
"""

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


WANDER_ENTRY_RE = re.compile(r"^\[\.([^\]]+)\]$")


def read_wander_ranges(path):
    """void npc name -> wander range, from void's `wander_ranges.tables.toml`."""
    ranges = {}
    current = None
    for line in path.read_text().splitlines():
        line = line.strip()
        match = WANDER_ENTRY_RE.match(line)
        if match:
            current = match.group(1)
        elif current and line.startswith("wander_range"):
            ranges[current] = int(line.split("=", 1)[1].strip())
            current = None
    return ranges


def editor_owned_names(repo):
    """Npc names belonging to modules that already run their own NpcEditor.

    Editing one type from two editors is exactly the conflict the content-group note warns
    about, so anything those modules name is left alone here.
    """
    def module_of(path):
        for parent in path.parents:
            if (parent / "build.gradle.kts").exists():
                return parent
        return None

    sources = [p for p in (repo / "content").rglob("*.kt") if "/build/" not in str(p)]
    modules = {
        module
        for module in (module_of(p) for p in sources if "NpcEditor()" in p.read_text())
        if module is not None
    }
    names = set()
    for module in modules:
        for path in module.rglob("*.kt"):
            if "/build/" in str(path):
                continue
            names.update(re.findall(r'find\("([a-z0-9_]+)"', path.read_text()))
    return names


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--void", required=True, type=pathlib.Path, help="void checkout")
    parser.add_argument("--npc-types", required=True, type=pathlib.Path)
    parser.add_argument("--mapsquares", required=True, type=pathlib.Path)
    parser.add_argument("--repo", required=True, type=pathlib.Path)
    parser.add_argument("--out", required=True, type=pathlib.Path)
    parser.add_argument("--builder", type=pathlib.Path, help="WorldNpcSpawns.kt to rewrite")
    parser.add_argument("--wander", type=pathlib.Path, help="WorldSpawnNpcs.kt to rewrite")
    args = parser.parse_args()

    symbols = read_symbols(args.repo / ".data/symbols/npc.sym")
    npc_types = read_npc_types(args.npc_types)
    bridge = Bridge(npc_types, symbols)
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
            if any(
                area["npc"] == name
                and area["level"] == level
                and area["box"][0] <= x <= area["box"][2]
                and area["box"][1] <= y <= area["box"][3]
                for area in REBUILT_AREAS
            ):
                stats["rebuilt-area"] += 1
                continue
            key = (name, level, x, y)
            if key in seen:
                stats["duplicate"] += 1
                continue
            seen.add(key)
            npc_id = ids[name]
            family = dedupe_key(name, npc_id)
            radius = dedupe_radius(family)
            if any(
                other_key == family
                and other_level == level
                and abs(other_x - x) <= radius
                and abs(other_y - y) <= radius
                for other_key, other_level, other_x, other_y in existing
            ):
                stats["near-existing"] += 1
                continue
            comment = "" if void_name == name else f"  # void: {void_name}"
            groups[group].append((name, coord_string(x, y, level), comment))
            stats["kept"] += 1

    for area in REBUILT_AREAS:
        for x, y in area["place"]:
            key = (area["npc"], area["level"], x, y)
            if key in seen:
                continue
            seen.add(key)
            coords = coord_string(x, y, area["level"])
            groups[area["group"]].append((area["npc"], coords, f'  # rebuilt: {area["why"]}'))
            stats["rebuilt-placed"] += 1

    args.out.mkdir(parents=True, exist_ok=True)
    for group, spawns in sorted(groups.items()):
        lines = [
            f"# Generated by tools/npc-spawns/generate.py from GregHib/void `{group}`.",
            "# Do not hand-edit: rerun the generator instead.",
            "",
        ]
        for name, coords, comment in spawns:
            lines.append("[[spawn]]")
            lines.append(f"npc = '{name}'{comment}")
            lines.append(f"coords = '{coords}'")
            lines.append("")
        (args.out / f"{group}.toml").write_text("\n".join(lines))

    if args.wander:
        ranges = read_wander_ranges(args.void / "data/entity/npc/wander_ranges.tables.toml")
        pinned = collections.defaultdict(list)
        for void_name, wander in ranges.items():
            name = bridge.resolve(void_name)
            if name is not None:
                pinned[name].append(wander)
        spawned = {name for spawns in groups.values() for name, _, _ in spawns}
        owned = editor_owned_names(args.repo)

        ids = {name: num for num, name in symbols.items()}

        def serves_a_counter(name):
            display_and_ops = npc_types.get(ids.get(name, -1))
            if display_and_ops is None:
                return False
            return any(op.lower() in COUNTER_OPS for op in display_and_ops[1])

        stationary = sorted(
            name
            for name in spawned
            if name not in owned
            and (set(pinned.get(name, [1])) == {0} or serves_a_counter(name))
        )
        listed = "\n".join(f'                "{name}",' for name in stationary)
        args.wander.write_text(
            WANDER_TEMPLATE.format(count=len(stationary), names=listed)
        )
        stats["wander-pinned"] = len(stationary)

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
