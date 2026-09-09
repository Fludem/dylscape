# Drop table import

`generate.py` builds every npc drop table in `content/custom/drop-tables` from the OSRS Wiki.

```bash
python3 tools/drop-tables/generate.py            # fetch, convert, write
python3 tools/drop-tables/generate.py --dry-run  # report only
```

It writes `content/custom/drop-tables/src/main/resources/org/rsmod/content/custom/droptables/data/`
(an `index.toml` plus one shard per initial letter) and refreshes `skipped.md` beside this file.
Nothing else in the repo is touched, and no cache pack is needed — the tables are pure server-side
data read at boot by `DropTableResourceLoader`.

## Where the data comes from

The wiki runs the Bucket extension, so its drop tables are queryable rather than scrapeable:
`api.php?action=bucket&query=bucket('dropsline').select(...).limit(5000).offset(0).run()`. Three
buckets are read — `dropsline` (one row per drop), `infobox_monster` and `infobox_item` (which
publish the **real npc and obj ids**).

Those ids are the whole trick. Monsters and items are bridged to rev 233 by id, never by display
name: `.data/symbols/npc.sym` and `obj.sym` map an id back to the internal name the loader wants,
and anything added to OSRS after rev 233 simply has no id in those files and falls out on its own.
That is why this needs no fuzzy name matching, unlike `tools/npc-spawns/generate.py`.

Two traps in the wiki's own data, both already handled, both worth knowing if you extend this:

- A page's `name` is only its *display* name. "Agrith Naar (Nightmare Zone)" is also named
  "Agrith Naar", so keying on `name` hands the real boss's table to the dream npcs. Page keys are
  kept separate from name keys and always tried first.
- An item name is not unique either: "Coins" is item 995, but also the Shilo Village and Mage
  Training Arena tokens, whose ids sort first. A drop row carries the item's *page*, so that is
  tried before the name.

## How a wiki row becomes a table

Wiki rarities are **effective per-kill** rates with the shared herb, gem and rare drop tables
already expanded into them — a chaos druid lists `Grimy guam leaf 1/11.1`, not "46/128 to reach the
herb table". So the shared tables are deliberately not modelled; adding them would double-count.

Per monster:

1. `Always` rows become `always` entries.
2. The rest are sorted rarest-last and taken into a single weighted roll while the running total
   still fits inside one roll. 860 of 1,259 monsters sum to under 1.0 on their own, so for most of
   them nothing spills.
3. What will not fit becomes a `tertiary`, an independent roll at the same probability. Sorting
   descending means the spill is always the rarest tail — pets, clues, rare-drop-table uniques —
   which vanilla rolls separately anyway.
4. The denominator is the smallest of `128, 256, 512, 1024, 4096, 16384, 100000, 10000000` that
   represents every rate in the table exactly, so a vanilla-shaped monster still reads as `/128`.
   The ceiling is not arbitrary: `DropTableRoller` scales weights by up to
   `boost.all.den * boost.rare.den` (50), and `denominator * scale` has to stay inside `Int`.
5. The gap up to the denominator is padded with a single empty slot, which is what
   `WeightedTableBuilder` requires.

Noted drops are emitted as `noted = true` and resolved through the cache's own `certlink`, rather
than the generator guessing a noted id.

## What it does not import

`skipped.md` is regenerated on every run and lists all of it: monsters with no rev-233 npc id,
items with no rev-233 obj id, rows whose rarity the wiki states as prose ("Common", "Varies"), any
slot rounded by more than 1%, and npcs claimed by more than one wiki page. Read it rather than
assuming a gap is a bug.

Two more filters live on the Kotlin side, because they need the cache:

- Only `combat` drops are imported. Thieving, hunter and reward drops belong to other content.
- `DropTableResourceLoader` skips npcs with no `Attack` op. A wiki page lists every id a monster
  goes by, including its cutscene, reset and sub-entity forms.

## After regenerating

```bash
./gradlew :content:custom:drop-tables:integration --rerun-tasks
```

`GeneratedDropTablesTest` is the gate. The loader collects bad rows instead of throwing — one stale
name must not take a live server down — so that test asserting the error list is empty is the only
thing standing between a bad import and a quiet hole in the game.

Hand-written tables in `LumbridgeDropTables` always win: `DropTableScript` registers them first and
skips any npc they already claim. Tune a monster there rather than editing the generated toml,
which the next run overwrites.
