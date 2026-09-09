#!/usr/bin/env python3
"""Pull the Herblore experience numbers off the OSRS Wiki.

The rev 233 cache already knows most of Herblore. `[proc,skill_guide_data_herblore]`
(clientscript 7886) carries, for every potion the vanilla skill guide lists, its level
requirement and its ingredient line, and `HerbloreDump` in `api/cache/src/integration`
prints them. What the cache does *not* carry is experience: no potion obj has a
`skill_xp` param -- the dump says 0 of 510 -- so the one thing that has to come from
outside is the xp per action.

This fetches it, rather than transcribing it by hand, and re-runs whenever the numbers
need checking. It reads two tables off the wiki's `Herblore` page:

    Cleaning herbs & unfinished potions   grimy -> clean level and xp, and the unf level
    Potions                               level, xp and ingredients per potion

The level columns overlap with what the cache already told us, and that overlap is the
point: `--check` joins the two sources on the potion name and fails on any level that
disagrees, so a wrong row cannot pass quietly. Names are only ever used to *join* the two
tables; the objs themselves are resolved in Kotlin by `find()` against
`.data/symbols/obj.sym`, which is the authority.

Usage:
    python3 tools/herblore/generate.py            # print the parsed tables as JSON
    python3 tools/herblore/generate.py --check    # also diff levels against a dump file
"""

import argparse
import json
import re
import sys
import urllib.error
import urllib.parse
import urllib.request

API = "https://oldschool.runescape.wiki/api.php"
USER_AGENT = "onyx-rsps herblore table build"


def wikitext(page):
    params = {
        "action": "parse",
        "page": page,
        "prop": "wikitext",
        "format": "json",
        "formatversion": "2",
    }
    url = f"{API}?{urllib.parse.urlencode(params)}"
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            payload = json.load(response)
    except (urllib.error.URLError, TimeoutError, json.JSONDecodeError) as error:
        raise SystemExit(f"could not fetch '{page}': {error}")
    if "error" in payload:
        raise SystemExit(f"wiki error for '{page}': {payload['error']}")
    return payload["parse"]["wikitext"]


def section(text, heading):
    """The wikitext of one section, up to the next heading of any depth."""
    heads = [
        (match.start(), match.group(2))
        for match in re.finditer(r"^(={2,4})\s*(.+?)\s*\1\s*$", text, re.M)
    ]
    for index, (start, name) in enumerate(heads):
        if name.strip().lower() == heading.lower():
            end = heads[index + 1][0] if index + 1 < len(heads) else len(text)
            return text[start:end]
    raise SystemExit(f"section '{heading}' not found; the wiki page has been restructured")


def rows(table_text):
    """Split one wikitable into rows of already-stripped cells."""
    body = table_text[table_text.index("{|") : table_text.rindex("|}")]
    out = []
    for chunk in body.split("\n|-")[1:]:
        cells = []
        for line in chunk.splitlines():
            line = line.strip()
            if line.startswith("|") and not line.startswith("|}"):
                cells.append(line[1:].strip())
        if cells:
            out.append(cells)
    return out


def plink(cell):
    """`{{plinkt|Grimy guam leaf}}` -> `Grimy guam leaf`; plain text passes through."""
    match = re.search(r"\{\{plink[a-z]*\|([^|}]+)", cell)
    if match:
        return match.group(1).strip()
    return re.sub(r"\[\[([^|\]]+\|)?([^\]]+)\]\]", r"\2", cell).strip()


def number(cell):
    match = re.search(r"-?\d+(?:\.\d+)?", cell.replace(",", ""))
    if not match:
        return None
    return float(match.group(0))


def parse_herbs(text):
    parsed = []
    for cells in rows(section(text, "Cleaning herbs & unfinished potions")):
        if len(cells) < 6:
            continue
        clean_level, grimy, clean_xp, clean, unf_level, unf = cells[:6]
        parsed.append(
            {
                "grimy": plink(grimy),
                "clean": plink(clean),
                "unf": plink(unf),
                "clean_level": int(number(clean_level)),
                "clean_xp": number(clean_xp),
                "unf_level": int(number(unf_level)),
            }
        )
    if len(parsed) < 14:
        raise SystemExit(f"only parsed {len(parsed)} herbs; the wiki table has changed shape")
    return parsed


def parse_potions(text):
    parsed = []
    for cells in rows(section(text, "Potions")):
        if len(cells) < 4:
            continue
        level, potion, xp = cells[0], cells[1], cells[2]
        if number(level) is None or number(xp) is None:
            continue
        parsed.append(
            {
                "potion": plink(potion),
                "level": int(number(level)),
                "xp": number(xp),
                "base": plink(cells[3]) if len(cells) > 3 else None,
                "ingredients": [plink(cell) for cell in cells[4:-1] if plink(cell)],
            }
        )
    if len(parsed) < 40:
        raise SystemExit(f"only parsed {len(parsed)} potions; the wiki table has changed shape")
    return parsed


def check(potions, dump_path):
    """Diff wiki levels against the levels `HerbloreDump` read out of the cache.

    The dump lines look like `   3  obj=221    'Attack potion'  '...'`; only the level and
    the name are used here. A disagreement means one of the two sources is wrong about a
    row, which is exactly the thing worth failing on.
    """
    cache = {}
    for line in open(dump_path, encoding="utf-8"):
        match = re.match(r"\s*(\d+)\s+obj=\d+\s+[\"'](.+?)[\"']", line)
        if match:
            cache.setdefault(match.group(2).strip().lower(), int(match.group(1)))
    mismatches, matched = [], 0
    for row in potions:
        level = cache.get(row["potion"].strip().lower())
        if level is None:
            continue
        matched += 1
        if level != row["level"]:
            mismatches.append(f"{row['potion']}: wiki={row['level']} cache={level}")
    print(f"cross-checked {matched} potions against the cache", file=sys.stderr)
    for line in mismatches:
        print(f"LEVEL MISMATCH {line}", file=sys.stderr)
    return not mismatches


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", metavar="DUMP", help="HerbloreDump stdout to diff levels against")
    args = parser.parse_args()

    text = wikitext("Herblore")
    herbs = parse_herbs(text)
    potions = parse_potions(text)

    if args.check and not check(potions, args.check):
        raise SystemExit("wiki and cache disagree on at least one level")

    json.dump({"herbs": herbs, "potions": potions}, sys.stdout, indent=2)
    sys.stdout.write("\n")


if __name__ == "__main__":
    main()
