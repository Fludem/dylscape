#!/usr/bin/env python3
"""Generates the website's static guide data from the repo's own content data.

The guide pages are not hand-maintained. Everything they show is read out of the files the game
itself uses, so a page cannot quietly drift from what the server actually does:

- Drop tables come from the TOML shards in `content/custom/drop-tables`.
- Teleport destinations come from `TeleportTable.kt`.
- Farming growth times come from `FarmingCrops.kt` and `FarmingRates.kt`.
- The feature list comes from the directories under `content/`.

Output lands in `web/site/data/`, which is gitignored and rebuilt on every deploy.

    python3 web/build_data.py

Stdlib only, like the rest of `tools/`. Every extractor hard-fails when it finds nothing, so a
refactor upstream breaks the build here rather than silently publishing an empty page.
"""

from __future__ import annotations

import json
import pathlib
import re
import string
import sys
import tomllib

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent / "api"))

from onyxdb import REAL_STAT_COUNT, skill_label  # noqa: E402

ROOT = pathlib.Path(__file__).resolve().parent.parent
DROPS_DIR = (
    ROOT / "content/custom/drop-tables/src/main/resources"
    "/org/rsmod/content/custom/droptables/data"
)
TELEPORT_KT = (
    ROOT / "content/custom/teleports/src/main/kotlin"
    "/org/rsmod/content/custom/teleports/configs/TeleportTable.kt"
)
FARMING_DIR = (
    ROOT / "content/skills/farming/src/main/kotlin/org/rsmod/content/skills/farming/data"
)
CONTENT_DIR = ROOT / "content"
OUT = ROOT / "web/site/data"

# Obj ids in the tables are symbol names. The cache holds the real display names, but decoding it
# needs a booted server, so names are prettified mechanically and these are the cases where that
# reads badly enough to be worth spelling out.
OBJ_NAME_FIXES = {
    "coins": "Coins",
    "vial_water": "Vial of water",
    "vial_empty": "Vial",
    "bucket_empty": "Bucket",
    "jug_empty": "Jug",
    "pot_empty": "Pot",
    "bones": "Bones",
    "big_bones": "Big bones",
    "grimy_guam": "Grimy guam leaf",
    "nothing": "Nothing",
}
# Suffixes the symbol names carry that mean something to a reader.
OBJ_SUFFIXES = [
    ("_p_plus_plus", " (p++)"),
    ("_p_plus", " (p+)"),
    ("_p", " (p)"),
    ("_noted", " (noted)"),
]
# The rune symbol names are one word ("firerune"), which reads badly. Every rune in the tables is
# listed here, so the split is a lookup rather than a guess at where a word boundary falls.
RUNE_PREFIXES = [
    "air", "astral", "blank", "blood", "body", "chaos", "cosmic", "death", "dust", "earth",
    "fire", "lava", "law", "mind", "mist", "mud", "nature", "smoke", "soul", "steam", "water",
    "wrath",
]


def pretty_obj(name: str) -> str:
    if not name:
        return "Nothing"
    if name in OBJ_NAME_FIXES:
        return OBJ_NAME_FIXES[name]
    suffix = ""
    for raw, nice in OBJ_SUFFIXES:
        if name.endswith(raw):
            name, suffix = name[: -len(raw)], nice
            break
    for prefix in RUNE_PREFIXES:
        if name == prefix + "rune":
            return prefix.capitalize() + " rune" + suffix
    return name.replace("_", " ").capitalize() + suffix


def pretty_source(source: str) -> str:
    """The tables came from the wiki, and 101 of the 1,027 names still carry the wiki's section
    anchor - "Abyssal demon#Catacombs of Kourend" distinguishes one variant's table from another's.
    The distinction is worth keeping; the "#" is not."""
    if "#" not in source:
        return source
    name, _, variant = source.partition("#")
    return f"{name.strip()} ({variant.strip()})"


def build_drops() -> dict:
    """One shard per initial letter, mirroring how the game data is already split.

    The full set is ~1000 monsters and ~24000 drop rows; splitting it keeps the browser from
    downloading all of it to answer a search for one monster.
    """
    if not DROPS_DIR.is_dir():
        sys.exit(f"build_data: no drop tables at {DROPS_DIR}")

    shards: dict[str, list] = {}
    index: list[dict] = []

    for path in sorted(DROPS_DIR.glob("monsters_*.toml")):
        for table in tomllib.loads(path.read_text()).get("table", []):
            source = table.get("source")
            if not source:
                continue
            source = pretty_source(source)
            letter = source[0].lower()
            if letter not in string.ascii_lowercase:
                letter = "0"

            entry = {
                "source": source,
                "always": [render_drop(d) for d in table.get("always", [])],
                "drops": [render_drop(d, table.get("out_of")) for d in table.get("drop", [])],
                "tertiary": [render_drop(d) for d in table.get("tertiary", [])],
            }
            shards.setdefault(letter, []).append(entry)
            index.append({"source": source, "letter": letter})

    if not index:
        sys.exit("build_data: parsed zero drop tables")

    drops_dir = OUT / "drops"
    drops_dir.mkdir(parents=True, exist_ok=True)
    for letter, tables in shards.items():
        tables.sort(key=lambda t: t["source"].lower())
        write(drops_dir / f"{letter}.json", tables)

    index.sort(key=lambda e: e["source"].lower())
    write(OUT / "drops-index.json", {"count": len(index), "sources": index})
    return {"monsters": len(index), "shards": len(shards)}


def render_drop(drop: dict, out_of: int | None = None) -> dict:
    """Flattens one drop row into what the page displays: a name, a quantity and a chance."""
    obj = drop.get("obj", "nothing")
    low = drop.get("count", 1)
    high = drop.get("count_max", low)
    quantity = str(low) if low == high else f"{low}–{high}"
    if drop.get("noted"):
        quantity += " (noted)"

    if "one_in" in drop:
        rarity = 1 / drop["one_in"]
    elif out_of and "weight" in drop:
        rarity = drop["weight"] / out_of
    else:
        rarity = 1.0

    return {"name": pretty_obj(obj), "quantity": quantity, "chance": pretty_chance(rarity),
            "rarity": round(rarity, 10)}


def pretty_chance(rarity: float) -> str:
    """Drop rates read as "1/128" in this game, never as a raw weight out of a table size.

    The tables store weights against wildly different denominators (512 for one monster, 10 million
    for another), so "2187500/10,000,000" is technically the truth and useless to a player.
    """
    if rarity >= 1:
        return "Always"
    if rarity <= 0:
        return "Never"
    n = 1 / rarity
    return f"1/{n:.1f}" if n < 10 else f"1/{round(n):,}"


# `entry("lumbridge", "Lumbridge", CoordGrid(3221, 3218, 0))`
ENTRY_RE = re.compile(
    r'entry\(\s*"(?P<key>[^"]+)"\s*,\s*"(?P<label>[^"]+)"\s*,\s*'
    r"CoordGrid\((?P<x>\d+),\s*(?P<z>\d+),\s*(?P<level>\d+)\)"
)
GROUP_RE = re.compile(r"^\s{4}val (?P<group>\w+): List<TeleportDestination>", re.MULTILINE)


def build_teleports() -> dict:
    """Parsed rather than duplicated, so the site cannot list a teleport the game does not have.

    The Kotlin is machine-regular (`TeleportDestinationsTest` checks every row against the collision
    map, so the format is not free to drift much), but this is still a regex over source: if it ever
    matches nothing, fail loudly instead of shipping an empty page.
    """
    if not TELEPORT_KT.is_file():
        sys.exit(f"build_data: no teleport table at {TELEPORT_KT}")
    source = TELEPORT_KT.read_text()

    bounds = [(m.group("group"), m.start()) for m in GROUP_RE.finditer(source)]
    if not bounds:
        sys.exit("build_data: found no teleport groups in TeleportTable.kt")

    groups = []
    for i, (name, start) in enumerate(bounds):
        end = bounds[i + 1][1] if i + 1 < len(bounds) else len(source)
        entries = [
            {
                "key": m.group("key"),
                "label": m.group("label"),
                "x": int(m.group("x")),
                "z": int(m.group("z")),
                "level": int(m.group("level")),
            }
            for m in ENTRY_RE.finditer(source[start:end])
        ]
        if entries:
            groups.append({"group": name, "label": name.capitalize(), "entries": entries})

    total = sum(len(g["entries"]) for g in groups)
    if total == 0:
        sys.exit("build_data: parsed zero teleport destinations from TeleportTable.kt")

    write(OUT / "teleports.json", {"count": total, "groups": groups})
    return {"teleports": total, "groups": len(groups)}


# Crop rows are one Kotlin constructor call per crop, always with the fields in the same order.
CROP_RE = re.compile(
    r"cropName = \"(?P<crop>[a-z_0-9]+)\",\s*"
    r"kind = PatchKind\.(?P<kind>\w+),\s*"
    r"seed = \w+\.(?P<seed>\w+),\s*"
    r"produce = (?:\w+\.(?P<produce>\w+)|null),\s*"
    r"level = (?P<level>\d+),\s*"
    r"plantXp = (?P<plant_xp>[\d.]+),\s*"
    r"harvestXp = (?P<harvest_xp>[\d.]+),\s*"
    r"checkXp = (?P<check_xp>[\d.]+),\s*"
    r"cycleMinutes = (?P<cycle>\d+),\s*"
    r"model = HarvestModel\.(?P<model>\w+),\s*"
    r"regrowMinutes = (?P<regrow>\d+),\s*"
    r"baseLives = (?P<lives>\d+),"
)
STAGES_RE = re.compile(r"growthStages = (?P<stages>\d+),")
# A counted crop holds one item per harvest state, and the loc state says how many are left.
HARVEST_STATES_RE = re.compile(r"harvestStates = intArrayOf\((?P<states>[^)]*)\),")
SPEEDUP_RE = re.compile(r"GROWTH_SPEEDUP: Int = (\d+)")
WEED_RE = re.compile(r"WEED_CYCLE_MINUTES: Int = (\d+)")
RAKES_RE = re.compile(r"^\s{4}(?P<kind>\w+)\(intArrayOf\([^)]*\), (?P<rakes>\d+), ", re.MULTILINE)

# What a player calls each patch. The enum name is the code's word for it, not the game's.
PATCH_LABELS = {
    "Allotment": "Allotment",
    "Flower": "Flower",
    "Herb": "Herb",
    "Hops": "Hops",
    "Bush": "Bush",
    "FruitTree": "Fruit tree",
    "Tree": "Tree",
    "Hardwood": "Hardwood tree",
    "Cactus": "Cactus",
    "Calquat": "Calquat",
    "Mushroom": "Mushroom",
    "Belladonna": "Belladonna",
    "Seaweed": "Seaweed",
    "SpiritTree": "Spirit tree",
    "Celastrus": "Celastrus",
    "Redwood": "Redwood",
    "Anima": "Anima",
    "Crystal": "Crystal tree",
    "Hespori": "Hespori",
    "Grape": "Vine",
}
# The order patches are shown in: roughly how soon a player meets them.
PATCH_ORDER = [
    "Allotment", "Flower", "Herb", "Hops", "Bush", "Tree", "FruitTree", "Cactus", "Mushroom",
    "Belladonna", "Seaweed", "Grape", "Calquat", "Celastrus", "Crystal", "Hardwood", "SpiritTree",
    "Redwood", "Anima", "Hespori",
]


# Crop names are symbol names, and mechanical prettifying reads badly for these. Same idea as
# OBJ_NAME_FIXES: spell out only the cases where a reader would notice.
CROP_NAME_FIXES = {
    "anima_attas": "Attas",
    "anima_iasor": "Iasor",
    "anima_kronos": "Kronos",
    "crystal_tree_tree": "Crystal tree",
    "poisonivy_bush": "Poison ivy bush",
    "snapegrass": "Snape grass",
    "redwood": "Redwood tree",
}


def pretty_crop(name: str) -> str:
    """`herb_ranarr_weed` -> `Ranarr weed`, `apple_tree` -> `Apple tree`."""
    if name in CROP_NAME_FIXES:
        return CROP_NAME_FIXES[name]
    trimmed = name[len("herb_"):] if name.startswith("herb_") else name
    trimmed = trimmed.replace("_clickzone", "").replace("_patch", "")
    return trimmed.replace("_", " ").capitalize()


def human_minutes(minutes: float) -> str:
    """Growth times span three seconds' worth of precision to over half a day."""
    total = round(minutes * 60)
    if total < 60:
        return f"{total}s"
    if total < 3600:
        secs = total % 60
        return f"{total // 60}m" + (f" {secs}s" if secs else "")
    mins = (total % 3600) // 60
    return f"{total // 3600}h" + (f" {mins}m" if mins else "")


def build_farming() -> dict:
    """Growth times are a division, not a table: one speedup constant re-times every crop.

    Reading the crops and the constant separately, and doing the arithmetic here, is what keeps the
    page honest when `GROWTH_SPEEDUP` changes -- which is the one number on this server that is
    expected to change.
    """
    crops_kt = FARMING_DIR / "FarmingCrops.kt"
    rates_kt = FARMING_DIR / "FarmingRates.kt"
    kinds_kt = FARMING_DIR / "PatchKind.kt"
    for path in (crops_kt, rates_kt, kinds_kt):
        if not path.is_file():
            sys.exit(f"build_data: no farming data at {path}")

    rates = rates_kt.read_text()
    speedup = SPEEDUP_RE.search(rates)
    weed_cycle = WEED_RE.search(rates)
    if not speedup or not weed_cycle:
        sys.exit("build_data: could not read the growth rates from FarmingRates.kt")
    speedup, weed_cycle = int(speedup.group(1)), int(weed_cycle.group(1))

    rakes = {m.group("kind"): int(m.group("rakes")) for m in RAKES_RE.finditer(kinds_kt.read_text())}
    if not rakes:
        sys.exit("build_data: could not read the rake counts from PatchKind.kt")

    source = crops_kt.read_text()
    stages = [int(m.group("stages")) for m in STAGES_RE.finditer(source)]
    held = [
        len([p for p in m.group("states").split(",") if p.strip()])
        for m in HARVEST_STATES_RE.finditer(source)
    ]
    matches = list(CROP_RE.finditer(source))
    if not matches:
        sys.exit("build_data: parsed zero crops from FarmingCrops.kt")
    if not len(stages) == len(held) == len(matches):
        sys.exit(
            f"build_data: {len(matches)} crops but {len(stages)} growthStages and "
            f"{len(held)} harvestStates in FarmingCrops.kt"
        )

    patches: dict[str, list] = {}
    for crop, growth_stages, harvest_states in zip(matches, stages, held):
        kind = crop.group("kind")
        cycle = int(crop.group("cycle"))
        regrow = int(crop.group("regrow"))
        vanilla = cycle * growth_stages
        patches.setdefault(kind, []).append(
            {
                "name": pretty_crop(crop.group("crop")),
                "level": int(crop.group("level")),
                "plant_xp": float(crop.group("plant_xp")),
                "harvest_xp": float(crop.group("harvest_xp")),
                "check_xp": float(crop.group("check_xp")),
                "grow": human_minutes(vanilla / speedup),
                "grow_seconds": round(vanilla * 60 / speedup),
                "vanilla": human_minutes(vanilla),
                "regrow": human_minutes(regrow / speedup) if regrow else None,
                # What a player gets out of it, which each harvest model counts differently: a
                # lives crop is picked until its lives run out, a counted one holds a fixed number
                # of items, and a tree pays out once when it is checked.
                "model": crop.group("model"),
                "yield": {
                    "Lives": int(crop.group("lives")),
                    "Counted": harvest_states,
                    "Checked": 0,
                }[crop.group("model")],
                "produce": pretty_obj(crop.group("produce")) if crop.group("produce") else None,
            }
        )

    groups = []
    for kind in PATCH_ORDER + sorted(k for k in patches if k not in PATCH_ORDER):
        crops = patches.get(kind)
        if not crops:
            continue
        crops.sort(key=lambda c: (c["level"], c["name"]))
        groups.append(
            {
                "kind": kind,
                "label": PATCH_LABELS.get(kind, kind),
                "rakes": rakes.get(kind, 0),
                "crops": crops,
            }
        )

    payload = {
        "speedup": speedup,
        "weed_cycle_minutes": weed_cycle,
        # Every patch takes the same number of rakes, but read it rather than assume it.
        "weed_regrow": human_minutes(weed_cycle * max(rakes.values())),
        "count": len(matches),
        "groups": groups,
    }
    write(OUT / "farming.json", payload)
    return {"crops": len(matches), "patches": len(groups)}

 # Modules whose directory name does not say what a player would call the feature. Anything under
# content/ that is not listed here and is not a skill is reported by its directory name.
FEATURE_LABELS = {
    "account-mode": ("Account modes", "Standard, Ironman, Ultimate and Hardcore, chosen at first login."),
    "city-shops": ("City shops", "Stocked shops in the starting cities."),
    "drop-tables": ("Drop tables", "Wiki-accurate drops for over a thousand monsters."),
    "knight-waves": ("Knight waves", "The Camelot training-grounds wave fight."),
    "leagues": ("Leagues", "Relic interface groundwork."),
    "teleports": ("Teleports", "A menu of city, skilling and dungeon destinations."),
    "toll-gate": ("Toll gates", "The Al Kharid gate and friends."),
    "world-spawns": ("World spawns", "Ground objects and world-placed items."),
    "canoe": ("Canoes", "River travel along the Lumbridge-Edgeville route."),
    "bank": ("Banking", "Booths and chests open the bank."),
    "first-login": ("First-login setup", "Pick an account type and an experience rate."),
    "skill-guides": ("Skill guides", "The in-game skill guide interfaces."),
    "world-map": ("World map", "The in-game world map."),
    "special-attacks": ("Special attacks", "Weapon special attacks."),
    "consumables": ("Food and drink", "Eating, drinking and their timings."),
    "emotes": ("Emotes", "The emote tab."),
    "commands": ("Commands", "In-game administrative commands."),
}
# Directories that are plumbing rather than a feature anyone would read about on a website.
FEATURE_SKIP = {
    "generic-locs", "generic-npcs", "fade-overlay", "gameframe", "menu", "channel-tab",
    "combat-tab", "journal-tab", "logout-tab", "prayer-tab", "settings", "equipment",
    "levelup", "skill-multi", "mapclock", "login", "chatchannel", "special-weapons",
    "windmill", "city", "misc",
}


def trainable_skills() -> tuple[list[str], list[str]]:
    """Splits the game's 23 skills into trainable and not, by looking for an XP-granting call.

    Listing `content/skills/` is the obvious approach and it is wrong: the five combat stats have
    no module there because their experience is granted by `api/combat`'s PlayerAttackManager from
    damage dealt. Doing it that way told players Attack and Strength were unimplemented.

    A skill counts as trainable when some non-test source passes `stats.<name>` to `statAdvance`.
    That is the single funnel every skill's experience goes through, so it does not care where the
    code lives.
    """
    stat_names = [
        line.split("\t")[1].strip()
        for line in (ROOT / ".data/symbols/stat.sym").read_text().splitlines()
        if line.strip()
    ][:REAL_STAT_COUNT]

    advancing = []
    for source in list(ROOT.glob("api/**/*.kt")) + list(ROOT.glob("content/**/*.kt")) + list(
        ROOT.glob("engine/**/*.kt")
    ):
        path = str(source)
        if "/test/" in path or "/integration/" in path or path.endswith("Test.kt"):
            continue
        text = source.read_text(errors="ignore")
        if "statAdvance" in text:
            advancing.append(text)
    blob = "\n".join(advancing)

    trainable = [s for s in stat_names if f"stats.{s}" in blob]
    missing = [s for s in stat_names if f"stats.{s}" not in blob]
    if len(trainable) < 10:
        sys.exit(f"build_data: only found {len(trainable)} trainable skills; the scan is broken")
    return trainable, missing


def build_features() -> dict:
    """Reads what is implemented off the content tree rather than from a hand-kept list."""
    if not CONTENT_DIR.is_dir():
        sys.exit(f"build_data: no content directory at {CONTENT_DIR}")

    skills, missing_skills = trainable_skills()

    features = []
    for group in sorted(CONTENT_DIR.iterdir()):
        if not group.is_dir() or group.name == "skills":
            continue
        for module in sorted(group.iterdir()):
            if not module.is_dir() or module.name in FEATURE_SKIP:
                continue
            if not (module / "build.gradle.kts").is_file():
                continue
            label, blurb = FEATURE_LABELS.get(
                module.name, (module.name.replace("-", " ").capitalize(), "")
            )
            features.append({"name": label, "blurb": blurb, "module": module.name})

    payload = {
        "skills": [{"key": s, "label": skill_label(s)} for s in skills],
        "missing_skills": [{"key": s, "label": skill_label(s)} for s in missing_skills],
        "features": features,
        # From engine/game/.../player/AccountMode.kt and XpRateTier.kt. The id is the multiplier.
        "account_modes": [
            {"id": 0, "label": "Standard", "blurb": "Trade, drop and bank freely."},
            {"id": 1, "label": "Ironman", "blurb": "No trading, no picking up other players' loot."},
            {"id": 2, "label": "Ultimate Ironman", "blurb": "Ironman, and no bank."},
            {"id": 3, "label": "Hardcore Ironman", "blurb": "Ironman until you die; death demotes you to Ironman."},
        ],
        "xp_tiers": [
            {"id": 10, "label": "10x", "blurb": "The slowest rate, and the largest drop-rate bonus."},
            {"id": 16, "label": "16x", "blurb": "The middle rate."},
            {"id": 30, "label": "30x", "blurb": "The fastest rate, and the smallest drop-rate bonus."},
        ],
    }
    write(OUT / "features.json", payload)
    return {"skills": len(skills), "features": len(features)}


def write(path: pathlib.Path, payload) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, separators=(",", ":")))


def main() -> int:
    OUT.mkdir(parents=True, exist_ok=True)
    stats = {}
    stats.update(build_drops())
    stats.update(build_teleports())
    stats.update(build_farming())
    stats.update(build_features())
    size = sum(f.stat().st_size for f in OUT.rglob("*.json"))
    print(f"build_data: {stats}, {size / 1024:.0f} KiB in {OUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
