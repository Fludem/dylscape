#!/usr/bin/env python3
"""Generate npc drop tables for `content/custom/drop-tables` from the OSRS Wiki.

The wiki exposes its structured data through the Bucket extension
(`api.php?action=bucket`, a Lua-flavoured query language). Three tables matter:

    dropsline        one row per drop, carrying a `drop_json` blob
    infobox_monster  every monster version, with its real npc ids
    infobox_item     every item version, with its real obj ids

Monsters and items are bridged to rev 233 through *ids*, never through display
names: the wiki publishes the same ids Jagex uses, and `.data/symbols/*.sym`
maps those ids back to the internal names the loader wants. Anything added to
OSRS after rev 233 has no id in the sym files and drops out on its own, so this
never has to know what "current" content is.

Note that dropsline rarities are *effective per-kill* rates with the shared
herb/gem/rare drop tables already expanded into them -- a chaos druid lists
`Grimy guam leaf 1/11.1` rather than "46/128 to reach the herb table". So the
shared tables are not modelled here; adding them would double-count.

Usage:  python3 tools/drop-tables/generate.py [--dry-run]
"""

import argparse
import collections
import json
import math
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SYMBOLS = REPO / ".data" / "symbols"
OUT_DIR = (
    REPO
    / "content/custom/drop-tables/src/main/resources"
    / "org/rsmod/content/custom/droptables/data"
)
REPORT = Path(__file__).resolve().parent / "skipped.md"

API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "rsmod-drop-table-import/1.0 (github.com/rsmod/rsmod fork; local dev)"
PAGE_SIZE = 5000

# Only combat deaths are modelled. `reward`, `thieving`, `hunter` and friends are
# earned through other content and would be wrong to hang off an npc's death.
DROP_TYPE = "combat"

# Nothing here implements treasure trails, so clue items are rewritten rather than
# imported. A monster's clue scroll drop keeps its wiki rate but becomes the key of
# the same tier, which the Edgeville clue chest (`content/custom/clue-chest`) opens
# into that tier's reward casket. Every other trail item is skipped: the step keys
# and reward caskets the wiki lists as "Always" only drop while on that clue step,
# and nothing drops a master key.
CLUE_KEYS = {
    "Clue scroll (beginner)": "trail_key_beginner",
    "Clue scroll (easy)": "trail_key_easy",
    "Clue scroll (medium)": "trail_clue_medium_riddle001_key",
    "Clue scroll (hard)": "trail_key_hard",
    "Clue scroll (elite)": "trail_elite_riddle_key32",
}
TRAIL_PREFIX = "trail_"

# Smallest denominator that represents every rate in a table exactly wins, so a
# vanilla-shaped monster still reads as `/128`. The ceiling is bounded by
# `DropTableRoller`: it scales weights by up to `boost.all.den * boost.rare.den`
# (50), and `denominator * scale` has to stay inside `Int`.
DENOMINATOR_LADDER = [128, 256, 512, 1024, 4096, 16384, 100_000, 10_000_000]
MAX_DENOMINATOR = DENOMINATOR_LADDER[-1]

# A weight is "exact" if scaling the rate by the denominator lands on an integer.
EXACT_EPSILON = 1e-6
# Anything rounded by more than this is called out in the report.
ROUNDING_REPORT_THRESHOLD = 0.01


# --------------------------------------------------------------------------
# wiki
# --------------------------------------------------------------------------


def _get(params, attempts=4):
    url = API + "?" + urllib.parse.urlencode(params)
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return json.load(response)
        except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
            if attempt == attempts - 1:
                raise
            wait = 2**attempt
            print(f"  retrying in {wait}s ({error})", file=sys.stderr)
            time.sleep(wait)


def bucket(table, fields, where=None):
    """Every row of `table`, paged. `limit` is capped at 5000 server-side.

    `where` is an optional `(field, value)` equality filter.
    """
    selects = ",".join(f"'{field}'" for field in fields)
    condition = f".where('{where[0]}','{where[1]}')" if where else ""
    rows = []
    offset = 0
    while True:
        query = (
            f"bucket('{table}').select({selects}){condition}"
            f".limit({PAGE_SIZE}).offset({offset}).run()"
        )
        payload = _get({"action": "bucket", "format": "json", "query": query})
        if payload.get("error"):
            raise SystemExit(f"bucket query failed for '{table}': {payload['error']}")
        page = payload.get("bucket") or []
        rows += page
        offset += PAGE_SIZE
        if len(page) < PAGE_SIZE:
            return rows


# --------------------------------------------------------------------------
# symbols
# --------------------------------------------------------------------------


def read_symbols(path):
    """id -> internal name. Only these ids exist in our cache."""
    symbols = {}
    for line in path.read_text().splitlines():
        if line.startswith("#") or "\t" not in line:
            continue
        num, name = line.split("\t", 1)
        symbols[int(num)] = name
    return symbols


# --------------------------------------------------------------------------
# parsing a dropsline row
# --------------------------------------------------------------------------


def parse_rarity(raw):
    """`1/128`, `5/128`, `1/2,730.67` or `Always`. None if the wiki hedged."""
    text = str(raw).replace(",", "").strip()
    if text.lower() == "always":
        return "always"
    match = re.fullmatch(r"([\d.]+)/([\d.]+)", text)
    if not match:
        return None
    numerator, denominator = float(match.group(1)), float(match.group(2))
    if denominator <= 0 or numerator <= 0:
        return None
    rate = numerator / denominator
    return rate if 0 < rate <= 1 else None


def parse_quantity(row):
    """(low, high). The wiki gives ints when it can and prose when it cannot."""
    low, high = row.get("Quantity Low"), row.get("Quantity High")
    if isinstance(low, int) and isinstance(high, int) and 0 < low <= high:
        return low, high
    text = str(row.get("Drop Quantity") or "").replace(",", "")
    match = re.search(r"(\d+)\s*[-\u2013]\s*(\d+)", text)
    if match:
        return int(match.group(1)), int(match.group(2))
    match = re.search(r"(\d+)", text)
    if match:
        value = int(match.group(1))
        return (value, value) if value > 0 else (1, 1)
    return 1, 1


def is_noted(row):
    return "noted" in str(row.get("Drop Quantity") or "").lower()


# --------------------------------------------------------------------------
# conversion
# --------------------------------------------------------------------------


def pick_denominator(rates):
    """Smallest ladder value representing every rate exactly, else the ceiling."""
    for denominator in DENOMINATOR_LADDER:
        scaled = [rate * denominator for rate in rates]
        if any(abs(value - round(value)) > EXACT_EPSILON * max(1.0, value) for value in scaled):
            continue
        if all(round(value) >= 1 for value in scaled) and sum(round(v) for v in scaled) <= denominator:
            return denominator, True
    return MAX_DENOMINATOR, False


def build_table(rows, report):
    """Split a monster's rows into always / one weighted roll / tertiary.

    The wiki publishes effective per-kill rates, so the honest reading of a row
    is "this drops with probability p". Rows are taken rarest-last into a single
    weighted roll while the running total still fits inside one roll; whatever
    will not fit becomes a tertiary, which is an independent roll at the same
    probability. Sorting descending means what spills is always the rarest tail
    -- pets, clues and rare-drop-table uniques -- which is what vanilla rolls
    separately anyway.
    """
    always = [row for row in rows if row["rate"] == "always"]
    weighted = sorted(
        (row for row in rows if row["rate"] != "always"),
        key=lambda row: -row["rate"],
    )

    main, tertiary, total = [], [], 0.0
    for row in weighted:
        if total + row["rate"] <= 1.0 + 1e-12:
            main.append(row)
            total += row["rate"]
        else:
            tertiary.append(row)

    out_of, exact = 0, True
    if main:
        out_of, exact = pick_denominator([row["rate"] for row in main])
        assigned = 0
        for row in main:
            weight = max(1, round(row["rate"] * out_of))
            row["weight"] = weight
            assigned += weight
            error = abs(weight / out_of - row["rate"]) / row["rate"]
            if error > ROUNDING_REPORT_THRESHOLD:
                report.rounded.append((row["source"], row["obj"], row["rarity"], error))
        if assigned > out_of:
            # Rounding up every slot can just overshoot; give the excess back to
            # the rarest slots, which are the ones that were rounded up hardest.
            for row in sorted(main, key=lambda r: r["weight"]):
                while assigned > out_of and row["weight"] > 1:
                    row["weight"] -= 1
                    assigned -= 1
                if assigned <= out_of:
                    break
        if assigned > out_of:
            return None, f"weights sum to {assigned} over {out_of}"

    return {
        "always": always,
        "out_of": out_of,
        "main": main,
        "tertiary": tertiary,
        "exact": exact,
    }, None


class Report:
    def __init__(self):
        self.unresolved_monsters = []
        self.unresolved_items = collections.Counter()
        self.unparsed_rarity = collections.Counter()
        self.rounded = []
        self.duplicate_npcs = []
        self.failed_tables = []
        self.multi_roll = collections.Counter()
        self.clue_keys = collections.Counter()
        self.trail_skipped = collections.Counter()


# --------------------------------------------------------------------------
# toml
# --------------------------------------------------------------------------


def toml_string(value):
    escaped = str(value).replace("\\", "\\\\").replace('"', '\\"')
    escaped = escaped.replace("\n", "\\n").replace("\t", "\\t")
    return f'"{escaped}"'


def inline_drop(row, weight_key=None):
    parts = []
    if weight_key:
        parts.append(f"{weight_key} = {row[weight_key]}")
    parts.append(f"obj = {toml_string(row['obj'])}")
    if row["low"] != 1 or row["high"] != 1:
        parts.append(f"count = {row['low']}")
        if row["high"] != row["low"]:
            parts.append(f"count_max = {row['high']}")
    if row["noted"]:
        parts.append("noted = true")
    return "{ " + ", ".join(parts) + " }"


def render_table(entry):
    lines = ["[[table]]"]
    # Casket tables (`caskets.py`) belong to no npc and are looked up by `source`.
    if entry["npc"]:
        npcs = ", ".join(toml_string(name) for name in entry["npc"])
        lines.append(f"npc = [{npcs}]")
    lines.append(f"source = {toml_string(entry['source'])}")

    table = entry["table"]
    if table["always"]:
        drops = ", ".join(inline_drop(row) for row in table["always"])
        lines.append(f"always = [{drops}]")

    if table["main"]:
        lines.append(f"out_of = {table['out_of']}")
        lines.append("drop = [")
        for row in table["main"]:
            lines.append(f"  {inline_drop(row, 'weight')},")
        remainder = table["out_of"] - sum(row["weight"] for row in table["main"])
        if remainder > 0:
            lines.append(f"  {{ weight = {remainder} }},")
        lines.append("]")

    if table["tertiary"]:
        lines.append("tertiary = [")
        for row in table["tertiary"]:
            one_in = min(MAX_DENOMINATOR, max(1, round(1 / row["rate"])))
            parts = [f"one_in = {one_in}", f"obj = {toml_string(row['obj'])}"]
            if row["low"] != 1 or row["high"] != 1:
                parts.append(f"count = {row['low']}")
                if row["high"] != row["low"]:
                    parts.append(f"count_max = {row['high']}")
            if row["noted"]:
                parts.append("noted = true")
            lines.append("  { " + ", ".join(parts) + " },")
        lines.append("]")

    return "\n".join(lines)


def obj_resolver(items, obj_symbols):
    """A `dropsline` row -> internal obj name (or None), from `infobox_item` rows.

    Three indexes, because a display name is not unique: "Coins" is the name of
    item 995 but also of the Shilo Village and Mage Training Arena tokens, whose
    ids sort first. A dropsline row carries the item's *page* (stashed as
    `_page`), so try that, then the name as it appears on its own page, and only
    then any page at all.
    """
    page_item_ids, canonical_item_ids, named_item_ids = {}, {}, collections.defaultdict(list)
    for row in items:
        ids = [int(i) for i in (row.get("item_id") or []) if str(i).isdigit()]
        if not ids:
            continue
        page, name = row.get("page_name"), row.get("item_name")
        if page:
            page_item_ids.setdefault(page, []).extend(ids)
        if name:
            named_item_ids[name].extend(ids)
            if name == page:
                canonical_item_ids.setdefault(name, []).extend(ids)

    def resolve_obj(row):
        page, name = row.get("_page"), row.get("Dropped item")
        lookups = (
            (page, page_item_ids),
            (name, canonical_item_ids),
            (name, named_item_ids),
        )
        for key, index in lookups:
            if not key:
                continue
            for obj_id in index.get(str(key)) or ():
                if obj_id in obj_symbols:
                    return obj_symbols[obj_id]
        return None

    return resolve_obj


def shard_name(source):
    first = source.strip()[:1].lower()
    return f"monsters_{first}.toml" if first.isalpha() else "monsters_misc.toml"


# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dry-run", action="store_true", help="report only; do not write any files"
    )
    args = parser.parse_args()

    npc_symbols = read_symbols(SYMBOLS / "npc.sym")
    obj_symbols = read_symbols(SYMBOLS / "obj.sym")
    print(f"symbols: {len(npc_symbols)} npcs, {len(obj_symbols)} objs")

    # Three of the keys are ours, so they only exist in the `.local` symbols.
    known_objs = set(obj_symbols.values()) | set(read_symbols(SYMBOLS / ".local" / "obj.sym").values())
    missing_keys = sorted(key for key in CLUE_KEYS.values() if key not in known_objs)
    if missing_keys:
        raise SystemExit(f"CLUE_KEYS names objs with no symbol: {missing_keys}")

    print("fetching dropsline ...")
    drops = bucket("dropsline", ["item_name", "drop_json"])
    print(f"  {len(drops)} rows")
    print("fetching infobox_monster ...")
    monsters = bucket("infobox_monster", ["page_name", "name", "id", "version_anchor"])
    print(f"  {len(monsters)} monster versions")
    print("fetching infobox_item ...")
    items = bucket("infobox_item", ["page_name", "item_name", "item_id"])
    print(f"  {len(items)} item versions")

    # Page keys are kept apart from name keys and always tried first. A monster's
    # `name` is only its display name, so "Agrith Naar (Nightmare Zone)" is also
    # named "Agrith Naar" -- folding the two together hands the real Agrith Naar's
    # table to the Nightmare Zone dream npcs, which drop nothing in vanilla.
    page_monster_ids = collections.defaultdict(set)
    named_monster_ids = collections.defaultdict(set)
    for row in monsters:
        ids = {int(i) for i in (row.get("id") or []) if str(i).isdigit()}
        if not ids:
            continue
        page, anchor, name = row.get("page_name"), row.get("version_anchor"), row.get("name")
        for index, key in ((page_monster_ids, page), (named_monster_ids, name)):
            if not key:
                continue
            index[key] |= ids
            if anchor:
                index[f"{key}#{anchor}"] |= ids

    resolve_obj = obj_resolver(items, obj_symbols)
    report = Report()

    by_monster = collections.defaultdict(list)
    for row in drops:
        blob = json.loads(row["drop_json"])
        if blob.get("Drop type") != DROP_TYPE:
            continue
        blob["_page"] = row.get("item_name")
        by_monster[str(blob.get("Dropped from"))].append(blob)

    print(f"\n{len(by_monster)} monsters with combat drops")

    entries = []
    claimed = {}
    for source in sorted(by_monster):
        wiki_rows = by_monster[source]
        base = source.split("#")[0]
        ids = (
            page_monster_ids.get(source)
            or page_monster_ids.get(base)
            or named_monster_ids.get(source)
            or named_monster_ids.get(base)
            or set()
        )
        names = sorted(
            (npc_symbols[i] for i in ids if i in npc_symbols),
            key=lambda name: name,
        )
        if not names:
            report.unresolved_monsters.append((source, len(wiki_rows)))
            continue

        rows = []
        for wiki_row in wiki_rows:
            item = str(wiki_row.get("Dropped item"))
            if item in CLUE_KEYS:
                obj = CLUE_KEYS[item]
                report.clue_keys[item] += 1
            else:
                obj = resolve_obj(wiki_row)
                if obj is None:
                    report.unresolved_items[item] += 1
                    continue
                if obj.startswith(TRAIL_PREFIX):
                    report.trail_skipped[f"{item} ({obj})"] += 1
                    continue
            rate = parse_rarity(wiki_row.get("Rarity"))
            if rate is None:
                report.unparsed_rarity[str(wiki_row.get("Rarity"))] += 1
                continue
            rolls = wiki_row.get("Rolls")
            if isinstance(rolls, int) and rolls > 1:
                report.multi_roll[source] += 1
            low, high = parse_quantity(wiki_row)
            rows.append(
                {
                    "obj": obj,
                    "rate": rate,
                    "rarity": str(wiki_row.get("Rarity")),
                    "low": low,
                    "high": high,
                    "noted": is_noted(wiki_row),
                    "source": source,
                }
            )

        if not rows:
            continue

        table, failure = build_table(rows, report)
        if table is None:
            report.failed_tables.append((source, failure))
            continue

        unclaimed = []
        for name in names:
            if name in claimed:
                report.duplicate_npcs.append((name, claimed[name], source))
                continue
            claimed[name] = source
            unclaimed.append(name)
        if not unclaimed:
            continue

        entries.append({"npc": unclaimed, "source": source, "table": table})

    print(f"{len(entries)} tables covering {len(claimed)} npcs")

    shards = collections.defaultdict(list)
    for entry in entries:
        shards[shard_name(entry["source"])].append(entry)

    if args.dry_run:
        print("\n--dry-run: no files written")
    else:
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        for stale in OUT_DIR.glob("monsters_*.toml"):
            stale.unlink()
        header = (
            "# Generated by tools/drop-tables/generate.py from the OSRS Wiki.\n"
            "# Do not edit by hand; hand-written tables belong in LumbridgeDropTables.\n"
        )
        for name in sorted(shards):
            body = "\n\n".join(render_table(entry) for entry in shards[name])
            (OUT_DIR / name).write_text(header + "\n" + body + "\n")
        index = "\n".join(f'  {toml_string(name)},' for name in sorted(shards))
        (OUT_DIR / "index.toml").write_text(header + "\nshard = [\n" + index + "\n]\n")
        print(f"\nwrote {len(shards)} shards + index.toml to {OUT_DIR.relative_to(REPO)}")

    write_report(report, entries, claimed, args.dry_run)


def write_report(report, entries, claimed, dry_run):
    total_always = sum(len(e["table"]["always"]) for e in entries)
    total_main = sum(len(e["table"]["main"]) for e in entries)
    total_tertiary = sum(len(e["table"]["tertiary"]) for e in entries)
    exact = sum(1 for e in entries if e["table"]["exact"] and e["table"]["main"])
    approximated = sum(1 for e in entries if not e["table"]["exact"] and e["table"]["main"])

    lines = [
        "# Drop table import: what did not make it",
        "",
        "Generated by `tools/drop-tables/generate.py`. Re-run it to refresh this file.",
        "",
        "## Summary",
        "",
        f"- {len(entries)} tables covering {len(claimed)} npc ids",
        f"- {total_always} always drops, {total_main} weighted slots, {total_tertiary} tertiary",
        f"- {exact} tables use an exact denominator, {approximated} are rounded to "
        f"1/{MAX_DENOMINATOR}",
        "",
        "## Monsters with no rev-233 npc id",
        "",
        "Post-rev-233 content, or npcs the wiki knows under an id our cache does not have.",
        "",
    ]
    for source, count in sorted(report.unresolved_monsters, key=lambda x: -x[1]):
        lines.append(f"- {source} ({count} drops)")

    lines += [
        "",
        "## Clue scrolls imported as keys",
        "",
        "Kept at the wiki rate as the key of the same tier; see `CLUE_KEYS`.",
        "",
    ]
    for name, count in report.clue_keys.most_common():
        lines.append(f"- {name} -> `{CLUE_KEYS[name]}` ({count} rows)")

    lines += [
        "",
        "## Trail items skipped",
        "",
        "Clue-step keys, reward caskets and master clues. Nothing implements trails.",
        "",
    ]
    for name, count in report.trail_skipped.most_common():
        lines.append(f"- {name} ({count} rows)")

    lines += ["", "## Items with no rev-233 obj id", ""]
    for name, count in report.unresolved_items.most_common():
        lines.append(f"- {name} ({count} rows)")

    lines += [
        "",
        "## Rarities the wiki does not state numerically",
        "",
        "These rows are dropped entirely; there is no defensible rate to invent.",
        "",
    ]
    for value, count in report.unparsed_rarity.most_common():
        lines.append(f"- `{value}` ({count} rows)")

    if report.rounded:
        lines += [
            "",
            f"## Slots rounded by more than {ROUNDING_REPORT_THRESHOLD:.0%}",
            "",
        ]
        for source, obj, rarity, error in sorted(report.rounded, key=lambda x: -x[3])[:100]:
            lines.append(f"- {source}: {obj} at `{rarity}` is off by {error:.1%}")

    if report.duplicate_npcs:
        lines += ["", "## Npcs claimed by more than one wiki page", "", "First page wins.", ""]
        for name, kept, dropped in report.duplicate_npcs:
            lines.append(f"- `{name}`: kept {kept}, skipped {dropped}")

    if report.failed_tables:
        lines += ["", "## Tables that could not be built", ""]
        for source, reason in report.failed_tables:
            lines.append(f"- {source}: {reason}")

    if report.multi_roll:
        lines += [
            "",
            "## Monsters with multi-roll drops",
            "",
            "The wiki's rate is per roll; these are imported as a single roll, so their",
            "listed drops are slightly rarer here than in vanilla.",
            "",
        ]
        for source, count in report.multi_roll.most_common():
            lines.append(f"- {source} ({count} rows)")

    text = "\n".join(lines) + "\n"
    if dry_run:
        print("\n" + "\n".join(lines[:20]))
    else:
        REPORT.write_text(text)
        print(f"wrote {REPORT.relative_to(REPO)}")


if __name__ == "__main__":
    main()
